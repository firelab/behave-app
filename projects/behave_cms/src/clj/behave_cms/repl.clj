(ns behave-cms.repl
  (:require [figwheel-sidecar.repl-api :as fig]))

(def ^:private config
  {:css-dirs     ["resources/public/css"]
   :ring-handler 'behave-cms.handler/development-app
   :server-port   8080
   :nrepl-port    1337
   :builds   [{:id           "browser"
               :source-paths ["src/cljc" "src/cljs"]
               :figwheel     true
               :compiler     {:preloads      ['behave-cms.devtools]
                              :main          "behave-cms.client"
                              :output-dir    "resources/public/cljs"
                              :output-to     "resources/public/cljs/app.js"
                              :asset-path    "/cljs"
                              :source-map    true
                              :optimizations :none
                              :pretty-print  true}}]})

(defn start-figwheel! []
  (fig/start-figwheel! config))

;; Connect to 1337
(defn start-repl! []
  (require '[figwheel-sidecar.repl-api :as fig])
  (fig/cljs-repl))
