(ns health.core
  "Building blocks for a `/health` endpoint."
  (:require [clojure.data.json :as json])
  (:import (java.io File)
           (java.lang.management ManagementFactory)
           (java.time Instant)))

;;; Thresholds (percent used)

(def ^:private DISK-WARN-PCT 90)
(def ^:private DISK-FAIL-PCT 95)
(def ^:private HEAP-WARN-PCT 90)

;;; Helpers

(def ^:private severity {"pass" 0 "warn" 1 "fail" 2})

(defn worst-status
  "Returns the most severe of `statuses`, or `pass` when empty."
  [statuses]
  (reduce #(max-key severity %1 %2) "pass" statuses))

(defn threshold-status
  "Status for a percentage `pct` given `warn` and optional `fail` limits."
  [pct warn fail]
  (cond
    (and fail (>= pct fail)) "fail"
    (>= pct warn)            "warn"
    :else                    "pass"))

(defn round1
  "Rounds `x` to one decimal place."
  [x]
  (/ (Math/round (* 10.0 x)) 10.0))

(defn- percent [part whole]
  (if (pos? whole)
    (round1 (/ (* 100.0 part) whole))
    0.0))

(defn- now [] (str (Instant/now)))

(defn run-check
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

(defn disk-check
  "Disk utilization of the filesystem holding `dir`, plus a write probe."
  [component-id ^File dir]
  (run-check
   {:componentId component-id :componentType "system"}
   #(let [pct (used-pct dir)]
      (write-probe! dir)
      {:observedValue pct
       :observedUnit  "percent"
       :status        (threshold-status pct DISK-WARN-PCT DISK-FAIL-PCT)})))

;;; Memory

(defn heap-check
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

(defn uptime-check
  "Seconds since this JVM started."
  []
  (run-check
   {:componentType "system"}
   (fn []
     {:observedValue (quot (.getUptime (ManagementFactory/getRuntimeMXBean)) 1000)
      :observedUnit  "s"
      :status        "pass"})))

;;; Assets

(defn asset-check
  "Passes when `lookup` (e.g. `io/resource`) finds the classpath resource `path`."
  [lookup path]
  (run-check
   {:componentId path :componentType "component"}
   #(if (lookup path)
      {:status "pass"}
      {:status "fail" :output "missing"})))

;;; Report

(defn report
  "Health report for `checks`, a map of `component:measurement` to a vector of
  check results. `version` is included when non-nil."
  [checks version]
  (cond-> {:status (worst-status (map :status (apply concat (vals checks))))
           :checks checks}
    version (assoc :version version)))

(defn response
  "Ring response for `health-report`: 200 for pass/warn, 503 for fail, never cached."
  [{:keys [status] :as health-report}]
  {:status  (if (= status "fail") 503 200)
   :headers {"Content-Type"  "application/health+json"
             "Cache-Control" "no-cache, no-store"}
   :body    (json/write-str health-report)})
