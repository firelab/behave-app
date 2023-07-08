(ns behave.core
  (:require [clojure.java.io          :as io]
            [clojure.edn              :as edn]
            [clojure.string           :as str]
            [clojure.stacktrace       :as st]
            [bidi.bidi                :refer [match-route]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.reload   :refer [wrap-reload]]
            [ring.util.codec          :refer [url-decode]]
            [ring.util.response       :refer [not-found]]
            [server.interface         :as server]
            [logging.interface        :as logging]
            [config.interface         :refer [get-config load-config]]
            [transport.interface      :refer [->clj mime->type]]
            [triangulum.logging       :refer [log-str]]
            [behave-routing.main      :refer [routes]]
            [behave.store             :as store]
            [behave.sync              :refer [sync-handler]]
            [behave.download-vms      :refer [export-from-vms]]
            [behave.views             :refer [render-page]])
  (:gen-class))

(defn expand-home [s]
  (str/replace s #"^~" (System/getProperty "user.home")))

(defn init! []
  (load-config (io/resource "config.edn"))
  (let [config (update-in (get-config :database :config) [:store :path] expand-home)]
    (log-str "LOADED CONFIG" (get-config :database :config))
    (io/make-parents (get-in config [:store :path]))
    (store/connect! config)))

(defn vms-sync! []
  (export-from-vms (get-config :vms :secret-token)
                   (get-config :vms :url)))

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
  (fn [{:keys [request-method content-type body query-string] :as req}]
    (if-let [req-type (mime->type content-type)]
      (let [get-params  (->clj query-string req-type)
            post-params (->clj (slurp body) req-type)]
        (handler (update req :params merge get-params post-params)))
      (handler req))))

(defn wrap-content-type [handler]
  (fn [{:keys [headers] :as req}]
    (handler (assoc req :content-type (get headers "content-type")))))

(defn wrap-accept [handler]
  (fn [{:keys [headers] :as req}]
    (handler (assoc req :accepts (get headers "accept")))))

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
      #_(wrap-defaults behave-defaults)
      (wrap-resource "public" {:allow-symlinks? true})
      wrap-accept
      wrap-content-type
      wrap-exceptions
      (optional-middleware #(wrap-reload % {:dirs (reloadable-clj-files)}) reload?)))

;; This is for Figwheel
(def development-app
  (create-handler-stack {:figwheel? true :reload? true}))

(defn -main [& _args]
  (init!)
  (vms-sync!)
  (server/start-server! {:handler (create-handler-stack {:reload? (= (get-config :server :mode) "dev") :figwheel? false})
                         :port    (or (get-config :server :http-port) 8080)})
  (logging/start-logging! {:log-dir             (get-config :logging :log-dir)
                           :log-memory-interval (get-config :logging :log-memory-interval)}))

(comment
  (-main)
  (server/stop-server!))
