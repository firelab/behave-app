(ns behave.execute-params-test
  "Exploratory tests for SQLite executeWithParams API.
   Verifies what param formats the WASM binding accepts."
  (:require [cljs.test :refer [deftest is async testing] :include-macros true]
            [absurder-sql.interface :as sql]
            [promesa.core :as p]))

;;; Helpers

(defn- fresh-conn!
  "Create a fresh SQLite connection with a test table.
   Returns a Promise of the connection."
  []
  (let [db-name (str "test-params-" (random-uuid) ".db")]
    (-> (sql/connect! db-name)
        (p/then (fn [conn]
                  (-> (sql/execute! conn "create table kv (k integer primary key, v text)")
                      (p/then (fn [_] conn))))))))

(defn- select-all [conn]
  (sql/select conn "select k, v from kv order by k"))

;;; Tests

(deftest ^:integration params-js-array-test
  (testing "executeWithParams with a plain JS array"
    (async done
           (-> (sql/init!)
               (p/then (fn [_] (fresh-conn!)))
               (p/then (fn [conn]
                         (-> (sql/execute-params! conn
                                                  "insert into kv (k, v) values (?, ?)"
                                                  [1 "hello"])
                             (p/then (fn [_] (select-all conn)))
                             (p/then (fn [rows]
                                       (is (= 1 (count rows)))
                                       (is (= {:k 1 :v "hello"} (first rows)))
                                       (sql/close! conn))))))
               (p/then (fn [_] (done)))
               (p/catch (fn [e]
                          (js/console.error "params-js-array-test:" e)
                          (is false (str "params-js-array-test failed: " e))
                          (done)))))))

(deftest ^:integration params-single-quotes-test
  (testing "executeWithParams safely handles single quotes"
    (async done
           (-> (sql/init!)
               (p/then (fn [_] (fresh-conn!)))
               (p/then (fn [conn]
                         (-> (sql/execute-params! conn
                                                  "insert into kv (k, v) values (?, ?)"
                                                  [1 "it's a 'test'"])
                             (p/then (fn [_] (select-all conn)))
                             (p/then (fn [rows]
                                       (is (= "it's a 'test'" (:v (first rows))))
                                       (sql/close! conn))))))
               (p/then (fn [_] (done)))
               (p/catch (fn [e]
                          (js/console.error "params-single-quotes-test:" e)
                          (is false (str "params-single-quotes-test failed: " e))
                          (done)))))))

(deftest ^:integration params-injection-test
  (testing "executeWithParams prevents SQL injection"
    (async done
           (-> (sql/init!)
               (p/then (fn [_] (fresh-conn!)))
               (p/then (fn [conn]
                         (-> (sql/execute-params! conn
                                                  "insert into kv (k, v) values (?, ?)"
                                                  [1 "'; DROP TABLE kv; --"])
                             (p/then (fn [_] (select-all conn)))
                             (p/then (fn [rows]
                                       (is (= 1 (count rows))
                                           "table should still exist with 1 row")
                                       (is (= "'; DROP TABLE kv; --" (:v (first rows)))
                                           "injection payload stored as literal string")
                                       (sql/close! conn))))))
               (p/then (fn [_] (done)))
               (p/catch (fn [e]
                          (js/console.error "params-injection-test:" e)
                          (is false (str "params-injection-test failed: " e))
                          (done)))))))

(deftest ^:integration params-multiple-types-test
  (testing "executeWithParams with integer, text, and null"
    (async done
           (-> (sql/init!)
               (p/then (fn [_] (fresh-conn!)))
               (p/then (fn [conn]
                         (-> (sql/execute-params! conn
                                                  "insert into kv (k, v) values (?, ?)"
                                                  [1 "text value"])
                             (p/then (fn [_]
                                       (sql/execute-params! conn
                                                            "insert into kv (k, v) values (?, ?)"
                                                            [2 nil])))
                             (p/then (fn [_]
                                       (sql/execute-params! conn
                                                            "insert into kv (k, v) values (?, ?)"
                                                            [3 ""])))
                             (p/then (fn [_] (select-all conn)))
                             (p/then (fn [rows]
                                       (is (= 3 (count rows)))
                                       (is (= "text value" (:v (first rows))))
                                       (is (nil? (:v (second rows)))
                                           "nil param should store as NULL")
                                       (is (= "" (:v (nth rows 2)))
                                           "empty string should store as empty string")
                                       (sql/close! conn))))))
               (p/then (fn [_] (done)))
               (p/catch (fn [e]
                          (js/console.error "params-multiple-types-test:" e)
                          (is false (str "params-multiple-types-test failed: " e))
                          (done)))))))

