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

(deftest import-bp7-pull-test
  (testing "d/pull retrieves worksheet attributes and component refs"
    (async done
           (go
             (try
               (let [db-bytes (<p! (fetch-bytes "/behave-test.bp7"))
                     db-name  (str "bp7-pull-" (random-uuid) ".db")
                     [db _ sql-conn] (<p! (import-bp7! db-bytes db-name))
                     ws-eid   (ffirst (d/q '[:find ?e :where [?e :worksheet/uuid _]] db))
                     pulled   (d/pull db [:worksheet/uuid
                                          :worksheet/modules
                                          {:worksheet/input-groups [:input-group/group-uuid
                                                                    {:input-group/inputs [:input/value]}]}]
                                      ws-eid)]
                 (is (some? ws-eid)
                     "Should find a worksheet entity")
                 (is (string? (:worksheet/uuid pulled))
                     "Pulled worksheet should have a UUID string")
                 (is (some? (:worksheet/modules pulled))
                     "Pulled worksheet should have modules")
                 (testing "pull traverses component refs"
                   (let [input-groups (:worksheet/input-groups pulled)]
                     (is (seq input-groups)
                         "Worksheet should have input groups")
                     (is (every? :input-group/group-uuid input-groups)
                         "Each input group should have a group-uuid")
                     (is (seq (mapcat :input-group/inputs input-groups))
                         "Input groups should contain inputs")))
                 (<p! (sql/close! sql-conn)))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(deftest import-bp7-entity-test
  (testing "d/entity provides lazy attribute access on imported db"
    (async done
           (go
             (try
               (let [db-bytes (<p! (fetch-bytes "/behave-test.bp7"))
                     db-name  (str "bp7-entity-" (random-uuid) ".db")
                     [db _ sql-conn] (<p! (import-bp7! db-bytes db-name))
                     ws-eid   (ffirst (d/q '[:find ?e :where [?e :worksheet/uuid _]] db))
                     entity   (d/entity db ws-eid)]
                 (is (some? entity)
                     "Entity should exist")
                 (is (= ws-eid (:db/id entity))
                     "Entity :db/id should match lookup eid")
                 (is (string? (:worksheet/uuid entity))
                     "Lazy attribute access should return worksheet UUID")
                 (is (some? (:worksheet/modules entity))
                     "Lazy attribute access should return worksheet modules")

                 (testing "entity navigates component refs"
                   (let [input-groups (:worksheet/input-groups entity)
                         first-ig     (first input-groups)]
                     (is (seq input-groups)
                         "Entity should have input-group refs")
                     (is (string? (:input-group/group-uuid first-ig))
                         "Input group entity should have group-uuid")
                     (is (seq (:input-group/inputs first-ig))
                         "Input group entity should have inputs")))

                 (testing "entity reverse ref navigation"
                   (let [ig-eid (ffirst (d/q '[:find ?e :where [?e :input-group/inputs _]] db))
                         ig     (d/entity db ig-eid)]
                     (is (some? (:worksheet/_input-groups ig))
                         "Reverse ref should navigate back to worksheet")))

                 (<p! (sql/close! sql-conn)))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))

(deftest import-bp7-query-joins-test
  (testing "d/q with joins across worksheet -> input-group -> input"
    (async done
           (go
             (try
               (let [db-bytes (<p! (fetch-bytes "/behave-test.bp7"))
                     db-name  (str "bp7-joins-" (random-uuid) ".db")
                     [db _ sql-conn] (<p! (import-bp7! db-bytes db-name))
                     ;; Join worksheet -> input-groups -> inputs, return input values
                     results (d/q '[:find ?ws-uuid ?gv-uuid ?val
                                    :where
                                    [?ws :worksheet/uuid ?ws-uuid]
                                    [?ws :worksheet/input-groups ?ig]
                                    [?ig :input-group/inputs ?inp]
                                    [?inp :input/group-variable-uuid ?gv-uuid]
                                    [?inp :input/value ?val]]
                                  db)]
                 (is (pos? (count results))
                     "Join query should return results")
                 (is (every? #(= 3 (count %)) results)
                     "Each result should be a 3-tuple")
                 (is (every? #(string? (first %)) results)
                     "Worksheet UUID should be a string")

                 (testing "aggregation query"
                   (let [counts (d/q '[:find ?ws-uuid (count ?inp)
                                       :where
                                       [?ws :worksheet/uuid ?ws-uuid]
                                       [?ws :worksheet/input-groups ?ig]
                                       [?ig :input-group/inputs ?inp]]
                                     db)]
                     (is (pos? (count counts))
                         "Aggregation query should return results")
                     (is (every? #(pos? (second %)) counts)
                         "Each worksheet should have at least one input")))

                 (<p! (sql/close! sql-conn)))
               (catch :default e
                 (is (nil? e) (str "Unexpected error: " e)))
               (finally
                 (done)))))))
