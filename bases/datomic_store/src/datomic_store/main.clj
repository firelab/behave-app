(ns datomic-store.main
  (:require [clojure.set           :refer [map-invert]]
            [clojure.string        :as str]
            [datomic.api           :as d]
            [datom-utils.interface :refer [ref-attrs
                                           safe-attr?
                                           split-datoms
                                           safe-deref
                                           unsafe-attrs]]))

;;; Connection

(defonce ^{:doc "Datomic Connection"} datomic-conn (atom nil))

;;; Helpers

(def unwrap-conn safe-deref)

(defn unwrap-db
  "Returns a Datomic DB instance from a Datomic connection."
  [conn]
  (cond
    (instance? clojure.lang.Atom conn)
    (unwrap-db (safe-deref conn))

    (instance? datomic.db.Db conn)
    conn

    (instance? datomic.peer.Connection conn)
    (d/db conn)))

(defn ->datomic-uri
  "Returns a Datomic Connection URI from a set of configuration parameters, which inclues:
  - `project`  [Required] - Project name within Datomic.
  - `dbtype`   [Optional, Default: 'postgresql'] - Type of DB for JDBC connection.
  - `host`     [Optional, Default: 'localhost'] - Host of JDBC connection.
  - `port`     [Optional, Default: 5432] - Port of JDBC connection.
  - `dbname`   [Optional, Default: 'datomic'] - Database name to connect to.
  - `user`     [Optional, Default: 'datomic'] - Username to the JDBC connection.
  - `password` [Optional, Default: 'datomic'] - Password to the JDBC connection."
  [{:keys [project dbtype host port dbname user password]
                      :or   {dbtype   "postgresql"
                             host     "localhost"
                             port     5432
                             dbname   "datomic"
                             user     "datomic"
                             password "datomic"}}]

  (format "datomic:sql://%s?jdbc:%s://%s:%d/%s?user=%s&password=%s"
          project
          dbtype
          host
          port
          dbname
          user
          password))

;;; Datomic Mapping

(defonce ^:private datomic->ds-eids (atom {}))
(defonce ^:private ds->datomic-eids (atom {}))

(defn load-ds-datomic-mapping
  "Given a set of datoms, create mappings from a DataScript friendly entity
  ID to a Datomic entity ID. These are stored in the above atoms."
  [datoms]
  (let [tx-eids (sort (set (map first datoms)))
        eids-map (into (sorted-map)
                       (zipmap (range 0 (count tx-eids)) tx-eids))]
    (reset! ds->datomic-eids eids-map)
    (reset! datomic->ds-eids (map-invert eids-map))))

(defn- map-datomic->ds-eid
  "Returns a re-mapped datom using the Datomic/DataScript mapping.

  Requires:
  - `ref-attrs` [set<keyword>] Set of attributes of type `db.type/ref`
  - `datom`     [vector]       Vector of the for `[e a v tx op]`"
  [ref-attrs datom]
  (cond-> datom
    ;; Remap entity ID
    :always
    (assoc 0 (get @datomic->ds-eids (first datom)))

    ;; Remap value ID if it is a reference attribute
    (ref-attrs (second datom))
    (assoc 2 (get @datomic->ds-eids (nth datom 2)))))

(defn- map-ds->datomic-eid
  "Returns a re-mapped datom using the Datomic/DataScript mapping.

  Requires:
  - `ref-attrs` [set<keyword>] Set of attributes of type `db.type/ref`
  - `datom`     [vector]       Vector of the for `[e a v tx op]`"
  [ref-attrs datom]
  (cond-> datom
    ;; Remap entity ID
    :always
    (assoc 0 (get @ds->datomic-eids (first datom)))

    ;; Remap value ID if it is a reference attribute
    (ref-attrs (second datom))
    (assoc 2 (get @ds->datomic-eids (nth datom 2)))))

;;; Unsafe Attributes

(defonce ^:private stored-unsafe-attrs (atom nil))

;;; Transaction Index

