(ns behave.vms.cache
  "IndexedDB cache of the hydrated VMS DataScript store.

  The VMS data is immutable per layout version (the `?v=<md5>` the server
  stamps on layout.msgpack), so the *hydrated* DB — serialized with
  [[datascript.core/serializable]], which is structured-clone friendly — is
  cached and restored with [[datascript.core/from-serializable]] on the next
  launch. A hit skips the download, the msgpack unpack, and the full
  `d/transact` (the bulk of frontend boot time).

  Layout (names must match `behave.views/data-prefetch-script`, which
  checks the cache from the page head before deciding to fetch):
  - database `behave-vms-cache`, object store `vms`
  - key `version` → the layout.msgpack version string
  - key `db`      → `#js {:schemaHash <int> :value <serializable>}`

  All operations are best-effort: any IndexedDB failure (private browsing,
  quota, blocked upgrade) resolves to nil and the caller falls back to the
  network path."
  (:require [goog.object :as gobj]))

(def ^:private db-name "behave-vms-cache")
(def ^:private store-name "vms")

(defn- open-db
  "Opens the cache database. Resolves with the IDBDatabase, or nil on any
  failure."
  []
  (js/Promise.
   (fn [resolve! _]
     (if-not (exists? js/indexedDB)
       (resolve! nil)
       (try
         (let [req (.open js/indexedDB db-name 1)]
           (set! (.-onupgradeneeded req)
                 (fn [e]
                   (let [idb (.. e -target -result)]
                     (when-not (.contains (.-objectStoreNames idb) store-name)
                       (.createObjectStore idb store-name)))))
           (set! (.-onsuccess req) (fn [e] (resolve! (.. e -target -result))))
           (set! (.-onerror req) (fn [_] (resolve! nil)))
           (set! (.-onblocked req) (fn [_] (resolve! nil))))
         (catch :default _ (resolve! nil)))))))

(defn get-cached
  "Resolves with the cached `#js {:schemaHash ... :value ...}` wrapper when
  the stored version matches `version`, else nil."
  [version]
  (-> (open-db)
      (.then
       (fn [idb]
         (if-not idb
           nil
           (js/Promise.
            (fn [resolve! _]
              (try
                (let [store (-> (.transaction idb store-name)
                                (.objectStore store-name))
                      v-req (.get store "version")]
                  (set! (.-onerror v-req) (fn [_] (.close idb) (resolve! nil)))
                  (set! (.-onsuccess v-req)
                        (fn [_]
                          (if (not= (.-result v-req) version)
                            (do (.close idb) (resolve! nil))
                            (let [d-req (.get (-> (.transaction idb store-name)
                                                  (.objectStore store-name))
                                              "db")]
                              (set! (.-onerror d-req) (fn [_] (.close idb) (resolve! nil)))
                              (set! (.-onsuccess d-req)
                                    (fn [_] (.close idb) (resolve! (.-result d-req)))))))))
                (catch :default _
                  (.close idb)
                  (resolve! nil))))))))
      (.catch (fn [_] nil))))

(defn put!
  "Stores `serialized` (a structured-clone friendly wrapper, see ns doc)
  under `version`, replacing any previous entry. Best-effort; returns a
  Promise that always resolves."
  [version serialized]
  (-> (open-db)
      (.then
       (fn [idb]
         (when idb
           (try
             (let [tx    (.transaction idb store-name "readwrite")
                   store (.objectStore tx store-name)]
               (.put store version "version")
               (.put store serialized "db")
               (set! (.-oncomplete tx) (fn [_] (.close idb)))
               (set! (.-onerror tx) (fn [_] (.close idb))))
             (catch :default _ (.close idb))))))
      (.catch (fn [_] nil))))

(defn cached-wrapper
  "Builds the wrapper object stored under the `db` key."
  [schema-hash serializable]
  #js {:schemaHash schema-hash :value serializable})

(defn wrapper-schema-hash [wrapper] (gobj/get wrapper "schemaHash"))
(defn wrapper-value       [wrapper] (gobj/get wrapper "value"))
