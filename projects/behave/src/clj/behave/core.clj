(ns behave.core
  (:gen-class)
  (:import [javax.swing JFrame SwingUtilities UIManager]
           [javax.imageio ImageIO])
  (:require [behave-routing.main               :refer [routes]]
            [behave.download-vms               :refer [export-from-vms export-images-from-vms]]
            [behave.init                       :refer [init-handler]]
            [behave.open                       :refer [open-handler]]
            [behave.save                       :refer [save-handler]]
            [behave.store                      :as store]
            [behave.sync                       :refer [sync-handler]]
            [behave.views                      :refer [render-page render-tests-page]]
            [bidi.bidi                         :refer [match-route]]
            [clojure.core.async                :refer [<! alts! chan go-loop put! timeout]]
            [clojure.edn                       :as edn]
            [clojure.java.browse               :refer [browse-url]]
            [clojure.java.io                   :as io]
            [clojure.stacktrace                :as st]
            [clojure.string                    :as str]
            [config.interface                  :refer [get-config load-config]]
            [file-utils.interface              :refer [os-path os-type app-data-dir app-logs-dir]]
            [jcef.interface                    :refer [create-cef-app! custom-request-handler]]
            [logging.interface                 :as l :refer [log log-str]]
            [ring.middleware.content-type      :refer [wrap-content-type]]
            [ring.middleware.multipart-params  :refer [wrap-multipart-params]]
            [ring.middleware.keyword-params    :refer [wrap-keyword-params]]
            [ring.middleware.reload            :refer [wrap-reload]]
            [ring.middleware.resource          :refer [wrap-resource]]
            [ring.util.codec                   :refer [url-decode]]
            [ring.util.response                :refer [not-found]]
            [server.interface                  :as server]
            [transport.interface               :refer [->clj mime->type]]))

;;; Constants

(def ^:private KILL-TIMEOUT-MS 5000) ;; 5 seconds

;;; State

(def ^:private kill-channel (atom nil))
(def ^:private cancel-channel (atom nil))
(def ^:private close-time (atom 0))

;;; Helpers

(defn- now-in-ms
  "Returns the current time since Jan. 1, 1970 in milliseconds."
  []
  (inst-ms (java.util.Date.)))

(defn- kill-app!
  "Use Runtime exit to kill entire JVM Process"
  []
  (.exit (Runtime/getRuntime) 0))

(defn- watch-kill-signal!
  "Creates a channel to listen on for a 'kill' signal. Once a message is put on the kill channel, waits 10 seconds to cancel the kill."
  []
  (let [kill-chan   (chan)
        cancel-chan (chan)]
    (go-loop []
      (<! kill-chan)
      (let [[_ ch] (alts! [cancel-chan (timeout KILL-TIMEOUT-MS)])]
        (if (not= ch cancel-chan)
          (kill-app!)
          (recur))))
    (reset! kill-channel kill-chan)
    (reset! cancel-channel cancel-chan)))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn init-config! []
  (load-config (io/resource "config.edn")))

(defn- standalone-db-location [org-name app-name]
  (str (io/file (app-data-dir org-name app-name) "db.sqlite")))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn init-db! [{:keys [config]}]
  (let [config (update-in config
                          [:store :path]
                          os-path)]
    (log-str [:DATASCRIPT-CONFIG config])
    (io/make-parents (get-in config [:store :path]))
    (store/connect! config)))

