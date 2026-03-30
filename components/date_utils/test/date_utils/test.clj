(ns date-utils.test
  (:require [clojure.test :refer [deftest is testing]]
            [date-utils.interface :as du]))

(deftest today-test
  (testing "returns a date string in yyyy-MM-dd format"
    (let [result (du/today)]
      (is (string? result))
      (is (re-matches #"\d{4}-\d{2}-\d{2}" result))))
  (testing "returns today's date"
    (let [expected (.format (java.time.LocalDateTime/now)
                           (java.time.format.DateTimeFormatter/ofPattern
                            "yyyy-MM-dd"))]
      (is (= expected (du/today))))))
