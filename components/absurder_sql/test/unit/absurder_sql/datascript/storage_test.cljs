(ns absurder-sql.datascript.storage-test
  (:require
   [absurder-sql.datascript.core :as d]
   [absurder-sql.datascript.protocols :as proto :refer [IStorage]]
   [absurder-sql.datascript.storage :as storage]
   [absurder-sql.interface :as sql]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [cljs.test :refer [async deftest is testing use-fixtures]]
   [clojure.edn :as edn]))

(defn- with-sqlite []
  (async done
         (go
           (<p! (sql/init!))
           (done))))

(use-fixtures :once {:before with-sqlite})

(deftype SimpleStorage [*disk *reads *writes *deletes]
  Object
  IStorage
  (-store [_ addr+data-seq]
    (doseq [[addr data] addr+data-seq]
      (vswap! *disk assoc addr (pr-str data))
      (when *writes
        (vswap! *writes conj addr))))

  (-restore [_ addr]
    (when *reads
      (vswap! *reads conj addr))
    (-> @*disk (get addr) edn/read-string))

  (-list-addresses [_]
    (keys @*disk))

  (-delete [_ addrs-seq]
    (doseq [addr addrs-seq]
      (vswap! *disk dissoc addr)
      (when *deletes
        (vswap! *deletes conj addr)))))

(defn- make-storage [& [_opts]]
  (SimpleStorage. (volatile! {}) (volatile! []) (volatile! []) (volatile! [])))

(defn- reset-stats [storage]
  (vreset! (.-*reads storage) [])
  (vreset! (.-*writes storage) [])
  (vreset! (.-*deletes storage) []))

(def ^:private tail-addr 1)

