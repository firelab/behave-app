(ns cucumber-test-generator.cartesian-expansion-test
  "Unit tests for cartesian product expansion logic.
   
   These tests do NOT require database access and can be run standalone."
  (:require [clojure.test :refer [deftest is testing]]
            [cucumber-test-generator.generate-scenarios :as gs]))

(deftest test-flatten-conditional-simple
  (testing "flatten-conditional-to-paths handles simple conditional without sub-conditionals"
    (let [conditional {:type :group-variable
                       :operator :equal
                       :values ["true"]
                       :group-variable {:io :output
                                        :path ["Surface" "Fire Behavior"]}}
          result (gs/flatten-conditional-to-paths conditional)]
      (is (= 1 (count result))
          "Should return single path")
      (is (= 1 (count (first result)))
          "Path should contain single conditional"))))

(deftest test-flatten-conditional-with-in-operator
  (testing "flatten-conditional-to-paths expands :in operator with multiple values"
    (let [conditional {:type :group-variable
                       :operator :in
                       :values ["20-Foot" "10-Meter"]
                       :group-variable {:io :input
                                        :path ["Surface" "Wind Measured at"]}}
          result (gs/flatten-conditional-to-paths conditional)]
      (is (= 2 (count result))
          "Should create 2 paths for 2 values")
      (is (= ["20-Foot"] (:values (first (first result))))
          "First path should have '20-Foot' value")
      (is (= ["10-Meter"] (:values (first (second result))))
          "Second path should have '10-Meter' value"))))

