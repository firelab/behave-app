(ns user
  (:require [fig-repl :as r]))

(r/start-figwheel!)

;; Connect to 1337
(r/start-repl!)

#_(config/load-config "deps.edn")

#_(config/get-config :aliases)

#_(m/new-component "storage")

#_(m/new-base "behave-routing")
#_(m/new-project "behave")
#_(m/new-project "behave-cms")

#_(load "manage")
#_(load-file "development/manage.clj")
