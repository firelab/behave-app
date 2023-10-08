(ns behave.core
  (:require [clojure.java.io              :as io]
            [clojure.java.browse          :refer [browse-url]]
            [clojure.edn                  :as edn]
            [clojure.string               :as str]
            [clojure.stacktrace           :as st]
            [bidi.bidi                    :refer [match-route]]
            [me.raynes.fs                 :as fs]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.resource     :refer [wrap-resource]]
            [ring.middleware.reload       :refer [wrap-reload]]
            [ring.util.codec              :refer [url-decode]]
            [ring.util.response           :refer [not-found]]
            [server.interface             :as server]
            [logging.interface            :refer [log-str] :as logging]
            [config.interface             :refer [get-config load-config]]
            [transport.interface          :refer [->clj mime->type]]
            [behave-routing.main          :refer [routes]]
            [behave.store                 :as store]
            [behave.sync                  :refer [sync-handler]]
            [behave.download-vms          :refer [export-from-vms export-images-from-vms]]
            [behave.views                 :refer [render-page render-tests-page]])
  (:gen-class))


(defn init! []
  (load-config (io/resource "config.edn"))
  (let [config (assoc-in (get-config :database :config)
                         [:store :path]
                         (str (io/file (fs/expand-home "~") ".behave" "db.sqlite")))]
    (log-str "LOADED CONFIG" (get-config :database :config))
    (io/make-parents (get-in config [:store :path]))
    (store/connect! config)))

(defn vms-sync! []
  (let [{:keys [secret-token url]} (get-config :vms)]
    (pmap #(% secret-token url) [export-from-vms export-images-from-vms])))

(defn vms-sync-handler [req]
  (log-str "Request Received:" (select-keys req [:uri :request-method :params]))
  (vms-sync!)
  {:status 200 :body "OK"})

(defn bad-uri?
  [uri]
  (str/includes? (str/lower-case uri) "php"))

(defn routing-handler [{:keys [uri] :as request}]
  (let [next-handler (cond
                       (bad-uri? uri)                     (not-found "404 Not Found")
                       (str/starts-with? uri "/vms-sync") #'vms-sync-handler
                       (str/starts-with? uri "/sync")     #'sync-handler
                       (str/starts-with? uri "/test")     #'render-tests-page
                       (match-route routes uri)           (render-page (match-route routes uri))
                       :else                              (not-found "404 Not Found"))]
    (next-handler request)))

(defn wrap-query-params [handler]
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

(defn wrap-params [handler]
  (fn [{:keys [content-type body query-string] :as req}]
    (if-let [req-type (mime->type content-type)]
      (let [query-params (->clj query-string req-type)
            body-params  (->clj (slurp body) req-type)]
        (handler (update req :params merge query-params body-params)))
      (handler req))))

(defn wrap-req-content-type+accept [handler]
  (fn [{:keys [headers] :as req}]
    (handler (assoc req
                    :content-type (get headers "content-type")
                    :accept       (get headers "accept")))))

(defn wrap-exceptions [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (let [{:keys [data cause]} (Throwable->map e)
               status (:status data)]
          (log-str "Error: " cause)
          (log-str (st/print-stack-trace e))
          {:status (or status 500) :body cause})))))

(defn reloadable-clj-files
  []
  (let [m       (meta #'reloadable-clj-files)
        ns      (:ns m)
        ns-file (-> ns
                    (str/replace "-" "_")
                    (str/replace "." "/")
                    (->> (format "/%s.clj")))
        path    (:file m)]
    [(str/replace path #"/projects/.*" "/components")
     (str/replace path #"/projects/.*" "/bases")
     (str/replace path ns-file "")]))

(defn optional-middleware [handler mw use?]
  (if use?
    (mw handler)
    handler))

(defn wrap-figwheel [handler figwheel?]
  (fn [request]
    (handler (assoc request :figwheel? figwheel?))))

(defn create-handler-stack [{:keys [reload? figwheel?]}]
  (-> routing-handler
      (wrap-figwheel figwheel?)
      wrap-params
      wrap-query-params
      wrap-req-content-type+accept
      (wrap-resource "public" {:allow-symlinks? true})
      (wrap-content-type {:mime-types {"wasm" "application/wasm"}})
      wrap-exceptions
      (optional-middleware #(wrap-reload % {:dirs (reloadable-clj-files)}) reload?)))

;; This is for Figwheel
(def development-app
  (create-handler-stack {:figwheel? true :reload? true}))

(defn -main [& _args]
  (init!)
  (let [mode      (get-config :server :mode)
        http-port (or (get-config :server :http-port) 8080)]
    (when (= "dev" mode) (vms-sync!))
    (server/start-server! {:handler (create-handler-stack {:reload? (= mode "dev") :figwheel? false})
                           :port    http-port})
    (logging/start-logging! {:log-dir             (get-config :logging :log-dir)
                             :log-memory-interval (get-config :logging :log-memory-interval)})
    (when (= "prod" mode) (browse-url (str "http://localhost:" http-port)))))

(comment
  (-main)
  (server/stop-server!))
