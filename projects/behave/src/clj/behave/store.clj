(ns behave.store
  (:require [behave.schema.core :refer [all-schemas]]
            [datom-store.main :as s]))

(defonce ^:private db-ready? (promise))

(defn connect! [config]
  (let [conn (s/default-conn all-schemas config)]
    (deliver db-ready? true)
    conn))

(defn abort-db!
  "Unblocks [[await-db!]] after a failed DB init so API calls fail fast
  instead of hanging until the timeout."
  []
  (deliver db-ready? false))

(defn await-db!
  "Blocks until [[connect!]] has delivered a DB connection. DB init runs on a
  background thread at startup so CEF/Jetty can boot in parallel; handlers
  that touch the DB call this first. Times out after 30s."
  []
  (deref db-ready? 30000 nil))
