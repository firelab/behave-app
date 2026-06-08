(ns behave-cms.store
  (:require [behave.schema.core       :refer [all-schemas]]
            [datomic-store.main       :as s]
            [schema-migrate.interface :as sm]
            [config.interface         :refer [get-config]]))

(defn connect!
  "Connects to datomic DB given `db-config`.

   Optionally can pass `true` as second to reset the connection."
  [db-config & [reset?]]
  (if reset?
    (s/reset-db! db-config all-schemas)
    (s/default-conn db-config
                    all-schemas
                    (fn [conn]
                      (s/migrate! conn all-schemas)
                      (sm/run-pending-migrations! conn "development/migrations"))
                    [:bp/migration-id])))

(defn default-conn
  "If a connection has already been established, returns
   the existing Datomic DB connection.

   Optionally can pass `true` as second force a connection reset."
  [& [reset?]]
  (if (or (nil? @s/datomic-conn) reset?)
    (connect! (get-config :database) reset?)
    @s/datomic-conn))
