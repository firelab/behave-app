(ns absurder-sql.datascript.storage
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [datascript.db :as db]
    [datascript.util :as util]))

(def ^:private ^:dynamic *store-buffer*)

(defn serializable-datom [^Datom d]
  [(.-e d) (.-a d) (.-v d) (.-tx d)])

(def ^:private root-addr
  0)

(def ^:private tail-addr
  1)

(defonce ^:private *max-addr
  (volatile! 1000000))

(defn- gen-addr []
  (vswap! *max-addr inc))

(defrecord StorageAdapter [storage]
  ISortedSetStorage
  (store [_ ^ANode node]
    (let [addr (gen-addr)
          _    (util/log "store" addr)
          keys (mapv serializable-datom (.keys node))
          data (cond-> {:level (.level node)
                        :keys  keys}
                 (instance? Leaf node)
                 (assoc :addresses (.addresses node)))]
      (vswap! *store-buffer* conj! [addr data])
      addr))
  (restore [_ addr]
    (util/log "restore" addr)
    (let [{:keys [level keys addresses]} (-restore storage addr)
          keys' (map (fn [[e a v tx]] (db/datom e a v tx)) keys)]
      (if addresses
        (BTSet. (int level) keys' addresses)
        (Leaf. keys' settings)))))

(defn make-storage-adapter [storage _opts]
  (let [settings (set/settings)]
    (StorageAdapter. storage settings)))

(defn maybe-adapt-storage [opts]
  (if-some [storage (:storage opts)]
    (update opts :storage make-storage-adapter opts)
    opts))

(defn storage-adapter [db]
  (when db
    (.-_storage ^PersistentSortedSet (:eavt db))))

(defn storage [db]
  (when-some [adapter (storage-adapter db)]
    (:storage adapter)))

(def ^:private stored-dbs (js/Array.))

(defn- remember-db [db]
  (.push stored-dbs (js/WeakRef. db)))

(defn store-impl! [db adapter force?]
  (locking (:storage adapter)
    (remember-db db)
    (binding [*store-buffer* (volatile! (transient []))]
      (let [eavt-addr (set/store (:eavt db) adapter)
            aevt-addr (set/store (:aevt db) adapter)
            avet-addr (set/store (:avet db) adapter)
            meta (merge
                   {:schema   (:schema db)
                    :max-eid  (:max-eid db)
                    :max-tx   (:max-tx db)
                    :eavt     eavt-addr
                    :aevt     aevt-addr
                    :avet     avet-addr
                    :max-addr @*max-addr}
                   (set/settings))]
        (when (or force? (pos? (count @*store-buffer*)))
          (vswap! *store-buffer* conj! [root-addr meta])
          (vswap! *store-buffer* conj! [tail-addr []])
          (-store (:storage adapter) (persistent! @*store-buffer*)))
        db))))

(defn store
  ([db]
   (if-some [adapter (storage-adapter db)]
     (store-impl! db adapter false)
     (throw (ex-info "Database has no associated storage" {}))))
  ([db storage]
   (if-some [adapter (storage-adapter db)]
     (let [current-storage (:storage adapter)]
       (if (identical? current-storage storage)
         (store-impl! db adapter false)
         (throw (ex-info "Database is already stored with another IStorage" {:storage current-storage}))))
     (let [settings (.-_settings ^PersistentSortedSet (:eavt db))
           adapter  (StorageAdapter. storage settings)]
       (store-impl! db adapter false)))))

(defn store-tail [db tail]
  (-store (storage db) [[tail-addr (mapv #(mapv serializable-datom %) tail)]]))

(defn restore-impl [storage opts]
  (locking storage
    (when-some [root (-restore storage root-addr)]
      (let [tail    (-restore storage tail-addr)
            {:keys [schema eavt aevt avet max-eid max-tx max-addr]} root
            _       (vswap! *max-addr max max-addr)
            opts    (merge root opts)
            adapter (make-storage-adapter storage opts)
            db      (db/restore-db
                      {:schema  schema
                       :eavt    (set/restore-by db/cmp-datoms-eavt eavt adapter opts)
                       :aevt    (set/restore-by db/cmp-datoms-aevt aevt adapter opts)
                       :avet    (set/restore-by db/cmp-datoms-avet avet adapter opts)
                       :max-eid max-eid
                       :max-tx  max-tx})]
        (remember-db db)
        [db (mapv #(mapv (fn [[e a v tx]] (db/datom e a v tx)) %) tail)]))))

(defn db-with-tail [db tail]
  (reduce
    (fn [db datoms]
      (if (empty? datoms)
        db
        (as-> db %
          (reduce db/with-datom % datoms)
          (assoc % :max-tx (:tx (first datoms))))))
    db tail))

(defn restore
  ([storage]
   (restore storage {}))
  ([storage opts]
   (let [[db tail] (restore-impl storage opts)]
     (db-with-tail db tail))))

;; TODO FIXME
(defn- addresses-impl [db visit-fn]
  {:pre [(db/db? db)]}
  (let []
    (set/walk-addresses (:eavt db) visit-fn)
    (set/walk-addresses (:aevt db) visit-fn)
    (set/walk-addresses (:avet db) visit-fn)))
  
(defn addresses [dbs]
  (let [*set     (volatile! (transient #{}))
        visit-fn #(vswap! *set conj! %)]
    (visit-fn root-addr)
    (visit-fn tail-addr)
    (doseq [db dbs]
      (addresses-impl db visit-fn))
    (persistent! @*set)))

(defn- read-stored-dbs [storage']
  (let [iter ^Iterator (.iterator stored-dbs)]
    (loop [res (transient [])]
      (if (.hasNext iter)
        (let [ref ^WeakReference (.next iter)
              db  (.get ref)]
          (cond
            (nil? db)
            (do
              (.remove iter)
              (recur res))
            
            (identical? (storage db) storage')
            (recur (conj! res db))
            
            :else
            (recur res)))
        (persistent! res)))))

(defn collect-garbage [storage']
  (let [dbs    (conj
                (read-stored-dbs storage')
                (restore storage')) ;; make sure we won’t gc currently stored db
        used   (addresses dbs)
        all    (-list-addresses storage')
        unused (into [] (remove used) all)]
    (util/log "GC: found" (count dbs) "alive db refs," (count used) "used addrs," (count all) "total addrs," (count unused) "unused")
    (-delete storage' unused)))