(defn vms-sync! []
  (let [{:keys [secret-token url]} (get-config :vms)]
    (pmap #(% secret-token url) [export-from-vms export-images-from-vms])))

(defn- vms-sync-handler [req]
  (log-str "Request Received:" (select-keys req [:uri :request-method :params]))
  (vms-sync!)
  {:status 200 :body "OK"})

;;; Logging

(defn- log-system-start! []
  (log-str [:SYSTEM])
  (doseq [[k v] (into {} (System/getProperties))]
    (log-str k ": " v))
  (log-str (get-config)))

(defn- start-logging! [log-opts]
  (let [log-opts (update log-opts :log-dir os-path)]
    (io/make-parents (:log-dir log-opts))
    (l/start-logging! log-opts)
    (log-system-start!)))

;;; Handlers

(defn- close-handler
  [{:keys [params]}]
  (log-str "Got /close request:" params)
  (if (= (get-config :server :mode) "prod")
    (let [{:keys [cancel]} params]
      (cond
        (nil? cancel)
        (do
          (reset! close-time (now-in-ms))
          (put! @kill-channel true))

        :else
        (put! @cancel-channel true))
      {:status 200 :body "OK"})
    {:status 404 :body "Not Found"}))

(defn- bad-uri?
  [uri]
  (str/includes? (str/lower-case uri) "php"))

(defn- routing-handler [{:keys [uri] :as request}]
  (let [next-handler (cond
                       (bad-uri? uri)                         (not-found "404 Not Found")
                       (str/starts-with? uri "/api/init")     #'init-handler
                       (str/starts-with? uri "/api/vms-sync") #'vms-sync-handler
                       (str/starts-with? uri "/api/sync")     #'sync-handler
                       (str/starts-with? uri "/api/save")     #'save-handler
                       (str/starts-with? uri "/api/open")     #'open-handler
                       (str/starts-with? uri "/api/test")     #'render-tests-page
                       (str/starts-with? uri "/api/close")    #'close-handler
                       (match-route routes uri)              (render-page (match-route routes uri))
                       :else                                 (not-found "404 Not Found"))]
    (next-handler request)))

(defn- wrap-query-params [handler]
  (fn [{:keys [params query-string] :or {params {}} :as req}]
    (if (empty? query-string)
      (handler req)
      (let [keyvals (-> (url-decode query-string)
                        (str/split #"&"))
            params  (reduce (fn [params keyval]
                              (let [[k v] (str/split keyval #"=")]
                               (assoc params (keyword k) (edn/read-string v))))
                           params keyvals)]
        (handler (assoc req :params params))))))

(defn- wrap-params [handler]
  (fn [{:keys [content-type body query-string] :as req}]
    (if-let [req-type (mime->type content-type)]
      (let [query-params (->clj query-string req-type)
            body-params  (->clj (slurp body) req-type)]
        (handler (update req :params merge query-params body-params)))
      (handler req))))

(defn- wrap-req-content-type+accept [handler]
  (fn [{:keys [headers] :as req}]
    (handler (assoc req
                    :content-type (get headers "content-type")
                    :accept       (get headers "accept")))))

(defn- wrap-exceptions [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (let [{:keys [data cause]} (Throwable->map e)
              status               (:status data)]
          (log-str "Error: " cause)
          (log-str (st/print-stack-trace e))
          {:status (or status 500) :body cause})))))

(defn- reloadable-clj-files
  []
  (let [m        (meta #'reloadable-clj-files)
        n-spaces (:ns m)
        ns-file  (-> n-spaces
                     (str/replace "-" "_")
                     (str/replace "." "/")
                     (->> (format "/%s.clj")))
        path     (:file m)]
    [(str/replace path #"/projects/.*" "/components")
     (str/replace path #"/projects/.*" "/bases")
     (str/replace path ns-file "")]))

(defn- optional-middleware [handler mw use?]
  (if use?
    (mw handler)
    handler))

(defn- wrap-figwheel [handler figwheel?]
  (fn [request]
    (handler (assoc request :figwheel? figwheel?))))

(defn- wrap-debug [handler]
  (fn [request]
    (println request)
    (handler request)))

(defn create-handler-stack [{:keys [reload? figwheel?]}]
  (-> routing-handler
      wrap-debug
      (wrap-figwheel figwheel?)
      wrap-params
      wrap-keyword-params
      wrap-query-params
      wrap-req-content-type+accept
      (wrap-resource "public" {:allow-symlinks? true})
      (wrap-content-type {:mime-types {"wasm" "application/wasm"}})
      wrap-multipart-params
      wrap-exceptions
      (optional-middleware #(wrap-reload % {:dirs (reloadable-clj-files)}) reload?)))

(defn create-api-handler-stack []
  (-> routing-handler
      wrap-debug
      wrap-params
      wrap-keyword-params
      wrap-query-params
      wrap-req-content-type+accept
      wrap-multipart-params
      wrap-exceptions))

;; This is for Figwheel
(def ^{:doc "Figwheel handler."}
  development-app
  (create-handler-stack {:figwheel? true :reload? true}))

(defn- set-properties! [props]
  (doseq [[k v] props]
    (System/setProperty k v)))

(defn- get-icons []
  (->> ["public/images/android-chrome-192x192.png"
        "public/images/android-touch-icon.png"
        "public/images/favicon-96x96.png"]
       (map #(ImageIO/read (io/resource %)))))

;; See: https://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
(defn- on-before-launch [^JFrame jframe title]
  (UIManager/setLookAndFeel
   (UIManager/getSystemLookAndFeelClassName))
  (SwingUtilities/updateComponentTreeUI jframe)

  (condp = (os-type)

    ;; See: https://docs.oracle.com/javase/8/docs/technotes/guides/swing/1.4/w2k_props.html
    :windows
    (do
      (.setUndecorated jframe true)
      (.setIcons jframe (get-icons))
      (set-properties! {}))

    :linux
    (set-properties! {"sun.java2d.xrender" "true"})

    ;; See: https://alvinalexander.com/java/make-java-application-look-feel-native-mac-osx/
    :mac
    (set-properties!
     {"apple.laf.useScreenMenuBar"                      "true"
      "apple.awt.application.appearance"                "system"
      "com.apple.mrj.application.apple.menu.about.name" title})))

(defn -main
  "Server start method."
  [& _args]
  (init-config!)
  (let [mode       (get-config :server :mode)
        http-port  (or (get-config :server :http-port) 8080)
        org-name   (get-config :site :org-name)
        app-name   (get-config :site :app-name)
        log-config (if (= "prod" mode)
                     (assoc (get-config :logging) :log-dir (app-logs-dir org-name app-name))
                     (get-config :logging))
        db-config  (if (= "prod" mode)
                     (assoc-in (get-config :database)
                               [:config :store :path]
                               (standalone-db-location org-name app-name))
                     (get-config :database))]

    (start-logging! log-config)
    (init-db! db-config)
    (cond
      (= "dev" mode)
      (do 
        #_(vms-sync!)
        (log-str "Starting server!")
        (server/start-server! {:handler (create-handler-stack {:reload? (= mode "dev") :figwheel? false})
                               :port    http-port}))

      (= "prod" mode)
      (create-cef-app!
       {:title       (get-config :site :title)
        :url         (str "http://localhost:" http-port)
        :fullscreen? true
        :menu        [{:title "File"
                       :items [{:label       "Open"
                                :mnemonic    "O"
                                :description "Opens a file"
                                :shortcut    "O"
                                :on-select   (fn [_]
                                               (println "Hello Open File!"))}
                               {:label       "Save"
                                :mnemonic    "S"
                                :description "Saves a file"
                                :shortcut    "S"
                                :on-select   (fn [_]
                                               (println "Hello Save File!"))}]}]
        :on-before-launch
        (fn [{:keys [client frame]}]
          (.addRequestHandler client (custom-request-handler {:protocol     "http"
                                                              :authority    "localhost:4242"
                                                              :resource-dir "public"
                                                              :ring-handler (create-api-handler-stack)}))
          (on-before-launch frame (get-config :site :title)))}))))

(comment
  (-main)
  (server/stop-server!))
