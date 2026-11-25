(ns health.interface
  "Shared checks and response handling for `/health` endpoints:
  `application/health+json`, an overall `status` of `pass` / `warn` / `fail`,
  and a `checks` map keyed by `component:measurement`. HTTP 200 for
  `pass`/`warn`, 503 for `fail`, so a plain uptime monitor alerts on status
  code alone."
  (:require [health.core :as c]))

;;; Building checks

(defn run-check
  "Merges the `component` keys (e.g. `:componentId`, `:componentType`) and a
  timestamp into the result of `check-fn`. Any exception becomes a `fail`
  result carrying its message in `output`."
  [component check-fn]
  (c/run-check component check-fn))

(defn threshold-status
  "Status for a percentage `pct` given `warn` and optional `fail` limits."
  [pct warn fail]
  (c/threshold-status pct warn fail))

(defn round1
  "Rounds `x` to one decimal place."
  [x]
  (c/round1 x))

;;; Standard checks

(defn disk-check
  "Disk utilization of the filesystem holding `dir` (warn 90%, fail 95%), plus
  a write probe that fails when the disk can't be written."
  [component-id dir]
  (c/disk-check component-id dir))

(defn heap-check
  "JVM heap in use as a percentage of -Xmx. Warn-only (90%)."
  []
  (c/heap-check))

(defn uptime-check
  "Seconds since this JVM started."
  []
  (c/uptime-check))

(defn asset-check
  "Passes when `lookup` (e.g. `io/resource`) finds the classpath resource `path`."
  [lookup path]
  (c/asset-check lookup path))

;;; Report

(defn report
  "Health report for `checks`, a map of `component:measurement` to a vector of
  check results, with an overall status. `version` is included when non-nil."
  [checks version]
  (c/report checks version))

(defn response
  "Ring response for `health-report`: 200 for pass/warn, 503 for fail, never cached."
  [health-report]
  (c/response health-report))
