(ns behave.store
  (:require [behave.schema.core   :refer [all-schemas]]
            [config.interface     :refer [get-config]]
            [datom-store.main     :as s]
            [file-utils.interface :refer [os-path]]))

(defn db-config
  "`:database :config` with an OS path. `-Dbehave.store.path` (test shards)
  overrides it; read per call since /api/init reloads config."
  []
  (let [config (update-in (get-config :database :config) [:store :path] os-path)]
    (if-let [p (System/getProperty "behave.store.path")]
      (assoc-in config [:store :path] (os-path p))
      config)))

(defn db-file
  "Worksheet SQLite file path."
  []
  (s/get-db-file (db-config)))

(defn connect!
  "Connects to, or reuses, the worksheet store."
  [config]
  (s/default-conn all-schemas config))
