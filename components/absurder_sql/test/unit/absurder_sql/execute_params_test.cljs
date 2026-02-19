(ns absurder-sql.execute-params-test
  "Tests for parameterized SQL execution via executeWithParams.
   Verifies the serde ColumnValue format, type handling, SQL injection
   safety, and upsert patterns used by the DataScript storage layer."
  (:require [absurder-sql.interface :as sql]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [cljs.test :refer [async deftest is use-fixtures] :include-macros true]))

;;; Fixtures

(defn- with-sqlite []
  (async done
    (go
      (<p! (sql/init!))
      (done))))

(use-fixtures :once {:before with-sqlite})

;;; Helpers

(defn- fresh-conn!
  "Create a fresh SQLite connection with a test table."
  []
  (go
    (let [conn (<p! (sql/connect! (str "test-params-" (random-uuid) ".db")))]
      (<p! (sql/execute! conn "create table kv (k integer primary key, v text)"))
      conn)))

(defn- select-all [conn]
  (sql/select conn "select k, v from kv order by k"))

;;; Tests — Type handling

(deftest params-integer-and-text-test
  (async done
    (go
      (let [conn (<p! (fresh-conn!))]
        (<p! (sql/execute-params! conn
                                  "insert into kv (k, v) values (?, ?)"
                                  [1 "hello"]))
        (let [rows (<p! (select-all conn))]
          (is (= 1 (count rows)))
          (is (= {:k 1 :v "hello"} (first rows))))
        (<p! (sql/close! conn))
        (done)))))

(deftest params-null-test
  (async done
    (go
      (let [conn (<p! (fresh-conn!))]
        (<p! (sql/execute-params! conn
                                  "insert into kv (k, v) values (?, ?)"
                                  [1 nil]))
        (let [rows (<p! (select-all conn))]
          (is (= 1 (count rows)))
          (is (nil? (:v (first rows))) "nil param should store as NULL"))
        (<p! (sql/close! conn))
        (done)))))

(deftest params-empty-string-test
  (async done
    (go
      (let [conn (<p! (fresh-conn!))]
        (<p! (sql/execute-params! conn
                                  "insert into kv (k, v) values (?, ?)"
                                  [1 ""]))
        (let [rows (<p! (select-all conn))]
          (is (= "" (:v (first rows))) "empty string should store as empty string"))
        (<p! (sql/close! conn))
        (done)))))

;;; Tests — SQL injection safety

(deftest params-single-quotes-test
  (async done
    (go
      (let [conn (<p! (fresh-conn!))]
        (<p! (sql/execute-params! conn
                                  "insert into kv (k, v) values (?, ?)"
                                  [1 "it's a 'test'"]))
        (let [rows (<p! (select-all conn))]
          (is (= "it's a 'test'" (:v (first rows)))))
        (<p! (sql/close! conn))
        (done)))))

(deftest params-injection-drop-table-test
  (async done
    (go
      (let [conn (<p! (fresh-conn!))]
        (<p! (sql/execute-params! conn
                                  "insert into kv (k, v) values (?, ?)"
                                  [1 "'; DROP TABLE kv; --"]))
        (let [rows (<p! (select-all conn))]
          (is (= 1 (count rows)) "table should still exist with 1 row")
          (is (= "'; DROP TABLE kv; --" (:v (first rows)))
              "injection payload stored as literal string"))
        (<p! (sql/close! conn))
        (done)))))

(deftest params-injection-insert-test
  (async done
    (go
      (let [conn (<p! (fresh-conn!))]
        (<p! (sql/execute-params! conn
                                  "insert into kv (k, v) values (?, ?)"
                                  [1 "abc'; INSERT INTO kv VALUES(999,'hacked'); --"]))
        (let [rows (<p! (select-all conn))]
          (is (= 1 (count rows)) "should have exactly 1 row, not 2")
          (is (nil? (some #(= 999 (:k %)) rows))
              "injected row 999 should not exist"))
        (<p! (sql/close! conn))
        (done)))))

;;; Tests — SELECT with params

(deftest params-select-where-test
  (async done
    (go
      (let [conn (<p! (fresh-conn!))]
        (<p! (sql/execute-params! conn "insert into kv (k, v) values (?, ?)" [1 "alpha"]))
        (<p! (sql/execute-params! conn "insert into kv (k, v) values (?, ?)" [2 "beta"]))
        (let [rows (<p! (sql/select-params conn "select k, v from kv where k = ?" [2]))]
          (is (= 1 (count rows)))
          (is (= {:k 2 :v "beta"} (first rows))))
        (<p! (sql/close! conn))
        (done)))))

;;; Tests — Upsert (the pattern used by DataScript storage)

(deftest params-upsert-test
  (async done
    (go
      (let [conn (<p! (fresh-conn!))]
        (<p! (sql/execute-params!
              conn
              "insert into kv (k, v) values (?, ?) on conflict(k) do update set v = excluded.v"
              [1 "original"]))
        (<p! (sql/execute-params!
              conn
              "insert into kv (k, v) values (?, ?) on conflict(k) do update set v = excluded.v"
              [1 "updated"]))
        (let [rows (<p! (select-all conn))]
          (is (= 1 (count rows)))
          (is (= "updated" (:v (first rows)))))
        (<p! (sql/close! conn))
        (done)))))

;;; Tests — Large pr-str data (simulates DataScript storage content)

(deftest params-large-prstr-test
  (async done
    (go
      (let [big-data (pr-str {:schema {:variable/kind {:db/index true}
                                        :bp/uuid {:db/unique :db.unique/identity}}
                               :nodes [{:id 1 :val "it's complex"}
                                       {:id 2 :val "has 'quotes' and \"doubles\""}
                                       {:id 3 :val "question? marks?"}]
                               :meta "some' tricky; -- data"})
            conn (<p! (fresh-conn!))]
        (<p! (sql/execute-params! conn "insert into kv (k, v) values (?, ?)" [1 big-data]))
        (let [rows (<p! (sql/select-params conn "select v from kv where k = ?" [1]))]
          (is (= big-data (:v (first rows)))
              "large pr-str data should round-trip exactly"))
        (<p! (sql/close! conn))
        (done)))))
