(ns logging.core
  "To send a message to the logger use `log` or `log-str`. `log` can take an
  optional argument to specify not default behavior. The default values are
  shown below. `log-str` always uses the default values.

  ```clojure
  (log \"Hello world\" {:newline? true :pprint? false :force-stdout? false})
  (log-str \"Hello\" \"world\")
  ```

  By default the above will log to standard out. If you would like to
  have the system log to YYYY-DD-MM.log, set a log path. You can either specify
  a path relative to the toplevel directory of the main project repository or an
  absolute path on your filesystem. The logger will keep the 10 most recent logs
  (where a new log is created every day at midnight). To stop the logging server
  set path to \"\"."
  (:import [java.text SimpleDateFormat]
           [java.util Date]
           [java.io File]
           [java.lang Thread Thread$UncaughtExceptionHandler])
  (:require [clojure.java.io :as io]
            [clojure.pprint  :as pp]
            [logging.runtime :refer [get-system-information]]))

(defonce ^:private synchronized-log-writer (agent nil))
(defonce ^:private output-path             (atom ""))
(defonce ^:private clean-up-service        (atom nil))
(defonce ^:private memory-logging          (atom nil))

(defn- max-length [string length]
  (subs string 0 (min length (count string))))

(defn log
  "Synchronously create a log entry. Logs will got to standard out as default.
   A log file location can be specified with set-log-path!.

   Default options are {:newline? true :pprint? false :force-stdout? false}"
  [data & {:keys [newline? pprint? force-stdout? truncate?]
           :or {newline? true pprint? false force-stdout? false truncate? true}}]
  (let [timestamp    (.format (SimpleDateFormat. "MM/dd HH:mm:ss") (Date.))
        log-filename (str (.format (SimpleDateFormat. "YYYY-MM-dd") (Date.)) ".log")
        max-data     (if truncate? (max-length data 500) data)
        line         (str timestamp
                          " "
                          (if pprint? (with-out-str (pp/pprint data)) max-data)
                          (when (and newline? (not pprint?)) "\n"))]
    (send-off synchronized-log-writer
              (if (or force-stdout? (= "" @output-path))
                (fn [_] (print line) (flush))
                (fn [_] (spit (io/file @output-path log-filename) line :append true)))))
  nil)

(defn log-str
  "A variadic version of log which concatenates all of the strings into one log line.
   Uses the default options for log."
  [& data]
  (log (apply str data)))

(defn- start-clean-up-service! []
  (log "Starting log file removal service." :force-stdout? true)
  (future
    (while true
      (Thread/sleep (* 1000 60 60 24)) ; 24 hours in milliseconds.
      (try (doseq [file (as-> (io/file @output-path) files
                          (.listFiles files)
                          (sort-by (fn [^File f] (.lastModified f)) files)
                          (take (- (count files) 10) files))]
             (io/delete-file file))
           (catch Exception _)))))

(defn set-log-path!
  "Sets a path to create file logs. When set to a directory, log files will be
   created with the date as part of the file name. When an empty string is set
   logging will be sent to standard out."
  [path]
  (cond
    (pos? (count path))
    (try
      (io/make-parents (io/file path "dummy.log"))
      (reset! output-path path)
      (log (str "Logging to: " path) :force-stdout? true)
      (when (nil? @clean-up-service)
        (reset! clean-up-service (start-clean-up-service!)))
      (catch Exception _
        (reset! output-path "")
        (log (str "Error setting log path to " path ". Check that you supplied a valid path.") :force-stdout? true)))

    (not (nil? @clean-up-service))
    (do
      (reset! output-path "")
      (log "Logging to: stdout" :force-stdout? true)
      (future-cancel @clean-up-service)
      (reset! clean-up-service nil))))

(defn start-memory-logging!
  "Logs the current system memory every `interval-in-minutes`."
  [interval-in-minutes]
  (log-str "Starting Memory Logging! Logging every " interval-in-minutes " minutes")
  (future
    (while true
      (Thread/sleep (* 1000 60 interval-in-minutes))
      (log-str (get-system-information)))))

(defn- capture-std-err! []
  (let [log-filename    (str (.format (SimpleDateFormat. "YYYY-MM-dd") (Date.)) ".log")
        log-file-stream (java.io.PrintStream. (str @output-path log-filename))]
    (System/setErr log-file-stream)))

(defn start-logging!
  "Begins the logging service."
  [{:keys [log-dir log-memory-interval capture-std-err]
                       :or   {log-dir ""}}]
  (log-str "Starting Logging Service!")
  (set-log-path! log-dir)
  (when (and log-memory-interval (nil? @memory-logging))
    (start-memory-logging! log-memory-interval))
  (when (and capture-std-err @output-path)
    (capture-std-err!)))

(defn stop-logging!
  "Stops the logging service, if there is one."
  []
  (set-log-path! "")
  (reset! memory-logging nil))
