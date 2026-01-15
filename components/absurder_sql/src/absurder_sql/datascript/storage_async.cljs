(ns absurder-sql.datascript.storage-async
  "Async storage implementation for Promise-based backends like SQLite"
  (:require
   [cljs.reader :as reader]
   [absurder-sql.datascript.db :as db]
   [absurder-sql.datascript.util :as util]
   #_[absurder-sql.datascript.storage :refer [IStorage]]
   [absurder-sql.datascript.protocols :as proto :refer [IStorage]]
   ["../../persistent_sorted_set_js/index.min" :as pss :refer [Branch Leaf PersistentSortedSet RefType Settings]]))

(def ^:private ^:dynamic *store-buffer*)

(defn serializable-datom [d]
  [(.-e d) (.-a d) (.-v d) (.-tx d)])

(def ^:private root-addr 0)
(def ^:private tail-addr 1)

(defonce ^:private *max-addr
  (volatile! 1000000))

(defn- gen-addr []
  (vswap! *max-addr inc))

(deftype AsyncStorageAdapter [^IStorage storage settings cache]
  Object
  ;; Async IStorage interface for PersistentSortedSet
  (restore [_ addr]
    (util/log "async-restore" addr)
    (if-some [cached (get @cache addr)]
      (js/Promise.resolve cached)
      (-> (proto/-restore storage addr)
          (.then (fn [data]
                   (when data
                     (let [{:keys [level keys addresses]} data
                           keys' (to-array (map (fn [[e a v tx]] (db/datom e a v tx)) keys))
                           node (if addresses
                                  (Branch. level (count keys') keys' (to-array addresses) nil settings)
                                  (Leaf. (count keys') keys' settings))]
                       (swap! cache assoc addr node)
                       node)))))))

  (store [_ node]
    (let [addr (gen-addr)
          _    (util/log "async-store" addr)
          keys (mapv serializable-datom (.keys node))
          data (cond-> {:level (.level node)
                        :keys  keys}
                 (instance? Branch node)
                 (assoc :addresses (vec (.addresses node))))]
      (vswap! *store-buffer* conj! [addr data])
      addr))

  (accessed [_ addr]
    ;; Optional: can be used for LRU cache tracking
    nil))

(defn make-async-storage-adapter [^IStorage storage opts]
  (let [branching-factor (or (:branching-factor opts) 512)
        ref-type (or (:ref-type opts) RefType.WEAK)
        settings (Settings. branching-factor ref-type nil)
        cache (atom {})]
    (AsyncStorageAdapter. storage settings cache)))

;; SyncStorageWrapper - bridges async storage with sync PersistentSortedSet
(deftype SyncStorageWrapper [async-adapter cache dirty-addrs settings]
  proto/IPersistentSortedSetStorage
  (restore [_ addr]
    (.restore _ addr))

  (store [_ node]
    (.store _ node))

  (accessed [_ addr]
    (.accessed _ addr))

  Object
  ;; Synchronous interface for PersistentSortedSet
  (restore [_ addr]
    (util/log "sync-restore" addr)
    (if-some [node (get @cache addr)]
      node
      (throw (js/Error. (str "Node not in cache: " addr ". Did you forget to prefetch?")))))

  (store [_ node]
    (let [addr (gen-addr)]
      (util/log "sync-store" addr)
      (swap! cache assoc addr node)
      (swap! dirty-addrs conj addr)
      addr))

  (accessed [_ addr]
    nil))

(defn make-sync-storage-wrapper
  "Create a sync storage wrapper around an async storage backend"
  [^IStorage storage opts]
  (let [branching-factor (or (:branching-factor opts) 512)
        ref-type (or (:ref-type opts) RefType.WEAK)
        settings (Settings. branching-factor ref-type nil)
        async-adapter (AsyncStorageAdapter. storage settings (atom {}))
        cache (atom {})
        dirty-addrs (atom #{})]
    (SyncStorageWrapper. async-adapter cache dirty-addrs settings)))

(defn prefetch-node!
  "Async: Load a single node from async storage into sync cache"
  [wrapper addr]
  (-> (.restore (.-async-adapter wrapper) addr)
      (.then (fn [node]
               (when node
                 (swap! (.-cache wrapper) assoc addr node))
               node))))

(defn prefetch-tree!
  "Async: Recursively load all nodes from root-addr into cache. Returns Promise."
  [wrapper root-addr]
  (js/Promise.
   (fn [resolve reject]
     (let [queue (atom [root-addr])
           visited (atom #{})]
       ((fn process-next []
          (if-let [addr (first @queue)]
            (if (@visited addr)
              (do
                (swap! queue rest)
                (process-next))
              (-> (prefetch-node! wrapper addr)
                  (.then (fn [node]
                           (swap! visited conj addr)
                           (swap! queue rest)
                            ;; If branch node, enqueue children addresses
                           (when (instance? Branch node)
                             (let [child-addrs (filter some? (vec (.-_addresses node)))]
                               (swap! queue concat child-addrs)))
                           (process-next)))
                  (.catch reject)))
            (resolve true))))))))

(defn flush-dirty!
  "Async: Write all dirty nodes back to async storage. Returns Promise."
  [wrapper]
  (let [dirty @(.-dirty-addrs wrapper)
        cache-val @(.-cache wrapper)]
    (if (empty? dirty)
      (js/Promise.resolve true)
      (binding [*store-buffer* (volatile! (transient []))]
        ;; Collect all dirty nodes into store buffer
        (doseq [addr dirty]
          (when-some [node (get cache-val addr)]
            (let [keys (mapv serializable-datom (.keys node))
                  data (cond-> {:level (.level node)
                                :keys  keys}
                         (instance? Branch node)
                         (assoc :addresses (vec (.addresses node))))]
              (vswap! *store-buffer* conj! [addr data]))))
        ;; Store all at once
        (-> (proto/-store (.-storage (.-async-adapter wrapper)) (persistent! @*store-buffer*))
            (.then (fn [_]
                     (reset! (.-dirty-addrs wrapper) #{})
                     true)))))))

(defn maybe-adapt-storage [opts]
  (if-some [^IStorage storage (:storage opts)]
    (update opts :storage make-sync-storage-wrapper opts)
    opts))

(defn storage-adapter [db]
  (when db
    (.-_storage (:eavt db))))

(defn storage [db]
  (when-some [adapter (storage-adapter db)]
    (.-storage adapter)))

;; WeakRef-based storage for remembered DBs
(def ^:private stored-dbs
  #js [])

(defn- remember-db [db]
  (.push stored-dbs (js/WeakRef. db)))

;; Helper to store a sorted set
(defn- store-set [set adapter]
  (.store set adapter))

;; Helper to get settings from a set
(defn- set-settings [set]
  (let [root (.root set)]
    {:branching-factor (.-_branchingFactor (.-_settings root))
     :ref-type (.-_refType (.-_settings root))}))

(defn store-impl!
  "Store DB to async storage. Returns a Promise that resolves to db."
  [db adapter force?]
  (remember-db db)
  (binding [*store-buffer* (volatile! (transient []))]
    (let [eavt-addr (store-set (:eavt db) adapter)
          aevt-addr (store-set (:aevt db) adapter)
          avet-addr (store-set (:avet db) adapter)
          meta (merge
                {:schema   (:schema db)
                 :max-eid  (:max-eid db)
                 :max-tx   (:max-tx db)
                 :eavt     eavt-addr
                 :aevt     aevt-addr
                 :avet     avet-addr
                 :max-addr @*max-addr}
                (set-settings (:eavt db)))]
      (if (or force? (pos? (count @*store-buffer*)))
        (do
          (vswap! *store-buffer* conj! [root-addr meta])
          (vswap! *store-buffer* conj! [tail-addr []])
          (-> (proto/-store (.-storage adapter) (persistent! @*store-buffer*))
              (.then (fn [_] db))))
        (js/Promise.resolve db)))))

(defn store
  "Store DB to async storage. Returns a Promise."
  ([db]
   (if-some [adapter (storage-adapter db)]
     (store-impl! db adapter false)
     (js/Promise.reject (ex-info "Database has no associated storage" {}))))
  ([db storage]
   (if-some [adapter (storage-adapter db)]
     (let [current-storage (.-storage adapter)]
       (if (identical? current-storage storage)
         (store-impl! db adapter false)
         (js/Promise.reject (ex-info "Database is already stored with another IAsyncStorage" {:storage current-storage}))))
     (let [settings (.-_settings (.root (:eavt db)))
           adapter  (AsyncStorageAdapter. storage settings (atom {}))]
       (store-impl! db adapter false)))))

(defn store-tail
  "Store tail to async storage. Returns a Promise."
  [db tail]
  (proto/-store (storage db) [[tail-addr (mapv #(mapv serializable-datom %) tail)]]))

;; Helper to restore a sorted set by address
(defn- restore-set-by [cmp addr adapter opts]
  (let [branching-factor (or (:branching-factor opts) 512)
        ref-type (or (:ref-type opts) RefType.WEAK)
        settings (Settings. branching-factor ref-type nil)]
    (PersistentSortedSet. cmp adapter settings addr nil -1 0)))

(defn restore-impl
  "Restore DB from async storage. Returns a Promise that resolves to [db tail]."
  [^IStorage storage opts]
  (-> (proto/-restore storage root-addr)
      (.then (fn [root]
               (if root
                 (-> (proto/-restore storage tail-addr)
                     (.then (fn [tail]
                              (let [{:keys [schema eavt aevt avet max-eid max-tx max-addr]} root
                                    _       (vswap! *max-addr max max-addr)
                                    opts    (merge root opts)
                                    adapter (make-async-storage-adapter storage opts)
                                    db      (db/restore-db
                                             {:schema  schema
                                              :eavt    (restore-set-by db/cmp-datoms-eavt eavt adapter opts)
                                              :aevt    (restore-set-by db/cmp-datoms-aevt aevt adapter opts)
                                              :avet    (restore-set-by db/cmp-datoms-avet avet adapter opts)
                                              :max-eid max-eid
                                              :max-tx  max-tx})]
                                (remember-db db)
                                [db (mapv #(mapv (fn [[e a v tx]] (db/datom e a v tx)) %) tail)]))))
                 (js/Promise.resolve nil))))))

(defn restore-impl-sync
  "Restore DB from async storage using sync wrapper with prefetch. Returns Promise."
  [^IStorage storage opts]
  (-> (proto/-restore storage root-addr)
      (.then (fn [root]
               (if root
                 (let [{:keys [schema eavt aevt avet max-eid max-tx max-addr]} root
                       _ (vswap! *max-addr max max-addr)
                       opts (merge root opts)
                       wrapper (make-sync-storage-wrapper storage opts)]
                   ;; Prefetch all three index trees
                   (-> (js/Promise.all
                        #js [(prefetch-tree! wrapper eavt)
                             (prefetch-tree! wrapper aevt)
                             (prefetch-tree! wrapper avet)])
                       (.then (fn [_]
                                ;; Now restore tail
                                (-> (proto/-restore storage tail-addr)
                                    (.then (fn [tail]
                                             ;; Create DB with sync wrapper - all nodes are cached
                                             (let [db (db/restore-db
                                                       {:schema  schema
                                                        :eavt    (restore-set-by db/cmp-datoms-eavt eavt wrapper opts)
                                                        :aevt    (restore-set-by db/cmp-datoms-aevt aevt wrapper opts)
                                                        :avet    (restore-set-by db/cmp-datoms-avet avet wrapper opts)
                                                        :max-eid max-eid
                                                        :max-tx  max-tx})]
                                               (remember-db db)
                                               [db (mapv #(mapv (fn [[e a v tx]] (db/datom e a v tx)) %) tail) wrapper]))))))))
                 (js/Promise.resolve nil))))))

(defn store-impl-sync!
  "Store DB using sync wrapper with flush. Returns Promise that resolves to db."
  [db wrapper force?]
  (remember-db db)
  (binding [*store-buffer* (volatile! (transient []))]
    (let [eavt-addr (store-set (:eavt db) wrapper)
          aevt-addr (store-set (:aevt db) wrapper)
          avet-addr (store-set (:avet db) wrapper)
          ;; Extract settings from wrapper instead of calling .root
          settings (.-settings wrapper)
          meta (merge
                {:schema   (:schema db)
                 :max-eid  (:max-eid db)
                 :max-tx   (:max-tx db)
                 :eavt     eavt-addr
                 :aevt     aevt-addr
                 :avet     avet-addr
                 :max-addr @*max-addr
                 :branching-factor (.-_branchingFactor settings)
                 :ref-type (.-_refType settings)})]
      (if (or force? (pos? (count @*store-buffer*)))
        (do
          (vswap! *store-buffer* conj! [root-addr meta])
          (vswap! *store-buffer* conj! [tail-addr []])
          ;; First store buffered nodes
          (-> (proto/-store (.-storage (.-async-adapter wrapper)) (persistent! @*store-buffer*))
              (.then (fn [_]
                       ;; Then flush any remaining dirty nodes
                       (flush-dirty! wrapper)))
              (.then (fn [_] db))))
        (js/Promise.resolve db)))))

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
  "Restore DB from async storage. Returns a Promise that resolves to db."
  ([^IStorage storage]
   (restore storage {}))
  ([^IStorage storage opts]
   (-> (restore-impl storage opts)
       (.then (fn [result]
                (when result
                  (let [[db tail] result]
                    (db-with-tail db tail))))))))

(defn restore-sync
  "Restore DB from async storage using sync wrapper with prefetch.
   All nodes are loaded into memory during initialization.
   Returns Promise that resolves to [db wrapper] tuple."
  ([^IStorage storage]
   (restore-sync storage {}))
  ([^IStorage storage opts]
   (-> (restore-impl-sync storage opts)
       (.then (fn [result]
                (when result
                  (let [[db tail wrapper] result]
                    [(db-with-tail db tail) wrapper])))))))

(defn- addresses-impl [db visit-fn]
  {:pre [(db/db? db)]}
  (.walkAddresses (:eavt db) visit-fn)
  (.walkAddresses (:aevt db) visit-fn)
  (.walkAddresses (:avet db) visit-fn))

(defn addresses [dbs]
  (let [*set     (volatile! (transient #{}))
        visit-fn #(do (vswap! *set conj! %) true)]
    (visit-fn root-addr)
    (visit-fn tail-addr)
    (doseq [db dbs]
      (addresses-impl db visit-fn))
    (persistent! @*set)))

(defn- read-stored-dbs [^IStorage storage']
  (let [res (transient [])]
    (dotimes [i (.-length stored-dbs)]
      (let [ref (aget stored-dbs i)
            db  (.deref ref)]
        (when (and (some? db)
                   (identical? (storage db) storage'))
          (vswap! res conj! db))))
    (persistent! @res)))

(defn collect-garbage
  "Collect garbage from async storage. Returns a Promise."
  [^IStorage storage']
  (-> (restore storage')
      (.then (fn [current-db]
               (let [dbs    (conj (read-stored-dbs storage') current-db)
                     used   (addresses dbs)]
                 (-> (proto/-list-addresses storage')
                     (.then (fn [all]
                              (let [unused (into [] (remove used) all)]
                                (util/log "GC: found" (count dbs) "alive db refs,"
                                          (count used) "used addrs," (count all) "total addrs,"
                                          (count unused) "unused")
                                (proto/-delete storage' unused))))))))))
