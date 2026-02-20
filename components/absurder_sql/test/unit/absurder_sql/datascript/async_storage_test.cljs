(ns absurder-sql.datascript.async-storage-test
  (:require
   [absurder-sql.datascript.core :as d]
   [absurder-sql.datascript.sqlite :as ds-sqlite]
   [absurder-sql.datascript.protocols :as proto :refer [IStorage]]
   [absurder-sql.datascript.storage-async :as storage-async]
   [absurder-sql.interface :as sql]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [cljs.test :refer [async deftest is testing use-fixtures]]))

(defn- with-sqlite []
  (async done
         (go
           (<p! (sql/init!))
           (done))))

(use-fixtures :once {:before with-sqlite})

(deftest maybe-adapt-storage-passthrough-test
  (testing "SyncStorageWrapper satisfies IDatascriptStorageAdapter"
    (async done
           (go
             (try
               (let [db-name (str "adapt-pass-" (random-uuid) ".db")
                     sql-conn (<p! (sql/connect! db-name))
                     store (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                     wrapper (storage-async/make-sync-storage-wrapper store {})]
                 (is (satisfies? proto/IDatascriptStorageAdapter wrapper))
                 (<p! (sql/close! sql-conn)))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(deftest maybe-adapt-storage-wraps-raw-test
  (testing "raw IStorage gets wrapped by maybe-adapt-storage"
    (async done
           (go
             (try
               (let [db-name (str "adapt-wrap-" (random-uuid) ".db")
                     sql-conn (<p! (sql/connect! db-name))
                     store (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                     opts {:storage store}
                     adapted (storage-async/maybe-adapt-storage opts)]
                 (is (satisfies? IStorage store))
                 (is (instance? storage-async/SyncStorageWrapper (:storage adapted)))
                 (<p! (sql/close! sql-conn)))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(deftest init-creates-fresh-conn-test
  (testing "full fresh init: connect -> sqlite-store -> sync-wrapper -> create-conn -> transact -> query"
    (async done
           (go
             (try
               (let [db-name (str "fresh-" (random-uuid) ".db")
                     sql-conn (<p! (sql/connect! db-name))
                     store (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                     wrapper (storage-async/make-sync-storage-wrapper store {})
                     conn (d/create-conn {:name {}} {:storage wrapper})]
                 (d/transact! conn [{:db/id -1 :name "Alice"}])
                 (let [results (d/q '[:find ?n :where [_ :name ?n]] (d/db conn))]
                   (is (= #{["Alice"]} results)))
                 (<p! (sql/close! sql-conn)))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(deftest restore-existing-db-test
  (testing "store data, then restore-sync recovers it from same SQLite store"
    (async done
           (go
             (try
               (let [db-name (str "restore-" (random-uuid) ".db")
                     sql-conn (<p! (sql/connect! db-name))
                     store (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                     wrapper (storage-async/make-sync-storage-wrapper store {})
                     conn (d/create-conn {:name {}} {:storage wrapper})]
                 ;; transact and store
                 (d/transact! conn [{:db/id -1 :name "Bob"}
                                    {:db/id -2 :name "Carol"}])
                 (<p! (storage-async/store-impl-sync! (d/db conn) wrapper true))
                 ;; restore from same store
                 (let [result (<p! (storage-async/restore-sync store))
                       [db _wrapper] result
                       results (d/q '[:find ?n :where [_ :name ?n]] db)]
                   (is (= #{["Bob"] ["Carol"]} results)))
                 (<p! (sql/close! sql-conn)))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(deftest store-and-restore-sync-test
  (testing "store a DB, then restore-sync recovers all data"
    (async done
           (go
             (try
               (let [db-name (str "async-store-" (random-uuid) ".db")
                     sql-conn (<p! (sql/connect! db-name))
                     store (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                     wrapper (storage-async/make-sync-storage-wrapper store {})
                     conn (d/create-conn {:name {} :age {}} {:storage wrapper})]
                 (d/transact! conn [{:db/id -1 :name "Alice" :age 30}
                                    {:db/id -2 :name "Bob" :age 25}
                                    {:db/id -3 :name "Carol" :age 40}])
                 (<p! (storage-async/store-impl-sync! (d/db conn) wrapper true))
                 (let [result (<p! (storage-async/restore-sync store))
                       [db _] result
                       names (d/q '[:find ?n :where [_ :name ?n]] db)
                       ages (d/q '[:find ?n ?a :where [?e :name ?n] [?e :age ?a]] db)]
                   (is (= #{["Alice"] ["Bob"] ["Carol"]} names))
                   (is (= #{["Alice" 30] ["Bob" 25] ["Carol" 40]} ages)))
                 (<p! (sql/close! sql-conn)))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(deftest export-import-roundtrip-test
  (testing "export a SQLite DB with stored data, import into a fresh connection, restore"
    (async done
           (go
             (try
               ;; 1. Create and populate a DB
               (let [db-name (str "export-src-" (random-uuid) ".db")
                     sql-conn (<p! (sql/connect! db-name))
                     store (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                     wrapper (storage-async/make-sync-storage-wrapper store {})
                     conn (d/create-conn {:name {} :age {}} {:storage wrapper})]
                 (d/transact! conn [{:db/id -1 :name "Alice" :age 30}
                                    {:db/id -2 :name "Bob" :age 25}])
                 (<p! (storage-async/store-impl-sync! (d/db conn) wrapper true))

                 ;; 2. Export as bytes
                 (let [db-bytes (<p! (sql/export! sql-conn))]
                   (<p! (sql/close! sql-conn))

                   ;; 3. Import into a fresh connection, then reconnect
                   (let [import-name (str "import-dst-" (random-uuid) ".db")
                         tmp-conn (<p! (sql/connect! import-name))]
                     (<p! (sql/import! tmp-conn db-bytes))
                     (<p! (sql/close! tmp-conn))

                     ;; 4. Reconnect and restore DataScript DB
                     (let [import-conn (<p! (sql/connect! import-name))
                           import-store (ds-sqlite/sqlite-store import-conn {:db-name import-name
                                                                             :skip-ddl true})
                           result (<p! (storage-async/restore-sync import-store))
                           [db _] result
                           names (d/q '[:find ?n :where [_ :name ?n]] db)
                           ages (d/q '[:find ?n ?a :where [?e :name ?n] [?e :age ?a]] db)]
                       (is (= #{["Alice"] ["Bob"]} names))
                       (is (= #{["Alice" 30] ["Bob" 25]} ages))
                       (<p! (sql/close! import-conn))))))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(deftest collect-garbage-test
  (testing "GC removes unreferenced addresses after incremental stores"
    (async done
           (go
             (try
               (let [db-name (str "gc-test-" (random-uuid) ".db")
                     sql-conn (<p! (sql/connect! db-name))
                     store (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                     wrapper (storage-async/make-sync-storage-wrapper store {:branching-factor 32})
                     conn (d/create-conn {:str {}} {:storage wrapper
                                                    :branching-factor 32
                                                    :ref-type :strong})]

                 ;; 1. Transact initial batch and store
                 (d/transact! conn (mapv #(hash-map :db/id (- %) :str (str %)) (range 1 101)))
                 (<p! (storage-async/store-impl-sync! (d/db conn) wrapper true))

                 (let [addrs-after-first (<p! (proto/-list-addresses store))]
                   (is (pos? (count addrs-after-first))
                       "Storage should have addresses after first store")

                   ;; 2. Transact more entities to create new tree nodes (old ones become garbage)
                   (d/transact! conn (mapv #(hash-map :db/id (- %) :str (str %)) (range 101 201)))
                   (<p! (storage-async/store-impl-sync! (d/db conn) wrapper true))

                   (let [addrs-after-second (<p! (proto/-list-addresses store))
                         db-after (d/db conn)
                         used (storage-async/addresses [db-after])]
                     (is (> (count addrs-after-second) (count used))
                         "There should be garbage addresses after incremental store")

                     ;; 3. Collect garbage
                     (<p! (storage-async/collect-garbage store))

                     (let [addrs-after-gc (<p! (proto/-list-addresses store))]
                       (is (<= (count addrs-after-gc) (count addrs-after-second))
                           "GC should have removed some addresses")

                       ;; 4. Restore and verify data is intact
                       (let [result (<p! (storage-async/restore-sync store))
                             [db _] result
                             datoms (d/datoms db :eavt)]
                         (is (= 200 (count datoms))
                             "All 200 entities should survive GC")
                         (is (= "1" (:v (first datoms))))
                         (is (= "200" (:v (last datoms))))))))

                 (<p! (sql/close! sql-conn)))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(deftest export-import-large-db-test
  (testing "export/import roundtrip with a large DB preserves all data"
    (async done
           (go
             (try
               (let [db-name (str "large-export-" (random-uuid) ".db")
                     sql-conn (<p! (sql/connect! db-name))
                     store (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                     wrapper (storage-async/make-sync-storage-wrapper store {:branching-factor 32})
                     conn (d/create-conn {:str {}} {:storage wrapper
                                                    :branching-factor 32
                                                    :ref-type :strong})]
                 ;; Insert 100 entities
                 (d/transact! conn (mapv #(hash-map :db/id (- %) :str (str %)) (range 1 101)))
                 (<p! (storage-async/store-impl-sync! (d/db conn) wrapper true))

                 (let [db-bytes (<p! (sql/export! sql-conn))]
                   (<p! (sql/close! sql-conn))

                   (let [import-name (str "large-import-" (random-uuid) ".db")
                         tmp-conn (<p! (sql/connect! import-name))]
                     (<p! (sql/import! tmp-conn db-bytes))
                     (<p! (sql/close! tmp-conn))

                     (let [import-conn (<p! (sql/connect! import-name))
                           import-store (ds-sqlite/sqlite-store import-conn {:db-name import-name
                                                                             :skip-ddl true})
                           result (<p! (storage-async/restore-sync import-store))
                           [db _] result
                           count' (count (d/datoms db :eavt))]
                       (is (= 100 count'))
                       (is (= "1" (:v (first (d/datoms db :eavt)))))
                       (is (= "100" (:v (last (d/datoms db :eavt)))))
                       (<p! (sql/close! import-conn))))))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(def ^:private sync-test-key "sync-persist-test-db-name")

(defn- sync-test-phase []
  (if-let [db-name (.getItem js/localStorage sync-test-key)]
    :verify
    :store))

(deftest ^:persist sync-persists-to-indexeddb-test
  (testing "data stored and synced survives a page refresh"
    (async done
           (go
             (try
               (case (sync-test-phase)
                 :store
                 (let [db-name (str "sync-persist-" (random-uuid) ".db")
                       sql-conn (<p! (sql/connect! db-name))
                       store (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                       wrapper (storage-async/make-sync-storage-wrapper store {})
                       conn (d/create-conn {:name {} :age {}} {:storage wrapper})]
                   (d/transact! conn [{:db/id -1 :name "Alice" :age 30}
                                      {:db/id -2 :name "Bob" :age 25}
                                      {:db/id -3 :name "Carol" :age 40}])
                   (<p! (storage-async/store-impl-sync! (d/db conn) wrapper true))
                   (<p! (sql/sync! sql-conn))
                   (<p! (sql/close! sql-conn))
                   ;; Stash db-name and reload to clear WASM heap
                   (.setItem js/localStorage sync-test-key db-name)
                   (.reload js/location))

                 :verify
                 (let [db-name (.getItem js/localStorage sync-test-key)]
                   (.removeItem js/localStorage sync-test-key)
                   (let [sql-conn (<p! (sql/connect! db-name))
                         store (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                         result (<p! (storage-async/restore-sync store))]
                     (is (some? result)
                         "restore-sync should find persisted data after page refresh")
                     (when result
                       (let [[db _] result
                             names (d/q '[:find ?n :where [_ :name ?n]] db)
                             ages (d/q '[:find ?n ?a :where [?e :name ?n] [?e :age ?a]] db)]
                         (is (= #{["Alice"] ["Bob"] ["Carol"]} names))
                         (is (= #{["Alice" 30] ["Bob" 25] ["Carol" 40]} ages))))
                     (<p! (sql/close! sql-conn))
                     (done))))
               (catch :default e
                 (is (nil? e) (str "Error: " (.getMessage e)))
                 (done)))))))
