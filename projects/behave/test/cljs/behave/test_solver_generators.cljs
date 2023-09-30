(ns behave.test-solver-generators
  (:require
   [cljs.test :refer [are deftest is testing use-fixtures] :include-macros true]
   [behave.solver.generators :refer [->run-plan
                                     depth
                                     generate-runs
                                     inputs-map-to-vector
                                     permutations]]))

(deftest test-inputs-map-to-vector
  (testing "Converts an inputs map back into vector form."
    ;; Arrange
    (let [m        {:G1 {0 {:GV ["30" 0]}
                         1 {:GV ["50" 1]}}
                    :G2 {0 {:GV2 ["2" nil]}}}
          expected [[:G1 0 :GV "30" 0] [:G1 1 :GV "50" 1] [:G2 0 :GV2 "2" nil]]]


      (is (= (inputs-map-to-vector m) expected)))))

(deftest test-depth
  (testing "Depth returns the # of levels of the map."
    (is (= (depth {:a 1}) 1))
    (is (= (depth {:a {:b 1}}) 2))
    (is (= (depth {:a {:b {:c 1}}}) 3))))

(deftest test-permutations
  (testing "Generate permutations."
    (let [single-input [[:a] [:b] [:c]]
          range-inputs [[[:d] [:e]]
                        [[:g] [:h]]]]
      (is (= (permutations single-input range-inputs)
             '([[:a] [:b] [:c] [:d] [:g]]
               [[:a] [:b] [:c] [:d] [:h]]
               [[:a] [:b] [:c] [:e] [:g]]
               [[:a] [:b] [:c] [:e] [:h]]))))))

(deftest test->run-plan
  (testing "Generates the run-plan for inputs vector."
    (let [inputs-vector [[:G1 0 :GV "30" 0] [:G1 1 :GV "50" 1] [:G2 0 :GV2 "2" :none]]
          expected      {:G1 {0 {:GV ["30" 0]}
                              1 {:GV ["50" 1]}}
                         :G2 {0 {:GV2 ["2" nil]}}}]
      (is (= (->run-plan inputs-vector) expected)))))

(deftest test-generate-runs
  (testing "Generates the runs for "
    (let [inputs-vector [[:G1 0 :GV "30" 0] [:G1 1 :GV "50" 1] [:G2 0 :GV2 "2" :none]]
          expected      {:G1 {0 {:GV ["30" 0]}
                              1 {:GV ["50" 1]}}
                         :G2 {0 {:GV2 ["2" nil]}}}]
      (is (= (->run-plan inputs-vector) expected)))))
