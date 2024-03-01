(ns datomic-store.main
  (:require [datomic.client.api :as d]
            [datahike.core         :refer [conn?]]
            [datahike.datom        :refer [datom]]
            [datom-utils.interface :refer [safe-attr?
                                               safe-deref
                                               split-datoms
                                               unsafe-attrs]]))
;;; Helpers

(defn unwrap
  "Recursively derefs the atom to get the DataHike/Script connection."
  [conn]
  (if (conn? conn)
    conn
    (unwrap @conn)))

;;; Unsafe Attributes

(defonce ^:private stored-unsafe-attrs (atom nil))

;;; Transaction Index

(defonce ^:private tx-index (atom (sorted-map)))

(defn- record-tx [{:keys [tx-data]}]
  (->> tx-data
       (split-datoms)
       (group-by #(nth % 3))
       (swap! tx-index merge)))

(defn- build-tx-index! [db]
  (let [db (safe-deref db)]
    (as-> db %
      (d/datoms % :eavt)
      (split-datoms %)
      (filter #(safe-attr? @stored-unsafe-attrs %) %)
      (group-by (fn [d] (nth d 3)) %)
      (swap! tx-index merge %))))

;;; Connection

(defonce ^{:doc "Datmoic Connection"} conn (atom nil))

(defn transact
  "Transacts to `db` `tx-data`. `tx-data` must be a collection."
  [db tx-data]
  (when (coll? tx-data)
    (d/transact (unwrap db) {:tx-data tx-data})))

(defn create-db!
  "Creates a DataHike DB using a the `config` map,
   and intializes the DB with `schema`."
  [config & [schema]]
  (d/create-database config)
  (let [db-conn (d/connect config)]
    (when schema
      (transact db-conn schema))
    db-conn))

(defn connect-datahike!
  "Connects to DataHike DB, if it exists, or creates DB
   using a the `config` map and intializes the DB with `schema`.
   Calls `setup-fn` after the connection for migrations, etc."
  [config schema & [setup-fn]]
  (reset! stored-unsafe-attrs (unsafe-attrs schema))
  (let [ds-conn (if (d/database-exists? config)
                  (d/connect config)
                  (create-db! config schema))]
    (when (fn? setup-fn) (setup-fn ds-conn))
    (build-tx-index! ds-conn)
    (d/listen ds-conn :record-tx record-tx)
    ds-conn))

(defn delete-datahike!
  "Deletes the DataHike DB."
  [cfg]
  (d/delete-database cfg)
  (reset! conn nil))

(defn reset-datahike!
  "Removes existing DataHike DB from `config`and `schema`,
   applying `setup-fn` once the setup has completed."
  [config schema & [setup-fn]]
  (when (d/database-exists? config)
    (delete-datahike! config))
  (connect-datahike! config schema setup-fn))

(defn default-conn
  "Creates/connects to DataHike DB from `config`
  `and `schema`."
  [config schema & [setup-fn]]
  (if @conn
    @conn
    (reset! conn
            (connect-datahike! config schema setup-fn))))

(defn release-conn!
  "Releases connection to DataHike."
  []
  (d/release @conn)
  (reset! conn nil))

;;; Sync datoms

(defn sync-datoms
  "Transacts a set of Datoms."
  [db datoms]
  (let [tx-map (group-by #(nth % 3) datoms)]
    (swap! tx-index merge tx-map)
    (transact db (mapv (partial apply datom) datoms))))

(defn export-datoms
  "Returns the set of Datoms in vector format (`[e a v tx]`)
  which are safe to export.

  NOTE: All datoms have a tx-id of the DB's `max-tx` value."
  [db]
  (let [db     (safe-deref db)
        max-tx (:max-tx db)]
    (->> (d/datoms db :eavt)
         (split-datoms)
         (filter #(safe-attr? @stored-unsafe-attrs %))
         (map #(assoc % 3 max-tx)))))

(defn latest-datoms
  "Retrieves the latest transactions since `tx-id`"
  [_ tx-id]
  (->> @tx-index
       (keys)
       (filter (partial < tx-id))
       (mapcat (partial get @tx-index))
       (into [])))

;;; Migrate

(defn- get-existing-schema
  "Retrieves all existing attributes."
  [db]
  (set (d/q '[:find [?ident ...]
              :where [?e :db/ident ?ident]]
            (safe-deref db))))

(defn migrate!
  "Adds any new attributes from `new-schema` to `db`."
  [db new-schema]
  (let [existing-schema (get-existing-schema db)
        new-schema      (as-> (map :db/ident new-schema) $
                          (set $)
                          (apply disj $ existing-schema)
                          (filter #($ (:db/ident %)) new-schema))]
    (when (seq new-schema)
      (print "Migrating DB with new schema: " new-schema)
      (transact db new-schema))))

;;; CRUD Operations

(defn pull
  "Pull from `db` all attributes for entity using `id`.
   Optionally takes a `q` query to execute."
  [db id & [q]]
  (d/pull (safe-deref db) (or q '[*]) id))

(defn pull-many
  "Pull from `db` all attributes for entities using `ids`.
   Optionally takes a `q` query to execute."
  [db ids & [q]]
  (d/pull-many (safe-deref db) (or q '[*]) ids))

(defn create!
  "Creates a new entity in `db` using the `data` map."
  [db data]
  (let [db (unwrap db)]
    (transact db [(assoc data :db/id -1)])))

(defn update!
  "Updates entity in `db` using the `data` map."
  [db data]
  (let [db (unwrap db)]
    (transact db [data])))

(defn delete!
  "Removes entity in `db` using the `data` map.
  `data` must have a `:db/id` key/value pair."
  [db {id :db/id}]
  (let [db (unwrap db)]
    (transact db [[:db/retractEntity id]])))
