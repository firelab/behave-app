(ns absurder-sql.test
  (:require [cljs.test :as ct :refer-macros [deftest is testing async]]
            [absurder-sql.interface :as absurder-sql]))

(deftest simple-test
  (testing "db function"
    (is (some? (absurder-sql/db :absurder-sql)))))

(deftest execute-test
  (ct/async
   done
   (-> (absurder-sql/db :absurder-sql)
       (.then (fn [db]
                (absurder-sql/execute! db "SELECT 1")))
       (.then (fn [result]
                (is (= "[{\\"1\\":1}]" result))
                (done))))))
