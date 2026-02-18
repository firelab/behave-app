(ns absurder-sql.datascript.async-storage-test
  (:require
   [absurder-sql.datascript.core :as d]
   [absurder-sql.datascript.sqlite :as ds-sqlite]
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

(deftest store-and-restore-sync-test
  (testing "store a DB, then restore-sync recovers all data"
    (async done
           (go
             (try
               (let [db-name  (str "async-store-" (random-uuid) ".db")
                     sql-conn (<p! (sql/connect! db-name))
                     store    (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                     wrapper  (storage-async/make-sync-storage-wrapper store {})
                     conn     (d/create-conn {:name {} :age {}} {:storage wrapper})]
                 (d/transact! conn [{:db/id -1 :name "Alice" :age 30}
                                    {:db/id -2 :name "Bob" :age 25}
                                    {:db/id -3 :name "Carol" :age 40}])
                 (<p! (storage-async/store-impl-sync! (d/db conn) wrapper true))
                 (let [result       (<p! (storage-async/restore-sync store))
                       [db _]       result
                       names        (d/q '[:find ?n :where [_ :name ?n]] db)
                       ages         (d/q '[:find ?n ?a :where [?e :name ?n] [?e :age ?a]] db)]
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
               (let [db-name   (str "export-src-" (random-uuid) ".db")
                     sql-conn  (<p! (sql/connect! db-name))
                     store     (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                     wrapper   (storage-async/make-sync-storage-wrapper store {})
                     conn      (d/create-conn {:name {} :age {}} {:storage wrapper})]
                 (d/transact! conn [{:db/id -1 :name "Alice" :age 30}
                                    {:db/id -2 :name "Bob" :age 25}])
                 (<p! (storage-async/store-impl-sync! (d/db conn) wrapper true))

                 ;; 2. Export as bytes
                 (let [db-bytes (<p! (sql/export! sql-conn))]
                   (<p! (sql/close! sql-conn))

                   ;; 3. Import into a fresh connection, then reconnect
                   (let [import-name (str "import-dst-" (random-uuid) ".db")
                         tmp-conn    (<p! (sql/connect! import-name))]
                     (<p! (sql/import! tmp-conn db-bytes))
                     (<p! (sql/close! tmp-conn))

                     ;; 4. Reconnect and restore DataScript DB
                     (let [import-conn  (<p! (sql/connect! import-name))
                           import-store (ds-sqlite/sqlite-store import-conn {:db-name import-name
                                                                             :skip-ddl true})
                           result       (<p! (storage-async/restore-sync import-store))
                           [db _]       result
                           names        (d/q '[:find ?n :where [_ :name ?n]] db)
                           ages         (d/q '[:find ?n ?a :where [?e :name ?n] [?e :age ?a]] db)]
                       (is (= #{["Alice"] ["Bob"]} names))
                       (is (= #{["Alice" 30] ["Bob" 25]} ages))
                       (<p! (sql/close! import-conn))))))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(deftest export-import-large-db-test
  (testing "export/import roundtrip with a large DB preserves all data"
    (async done
           (go
             (try
               (let [db-name   (str "large-export-" (random-uuid) ".db")
                     sql-conn  (<p! (sql/connect! db-name))
                     store     (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                     wrapper   (storage-async/make-sync-storage-wrapper store {:branching-factor 32})
                     conn      (d/create-conn {:str {}} {:storage wrapper
                                                         :branching-factor 32
                                                         :ref-type :strong})]
                 ;; Insert 100 entities
                 (d/transact! conn (mapv #(hash-map :db/id (- %) :str (str %)) (range 1 101)))
                 (<p! (storage-async/store-impl-sync! (d/db conn) wrapper true))

                 (let [db-bytes (<p! (sql/export! sql-conn))]
                   (<p! (sql/close! sql-conn))

                   (let [import-name (str "large-import-" (random-uuid) ".db")
                         tmp-conn    (<p! (sql/connect! import-name))]
                     (<p! (sql/import! tmp-conn db-bytes))
                     (<p! (sql/close! tmp-conn))

                     (let [import-conn  (<p! (sql/connect! import-name))
                           import-store (ds-sqlite/sqlite-store import-conn {:db-name import-name
                                                                             :skip-ddl true})
                           result       (<p! (storage-async/restore-sync import-store))
                           [db _]       result
                           count'       (count (d/datoms db :eavt))]
                       (is (= 100 count'))
                       (is (= "1" (:v (first (d/datoms db :eavt)))))
                       (is (= "100" (:v (last (d/datoms db :eavt)))))
                       (<p! (sql/close! import-conn))))))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))
