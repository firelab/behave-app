(ns behave.db.core
  (:require [clojure.string :as str]
            #?(:clj [clojure.java.io :as io])
            [#?(:clj datahike.api   :cljs datascript.core) :as d]
            [#?(:clj datahike.datom :cljs datascript.db) :refer [datom]]
            #?(:clj [me.raynes.fs :as fs])
            [behave.schema.core :refer [all-schemas]]
            [behave.db.utils    :refer [safe-attr? safe-split-datoms safe-deref unwrap]]
            #?(:clj [triangulum.config  :refer [get-config]])
            #?(:clj [triangulum.logging :refer [log-str]]))
  #?(:clj (:import [java.util UUID])))

;;; Helpers

(defn log [& s]
  #?(:clj  (apply log-str s)
     :cljs (println (str/join " " s))))

(defn ->uuid [s]
  (if (uuid? s)
    s
    #?(:clj (UUID/fromString s)
       :cljs (uuid s))))

;;; DataScript Helpers

(defn keep-key
  [m k valid-vals]
  (if (contains? valid-vals (get m k))
    m
    (dissoc m k)))

(defn- simplify-schema [schema]
  (-> schema
      (select-keys [:db/ident :db/valueType :db/index :db/unique :db/cardinality :db/tupleAttrs])
      (keep-key :db/valueType #{:db.type/ref :db.type/tuple})
      (keep-key :db/cardinality #{:db.cardinality/many})))

(defn- required-schema? [schema]
  (or (:db/index schema)
      (:db/unique schema)
      (= (:db/cardinality schema) :db.cardinality/many)
      (#{:db.type/ref :db.type/tuple} (:db/valueType schema))))

(defn datascript-schema [schema]
  (->> schema
       (filter required-schema?)
       (map simplify-schema)
       (reduce (fn [acc cur] (assoc acc
                                    (:db/ident cur)
                                    (dissoc cur :db/ident)))
               {})))

(defn datahike-schema [schema]
  (into {} (map-indexed (fn [idx s]
                          [(:db/ident s) (assoc s :db/id (inc idx))])
                        schema)))

(defn transact [db tx-data]
  (when (coll? tx-data)
    (d/transact (unwrap db) #?(:clj {:tx-data tx-data} :cljs tx-data))))

;;; Transaction Index

(defonce tx-index (atom (sorted-map)))

(defn record-tx [{:keys [tx-data]}]
  (->> tx-data
       (safe-split-datoms)
       (group-by #(nth % 3))
       (swap! tx-index merge)))

(defn build-tx-index! [db]
  (let [db (safe-deref db)]
    (as-> db %
      (d/datoms % :eavt)
      (safe-split-datoms %)
      (group-by (fn [d] (nth d 3)) %)
      (swap! tx-index merge %))))

;;; Connection

(defonce conn (atom nil))

#?(:clj
   (defn create-db! [cfg & [schema]]
     (log "Creating Datahike DB with:" cfg)
     (d/create-database cfg)
     (let [conn (d/connect cfg)]
       (when schema
         (d/transact (unwrap conn) {:tx-data schema}))
       conn)))

#?(:clj
   (defn connect-datahike! [cfg schema & [setup-fn]]
     (let [db-path (get-in cfg [:store :path])
           fresh?  (or (nil? db-path) (not (fs/directory? (io/file db-path))))]
       (print "Fresh DB?" fresh?)
       (let [conn (if fresh? (create-db! cfg schema) (d/connect cfg))]
         (when (fn? setup-fn) (setup-fn conn))
         (build-tx-index! conn)
         (d/listen conn :record-tx record-tx)
         conn))))

#?(:cljs
   (defn connect-datascript! [schema & [setup-fn]]
     (let [conn (d/create-conn (datascript-schema schema))]
       (when (fn? setup-fn) (setup-fn conn))
       (d/listen! conn :record-tx record-tx)
       conn)))

(defn default-conn []
  (if @conn
    @conn
    (reset! conn
            #?(:clj (connect-datahike! (get-config :database :config) all-schemas)
               :cljs (connect-datascript! all-schemas)))))

;;; Sync datoms

(defn sync-datoms [db datoms-coll]
  (log "Syncing datoms" datoms-coll)
  (let [tx-map (group-by #(nth % 3) (filter safe-attr? datoms-coll))]
    (swap! tx-index merge tx-map)
    (log "New TX index" @tx-index)
    (d/transact db {:tx-data (mapv (partial apply datom) datoms-coll)})))

(defn sync-tx-data [db tx-data]
  (when (coll? tx-data)
    (d/transact db #?(:clj {:tx-data tx-data} :cljs tx-data))))

(defn export-datoms
  [db]
  (let [db     (safe-deref db)
        max-tx (:max-tx db)]
    (->> (d/datoms db :eavt)
         (safe-split-datoms)
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

  (sync-tx-data @conn [{:db/id -1 :variable/name "Fire Area"}])

  )
