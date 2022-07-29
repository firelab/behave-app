(ns behave.core
  (:require [server.interface             :refer [start-server!]]
            [ring.middleware.defaults     :refer [wrap-defaults site-defaults]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.resource     :refer [wrap-resource resource-request]]
            [ring.middleware.reload       :refer [wrap-reload]]
            [ring.util.response           :refer [response content-type]]
            [ring.util.request            :as request]
            )
  (:gen-class))

(defn default-app [_req]
  (content-type (response "<!DOCTYPE HTML><head><script src=\"/cljs/app.js\" type=\"text/javascript\"></script></head><body>OK DUDE NOW ITS WORKING</body>") "text/html"))

(defn create-handler-stack []
  (-> (constantly nil)
      (wrap-resource "public")
      wrap-content-type
      wrap-reload))

;; This is for Figwheel
(def development-app
  (create-handler-stack))

(defn -main [& _args]
  (start-server! {:handler development-app}))

(comment


  (require '[ring.mock.request :refer [request header]])
  (require '[clojure.java.io :as io])

  (development-app {:uri "/index.html"})
  (development-app {:uri "/cljs/app.js"})
  (development-app {:uri "/cljs/app.js" :request-method :get})
  (development-app {:uri "/index.html" :request-method :get})

  (io/resource "public/cljs/app.js")

  (def req {:uri "/cljs/app.js"})

  (request/path-info req)
  (-main)

  )