(extend-type SimpleStorage
  proto/IDatascriptStorageAdapter
  (-ds-store! [this db force?]
    (let [adapter (storage/->StorageAdapter this (.-_settings (.root (:eavt db))))]
      (storage/store-impl! db adapter force?)))
  (-ds-store-tail! [this _db tail]
    (proto/-store this
                  [[tail-addr (mapv #(mapv storage/serializable-datom %) tail)]]))
  (-ds-get-storage [this] this)
  (-restore-impl [this opts]
    (storage/restore-impl this opts))
  (-addresses [_this dbs]
    (storage/addresses dbs))
  (-store-db [this db]
    (storage/store db this))
  (-storage [this] this)
  (-restore-storage [this opts]
    (storage/restore this opts))
  (-collect-garbage [this]
    (storage/collect-garbage this)))

(defn- small-db [& [opts]]
  (-> (d/empty-db nil (merge {:branching-factor 32, :ref-type :strong} opts))
      (d/db-with [[:db/add 1 :name "Ivan"]
                   [:db/add 2 :name "Oleg"]
                   [:db/add 3 :name "Pepper"]])))

(defn- large-db [& [opts]]
  (d/db-with
   (d/empty-db nil (merge {:branching-factor 32, :ref-type :strong} opts))
   (map #(vector :db/add % :str (str %)) (range 1 1001))))

(deftest test-basics
  (testing "empty db"
    (let [db      (d/empty-db)
          storage (make-storage)]
      (d/store storage db)
      (is (= 5 (count @(.-*writes storage))))
      (let [db' (d/restore storage)]
        (is (= 2 (count @(.-*reads storage))))
        (is (= db db'))
        (is (= 3 (count @(.-*reads storage)))))))

  (testing "small db"
    (let [db      (small-db)
          storage (make-storage)]
      (testing "store"
        (d/store storage db)
        (is (= 0 (count @(.-*reads storage))))
        (is (= 5 (count @(.-*writes storage)))))
      (testing "restore"
        (let [db' (d/restore storage)]
          (is (= 2 (count @(.-*reads storage))))
          (is (= db db'))
          (is (= 3 (count @(.-*reads storage))))
          ;; Force lazy index traversal
          (is (seq (vec (d/datoms db' :aevt))))
          (is (= 4 (count @(.-*reads storage)))))

        (testing "count"
          (reset-stats storage)
          (let [db' (d/restore storage)]
            (is (pos? (count db')))
            (is (= 3 (count @(.-*reads storage))))))

        (testing "settings"
          (let [db' (d/restore storage)]
            (is (= {:branching-factor 32, :ref-type :strong} (d/settings db'))))))))

  (testing "large db"
    (let [db      (large-db)
          storage (make-storage)]

      (testing "store"
        (d/store storage db)
        (is (= 135 (count @(.-*writes storage))))

        (d/store storage db)
        (is (= 135 (count @(.-*writes storage)))))

      (testing "restore"
        (let [db' (d/restore storage)]
          (is (= 2 (count @(.-*reads storage))))

          (is (= [1 :str "1"] (-> (d/datoms db' :eavt) first ((juxt :e :a :v)))))
          (is (= 5 (count @(.-*reads storage))))

          ;; Repeated access should be cached
          (is (some? (first (d/datoms db' :eavt))))
          (is (= 5 (count @(.-*reads storage))))

          (is (seq (vec (d/datoms db' :eavt))))
          (is (= 68 (count @(.-*reads storage))))

          ;; Second full traversal uses cache
          (is (seq (vec (d/datoms db' :eavt))))
          (is (= 68 (count @(.-*reads storage))))

          (is (= db db'))
          (is (= (:eavt db) (:eavt db')))
          (is (= (:aevt db) (:aevt db')))
          (is (= (:avet db) (:avet db')))))

      (testing "count"
        (reset-stats storage)
        (let [db' (d/restore storage)]
          (is (= 1000 (count db')))
          (is (= 68 (count @(.-*reads storage))))))

      (testing "incremental store"
        (reset-stats storage)
        (let [db' (d/db-with db [[:db/add 1001 :str "1001"]])]
          (d/store storage db')
          (is (= 8 (count @(.-*writes storage)))))))))

;; Commented out: SQLiteStorage implements IStorage, not IDatascriptStorageAdapter.
;; Use storage-async/store-impl-sync! + restore-sync instead (see async_storage_test).
#_(deftest test-sqlite-storage
    (async done
           (go
             (let [db-name (str "test-storage-" (random-uuid) ".db")
                   db-conn (<p! (sql/connect! db-name))
                   storage (ds-sqlite/sqlite-store db-conn {:db-name db-name})]

               (testing "empty db with SQLite"
                 (let [db (d/empty-db)]
                   (d/store db storage)
                   (let [db' (d/restore storage)]
                     (is (= db db')))))

               (testing "small db with SQLite"
                 (let [db (small-db)]
                   (d/store db storage)
                   (let [db' (d/restore storage)]
                     (is (= db db'))
                     (is (= (:eavt db) (:eavt db')))
                     (is (= (:aevt db) (:aevt db')))
                     (is (= (:avet db) (:avet db'))))))

               (testing "large db with SQLite"
                 (let [db (large-db)]
                   (d/store db storage)
                   (let [db' (d/restore storage)]
                     (is (= db db'))
                     (is (= (:eavt db) (:eavt db')))
                     (is (= (:aevt db) (:aevt db')))
                     (is (= (:avet db) (:avet db'))))))

               (<p! (sql/close! db-conn))
               (done)))))

(deftest test-gc
  (let [storage (make-storage)]
    (let [db (large-db)]
      (d/store storage db)
      (is (= 135 (count (d/addresses storage db))))
      (is (= 135 (count (proto/-list-addresses storage))))
      (is (= (d/addresses storage db) (set (proto/-list-addresses storage))))

      (let [db' (d/db-with db [[:db/add 1001 :str "1001"]])]
        (d/store storage db')
        (let [all-after-second (count (proto/-list-addresses storage))
              used-by-db'      (count (d/addresses storage db'))]
          (is (> all-after-second used-by-db')
              "Incremental store should leave garbage addresses"))))

    (testing "GC cleans up garbage and preserves current db"
      (reset-stats storage)
      (d/collect-garbage storage)
      (let [db' (d/restore storage)]
        (is (= (d/addresses storage db') (set (proto/-list-addresses storage)))
            "After GC, stored addresses should match the current db")
        (is (pos? (count @(.-*deletes storage)))
            "GC should have deleted some addresses")))

    (testing "repeated GC on clean storage is a no-op"
      (reset-stats storage)
      (d/collect-garbage storage)
      (is (pos? (count (proto/-list-addresses storage)))
          "Storage should still have addresses after GC")
      (is (= 0 (count @(.-*deletes storage)))
          "No further deletes on already-clean storage"))))

(deftest test-conn
  (let [storage (make-storage)
        conn    (d/create-conn nil {:storage          storage
                                    :branching-factor 32
                                    :ref-type         :strong})]
    (is (= 5 (count @(.-*writes storage))))

    (d/transact! conn [[:db/add 1 :name "Ivan"]])
    (is (= 6 (count @(.-*writes storage))))

    (d/transact! conn [[:db/add 2 :name "Oleg"]])
    (is (= 7 (count @(.-*writes storage))))
    (is (= 2 (count (:tx-tail @(:atom conn)))))
    (is (= 2 (count (apply concat (:tx-tail @(:atom conn))))))

    (d/transact! conn (mapv #(vector :db/add % :name (str %)) (range 3 33)))
    (is (= 8 (count @(.-*writes storage))))
    (is (= 3 (count (:tx-tail @(:atom conn)))))
    (is (= 32 (count (apply concat (:tx-tail @(:atom conn))))))

    (d/transact! conn [[:db/add 33 :name "Petr"]])
    (is (= 16 (count @(.-*writes storage))))

    (d/transact! conn [[:db/add 34 :name "Anna"]])
    (is (= 17 (count @(.-*writes storage))))

    (let [conn' (d/restore-conn storage)]
      (is (= @conn @conn'))
      (is (= (:max-eid @conn) (:max-eid @conn')))
      (is (= (:max-tx @conn) (:max-tx @conn')))

      (d/transact! conn' [[:db/add 35 :name "Vera"]])
      (is (= 18 (count @(.-*writes storage))))

      (d/transact! conn' (mapv #(vector :db/add % :name (str %)) (range 36 80)))
      (is (= 28 (count @(.-*writes storage))))

      (let [conn'' (d/restore-conn storage)]
        (is (= @conn' @conn''))

        (d/transact! conn'' [[:db/add 80 :name "Ilya"]])
        (is (= 29 (count @(.-*writes storage))))

        (is (> (count (proto/-list-addresses storage))
               (count (d/addresses storage (:db-last-stored @(:atom conn''))))))

        (d/collect-garbage storage)
        (is (= (count (proto/-list-addresses storage))
               (count (d/addresses storage (:db-last-stored @(:atom conn''))))))

        (let [conn''' (d/restore-conn storage)]
          (is (= @conn'' @conn''')))))))

(deftest test-noop-transactions
  (testing "No-op transactions should not trigger storage writes"
    (let [storage (make-storage)
          conn (d/create-conn {:name {:db/unique :db.unique/identity}}
                              {:storage storage})]

      (testing "empty tx-data"
        (reset-stats storage)
        (let [report (d/transact! conn [])]
          (is (empty? (:tx-data report)))
          (is (empty? @(.-*writes storage)))))

      (let [report (d/transact! conn [[:db/add 1 :name "Alice"]])]
        (is (seq (:tx-data report)))
        (is (= 1 (count @(.-*writes storage)))))

      (testing "adding existing fact"
        (reset-stats storage)
        (let [report (d/transact! conn [[:db/add 1 :name "Alice"]])]
          (is (empty? (:tx-data report)))
          (is (empty? @(.-*writes storage)))))

      (let [report (d/transact! conn [[:db/retract 1 :name "Alice"]])]
        (is (seq (:tx-data report)))
        (is (= 1 (count @(.-*writes storage)))))

      (testing "retracting non-existing fact"
        (reset-stats storage)
        (let [report (d/transact! conn [[:db/retract 1 :name "Alice"]])]
          (is (empty? (:tx-data report)))
          (is (empty? @(.-*writes storage))))))))
