(ns behave.pivot-test
  (:require  [cljs.test                       :refer [is deftest testing]]
             [behave.components.results.table :as sut]))

(deftest pivot-test
  (let [data       [{:a 1 :b 2 :c 5 :d 42}
                    {:a 1 :b 2 :c 5 :d 43}
                    {:a 1 :b 3 :c 5 :d 44}
                    {:a 1 :b 3 :c 5 :d 45}
                    {:a 3 :b 4 :c 6 :d 46}]
        pivot-rows [:a :b :c]]
    (testing "sum"
      (is (= (sut/pivot-table-data pivot-rows [[:d :sum]] data)
             [{:a 1, :b 2, :c 5, :d 85}
              {:a 1, :b 3, :c 5, :d 89}
              {:a 3, :b 4, :c 6, :d 46}])))
    (testing "min"
      (is (= (sut/pivot-table-data pivot-rows [[:d :min]] data)
             [{:a 1, :b 2, :c 5, :d 42}
              {:a 1, :b 3, :c 5, :d 44}
              {:a 3, :b 4, :c 6, :d 46}])))
    (testing "max"
      (is (= (sut/pivot-table-data pivot-rows [[:d :min]] data)
             [{:a 1, :b 2, :c 5, :d 43}
              {:a 1, :b 3, :c 5, :d 45}
              {:a 3, :b 4, :c 6, :d 46}])))
    (testing "count"
      (is (= (sut/pivot-table-data pivot-rows [[:d :count]] data)
             [{:a 1, :b 2, :c 5, :d 2}
              {:a 1, :b 3, :c 5, :d 2}
              {:a 3, :b 4, :c 6, :d 1}])))))
