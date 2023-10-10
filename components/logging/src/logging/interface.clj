(ns logging.interface
  (:require [triangulum.logging :as l]
            [logging.core :as core]))

(def ^{:argslist '([config])
       :doc      "Starts triangulum logging service with configuration.

            Configuration should include:
            - `:log-dir`               Which directory do you want triangulum's `log` or `log-str` to be written to.
            - `:log-memory-interval`   The interval in minutes to log memory usage. (Optional)"}
  start-logging! core/start-logging!)

(def ^{:argslist '([])
       :doc      "Stops the logging service, if one has been started."}
  stop-logging! core/stop-logging!)

(def ^{:argslist '([& s])
       :doc      "A variadic version of log which concatenates all of the strings into one log line.
            Uses the default options for log."}
  log-str l/log-str)

(def
  ^{:argslist '([data] [data opts])
    :doc      "Synchronously create a log entry. Logs will got to standard out as default.
     A log file location can be specified with set-log-path!.

    Options are:
    - :newline       [bool] (Default: true) - Force a newline.
    - :pprint        [bool] (Default: false) - Pretty print data.
    - :force-stdout  [bool] (Default: false) - Display log message to stdout as well."}
  log l/log)