(deftest ^:integration params-select-test
  (testing "executeWithParams works for SELECT queries too"
    (async done
           (-> (sql/init!)
               (p/then (fn [_] (fresh-conn!)))
               (p/then (fn [conn]
                         (-> (sql/execute-params! conn
                                                  "insert into kv (k, v) values (?, ?)"
                                                  [1 "alpha"])
                             (p/then (fn [_]
                                       (sql/execute-params! conn
                                                            "insert into kv (k, v) values (?, ?)"
                                                            [2 "beta"])))
                             (p/then (fn [_]
                                       (sql/select-params conn
                                                          "select k, v from kv where k = ?"
                                                          [2])))
                             (p/then (fn [rows]
                                       (is (= 1 (count rows)))
                                       (is (= {:k 2 :v "beta"} (first rows)))
                                       (sql/close! conn))))))
               (p/then (fn [_] (done)))
               (p/catch (fn [e]
                          (js/console.error "params-select-test:" e)
                          (is false (str "params-select-test failed: " e))
                          (done)))))))

(deftest ^:integration params-upsert-test
  (testing "executeWithParams works with upsert (INSERT ... ON CONFLICT)"
    (async done
           (-> (sql/init!)
               (p/then (fn [_] (fresh-conn!)))
               (p/then (fn [conn]
                         (-> (sql/execute-params! conn
                                                  "insert into kv (k, v) values (?, ?) on conflict(k) do update set v = excluded.v"
                                                  [1 "original"])
                             (p/then (fn [_]
                                       (sql/execute-params! conn
                                                            "insert into kv (k, v) values (?, ?) on conflict(k) do update set v = excluded.v"
                                                            [1 "updated"])))
                             (p/then (fn [_] (select-all conn)))
                             (p/then (fn [rows]
                                       (is (= 1 (count rows)))
                                       (is (= "updated" (:v (first rows))))
                                       (sql/close! conn))))))
               (p/then (fn [_] (done)))
               (p/catch (fn [e]
                          (js/console.error "params-upsert-test:" e)
                          (is false (str "params-upsert-test failed: " e))
                          (done)))))))

(deftest ^:integration params-large-text-test
  (testing "executeWithParams with large pr-str output containing special chars"
    (async done
           (let [;; Simulate what DataScript storage produces via pr-str
                 big-data (pr-str {:schema {:variable/kind {:db/index true}
                                            :bp/uuid {:db/unique :db.unique/identity}}
                                   :nodes [{:id 1 :val "it's complex"}
                                           {:id 2 :val "has 'quotes' and \"doubles\""}
                                           {:id 3 :val "question? marks?"}]
                                   :meta "some' tricky; -- data"})]
             (-> (sql/init!)
                 (p/then (fn [_] (fresh-conn!)))
                 (p/then (fn [conn]
                           (-> (sql/execute-params! conn
                                                    "insert into kv (k, v) values (?, ?)"
                                                    [1 big-data])
                               (p/then (fn [_]
                                         (sql/select-params conn
                                                            "select v from kv where k = ?"
                                                            [1])))
                               (p/then (fn [rows]
                                         (is (= big-data (:v (first rows)))
                                             "large pr-str data should round-trip exactly")
                                         (sql/close! conn))))))
                 (p/then (fn [_] (done)))
                 (p/catch (fn [e]
                            (js/console.error "params-large-text-test:" e)
                            (is false (str "params-large-text-test failed: " e))
                            (done))))))))
