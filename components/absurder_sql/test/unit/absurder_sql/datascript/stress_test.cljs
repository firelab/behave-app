(ns absurder-sql.datascript.stress-test
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

;;; Helpers

(defn- fresh-conn!
  "Connect to a fresh SQLite DB, create a DataScript conn with sync wrapper.
   Returns a Promise of {:sql-conn :conn :wrapper :db-name}."
  [schema opts]
  (let [db-name (str "stress-" (random-uuid) ".db")]
    (-> (sql/connect! db-name)
        (.then (fn [sql-conn]
                 (let [store   (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                       wrapper (storage-async/make-sync-storage-wrapper store opts)
                       conn    (d/create-conn schema {:storage          wrapper
                                                      :branching-factor (or (:branching-factor opts) 512)})]
                   {:sql-conn sql-conn :conn conn :wrapper wrapper :db-name db-name}))))))

(defn- store-and-restore!
  "Store the current DB, then restore from SQLite. Returns Promise of {:db :sql-conn}."
  [{:keys [conn wrapper db-name sql-conn]}]
  (-> (storage-async/store-impl-sync! (d/db conn) wrapper true)
      (.then (fn [_] (sql/close! sql-conn)))
      (.then (fn [_] (sql/connect! db-name)))
      (.then (fn [fresh-conn]
               (let [store (ds-sqlite/sqlite-store fresh-conn {:db-name  db-name
                                                                :skip-ddl true})]
                 (-> (storage-async/restore-sync store)
                     (.then (fn [[db _]] {:db db :sql-conn fresh-conn}))))))))

(defn- timed
  "Execute a 0-arg fn, return Promise of {:result _ :elapsed-ms _}."
  [f]
  (let [t0 (js/performance.now)]
    (-> (js/Promise.resolve (f))
        (.then (fn [result]
                 {:result result :elapsed-ms (- (js/performance.now) t0)})))))

(defn- log! [& args]
  (apply js/console.log "[stress]" (map str args)))

(def ^:private batch-size 10000)

(defn- transact-batched!
  "Transact `n` entities in batches, flushing storage between each batch
   to avoid stack overflow and serializing SQLite writes. Returns a Promise."
  [conn wrapper n]
  (let [batches (partition-all batch-size (range 1 (inc n)))]
    (reduce (fn [p batch]
              (.then p (fn [_]
                         (d/transact! conn (mapv (fn [i] {:db/id (- i) :val (str i)}) batch))
                         (storage-async/store-impl-sync! (d/db conn) wrapper true))))
            (js/Promise.resolve nil)
            batches)))

(defn- run-entity-stress!
  "Transact `n` entities with a single `:val` attr, store, restore, and verify.
   Returns a Promise."
  [n]
  (-> (fresh-conn! {:val {}} {})
      (.then
       (fn [ctx]
         (let [{:keys [conn wrapper]} ctx
               label (str n)]
           (-> (timed #(transact-batched! conn wrapper n))
               (.then (fn [{:keys [elapsed-ms]}]
                        (log! label "transact+store:" (.toFixed elapsed-ms 0) "ms")))
               (.then (fn [_] (timed #(store-and-restore! ctx))))
               (.then (fn [{:keys [result elapsed-ms]}]
                        (let [{:keys [db sql-conn]} result]
                          (log! label "restore:" (.toFixed elapsed-ms 0) "ms")
                          (is (= n (count (d/datoms db :eavt)))
                              (str "Expected " n " datoms"))
                          (sql/close! sql-conn))))))))))

;;; Entity scale tests

(deftest ^:stress stress-1k-test
  (testing "1,000 entities"
    (async done
           (go
             (try
               (<p! (run-entity-stress! 1000))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))

(deftest ^:stress stress-10k-test
  (testing "10,000 entities"
    (async done
           (go
             (try
               (<p! (run-entity-stress! 10000))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))

(deftest ^:stress stress-50k-test
  (testing "50,000 entities"
    (async done
           (go
             (try
               (<p! (run-entity-stress! 50000))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))

(deftest ^:stress stress-100k-test
  (testing "100,000 entities"
    (async done
           (go
             (try
               (<p! (run-entity-stress! 100000))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))

(deftest ^:stress stress-500k-test
  (testing "500,000 entities"
    (async done
           (go
             (try
               (<p! (run-entity-stress! 500000))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))

(deftest ^:stress stress-1m-test
  (testing "1,000,000 entities"
    (async done
           (go
             (try
               (<p! (run-entity-stress! 1000000))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))

;;; Shape tests (wide, large values, cardinality-many, incremental txs)

(deftest ^:stress stress-wide-entities-test
  (testing "100 entities with 50 attributes each"
    (async done
           (go
             (try
               (let [attrs   (mapv #(keyword (str "attr-" %)) (range 50))
                     schema  (into {} (map (fn [a] [a {}]) attrs))
                     ctx     (<p! (fresh-conn! schema {}))
                     {:keys [conn]} ctx
                     tx-data (vec (for [i (range 1 101)]
                                    (into {:db/id (- i)}
                                          (map (fn [a] [a (str (name a) "-" i)]) attrs))))]
                 (d/transact! conn tx-data)
                 (let [{:keys [result]} (<p! (timed #(store-and-restore! ctx)))
                       {:keys [db sql-conn]} result]
                   (is (= 5000 (count (d/datoms db :eavt)))
                       "100 entities * 50 attrs = 5000 datoms")
                   (<p! (sql/close! sql-conn))))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))

(deftest ^:stress stress-large-values-test
  (testing "100 entities with 10KB string values"
    (async done
           (go
             (try
               (let [ctx     (<p! (fresh-conn! {:blob {}} {}))
                     {:keys [conn]} ctx
                     big-str (apply str (repeat 10000 "x"))
                     tx-data (mapv (fn [i] {:db/id (- i) :blob (str i "-" big-str)})
                                   (range 1 101))]
                 (d/transact! conn tx-data)
                 (let [{:keys [result]} (<p! (timed #(store-and-restore! ctx)))
                       {:keys [db sql-conn]} result]
                   (is (= 100 (count (d/datoms db :eavt))))
                   (is (< 10000 (count (:v (first (d/datoms db :eavt))))))
                   (<p! (sql/close! sql-conn))))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))

(deftest ^:stress stress-cardinality-many-test
  (testing "10 entities with 500 cardinality-many values each"
    (async done
           (go
             (try
               (let [ctx     (<p! (fresh-conn! {:tags {:db/cardinality :db.cardinality/many}} {}))
                     {:keys [conn]} ctx
                     tx-data (vec (for [i (range 1 11)]
                                    (into {:db/id (- i)}
                                          [[:tags (mapv #(str "tag-" i "-" %) (range 500))]])))]
                 (d/transact! conn tx-data)
                 (let [{:keys [result]} (<p! (timed #(store-and-restore! ctx)))
                       {:keys [db sql-conn]} result]
                   (is (= 5000 (count (d/datoms db :eavt)))
                       "10 entities * 500 tags = 5000 datoms")
                   (<p! (sql/close! sql-conn))))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))

(deftest ^:stress stress-incremental-transactions-test
  (testing "500 small transactions accumulate correctly"
    (async done
           (go
             (try
               (let [ctx (<p! (fresh-conn! {:counter {}} {}))
                     {:keys [conn]} ctx
                     n 500]
                 (dotimes [i n]
                   (d/transact! conn [{:db/id -1 :counter (str i)}]))
                 (let [{:keys [result]} (<p! (timed #(store-and-restore! ctx)))
                       {:keys [db sql-conn]} result]
                   (is (= n (count (d/datoms db :eavt))))
                   (<p! (sql/close! sql-conn))))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))

(defn- run-export-import-stress!
  "Transact `n` entities, store, export, import into fresh DB, restore, verify.
   Returns a Promise."
  [n]
  (-> (fresh-conn! {:val {}} {})
      (.then
       (fn [ctx]
         (let [{:keys [conn wrapper sql-conn]} ctx
               label (str n)]
           (-> (timed #(transact-batched! conn wrapper n))
               (.then (fn [{:keys [elapsed-ms]}]
                        (log! label "export-import transact+store:" (.toFixed elapsed-ms 0) "ms")))
               (.then (fn [_] (timed #(sql/export! sql-conn))))
               (.then (fn [{:keys [result elapsed-ms]}]
                        (log! label "export:" (.toFixed elapsed-ms 0) "ms,"
                              (.-length result) "bytes")
                        (-> (sql/close! sql-conn)
                            (.then (fn [_] result)))))
               (.then (fn [db-bytes]
                        (let [import-name (str "stress-import-" (random-uuid) ".db")]
                          (-> (sql/connect! import-name)
                              (.then (fn [tmp-conn]
                                       (-> (timed #(sql/import! tmp-conn db-bytes))
                                           (.then (fn [{:keys [elapsed-ms]}]
                                                    (log! label "import:" (.toFixed elapsed-ms 0) "ms")
                                                    (sql/close! tmp-conn))))))
                              (.then (fn [_] (sql/connect! import-name)))
                              (.then (fn [fresh-conn]
                                       (let [store (ds-sqlite/sqlite-store fresh-conn {:db-name  import-name
                                                                                       :skip-ddl true})]
                                         (-> (timed #(storage-async/restore-sync store))
                                             (.then (fn [{:keys [result elapsed-ms]}]
                                                      (let [[db _] result]
                                                        (log! label "import restore:" (.toFixed elapsed-ms 0) "ms")
                                                        (is (= n (count (d/datoms db :eavt)))
                                                            (str "Expected " n " datoms after import"))
                                                        (sql/close! fresh-conn))))))))))))))))))

(deftest ^:stress stress-export-import-10k-test
  (testing "export/import roundtrip at 10k entities"
    (async done
           (go
             (try
               (<p! (run-export-import-stress! 10000))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))

(deftest ^:stress stress-export-import-100k-test
  (testing "export/import roundtrip at 100k entities"
    (async done
           (go
             (try
               (<p! (run-export-import-stress! 100000))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))

(deftest ^:stress stress-export-import-1m-test
  (testing "export/import roundtrip at 1M entities"
    (async done
           (go
             (try
               (<p! (run-export-import-stress! 1000000))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally (done)))))))
