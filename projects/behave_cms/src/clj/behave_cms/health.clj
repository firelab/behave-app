(ns behave-cms.health
  "CMS `GET /health`: shared checks plus Datomic and assets."
  (:require [behave-cms.views    :refer [find-app-js]]
            [clojure.java.io     :as io]
            [datomic-store.main  :as s]
            [datomic.api         :as d]
            [health.interface    :as health])
  (:import (java.io File)))

;;; Thresholds

(def ^:private DATOMIC-TIMEOUT-MS 2000)

;;; Disk

(defn- temp-dir
  "Where exports are staged."
  ^File []
  (io/file (System/getProperty "java.io.tmpdir")))

;;; Database

(defn- datomic-sync!
  "Syncs `conn` with the transactor; returns ms. Throws on timeout."
  [conn]
  (let [start (System/nanoTime)]
    (when (= ::timeout (deref (d/sync conn) DATOMIC-TIMEOUT-MS ::timeout))
      (throw (ex-info "Datomic transactor did not respond" {})))
    (quot (- (System/nanoTime) start) 1000000)))

(defn- datomic-check []
  (health/run-check
   {:componentId "vms" :componentType "datastore"}
   #(let [conn (or @s/datomic-conn
                   (throw (ex-info "Datomic connection not initialized" {})))]
      {:observedValue (datomic-sync! conn)
       :observedUnit  "ms"
       :status        "pass"})))

;;; Assets

(defn- required-assets
  "Files the browser needs to boot."
  []
  [(str "public" (find-app-js))
   "public/js/katex.min.js"
   "public/js/fontawesome.js"])

;;; Report

(defn health
  "Runs all checks."
  []
  (health/report {"disk:utilization"     [(health/disk-check "tmp" (temp-dir))]
                  "datomic:responseTime" [(datomic-check)]
                  "heap:utilization"     [(health/heap-check)]
                  "assets:availability"  (mapv #(health/asset-check io/resource %) (required-assets))
                  "uptime"               [(health/uptime-check)]}
                 nil))

(defn health-handler
  "`GET /health`."
  [_request]
  (health/response (health)))
