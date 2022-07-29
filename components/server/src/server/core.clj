(ns server.core
  (:require [ring.adapter.jetty  :refer [run-jetty]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.reload   :refer [wrap-reload]]))

;;; Handler

(defn optional-middeware [handler mw optional?]
  (if optional?
    (mw handler)
    mw))

(defn create-handler-stack
  [{:keys [handler reload? defaults? middleware] :or {defaults? true reload? true}}]
  (if (nil? handler)
    (do
      (println "ERROR: Must supply a :handler in the configuration.")
      (System/exit 1))
    (-> handler
      (optional-middeware middleware (fn? middleware))
      (optional-middeware #(wrap-defaults % site-defaults) (fn? middleware))
      defaults? (wrap-defaults site-defaults)
      reload? (wrap-reload))))

;;; Server

(defonce ^:private server (atom nil))

(defn start-server! [{:keys [port handler] :or {port 8080}}]
  (when (nil? @server)
    (let [ring-config  {:port port :join? false}]
      (reset! server (run-jetty handler ring-config)))))

(defn stop-server! []
  (when @server
    (@server)
    (reset! server nil)))
