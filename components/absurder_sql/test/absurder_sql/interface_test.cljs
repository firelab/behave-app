(ns absurder-sql.interface-test
  (:require [absurder-sql.interface :as sut]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [cljs.test :as t :include-macros true :refer [async deftest is use-fixtures]]))

(defn- with-sqlite []
  (async done 
    (go 
      (<p! (sut/init!))
      (done))))

(use-fixtures :once {:before with-sqlite})

#_(sqlite init-test
  (is (= true (sut/connected?))))

#_(deftest connect-test
  (async done
    (go
      (let [db (<p! (sut/connect! "first.db"))]
        (println [:SQLiteDB db (type db)])
        (is (= js/sqlite.Database (type db)))
        (done)))))

#_(deftest connect-execute-test
  (async done
    (go
      (let [db     (<p! (sut/connect! (str "users-" (rand-int 100) ".db")))
            res1   (<p! (sut/execute! db "CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, first_name TEXT NOT NULL, last_name TEXT NOT NULL, email TEXT UNIQUE NOT NULL, age INTEGER);"))
            res2 (<p! (sut/execute! db "INSERT INTO users (first_name, last_name, email, age) VALUES ('Alice', 'Johnson', 'alice@example.com', 28), ('Bob', 'Smith', 'bob@example.com', 34), ('Carol', 'Davis', 'carol@example.com', 22);"))
            res3 (js->clj (<p! (sut/execute! db "SELECT COUNT(*) FROM users;")) :keywordize-keys true)]
        #_(println [:EXEC-RESULT res1 res2 res3 (get-in res3 [:rows 0 :values 0 :value])])
        (is (= 3 (get-in res3 [:rows 0 :values 0 :value])))
        (done)))))

#_(deftest export-import-db
  (async done
    (go
      (let [db1          (<p! (sut/connect! (str "export-users-" (rand-int 100) ".db")))
            _            (<p! (sut/execute! db1 "CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, first_name TEXT NOT NULL, last_name TEXT NOT NULL, email TEXT UNIQUE NOT NULL, age INTEGER);"))
            _            (<p! (sut/execute! db1 "INSERT INTO users (first_name, last_name, email, age) VALUES ('Alice', 'Johnson', 'alice@example.com', 28), ('Bob', 'Smith', 'bob@example.com', 34), ('Carol', 'Davis', 'carol@example.com', 22);"))
            export-bytes (<p! (sut/export! db1))
            _            (<p! (sut/close! db1))

            ;; Create second DB 
            db2-name (str "import-users-" (rand-int 100) ".db")
            db2 (<p! (sut/connect! db2-name))
            _   (<p! (sut/import! db2 export-bytes))
            db2 (<p! (sut/connect! db2-name))
            res (js->clj (<p! (sut/execute! db2 "SELECT COUNT(*) FROM users;")) :keywordize-keys true)]
        (println [:IMPORT-RESULT res])
        (is (= 3 (get-in res [:rows 0 :values 0 :value])))
        (done)))))

(deftest download-db
  (async done
    (go
      (let [db-name (str "dl-users-" (rand-int 100) ".db")
            db (<p! (sut/connect! db-name))
            _   (<p! (sut/execute! db "CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, first_name TEXT NOT NULL, last_name TEXT NOT NULL, email TEXT UNIQUE NOT NULL, age INTEGER);"))
            _   (<p! (sut/execute! db "INSERT INTO users (first_name, last_name, email, age) VALUES ('Alice', 'Johnson', 'alice@example.com', 28), ('Bob', 'Smith', 'bob@example.com', 34), ('Carol', 'Davis', 'carol@example.com', 22);"))
            _   (<p! (sut/download! db db-name))]
        #_(println [:IMPORT-RESULT res])
        #_(is (= 3 (get-in res [:rows 0 :values 0 :value])))
        (done)))))

(deftest import-db-from-file
  (async done
    (go

      (let [res (<p! (js/fetch ))])


      (let [db1          (<p! (sut/connect! (str "export-users-" (rand-int 100) ".db")))
            _            (<p! (sut/execute! db1 "CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, first_name TEXT NOT NULL, last_name TEXT NOT NULL, email TEXT UNIQUE NOT NULL, age INTEGER);"))
            _            (<p! (sut/execute! db1 "INSERT INTO users (first_name, last_name, email, age) VALUES ('Alice', 'Johnson', 'alice@example.com', 28), ('Bob', 'Smith', 'bob@example.com', 34), ('Carol', 'Davis', 'carol@example.com', 22);"))
            export-bytes (<p! (sut/export! db1))
            _            (<p! (sut/close! db1))

            ;; Create second DB 
            db2-name (str "import-users-" (rand-int 100) ".db")
            db2 (<p! (sut/connect! db2-name))
            _   (<p! (sut/import! db2 export-bytes))
            db2 (<p! (sut/connect! db2-name))
            res (js->clj (<p! (sut/execute! db2 "SELECT COUNT(*) FROM users;")) :keywordize-keys true)]
        (println [:IMPORT-RESULT res])
        (is (= 3 (get-in res [:rows 0 :values 0 :value])))
        (done)))))
