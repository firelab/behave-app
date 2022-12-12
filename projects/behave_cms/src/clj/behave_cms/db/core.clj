(ns behave-cms.db.core
  (:require [clojure.java.io    :as io]
            [datahike.api       :as d]
            [datahike.datom     :refer [datom]]
            [me.raynes.fs       :as fs]
            [behave.schema.core :refer [all-schemas]]
            [behave.db.utils    :refer [safe-attr? safe-deref split-datoms unwrap]]
            [triangulum.config  :refer [get-config]]
            [triangulum.logging :refer [log-str]])
  (:import [java.util UUID]))

;;; Helpers

(defn ->uuid [s]
  (if (uuid? s)
    s
    (UUID/fromString s)))

;;; Connection

(defonce conn (atom nil))

(defonce tx-index (atom (sorted-map)))

(defn add-schemas! [db schemas]
  (doseq [schema schemas]
    (d/transact (unwrap db) {:tx-data schema})))

(defn record-tx [{:keys [tx-data]}]
  (->> tx-data
       (split-datoms)
       (filter safe-attr?)
       (group-by #(nth % 3))
       (swap! tx-index merge)))

(defn connect! [db-path]
  (if @conn
    @conn
    (let [cfg {:store {:backend :file :path db-path}}]
      (when-not (fs/directory? (io/file db-path))
        (log-str "Creating Datahike DB at:" db-path)
        (d/create-database cfg))
      (log-str "Connecting to Datahike DB at:" db-path)
      (reset! conn (d/connect cfg))
      (d/listen @conn :record-tx record-tx)
      @conn)))

(defn default-conn []
  (if @conn
    @conn
    (connect! (get-config :database :path))))

;;; Sync datoms

(defn build-tx-index! [db]
  (let [db (safe-deref db)]
    (as-> db %
      (d/history %)
      (d/filter % safe-attr?)
      (d/datoms % :eavt)
      (split-datoms %)
      (group-by (fn [d] (nth d 3)) %)
      (swap! tx-index merge %))))

(defn sync-datoms [db datoms-coll]
  (log-str "Syncing datoms" datoms-coll)
  (let [tx-map (group-by #(nth % 3) (filter safe-attr? datoms-coll))]
    (swap! tx-index merge tx-map)
    (log-str "New TX index" @tx-index)
    (d/transact db {:tx-data (mapv (partial apply datom) datoms-coll)})))

(defn sync-tx-data [db tx-data]
  (when (coll? tx-data)
    (d/transact db {:tx-data tx-data})))

(defn export-datoms
  [db]
  (let [db     (safe-deref db)
        max-tx (:max-tx db)]
    (->> (d/datoms db :eavt)
        (split-datoms)
        (filter safe-attr?)
        (map #(assoc % 3 max-tx)))))

(defn latest-datoms
  "Retrieves the latest transactions since `tx-id`"
  [db tx-id]
  (when (empty? @tx-index) (build-tx-index! db))
  (->> @tx-index
       (keys)
       (filter (partial < tx-id))
       (mapcat (partial get @tx-index))
       (into [])))

;;; CRUD Operations

(defn get-entity [db {id :db/id}]
  (d/pull (safe-deref db) '[*] id))

(defn create-entity! [db data]
  (let [db (unwrap db)]
    (d/transact db {:tx-data [(assoc data :db/id -1)]})))

(defn update-entity! [db data]
  (let [db (unwrap db)]
    (d/transact db {:tx-data [data]})))

(defn delete-entity! [db {id :db/id}]
  (let [db (unwrap db)]
    (d/transact db {:tx-data [[:db/retractEntity id]]})))

(comment

  (def cfg {:store {:backend :file :path (get-config :database :path)}})

  (reset! conn nil)
  (reset! tx-index nil)
  (d/delete-database cfg)
  (d/create-database cfg)
  (connect! (get-config :database :path))
  (add-schemas! @conn all-schemas)
  (build-tx-index! conn)
  (:max-tx @@conn)

  (sync-tx-data @conn [{:db/id -1 :variable/name "Fire Area"}])

  )
