(ns behave.health
  "Behave `GET /health`: shared checks plus worksheet DB, temp worksheets and
  assets."
  (:require [behave.store        :refer [db-file]]
            [behave.views        :refer [find-app-js resource]]
            [clojure.edn         :as edn]
            [clojure.java.io     :as io]
            [datom-store.main    :as s]
            [health.interface    :as health])
  (:import (java.io File)
           (java.sql DriverManager)))

;;; Thresholds

(def ^:private SQLITE-TIMEOUT-S 2)
(def ^:private TEMP-WORKSHEETS-WARN-MB 1024)

;;; Helpers

(defn- temp-dir
  "Where /api/open and /api/save copy worksheets."
  ^File []
  (io/file (System/getProperty "java.io.tmpdir")))

;;; Disk

(defn- disk-checks []
  [(health/disk-check "database" (.getParentFile (io/file (db-file))))
   (health/disk-check "tmp" (temp-dir))])

;;; Temp worksheets
;; Leftover `.bp7` copies filled the prod disk (BHP1-1708).

(defn- temp-worksheets
  "`.bp7` files in `dir`."
  [^File dir]
  (filter #(.endsWith (.getName ^File %) ".bp7") (.listFiles dir)))

(defn- temp-worksheets-check []
  (health/run-check
   {:componentId "tmp" :componentType "system"}
   #(let [files (temp-worksheets (temp-dir))
          mb    (health/round1 (/ (reduce + 0 (map (fn [^File f] (.length f)) files)) 1048576.0))]
      {:observedValue mb
       :observedUnit  "MB"
       :fileCount     (count files)
       :status        (health/threshold-status mb TEMP-WORKSHEETS-WARN-MB nil)})))

;;; Assets

(defn- required-assets
  "Files the browser needs to boot."
  []
  ["public/layout.msgpack"
   "public/js/behave-min.js"
   "public/js/behave-min.wasm"
   (str "public" (find-app-js))])

;;; Database

(defn- sqlite-ping!
  "Trivial worksheet DB read; returns ms."
  [path]
  ;; The driver would create a missing file.
  (when-not (.exists (io/file path))
    (throw (ex-info "Worksheet database file not found" {})))
  (let [start (System/nanoTime)]
    (with-open [conn (DriverManager/getConnection (str "jdbc:sqlite:" path))
                stmt (.createStatement conn)]
      (.setQueryTimeout stmt SQLITE-TIMEOUT-S)
      (.executeQuery stmt "SELECT count(*) FROM sqlite_master"))
    (quot (- (System/nanoTime) start) 1000000)))

(defn- sqlite-check []
  (health/run-check
   {:componentId "worksheets" :componentType "datastore"}
   #(do
      (when-not @s/conn (throw (ex-info "DataScript connection not initialized" {})))
      {:observedValue (sqlite-ping! (db-file))
       :observedUnit  "ms"
       :status        "pass"})))

;;; Report

(def ^:private version
  (delay (some-> (resource "version.edn") slurp edn/read-string :version)))

(defn health
  "Runs all checks."
  []
  (health/report {"disk:utilization"    (disk-checks)
                  "sqlite:responseTime" [(sqlite-check)]
                  "heap:utilization"    [(health/heap-check)]
                  "tempWorksheets:size" [(temp-worksheets-check)]
                  "assets:availability" (mapv #(health/asset-check resource %) (required-assets))
                  "uptime"              [(health/uptime-check)]}
                 @version))

(defn health-handler
  "`GET /health`."
  [_request]
  (health/response (health)))
