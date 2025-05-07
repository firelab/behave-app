(ns behave.core
  (:gen-class)
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
            [file-utils.interface              :refer [os-path]]
            [jcef.interface                    :refer [create-cef-app!]]
            [logging.interface                 :as l :refer [log-str]]
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
                       (bad-uri? uri)                     (not-found "404 Not Found")
                       (str/starts-with? uri "/init")     #'init-handler
                       (str/starts-with? uri "/vms-sync") #'vms-sync-handler
                       (str/starts-with? uri "/sync")     #'sync-handler
                       (str/starts-with? uri "/save")     #'save-handler
                       (str/starts-with? uri "/open")     #'open-handler
                       (str/starts-with? uri "/test")     #'render-tests-page
                       (str/starts-with? uri "/close")    #'close-handler
                       (match-route routes uri)           (render-page (match-route routes uri))
                       :else                              (not-found "404 Not Found"))]
    (next-handler request)))

(defn- wrap-query-params [handler]
  (fn [{:keys [params query-string] :or {params {}} :as req}]
    (if (empty? query-string)
      (handler req)
      (let [keyvals (-> (url-decode query-string)
                        (str/split #"&"))
            params (reduce (fn [params keyval]
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
               status (:status data)]
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

(defn- create-handler-stack [{:keys [reload? figwheel?]}]
  (-> routing-handler
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

;; This is for Figwheel
(def ^{:doc "Figwheel handler."}
  development-app
  (create-handler-stack {:figwheel? true :reload? true}))

(defn -main
  "Server start method."
  [& _args]
  (init-config!)
  (start-logging! (get-config :logging))
  (init-db! (get-config :database))
  (let [mode      (get-config :server :mode)
        http-port (or (get-config :server :http-port) 8080)]
    (when (= "dev" mode) (vms-sync!))
    (log-str "Starting server!")
    (server/start-server! {:handler (create-handler-stack {:reload? (= mode "dev") :figwheel? false})
                           :port    http-port})
    (when (= "prod" mode)
      #_(watch-kill-signal!) ;; Watch on the main thread
      #_(browse-url (str "http://localhost:" http-port))
      (create-cef-app!
       {:title       (get-config :site :title)
        :url         (str "http://localhost:" http-port)
        :fullscreen? true}))))

(comment
  (-main)
  (server/stop-server!))
