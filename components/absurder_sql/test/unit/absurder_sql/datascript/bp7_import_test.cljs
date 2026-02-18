(ns absurder-sql.datascript.bp7-import-test
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

(defn- fetch-bytes
  "Fetch a URL as a Uint8Array. Returns a Promise."
  [url]
  (-> (js/fetch url)
      (.then (fn [resp]
               (when-not (.-ok resp)
                 (throw (js/Error. (str "Failed to fetch " url ": " (.-status resp)))))
               (.arrayBuffer resp)))
      (.then (fn [buf] (js/Uint8Array. buf)))))

(defn- import-bp7!
  "Import bp7 bytes into a fresh SQLite connection and restore DataScript DB.
   Returns a Promise of [db wrapper sql-conn]."
  [db-bytes db-name]
  (-> (sql/connect! db-name)
      (.then (fn [tmp-conn]
               (-> (sql/import! tmp-conn db-bytes)
                   (.then (fn [_] (sql/close! tmp-conn))))))
      (.then (fn [_] (sql/connect! db-name)))
      (.then (fn [fresh-conn]
               (let [store (ds-sqlite/sqlite-store fresh-conn {:db-name  db-name
                                                                :skip-ddl true})]
                 (-> (storage-async/restore-sync store)
                     (.then (fn [[db wrapper]]
                              [db wrapper fresh-conn]))))))))

(deftest import-bp7-restores-datoms-test
  (testing "import a JVM-created .bp7 file and verify datoms are restored"
    (async done
           (go
             (try
               (let [db-bytes (<p! (fetch-bytes "/behave-test.bp7"))
                     db-name  (str "bp7-import-" (random-uuid) ".db")
                     [db _ sql-conn] (<p! (import-bp7! db-bytes db-name))
                     datom-count (count (d/datoms db :eavt))]
                 (is (pos? datom-count)
                     "Restored DB should contain datoms")
                 (is (< 100 datom-count)
                     (str "Expected many datoms from behave bp7, got " datom-count))
                 (<p! (sql/close! sql-conn)))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(deftest import-bp7-schema-test
  (testing "imported bp7 preserves the DataScript schema"
    (async done
           (go
             (try
               (let [db-bytes (<p! (fetch-bytes "/behave-test.bp7"))
                     db-name  (str "bp7-schema-" (random-uuid) ".db")
                     [db _ sql-conn] (<p! (import-bp7! db-bytes db-name))
                     schema   (:schema db)]
                 (is (map? schema)
                     "Schema should be a map")
                 (is (contains? schema :worksheet/uuid)
                     "Schema should contain :worksheet/uuid")
                 (is (contains? schema :input-group/inputs)
                     "Schema should contain :input-group/inputs")
                 (is (= :db.unique/identity
                         (get-in schema [:worksheet/uuid :db/unique]))
                     ":worksheet/uuid should be :db.unique/identity")
                 (<p! (sql/close! sql-conn)))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(deftest import-bp7-queryable-test
  (testing "imported bp7 DB is queryable"
    (async done
           (go
             (try
               (let [db-bytes  (<p! (fetch-bytes "/behave-test.bp7"))
                     db-name   (str "bp7-query-" (random-uuid) ".db")
                     [db _ sql-conn] (<p! (import-bp7! db-bytes db-name))
                     worksheets (d/q '[:find ?uuid
                                       :where [_ :worksheet/uuid ?uuid]]
                                     db)]
                 (is (pos? (count worksheets))
                     "Should find at least one worksheet entity")
                 (<p! (sql/close! sql-conn)))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))
