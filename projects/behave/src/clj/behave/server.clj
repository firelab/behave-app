(ns behave.server
  (:gen-class)
  (:require [behave.store         :as store]
            [behave.handlers      :refer [server-handler-stack vms-sync! watch-kill-signal!]]
            [clojure.java.browse  :refer [browse-url]]
            [clojure.java.io      :as io]
            [config.interface     :refer [get-config load-config]]
            [file-utils.interface :refer [os-path]]
            [logging.interface    :as l :refer [log-str]]
            [server.interface     :as server])
  (:import [java.awt GraphicsEnvironment SplashScreen]))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn init-config! []
  (load-config (io/resource "config.edn")))

(defn init-db!
  "Initialize DB using configuration."
  [database-config]
  (let [database-config (update-in database-config [:store :path] os-path)]
    (log-str [:DB-CONFIG database-config])
    (io/make-parents (get-in database-config [:store :path]))
    (store/connect! database-config)))

;;; Logging

(defn- log-system-start! []
  (log-str [:SYSTEM])
  (doseq [[k v] (into {} (System/getProperties))]
    (log-str k ": " v))
  (log-str (get-config)))

(defn- start-logging! [log-opts]
  (let [log-opts (update log-opts :log-dir os-path)]
    (io/make-parents (:log-dir log-opts))
    (l/start-logging! log-opts)
    (log-system-start!)))

(defn start-server!
  "Starts the Behave7 Application server."
  []
  (init-config!)
  (let [mode      (get-config :server :mode)
        http-port (or (get-config :server :http-port) 8080)
        org-name  (get-config :site :org-name)
        app-name  (get-config :site :app-name)]
    (start-logging! (get-config :logging))
    (init-db! (get-config :database :config))
    (log-str (format "Starting %s %s server at http://localhost:%s" org-name app-name http-port))
    (server/start-server! {:handler (server-handler-stack {:reload? (= mode "dev") :figwheel? false})
                           :port    http-port})
    (condp = mode
      "dev"
      (vms-sync!)

      "prod"
      (when (get-config :client :jar-local?)
        (browse-url (format "http://localhost:%s" http-port))
        (watch-kill-signal!)))))

;;; Splash Screen

(defn- close-splash!
  "Closes the splash screen shown by a `java -jar` launch, if any."
  []
  (when-not (GraphicsEnvironment/isHeadless)
    (some-> (SplashScreen/getSplashScreen) .close)))

(defn -main
  "Server start method."
  [& _args]
  (try
    (start-server!)
    (finally
      (close-splash!))))

(comment
  (-main)
  (server/stop-server!))
