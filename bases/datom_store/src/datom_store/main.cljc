(ns datom-store.main
  (:require [#?(:clj datahike.api   :cljs datascript.core) :as d]
            [#?(:clj datahike.datom :cljs datascript.db) :refer [datom]]
            [ds-schema-utils.interface :refer [->ds-schema]]
            [datom-utils.interface :refer [safe-attr?
                                           safe-deref
                                           split-datoms
                                           unsafe-attrs
                                           unwrap]]))

;;; Helpers

(defn transact [db tx-data]
  (when (coll? tx-data)
    (d/transact (unwrap db) #?(:clj {:tx-data tx-data} :cljs tx-data))))

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

#?(:clj
   (defn create-db! [cfg & [schema]]
     (d/create-database cfg)
     (let [conn (d/connect cfg)]
       (when schema
         (transact conn schema))
       conn)))

#?(:clj
   (defn connect-datahike! [cfg schema & [setup-fn]]
     (reset! stored-unsafe-attrs (unsafe-attrs schema))
     (let [conn (if (d/database-exists? cfg) (d/connect cfg) (create-db! cfg schema))]
       (when (fn? setup-fn) (setup-fn conn))
       (build-tx-index! conn)
       (d/listen conn :record-tx record-tx)
       conn)))

#?(:cljs
   (defn connect-datascript! [schema & [setup-fn]]
     (reset! stored-unsafe-attrs (unsafe-attrs schema))
     (let [conn (d/create-conn (->ds-schema schema))]
       (when (fn? setup-fn) (setup-fn conn))
       (d/listen! conn :record-tx record-tx)
       conn)))

(defn default-conn [schemas & [config]]
  (if @conn
    @conn
    (reset! conn
            #?(:clj (connect-datahike! config schemas)
               :cljs (connect-datascript! schemas)))))

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
