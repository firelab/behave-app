(ns behave.server
  (:gen-class)
  (:require [behave.handlers      :refer [server-handler-stack vms-sync! watch-kill-signal!]]
            [behave.store         :as store]
            [clojure.java.browse  :refer [browse-url]]
            [clojure.java.io      :as io]
            [config.interface     :refer [get-config load-config merge-config!]]
            [file-utils.interface :refer [os-path]]
            [logging.interface    :as l :refer [log-str]]
            [server.interface     :as server]))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn init-config! []
  (load-config (io/resource "config.edn")))

(defn enrich-config!
  "Computes runtime values and merges them into loaded config."
  []
  (let [cef?       (some? (System/getProperty "app.dir"))
        mode       (or (get-config :server :mode)
                       (if cef? "prod" "dev"))
        jar-local? (and (= mode "prod") (not cef?))]
    (merge-config! {:server {:mode mode}
                    :client {:jar-local? jar-local?}})))

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
  (enrich-config!)
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

(defn -main
  "Server start method."
  [& _args]
  (start-server!))

(comment
  (-main)
  (server/stop-server!))
