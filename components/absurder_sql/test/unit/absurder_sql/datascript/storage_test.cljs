(ns absurder-sql.datascript.storage-test
  (:require
   [absurder-sql.datascript.core :as d]
   [absurder-sql.datascript.protocols :as proto :refer [IStorage]]
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


