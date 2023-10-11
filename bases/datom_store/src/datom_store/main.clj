(ns datom-store.main
  (:require [datascript.core             :as d]
            [datascript.db               :refer [datom]]
            [me.raynes.fs                :as fs]
            [ds-schema-utils.interface   :refer [->ds-schema]]
            [datascript.storage.sql.core :as storage-sql]
            [datom-utils.interface       :refer [safe-attr?
                                                 safe-deref
                                                 split-datoms
                                                 unsafe-attrs
                                                 unwrap]])
  (:import [java.sql DriverManager]))

;;; Helpers

(defn transact [db tx-data]
  (when (coll? tx-data)
    (d/transact (unwrap db) tx-data)))

;;; Unsafe Attributes

(defonce stored-unsafe-attrs (atom nil))

;;; Transaction Index

(defonce tx-index (atom (sorted-map)))

(defn record-tx [{:keys [tx-data]}]
  (->> tx-data
       (split-datoms)
       (group-by #(nth % 3))
       (swap! tx-index merge)))

(defn build-tx-index! [db]
  (let [db (safe-deref db)]
    (as-> db %
      (d/datoms % :eavt)
      (split-datoms %)
      (filter #(safe-attr? @stored-unsafe-attrs %) %)
      (group-by (fn [d] (nth d 3)) %)
      (swap! tx-index merge %))))

;;; Connection

(defonce conn (atom nil))
(defonce storage (atom nil))

(defn create-storage! [db-file]
  (if @storage
    @storage
    (let [sql-conn (DriverManager/getConnection (format "jdbc:sqlite:%s" db-file))]
      (reset! storage (storage-sql/make sql-conn {:dbtype :sqlite})))))

(defn get-db-file [config]
  (str (fs/expand-home (get-in config [:store :path]))))

(defn create-conn-with-storage! [config schema]
  (let [db-file (get-db-file config)
        exists? (fs/exists? db-file)
        storage (create-storage! db-file)]
    (println [:EXISTS? db-file exists? storage])
    (if exists?
      (d/restore-conn storage)
      (d/create-conn (->ds-schema schema) {:storage storage}))))

(defn connect! [config schema & [setup-fn]]
  (reset! stored-unsafe-attrs (unsafe-attrs schema))
  (let [conn (create-conn-with-storage! config schema)]
    (when (fn? setup-fn) (setup-fn conn))
    (build-tx-index! conn)
    (d/listen! conn :record-tx record-tx)
    conn))

(defn delete-db! [config]
  (when @storage
    (storage-sql/close @storage)
    (reset! storage nil))
  (when @conn 
    (reset! conn nil))
  (fs/delete (get-db-file config)))

(defn reset-db! [config schema & [setup-fn]]
  (when (fs/exists? (get-db-file config))
    (delete-db! config))
  (connect! config schema setup-fn))

(defn default-conn [schemas & [config setup-fn]]
  (if @conn
    @conn
    (reset! conn (connect! config schemas setup-fn))))

(defn release-conn! []
  (when @storage
    (storage-sql/close @storage)
    (reset! storage nil))
  (when @conn 
    (reset! conn nil)))

;;; Sync datoms

(defn sync-datoms
  [db datoms]
  (let [tx-map (group-by #(nth % 3) datoms)]
    (swap! tx-index merge tx-map)
    (transact db (mapv (partial apply datom) datoms))))

(defn export-datoms
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

;;; CRUD Operations

(defn pull [db id & [q]]
  (d/pull (safe-deref db) (or q '[*]) id))

(defn pull-many [db ids & [q]]
  (d/pull-many (safe-deref db) (or q '[*]) ids))

(defn create! [db data]
  (let [db (unwrap db)]
    (transact db [(assoc data :db/id -1)])))

(defn update! [db data]
  (let [db (unwrap db)]
    (transact db [data])))

(defn delete! [db {id :db/id}]
  (let [db (unwrap db)]
    (transact db [[:db/retractEntity id]])))

#_(map first (ns-publics 'datom-store.main))
