(ns logging.core
  (:require [triangulum.logging :refer [log-str set-log-path!]]
            [logging.runtime    :refer [get-system-information]]))

(defonce ^:private memory-logging (atom nil))

(defn start-memory-logging!
  "Logs the current system memory every `interval-in-minutes`."
  [interval-in-minutes]
  (log-str "Starting Memory Logging! Logging every " interval-in-minutes " minutes")
  (future
    (while true
      (Thread/sleep (* 1000 60 interval-in-minutes))
      (log-str (get-system-information)))))

(defn start-logging!
  "Begins the logging service."
  [{:keys [log-dir log-memory-interval]
                       :or   {log-dir ""}}]
  (log-str "Starting Logging Service!")
  (set-log-path! log-dir)
  (when (and log-memory-interval (nil? @memory-logging))
    (start-memory-logging! log-memory-interval)))

(defn stop-logging!
  "Stops the logging service, if there is one."
  []
  (set-log-path! "")
  (reset! memory-logging nil))
