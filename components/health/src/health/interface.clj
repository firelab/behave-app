(ns health.interface
  "Shared `/health` checks. Responds `application/health+json`: 200 for
  pass/warn, 503 for fail."
  (:require [health.core :as c]))

;;; Building checks

(defn run-check
  "`(check-fn)` plus `component` and a timestamp; exceptions become `fail`."
  [component check-fn]
  (c/run-check component check-fn))

(defn threshold-status
  "pass/warn/fail for `pct`; `fail` is optional."
  [pct warn fail]
  (c/threshold-status pct warn fail))

(defn round1
  "Rounds `x` to one decimal."
  [x]
  (c/round1 x))

;;; Standard checks

(defn disk-check
  "Disk use of `dir` (warn 90%, fail 95%), plus a write probe."
  [component-id dir]
  (c/disk-check component-id dir))

(defn heap-check
  "Heap used as % of -Xmx. Warn-only (90%)."
  []
  (c/heap-check))

(defn uptime-check
  "JVM uptime in seconds."
  []
  (c/uptime-check))

(defn asset-check
  "Fails when `(lookup path)` is nil."
  [lookup path]
  (c/asset-check lookup path))

;;; Report

(defn report
  "Report for `checks` (`component:measurement` → results); omits nil `version`."
  [checks version]
  (c/report checks version))

(defn response
  "200 for pass/warn, 503 for fail; never cached."
  [health-report]
  (c/response health-report))
