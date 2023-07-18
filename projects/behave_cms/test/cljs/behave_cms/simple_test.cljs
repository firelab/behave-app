(ns behave-cms.simple-test
  (:require [cljs.test :refer [is deftest testing]]))

(deftest a-simple-test
  (testing "Does this work!"
    (is (= 1 1))))
