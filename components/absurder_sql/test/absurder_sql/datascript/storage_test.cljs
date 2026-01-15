(ns absurder-sql.datascript.storage-test
  (:require
   [absurder-sql.datascript.core :as d]
   [absurder-sql.datascript.sqlite :as sqlite]
   #_[absurder-sql.datascript.storage :refer [IStorage]]
   [absurder-sql.datascript.protocols :refer [IStorage]]
   [absurder-sql.interface :as sql]
   [cljs.core.async :refer [go <!]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [cljs.test :as t :include-macros true :refer [async deftest is testing use-fixtures]]
   [clojure.edn :as edn]))

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

(defn make-storage [& [opts]]
  (SimpleStorage. (volatile! {}) (volatile! []) (volatile! []) (volatile! [])))

(defn reset-stats [storage]
  (vreset! (.-*reads storage) [])
  (vreset! (.-*writes storage) [])
  (vreset! (.-*deletes storage) []))

(defn small-db [& [opts]]
  (-> (d/empty-db nil (merge {:branching-factor 32, :ref-type :strong} opts))
      (d/db-with [[:db/add 1 :name "Ivan"]
                  [:db/add 2 :name "Oleg"]
                  [:db/add 3 :name "Pepper"]])))

(defn large-db [& [opts]]
  (d/db-with
   (d/empty-db nil (merge {:branching-factor 32, :ref-type :strong} opts))
   (map #(vector :db/add % :str (str %)) (range 1 1001))))

(defn- with-sqlite []
  (async done
         (go
           (<p! (sql/init!))
           (done))))

(use-fixtures :once {:before with-sqlite})

(deftest test-basics
  (testing "empty db"
    (let [db      (d/empty-db)
          storage (make-storage {:stats true})]
      (d/store db storage)
      (is (= 5 (count @(.-*writes storage))))
      (let [db' (d/restore storage)]
        (is (= 2 (count @(.-*reads storage))))
        (is (= db db'))
        (is (= 3 (count @(.-*reads storage)))))))

  (testing "small db"
    (let [db      (small-db)
          storage (make-storage {:stats true})]
      (testing "store"
        (d/store db storage)
        (is (= 0 (count @(.-*reads storage))))
        (is (= 5 (count @(.-*writes storage)))))
      (testing "restore"
        (println [:DISK-BEFORE-RESTORE @(.-*disk storage)])
        (let [db' (d/restore storage)]
          (is (= 2 (count @(.-*reads storage))))
          (is (= db db'))
          (is (= 3 (count @(.-*reads storage))))
          (vec (d/datoms db' :aevt))
          (is (= 4 (count @(.-*reads storage))))
          (vec (d/datoms db' :avet))
          (is (= 5 (count @(.-*reads storage)))))

        (testing "count"
          (reset-stats storage)
          (let [db' (d/restore storage)]
            (count db')
            (is (= 3 (count @(.-*reads storage))))))

        (testing "settings"
          (let [db' (d/restore storage)]
            (is (= {:branching-factor 32, :ref-type :strong} (d/settings db'))))))))

  (testing "large db"
    (let [db      (large-db)
          storage (make-storage {:stats true})]

      (testing "store"
        (d/store db storage)
        (is (= 135 (count @(.-*writes storage))))

        (d/store db storage)
        (is (= 135 (count @(.-*writes storage)))))

      (testing "restore"
        (let [db' (d/restore storage)]
          (is (= 2 (count @(.-*reads storage))))

          (is (= [1 :str "1"] (-> (d/datoms db' :eavt) first ((juxt :e :a :v)))))
          (is (= 5 (count @(.-*reads storage))))

          (first (d/datoms db' :eavt))
          (is (= 5 (count @(.-*reads storage))))

          (vec (d/datoms db' :eavt))
          (is (= 68 (count @(.-*reads storage))))

          (vec (d/datoms db' :eavt))
          (is (= 68 (count @(.-*reads storage))))

          (is (= db db'))
          (is (= (:eavt db) (:eavt db')))
          (is (= (:aevt db) (:aevt db')))
          (is (= (:avet db) (:avet db')))))

      (testing "count"
        (reset-stats storage)
        (let [db' (d/restore storage)]
          (= 1000 (count db'))
          (is (= 68 (count @(.-*reads storage))))))

      (testing "incremental store"
        (reset-stats storage)
        (let [db' (d/db-with db [[:db/add 1001 :str "1001"]])]
          (d/store db' storage)
          (is (= 8 (count @(.-*writes storage)))))))))

#_(deftest test-sqlite-storage
  (async done
         (go
           (let [db-name (str "test-storage-" (random-uuid) ".db")
                 db-conn (<p! (sql/connect! db-name))
                 storage (sqlite/sqlite-store db-conn {:db-name db-name})]

             (testing "empty db with SQLite"
               (let [db (d/empty-db)]
                 (d/store db storage)
                 (let [db' (d/restore storage)]
                   (is (= db db')))))

             #_(testing "small db with SQLite"
               (let [db (small-db)]
                 (d/store db storage)
                 (let [db' (d/restore storage)]
                   (is (= db db'))
                   (is (= (:eavt db) (:eavt db')))
                   (is (= (:aevt db) (:aevt db')))
                   (is (= (:avet db) (:avet db'))))))

             #_(testing "large db with SQLite"
               (let [db (large-db)]
                 (d/store db storage)
                 (let [db' (d/restore storage)]
                   (is (= db db'))
                   (is (= (:eavt db) (:eavt db')))
                   (is (= (:aevt db) (:aevt db')))
                   (is (= (:avet db) (:avet db'))))))

             (<p! (sql/close! db-conn))
             (done)))))

#_(deftest test-gc
  (let [storage (make-storage {:stats true})]
    (let [db (large-db {:storage storage})]
      (d/store db storage)
      (is (= 135 (count (d/addresses db))))
      (is (= 135 (count ((get (meta storage) 'datascript.storage/-list-addresses) storage))))
      (is (= (d/addresses db) (set ((get (meta storage) 'datascript.storage/-list-addresses) storage))))

      (let [db' (d/db-with db [[:db/add 1001 :str "1001"]])]
        (d/store db' storage)
        (is (> (count ((get (meta storage) 'datascript.storage/-list-addresses) storage))
               (count (d/addresses db'))))

        (d/collect-garbage storage)
        (is (= (into (set (d/addresses db))
                     (set (d/addresses db')))
               (set ((get (meta storage) 'datascript.storage/-list-addresses) storage))))
        (is (= 0 (count @(.-*deletes storage))))))

    (let [db'' (d/restore storage)]
      (d/collect-garbage storage)
      (is (= (d/addresses db'') (set ((get (meta storage) 'datascript.storage/-list-addresses) storage))))
      (is (= 6 (count @(.-*deletes storage)))))

    (testing "don't delete currently stored db"
      (d/collect-garbage storage)
      (is (pos? (count ((get (meta storage) 'datascript.storage/-list-addresses) storage)))))))

#_(deftest test-conn
  (let [storage (make-storage {:stats true})
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

        (is (> (count ((get (meta storage) 'datascript.storage/-list-addresses) storage))
               (count (d/addresses (:db-last-stored @(:atom conn''))))))

        (d/collect-garbage storage)
        (is (= (count ((get (meta storage) 'datascript.storage/-list-addresses) storage))
               (count (d/addresses (:db-last-stored @(:atom conn''))))))

        (let [conn''' (d/restore-conn storage)]
          (is (= @conn'' @conn''')))))))

#_(deftest test-noop-transactions
  (testing "No-op transactions should not trigger storage writes"
    (let [storage (make-storage {:stats true})
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

(comment
  (t/run-tests)
  (t/test-ns *ns*))