(defonce ^:private tx-queue (atom '()))
(defonce ^:private tx-index (atom (sorted-map)))

(defn- record-tx [{:keys [tx-data]}]
  (->> tx-data
       (split-datoms)
       (group-by #(nth % 3))
       (swap! tx-index merge)))

(defn- tx-queue-watcher
  [_key _atom _old-state new-state]
  (when-let [tx (first new-state)]
    (record-tx @tx)
    (when (> (count @tx-queue) 10)
      (reset! tx-queue '()))))

(defn- build-tx-index! [conn]
  (let [db (unwrap-db conn)]
    (as-> db %
      (d/datoms % :eavt)
      (split-datoms %)
      (filter #(safe-attr? @stored-unsafe-attrs %) %)
      (group-by (fn [d] (nth d 3)) %)
      (swap! tx-index merge %))))

(defn transact
  "Transacts to `conn` `tx-data`. `tx-data` must be a collection."
  [conn tx-data]
  (when (coll? tx-data)
    (let [tx (d/transact (unwrap-conn conn) tx-data)]
      (swap! tx-queue conj tx)
      tx)))

(defn init-db!
  "Connects to and intializes the DB with `schema`."
  [datomic-uri & [schema]]
  (let [conn (d/connect datomic-uri)]
    (when schema
      (transact conn schema))
    conn))

(defn connect!
  "Connects to DB, if it exists, or creates DB
   using a the `config` map and intializes the DB with `schema`.
   Calls `setup-fn` after the connection for migrations, etc."
  [config schema & [setup-fn]]
  (reset! stored-unsafe-attrs (unsafe-attrs schema))
  (let [datomic-uri (->datomic-uri config)
        conn        (if (d/create-database datomic-uri)
                      (init-db! datomic-uri schema)
                      (d/connect datomic-uri))]
    (when (fn? setup-fn) (setup-fn conn))
    (build-tx-index! conn)
    (add-watch tx-queue :tx-queue-watcher tx-queue-watcher)
    conn))

(defn delete-db!
  "Deletes the DB."
  [config]
  (d/delete-database (->datomic-uri config))
  (reset! datomic-conn nil))

(defn reset-db!
  "Removes existing database, initializes a new DB with `schema`
  and applies an optional `setup-fn` once the setup has completed."
  [config schema & [setup-fn]]
  (when (d/db-stats (->datomic-uri config))
    (delete-db! config))
  (connect! config schema setup-fn))

(defn default-conn
  "Creates/connects to DataHike DB from `config`
  `and `schema`."
  [config schema & [setup-fn]]
  (if @datomic-conn
    @datomic-conn
    (reset! datomic-conn (connect! config schema setup-fn))))

(defn release-conn!
  "Releases connection to DataHike."
  []
  (d/release @datomic-conn)
  (reset! datomic-conn nil))

;;; Sync datoms

(defn max-tx
  "Obtains the maximum tx id from a connection"
  [conn]
  (-> conn
      (d/log)
      (get-in [:tail :txes])
      (last)
      (:data)
      (last)
      (nth 3)))

(defn ->tx
  "Turns a datom into a add/retract tx"
  [datom]
  (let [add-retract (if (last datom) :db/add :db/retract)]
    (apply conj [add-retract] (take 3 datom))))

(defn sync-datoms
  "Transacts a set of Datoms."
  [conn datoms & [ds-mapping? schema]]
  (let [datoms (if ds-mapping?
                 (mapv (partial map-ds->datomic-eid (ref-attrs schema)) datoms)
                 datoms)
        tx-map (group-by #(nth % 3) datoms)]
    (swap! tx-index merge tx-map)
    (transact (unwrap-conn conn)
              (mapv ->tx datoms))))

(defn get-attrs-map
  "Retrieves all attribute's and their ID's."
  [conn]
  (into {} (d/q '[:find ?attr-id ?attr
                  :where [?attr-id :db/ident ?attr]]
                (unwrap-db conn))))

(defn export-datoms
  "Returns the set of Datoms in vector format (`[e a v tx]`)
  which are safe to export.

  NOTE: All datoms have a tx-id of the DB's `max-tx` value."
  [conn & [ds-mappings? schema]]
  (let [db        (unwrap-db conn)
        db-max-tx (max-tx (unwrap-conn conn))
        attrs-map (get-attrs-map conn)
        datoms
        (->> (d/datoms db :eavt)
             (split-datoms)
             (map #(assoc % 1 (get attrs-map (second %))))
             (filter #(safe-attr? @stored-unsafe-attrs %))
             (map #(assoc % 3 db-max-tx)))]
    (if ds-mappings?
      (do
        (load-ds-datomic-mapping datoms)
        (mapv (partial map-datomic->ds-eid (ref-attrs schema)) datoms))
      datoms)))

(defn latest-datoms
  "Retrieves the latest transactions since `tx-id`"
  [_ tx-id & [ds-mapping? schema]]
  (->> @tx-index
       (keys)
       (filter (partial < tx-id))
       (mapcat (partial get @tx-index))
       #(if ds-mapping?
          (mapv (partial map-datomic->ds-eid (ref-attrs schema)) %)
          identity)
       (into [])))

;;; Migrate

(defn- get-existing-schema
  "Retrieves all existing attributes."
  [conn]
  (set (d/q '[:find [?ident ...]
              :where [?e :db/ident ?ident]]
            (unwrap-db conn))))

(defn migrate!
  "Adds any new attributes from `new-schema` to `conn`."
  [conn new-schema]
  (let [existing-schema (get-existing-schema conn)
        new-schema      (as-> (map :db/ident new-schema) $
                          (set $)
                          (apply disj $ existing-schema)
                          (filter #($ (:db/ident %)) new-schema))]
    (when (seq new-schema)
      (println "Migrating DB with new schema: " new-schema)
      (transact conn new-schema))))

;;; CRUD Operations

(defn pull
  "Pull from `conn` all attributes for entity using `id`.
   Optionally takes a `q` query to execute."
  [conn id & [q]]
  (d/pull (unwrap-db conn) (or q '[*]) id))

(defn pull-many
  "Pull from `conn` all attributes for entities using `ids`.
   Optionally takes a `q` query to execute."
  [conn ids & [q]]
  (d/pull-many (unwrap-db conn) (or q '[*]) ids))

(defn q
  "Perform query on Datomic connection."
  [& args]
  (let [[query conn & rest] args]
    (apply d/q query (unwrap-db conn) rest)))

(defn entity
  "Retreive from `conn` the `entity` (map with `:db/id`)."
  [conn {id :db/id}]
  (pull (unwrap-db conn) id '[*]))

(defn create!
  "Creates a new entity in `conn` using the `data` map."
  [conn data]
  (transact conn [(assoc data :db/id -1)]))

(defn update!
  "Updates entity in `conn` using the `data` map."
  [conn data]
  (transact conn [data]))

(defn delete!
  "Removes entity in `conn` using the `data` map.
  `data` must have a `:conn/id` key/value pair."
  [conn {id :db/id}]
  (transact conn [[:db/retractEntity id]]))
