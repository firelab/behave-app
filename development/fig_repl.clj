(ns fig-repl
  (:require [figwheel-sidecar.repl-api :as fig]))

(def ^:private config
  {:css-dirs     ["projects/behave/resources/public/css"]
   :ring-handler 'behave.core/development-app
   :server-port  8080
   :nrepl-port   1337
   :builds       [{:id           "browser"
                   :source-paths ["projects/behave/src/cljc" "projects/behave/src/cljs"]
                   :figwheel     true
                   :compiler     {;:preloads      ['devtools.preload]
                                  :main          "behave.client"
                                  :output-dir    "projects/behave/resources/public/cljs"
                                  :output-to     "projects/behave/resources/public/cljs/app.js"
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
