(ns behave.server
  (:gen-class)
  (:require [behave.handlers      :refer [server-handler-stack vms-sync! watch-kill-signal!]]
            [behave.store         :as store]
            [behave.views         :refer [warm-version-cache!]]
            [clojure.java.browse  :refer [browse-url]]
            [clojure.java.io      :as io]
            [config.interface     :refer [get-config load-config merge-config!]]
            [file-utils.interface :refer [os-path]]
            [logging.interface    :as l :refer [log-str timed]]
            [server.interface     :as server])
  (:import [java.lang ProcessHandle]))

(defn jvm-uptime-ms
  "Milliseconds since the process started (captures class-loading time spent
  before `-main` is entered). Uses `ProcessHandle` (java.base) rather than
  `ManagementFactory` — `java.management` isn't in the jlinked runtime."
  []
  (let [start (-> (ProcessHandle/current) .info .startInstant (.orElse nil))]
    (if start
      (- (System/currentTimeMillis) (.toEpochMilli start))
      -1)))

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

(defn init-db-async!
  "Initializes the DB on a background thread so CEF/Jetty startup proceeds in
  parallel. Handlers that touch the DB block on [[behave.store/await-db!]].
  Failures are logged and unblock waiters (futures otherwise swallow them)."
  [database-config]
  (future
    (try
      (timed "init-db (async)" (init-db! database-config))
      (catch Throwable t
        (log-str "DB init failed: " (ex-message t))
        (log-str (Throwable->map t))
        (store/abort-db!)))))

;;; Logging

(defn- log-system-start! []
  (log-str [:SYSTEM {:java (System/getProperty "java.version")
                     :os   (str (System/getProperty "os.name") " " (System/getProperty "os.version"))
                     :arch (System/getProperty "os.arch")}])
  (log-str (get-config)))

(defn- start-logging! [log-opts]
  (let [log-opts (update log-opts :log-dir os-path)]
    (io/make-parents (:log-dir log-opts))
    (l/start-logging! log-opts)
    (log-system-start!)))

(defn start-server!
  "Starts the Behave7 Application server."
  []
  (timed "load-config"
         (init-config!)
         (enrich-config!))
  (let [mode      (get-config :server :mode)
        http-port (or (get-config :server :http-port) 8080)
        org-name  (get-config :site :org-name)
        app-name  (get-config :site :app-name)]
    (timed "start-logging" (start-logging! (get-config :logging)))
    (init-db-async! (get-config :database :config))
    (warm-version-cache!)
    (log-str (format "Starting %s %s server at http://localhost:%s" org-name app-name http-port))
    (timed "start-jetty"
           (server/start-server! {:handler (server-handler-stack {:reload? (= mode "dev") :figwheel? false})
                                  :port    http-port}))
    (log-str "[TIMING] server ready " (jvm-uptime-ms) "ms after JVM start")
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
