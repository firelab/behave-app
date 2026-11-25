(ns behave-cms.health
  "`GET /health` for the Behave CMS (see [[health.interface]]). Adds the
  Datomic transactor and app asset checks to the shared disk/heap/uptime
  checks."
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
  "java.io.tmpdir, where exports and uploads are staged (`behave-cms-tmp*`)."
  ^File []
  (io/file (System/getProperty "java.io.tmpdir")))

;;; Database

(defn- datomic-sync!
  "Waits for `conn` to catch up with the transactor; returns elapsed ms.
  Throws when the transactor doesn't answer within the timeout."
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
  "Classpath paths of the files the browser needs to boot the CMS."
  []
  [(str "public" (find-app-js))
   "public/js/katex.min.js"
   "public/js/fontawesome.js"])

;;; Report

(defn health
  "Runs all checks and returns the health report map."
  []
  (health/report {"disk:utilization"     [(health/disk-check "tmp" (temp-dir))]
                  "datomic:responseTime" [(datomic-check)]
                  "heap:utilization"     [(health/heap-check)]
                  "assets:availability"  (mapv #(health/asset-check io/resource %) (required-assets))
                  "uptime"               [(health/uptime-check)]}
                 nil))

(defn health-handler
  "Ring handler for `GET /health`."
  [_request]
  (health/response (health)))
