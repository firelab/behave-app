(ns behave.health
  "Liveness/readiness probe for the Behave server.

  Responds in the IETF Health Check Response Format
  (draft-inadarei-api-health-check): `application/health+json`, an overall
  `status` of `pass` / `warn` / `fail`, and a `checks` map keyed by
  `component:measurement`. HTTP 200 for `pass`/`warn`, 503 for `fail`, so a
  plain uptime monitor alerts on status code alone."
  (:require [behave.store        :refer [db-file]]
            [behave.views        :refer [find-app-js resource]]
            [clojure.edn         :as edn]
            [clojure.java.io     :as io]
            [datom-store.main    :as s]
            [transport.interface :refer [clj->json]])
  (:import (java.io File)
           (java.lang.management ManagementFactory)
           (java.sql DriverManager)
           (java.time Instant)))

;;; Thresholds (percent used)

(def ^:private DISK-WARN-PCT 90)
(def ^:private DISK-FAIL-PCT 95)
(def ^:private HEAP-WARN-PCT 90)
(def ^:private SQLITE-TIMEOUT-S 2)
(def ^:private TEMP-WORKSHEETS-WARN-MB 1024)

;;; Helpers

(def ^:private severity {"pass" 0 "warn" 1 "fail" 2})

(defn- worst-status
  "Returns the most severe of `statuses`, or `pass` when empty."
  [statuses]
  (reduce #(max-key severity %1 %2) "pass" statuses))

(defn- threshold-status
  "Status for a percentage `pct` given `warn` and optional `fail` limits."
  [pct warn fail]
  (cond
    (and fail (>= pct fail)) "fail"
    (>= pct warn)            "warn"
    :else                    "pass"))

(defn- round1
  "Rounds `x` to one decimal place."
  [x]
  (/ (Math/round (* 10.0 x)) 10.0))

(defn- percent [part whole]
  (if (pos? whole)
    (round1 (/ (* 100.0 part) whole))
    0.0))

(defn- now [] (str (Instant/now)))

(defn- temp-dir
  "java.io.tmpdir, where /api/open and /api/save write worksheet copies."
  ^File []
  (io/file (System/getProperty "java.io.tmpdir")))

(defn- run-check
  "Merges the `component` keys and a timestamp into the result of `check-fn`.
  Any exception becomes a `fail` result carrying its message in `output`."
  [component check-fn]
  (merge component
         (try
           (check-fn)
           (catch Exception e
             {:status "fail" :output (or (ex-message e) (str (class e)))}))
         {:time (now)}))

;;; Disk

(defn- used-pct
  "Percent of the filesystem holding `dir` that is unavailable to this JVM."
  [^File dir]
  (let [total (.getTotalSpace dir)]
    (percent (- total (.getUsableSpace dir)) total)))

(defn- write-probe!
  "Creates and deletes a temp file in `dir`, throwing if the disk can't be written."
  [^File dir]
  (.delete (File/createTempFile "health" ".probe" dir)))

(defn- disk-check
  "Disk utilization of the filesystem holding `dir`, plus a write probe."
  [component-id ^File dir]
  (run-check
   {:componentId component-id :componentType "system"}
   #(let [pct (used-pct dir)]
      (write-probe! dir)
      {:observedValue pct
       :observedUnit  "percent"
       :status        (threshold-status pct DISK-WARN-PCT DISK-FAIL-PCT)})))

(defn- disk-checks []
  [(disk-check "database" (.getParentFile (io/file (db-file))))
   (disk-check "tmp" (temp-dir))])

;;; Temp worksheets
;; /api/open and /api/save write `.bp7` copies to java.io.tmpdir; left behind,
;; they are what filled the prod disk (BHP1-1708).

(defn- temp-worksheets
  "The `.bp7` files sitting in `dir`."
  [^File dir]
  (filter #(.endsWith (.getName ^File %) ".bp7") (.listFiles dir)))

(defn- temp-worksheets-check []
  (run-check
   {:componentId "tmp" :componentType "system"}
   #(let [files (temp-worksheets (temp-dir))
          mb    (round1 (/ (reduce + 0 (map (fn [^File f] (.length f)) files)) 1048576.0))]
      {:observedValue mb
       :observedUnit  "MB"
       :fileCount     (count files)
       :status        (threshold-status mb TEMP-WORKSHEETS-WARN-MB nil)})))

;;; Assets

(defn- required-assets
  "Classpath paths of the files the browser needs to boot the app."
  []
  ["public/layout.msgpack"
   "public/js/behave-min.js"
   "public/js/behave-min.wasm"
   (str "public" (find-app-js))])

(defn- asset-check [path]
  (run-check
   {:componentId path :componentType "component"}
   #(if (resource path)
      {:status "pass"}
      {:status "fail" :output "missing"})))

;;; Database

(defn- sqlite-ping!
  "Opens the worksheet database and runs a trivial read; returns elapsed ms."
  [path]
  ;; The SQLite driver silently creates a missing file, so check first.
  (when-not (.exists (io/file path))
    (throw (ex-info "Worksheet database file not found" {})))
  (let [start (System/nanoTime)]
    (with-open [conn (DriverManager/getConnection (str "jdbc:sqlite:" path))
                stmt (.createStatement conn)]
      (.setQueryTimeout stmt SQLITE-TIMEOUT-S)
      (.executeQuery stmt "SELECT count(*) FROM sqlite_master"))
    (quot (- (System/nanoTime) start) 1000000)))

(defn- sqlite-check []
  (run-check
   {:componentId "worksheets" :componentType "datastore"}
   #(do
      (when-not @s/conn (throw (ex-info "DataScript connection not initialized" {})))
      {:observedValue (sqlite-ping! (db-file))
       :observedUnit  "ms"
       :status        "pass"})))

;;; Memory

(defn- heap-check
  "JVM heap in use as a percentage of the maximum heap (-Xmx).
  Warn-only: heap usage spikes between GCs, so it shouldn't page anyone alone."
  []
  (run-check
   {:componentId "jvm" :componentType "system"}
   #(let [rt   (Runtime/getRuntime)
          used (- (.totalMemory rt) (.freeMemory rt))
          pct  (percent used (.maxMemory rt))]
      {:observedValue pct
       :observedUnit  "percent"
       :status        (threshold-status pct HEAP-WARN-PCT nil)})))

;;; Uptime

(defn- uptime-check []
  (run-check
   {:componentType "system"}
   (fn []
     {:observedValue (quot (.getUptime (ManagementFactory/getRuntimeMXBean)) 1000)
      :observedUnit  "s"
      :status        "pass"})))

;;; Report

(def ^:private version
  (delay (some-> (resource "version.edn") slurp edn/read-string :version)))

(defn health
  "Runs all checks and returns the health report map."
  []
  (let [checks {"disk:utilization"    (disk-checks)
                "sqlite:responseTime" [(sqlite-check)]
                "heap:utilization"    [(heap-check)]
                "tempWorksheets:size" [(temp-worksheets-check)]
                "assets:availability" (mapv asset-check (required-assets))
                "uptime"              [(uptime-check)]}]
    {:status  (worst-status (map :status (apply concat (vals checks))))
     :version @version
     :checks  checks}))

(defn health-handler
  "Ring handler for `GET /health`."
  [_request]
  (let [{:keys [status] :as report} (health)]
    {:status  (if (= status "fail") 503 200)
     :headers {"Content-Type"  "application/health+json"
               "Cache-Control" "no-cache, no-store"}
     :body    (clj->json report)}))
