(ns behave.core
  (:require [clojure.java.io          :as io]
            [clojure.edn              :as edn]
            [clojure.string           :as str]
            [clojure.stacktrace       :as st]
            [bidi.bidi                :refer [match-route]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.reload   :refer [wrap-reload]]
            [ring.util.codec          :refer [url-decode]]
            [ring.util.request        :as request]
            [ring.util.response       :refer [not-found]]
            [server.interface         :as server]
            [config.interface         :refer [get-config load-config]]
            [transport.interface      :refer [->clj mime->type]]
            [triangulum.logging       :refer [log-str set-log-path!]]
            [behave-routing.main      :refer [routes]]
            [behave.store             :as store]
            [behave.sync              :refer [sync-handler]]
            [behave.download-vms      :refer [export-from-vms]]
            [behave.views             :refer [render-page]])
  (:gen-class))

(defn init! []
  (export-from-vms "b4c8a1048d5e406c93cc828e34ac9fc6")
  (load-config (io/resource "config.edn"))
  (store/connect! (get-config :database :config)))

(defn bad-uri?
  [uri]
  (str/includes? (str/lower-case uri) "php"))

(defn routing-handler [{:keys [uri] :as request}]
  (let [next-handler (cond
                       (bad-uri? uri)                 (not-found "404 Not Found")
                       (str/starts-with? uri "/sync") #'sync-handler
                       (match-route routes uri)       (render-page (match-route routes uri))
                       :else                          (not-found "404 Not Found"))]
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
                           params keyvals)

            _ (log-str "-- FOUND QUERY PARAMS:" params)]
        (handler (assoc req :params params))))))

(defn wrap-params [handler]
  (fn [{:keys [body content-type params] :as req}]
    (if-let [req-type (mime->type content-type)]
      (handler (assoc req :params (merge params (->clj body req-type))))
      (handler req))))

(defn wrap-content-type [handler]
  (fn [{:keys [headers] :as req}]
    (handler (assoc req :content-type (get headers "Content-Type")))))

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

(defn create-handler-stack []
  (-> routing-handler
      wrap-params
      wrap-query-params
      #_(wrap-defaults behave-defaults)
      (wrap-resource "public" {:allow-symlinks? true})
      wrap-accept
      wrap-content-type
      wrap-exceptions
      wrap-reload))

;; This is for Figwheel
(def development-app
  (create-handler-stack))

(defn -main [& _args]
  (server/start-server! {:handler development-app :port 8003}))

(comment


  (require '[ring.mock.request :refer [request header]])
  (require '[clojure.java.io :as io])

  (def app (create-handler-stack))

  (development-app {:uri "/js/out/app.js"})
  (development-app {:uri "/cljs/app.js" :request-method :get})
  (development-app {:uri "/index.html" :request-method :get})

  (io/resource "public/cljs/app.js")

  (def req {:uri "/cljs/app.js"})

  (request/path-info req)

  (-main)

  (server/stop-server!)

  )

