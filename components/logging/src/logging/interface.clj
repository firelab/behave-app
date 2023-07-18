(ns logging.interface
  (:require [logging.core :as core]))

(def ^{:argslist '([config])
       :doc      "Starts triangulum logging service with configuration.

            Configuration should include:
            - `:log-dir`               Which directory do you want triangulum's `log` or `log-str` to be written to.
            - `:log-memory-interval`   The interval in minutes to log memory usage. (Optional)"}
  start-logging! core/start-logging!)

(def ^{:argslist '([])
       :doc      "Stops the logging, if one has been started."}
  stop-logging! core/stop-logging!)
