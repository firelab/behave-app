(ns behave.core
  (:require [clojure.java.io              :as io]
            [clojure.string               :as str]
            [bidi.bidi                    :refer [match-route]]
            [hiccup.page                  :refer [html5 include-css include-js]]
            [ring.middleware.defaults     :refer [wrap-defaults site-defaults]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.reload       :refer [wrap-reload]]
            [ring.util.request            :as request]
            [ring.util.response           :refer [response not-found]]
            [server.interface             :refer [start-server!]]
            [config.interface             :refer [get-config load-config]]
            [transport.interface          :refer [->clj mime->type]]
            [behave-routing.main          :refer [routes]]
            [behave.store                 :as store]
            [behave.views                 :refer [render-page]]
            )
  (:gen-class))

(defn init! []
  (load-config (io/resource "config.edn"))
  (store/connect! (get-config :database :config)))

(defn bad-uri?
  [uri]
  (str/includes? (str/lower-case uri) "php"))

(defn routing-handler [{:keys [uri] :as request}]
  (let [next-handler (cond
                       (bad-uri? uri)           (not-found "404 Not Found")
                       (match-route routes uri) (render-page (match-route routes uri))
                       :else                    (not-found "404 Not Found"))]
    (next-handler request)))

(defn wrap-content-type [handler]
  (fn [{:keys [body content-type params] :as req}]
    (if-let [req-type (mime->type content-type)]
      (handler (assoc req :params (merge params (->clj body req-type))))
      (handler req))))

(defn create-handler-stack []
  (-> routing-handler
      (wrap-content-type)
      (wrap-defaults (assoc-in site-defaults [:static :resources] "public"))
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

