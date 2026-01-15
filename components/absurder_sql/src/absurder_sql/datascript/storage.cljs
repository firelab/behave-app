(ns absurder-sql.datascript.storage
  (:require
    [cljs.reader :as reader]
    [absurder-sql.datascript.db :as db]
    [absurder-sql.datascript.protocols :as proto :refer [IPersistentSortedSetStorage IStorage]]
    [absurder-sql.datascript.storage-async :refer [make-async-storage-adapter]]
    [absurder-sql.datascript.util :as util]
    ["../../persistent_sorted_set_js/index.min" :as pss :refer [Branch Leaf PersistentSortedSet RefType Settings]]))

(def ^:private ^:dynamic *store-buffer*)

(defn serializable-datom [d]
  [(.-e d) (.-a d) (.-v d) (.-tx d)])

(def ^:private root-addr
  0)

(def ^:private tail-addr
  1)

(defonce ^:private *max-addr
  (volatile! 1000000))

(defn- gen-addr []
  (vswap! *max-addr inc))

(deftype StorageAdapter [^IStorage storage settings]
  IPersistentSortedSetStorage
  (restore [this addr]
    (.restore this addr))

  (store [this node]
    (.store this node))

  (accessed [this addr]
    (.accessed this addr))

  Object
  (restore [_ addr]
    (util/log "restore" addr)
    (let [{:keys [level keys addresses]} (proto/-restore storage addr)
          keys' (to-array (map (fn [[e a v tx]] (db/datom e a v tx)) keys))]
      (if addresses
        (Branch. level (count keys') keys' (to-array addresses) nil settings)
        (Leaf. (count keys') keys' settings))))

  (store [_ node]
    (let [addr (gen-addr)
          _    (util/log "store" addr)
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

(defn make-storage-adapter [^IStorage storage opts]
  (let [branching-factor (or (:branching-factor opts) 512)
        ref-type (or (:ref-type opts) RefType.WEAK)
        settings (Settings. branching-factor ref-type nil)]
    (StorageAdapter. storage settings)))

(defn maybe-adapt-storage [opts]
  (if-some [storage (:storage opts)]
    (update opts
            :storage
            (if (:async? opts) make-async-storage-adapter make-storage-adapter)
            opts)
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

(defn store-impl! [db adapter force?]
  ;; Note: In JS/browser, locking is not available, so we skip it
  ;; If running in Node.js with SharedArrayBuffer, you'd need a different approach
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
      (when (or force? (pos? (count @*store-buffer*)))
        (vswap! *store-buffer* conj! [root-addr meta])
        (vswap! *store-buffer* conj! [tail-addr []])
        (let [^IStorage storage (.-storage adapter)]
          (println [:STORAGE storage])
          (println [:STORAGE-KEYS (js-keys storage) (satisfies? IStorage storage)])
          (proto/-store storage (persistent! @*store-buffer*))))
      db)))

(defn store
  ([db]
   (if-some [adapter (storage-adapter db)]
     (store-impl! db adapter false)
     (throw (ex-info "Database has no associated storage" {}))))
  ([db storage]
   (if-some [adapter (storage-adapter db)]
     (let [current-storage (.-storage adapter)]
       (if (identical? current-storage storage)
         (store-impl! db adapter false)
         (throw (ex-info "Database is already stored with another IStorage" {:storage current-storage}))))
     (let [settings (.-_settings (.root (:eavt db)))
           adapter  (StorageAdapter. storage settings)]
       (store-impl! db adapter false)))))

(defn store-tail [db tail]
  (proto/-store (storage db) [[tail-addr (mapv #(mapv serializable-datom %) tail)]]))

;; Helper to restore a sorted set by address
(defn- restore-set-by [cmp addr adapter opts]
  (let [branching-factor (or (:branching-factor opts) 512)
        ref-type (or (:ref-type opts) RefType.WEAK)
        settings (Settings. branching-factor ref-type nil)]
    (PersistentSortedSet. cmp adapter settings addr nil -1 0)))

(defn restore-impl [^IStorage storage opts]
  ;; Note: locking not available in JS
  (when-some [root (proto/-restore storage root-addr)]
    (let [tail    (proto/-restore storage tail-addr)
          {:keys [schema eavt aevt avet max-eid max-tx max-addr]} root
          _       (vswap! *max-addr max max-addr)
          opts    (merge root opts)
          adapter (make-storage-adapter storage opts)
          _       (println [:RESTORED root tail])
          db      (db/restore-db
                    {:schema  schema
                     :eavt    (restore-set-by db/cmp-datoms-eavt eavt adapter opts)
                     :aevt    (restore-set-by db/cmp-datoms-aevt aevt adapter opts)
                     :avet    (restore-set-by db/cmp-datoms-avet avet adapter opts)
                     :max-eid max-eid
                     :max-tx  max-tx})]
      (remember-db db)
      [db (mapv #(mapv (fn [[e a v tx]] (db/datom e a v tx)) %) tail)])))

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
  ([^IStorage storage]
   (restore storage {}))
  ([^IStorage storage opts]
   (let [[db tail] (restore-impl storage opts)]
     (db-with-tail db tail))))

(defn- addresses-impl [db visit-fn]
  {:pre [(db/db? db)]}
  (.walkAddresses (:eavt db) visit-fn)
  (.walkAddresses (:aevt db) visit-fn)
  (.walkAddresses (:avet db) visit-fn))

(defn addresses [dbs]
  (let [*set     (volatile! (transient #{}))
        visit-fn #(do (vswap! *set conj! %) true)] ;; return true to continue
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

(defn collect-garbage [^IStorage storage']
  ;; Note: JS doesn't have System/gc, but WeakRef will handle cleanup automatically
  (let [dbs    (conj
                 (read-stored-dbs storage')
                 (restore storage')) ;; make sure we won't gc currently stored db
        used   (addresses dbs)
        all    (proto/-list-addresses storage')
        unused (into [] (remove used) all)]
    (util/log "GC: found" (count dbs) "alive db refs," (count used) "used addrs," (count all) "total addrs," (count unused) "unused")
    (proto/-delete storage' unused)))

;; Browser/Node.js compatible storage implementations

(defn memory-storage
  "In-memory storage for testing"
  []
  (let [store (atom {})]
    (reify IStorage
      (-store [_ addr+data-seq]
        (doseq [[addr data] addr+data-seq]
          (util/log "memory-store" addr)
          (swap! store assoc addr data)))

      (-restore [_ addr]
        (util/log "memory-restore" addr)
        (get @store addr))

      (-list-addresses [_]
        (keys @store))

      (-delete [_ addrs-seq]
        (doseq [addr addrs-seq]
          (util/log "memory-delete" addr)
          (swap! store dissoc addr))))))

(defn local-storage
  "Browser localStorage-based storage"
  ([]
   (local-storage "datascript"))
  ([prefix]
   (let [addr->key (fn [addr] (str prefix "-" addr))
         key->addr (fn [k] (js/parseInt (.substring k (inc (count prefix)))))]
     (reify IStorage
       (-store [_ addr+data-seq]
         (doseq [[addr data] addr+data-seq]
           (util/log "localStorage-store" addr)
           (js/localStorage.setItem
             (addr->key addr)
             (pr-str data))))

       (-restore [_ addr]
         (util/log "localStorage-restore" addr)
         (when-some [data (js/localStorage.getItem (addr->key addr))]
           (reader/read-string data)))

       (-list-addresses [_]
         (let [len (.-length js/localStorage)]
           (into []
             (comp
               (map #(js/localStorage.key %))
               (filter #(.startsWith % prefix))
               (map key->addr))
             (range len))))

       (-delete [_ addrs-seq]
         (doseq [addr addrs-seq]
           (util/log "localStorage-delete" addr)
           (js/localStorage.removeItem (addr->key addr))))))))

(defn indexed-db-storage
  "IndexedDB-based storage (async operations wrapped in promises)"
  [db-name store-name]
  ;; This is a simplified version - in production you'd want proper async handling
  (let [db-promise (js/Promise.
                     (fn [resolve reject]
                       (let [request (.open js/indexedDB db-name 1)]
                         (set! (.-onupgradeneeded request)
                           (fn [e]
                             (let [db (.-result (.-target e))]
                               (when-not (.contains (.-objectStoreNames db) store-name)
                                 (.createObjectStore db store-name #js {:keyPath "addr"})))))
                         (set! (.-onsuccess request)
                           (fn [e]
                             (resolve (.-result (.-target e)))))
                         (set! (.-onerror request)
                           (fn [e]
                             (reject (.-error (.-target e))))))))]
    (reify IStorage
      (-store [_ addr+data-seq]
        (-> db-promise
            (.then (fn [db]
                     (let [tx (.transaction db #js [store-name] "readwrite")
                           store (.objectStore tx store-name)]
                       (doseq [[addr data] addr+data-seq]
                         (util/log "indexedDB-store" addr)
                         (.put store #js {:addr addr :data (pr-str data)}))
                       (js/Promise.
                         (fn [resolve reject]
                           (set! (.-oncomplete tx) #(resolve nil))
                           (set! (.-onerror tx) #(reject (.-error tx))))))))))

      (-restore [_ addr]
        (util/log "indexedDB-restore" addr)
        (-> db-promise
            (.then (fn [db]
                     (js/Promise.
                       (fn [resolve reject]
                         (let [tx (.transaction db #js [store-name] "readonly")
                               store (.objectStore tx store-name)
                               request (.get store addr)]
                           (set! (.-onsuccess request)
                             (fn [e]
                               (if-some [result (.-result (.-target e))]
                                 (resolve (reader/read-string (.-data result)))
                                 (resolve nil))))
                           (set! (.-onerror request)
                             (fn [e]
                               (reject (.-error (.-target e))))))))))))

      (-list-addresses [_]
        (-> db-promise
            (.then (fn [db]
                     (js/Promise.
                       (fn [resolve reject]
                         (let [tx (.transaction db #js [store-name] "readonly")
                               store (.objectStore tx store-name)
                               request (.getAllKeys store)]
                           (set! (.-onsuccess request)
                             (fn [e]
                               (resolve (vec (.-result (.-target e))))))
                           (set! (.-onerror request)
                             (fn [e]
                               (reject (.-error (.-target e))))))))))))

      (-delete [_ addrs-seq]
        (-> db-promise
            (.then (fn [db]
                     (let [tx (.transaction db #js [store-name] "readwrite")
                           store (.objectStore tx store-name)]
                       (doseq [addr addrs-seq]
                         (util/log "indexedDB-delete" addr)
                         (.delete store addr))
                       (js/Promise.
                         (fn [resolve reject]
                           (set! (.-oncomplete tx) #(resolve nil))
                           (set! (.-onerror tx) #(reject (.-error tx)))))))))))))
