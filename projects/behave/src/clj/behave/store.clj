(ns behave.store
  (:require [behave.schema.core   :refer [all-schemas]]
            [config.interface     :refer [get-config]]
            [datom-store.main     :as s]
            [file-utils.interface :refer [os-path]]))

(defn db-config
  "The `:database :config` map with its store path resolved for this OS.

  Test-support (parallel sharding): a per-shard server JVM sets
  `-Dbehave.store.path` so each shard resets+connects its OWN db file. Read on
  every call because config.edn is reloaded on every /api/init. Inert (no
  property) in dev/prod."
  []
  (let [config (update-in (get-config :database :config) [:store :path] os-path)]
    (if-let [p (System/getProperty "behave.store.path")]
      (assoc-in config [:store :path] (os-path p))
      config)))

(defn db-file
  "Absolute path of the worksheet SQLite database file."
  []
  (s/get-db-file (db-config)))

(defn connect!
  "Connects (or returns the existing connection) to the worksheet store at `config`."
  [config]
  (s/default-conn all-schemas config))
