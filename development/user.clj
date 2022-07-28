(ns user
  (:require [manage :as m]
            [config.interface :as config]))

(config/load-config "deps.edn")

(config/get-config :aliases)

#_(m/new-component "storage")

#_(m/new-base "behave-routing")
(m/new-project "behave")
#_(m/new-project "behave-cms")

#_(load "manage")
#_(load-file "development/manage.clj")