(deftest test-flatten-conditional-with-nested-or-sub-conditionals
  (testing "flatten-conditional-to-paths expands nested :or sub-conditionals"
    (let [conditional {:type :group-variable
                       :operator :in
                       :values ["20-Foot" "10-Meter"]
                       :group-variable {:io :input
                                        :path ["Surface" "Wind Measured at"]}
                       :sub-conditionals
                       [{:type :group-variable
                         :operator :equal
                         :values ["true"]
                         :group-variable {:io :output
                                          :path ["Crown" "Rate of Spread"]}}
                        {:type :group-variable
                         :operator :equal
                         :values ["true"]
                         :group-variable {:io :output
                                          :path ["Crown" "Flame Length"]}}]
                       :sub-conditional-operator :or}
          result (gs/flatten-conditional-to-paths conditional)]
      (is (= 4 (count result))
          "Should create 4 paths: 2 values × 2 sub-conditionals")
      (is (every? #(= 2 (count %)) result)
          "Each path should have 2 conditionals (parent + sub)")
      (is (every? (fn [path]
                    (every? #(nil? (:sub-conditionals %)) path))
                  result)
          "All sub-conditionals should be flattened"))))

(deftest test-expand-or-conditionals-with-or-operator
  (testing "expand-or-conditionals creates separate branches for :or operator"
    (let [conditionals-info {:conditionals
                             [{:type :group-variable
                               :operator :equal
                               :values ["true"]
                               :group-variable {:io :output
                                                :path ["Surface" "A"]}}
                              {:type :group-variable
                               :operator :equal
                               :values ["true"]
                               :group-variable {:io :output
                                                :path ["Surface" "B"]}}]
                             :conditionals-operator :or}
          result (gs/expand-or-conditionals conditionals-info)]
      (is (= 2 (count result))
          "Should create 2 branches for :or operator")
      (is (every? sequential? result)
          "Each branch should be a sequence"))))

(deftest test-expand-or-conditionals-with-and-operator
  (testing "expand-or-conditionals combines all conditionals for :and operator"
    (let [conditionals-info {:conditionals
                             [{:type :group-variable
                               :operator :equal
                               :values ["true"]
                               :group-variable {:io :output
                                                :path ["Surface" "A"]}}
                              {:type :group-variable
                               :operator :equal
                               :values ["true"]
                               :group-variable {:io :output
                                                :path ["Surface" "B"]}}]
                             :conditionals-operator :and}
          result (gs/expand-or-conditionals conditionals-info)]
      (is (= 1 (count result))
          "Should create single branch for :and operator")
      (is (= 2 (count (first result)))
          "Branch should contain both conditionals"))))

(deftest test-expand-ancestor-or-branches-cartesian-product
  (testing "expand-ancestor-or-branches creates cartesian product of all ancestor branches"
    (let [ancestors [{:conditionals
                      {:conditionals
                       [{:type :group-variable
                         :operator :equal
                         :values ["true"]
                         :group-variable {:io :output
                                          :path ["Surface" "A"]}}
                        {:type :group-variable
                         :operator :equal
                         :values ["true"]
                         :group-variable {:io :output
                                          :path ["Surface" "B"]}}]
                       :conditionals-operator :or}}
                     {:conditionals
                      {:conditionals
                       [{:type :group-variable
                         :operator :equal
                         :values ["true"]
                         :group-variable {:io :output
                                          :path ["Surface" "C"]}}
                        {:type :group-variable
                         :operator :equal
                         :values ["true"]
                         :group-variable {:io :output
                                          :path ["Surface" "D"]}}]
                       :conditionals-operator :or}}]
          result (gs/expand-ancestor-or-branches ancestors)]
      (is (= 4 (count result))
          "Should create 4 combinations: 2 × 2 = 4")
      (is (every? #(= 2 (count %)) result)
          "Each combination should have 2 conditionals (1 from each ancestor)"))))

(deftest test-wind-adjustment-factor-full-expansion
  (testing "Wind Adjustment Factor example generates 8 ancestor combinations"
    (let [ancestors [{:path ["Surface" "Wind and Slope"]
                      :conditionals
                      {:conditionals
                       [{:type :group-variable
                         :operator :equal
                         :values ["true"]
                         :group-variable {:group-variable/translated-name "Heading"
                                          :io :output
                                          :path ["Surface" "Fire Behavior" "Direction Mode"]}}
                        {:type :group-variable
                         :operator :equal
                         :values ["true"]
                         :group-variable {:group-variable/translated-name "Direction of Interest"
                                          :io :output
                                          :path ["Surface" "Fire Behavior" "Direction Mode"]}}]
                       :conditionals-operator :or}}
                     {:path ["Surface" "Wind and Slope" "Wind Adjustment Factor"]
                      :conditionals
                      {:conditionals
                       [{:type :group-variable
                         :operator :in
                         :values ["20-Foot" "10-Meter"]
                         :group-variable {:group-variable/translated-name "Wind Measured at:"
                                          :io :input
                                          :path ["Surface" "Wind and Slope" "Wind Measured at:"]}
                         :sub-conditionals
                         [{:type :group-variable
                           :operator :equal
                           :values ["true"]
                           :group-variable {:group-variable/translated-name "Rate of Spread"
                                            :io :output
                                            :path ["Crown" "Fire Behavior" "Fire Behavior"]}}
                          {:type :group-variable
                           :operator :equal
                           :values ["true"]
                           :group-variable {:group-variable/translated-name "Flame Length"
                                            :io :output
                                            :path ["Crown" "Fire Behavior" "Fire Behavior"]}}]
                         :sub-conditional-operator :or}]
                       :conditionals-operator :and}}]
          result (gs/expand-ancestor-or-branches ancestors)]
      (is (= 8 (count result))
          "Should create 8 combinations: 2 (ancestor1) × 4 (ancestor2: 2 values × 2 sub-OR) = 8")
      (is (every? #(= 3 (count %)) result)
          "Each combination should have 3 conditionals")
      (is (every? (fn [branch]
                    (every? #(nil? (:sub-conditionals %)) branch))
                  result)
          "All sub-conditionals should be flattened")

      ;; Verify we have both Heading and Direction of Interest
      (let [has-heading? (some (fn [branch]
                                 (some #(= "Heading" (get-in % [:group-variable :group-variable/translated-name]))
                                       branch))
                               result)
            has-direction? (some (fn [branch]
                                   (some #(= "Direction of Interest" (get-in % [:group-variable :group-variable/translated-name]))
                                         branch))
                                 result)]
        (is has-heading? "Should have branches with 'Heading'")
        (is has-direction? "Should have branches with 'Direction of Interest'"))

      ;; Verify we have both wind values
      (let [has-20-foot? (some (fn [branch]
                                 (some #(= ["20-Foot"] (:values %)) branch))
                               result)
            has-10-meter? (some (fn [branch]
                                  (some #(= ["10-Meter"] (:values %)) branch))
                                result)]
        (is has-20-foot? "Should have branches with '20-Foot'")
        (is has-10-meter? "Should have branches with '10-Meter'"))

      ;; Verify we have both crown outputs
      (let [has-ros? (some (fn [branch]
                             (some #(= "Rate of Spread" (get-in % [:group-variable :group-variable/translated-name]))
                                   branch))
                           result)
            has-fl? (some (fn [branch]
                            (some #(= "Flame Length" (get-in % [:group-variable :group-variable/translated-name]))
                                  branch))
                          result)]
        (is has-ros? "Should have branches with 'Rate of Spread'")
        (is has-fl? "Should have branches with 'Flame Length'")))))
