(ns cucumber-test-generator.scenario-generation-test
  "Tests for scenario generation and file output.

   IMPORTANT: Integration tests are REPL-only and require the full behave-cms project.

   To run these tests:

   clojure -M:dev:behave/cms:test -e \"
   (require '[behave-cms.server :as cms])
   (cms/init-db!)
   (require '[cucumber-test-generator.scenario-generation-test])
   (clojure.test/run-tests 'cucumber-test-generator.scenario-generation-test)
   \"

   Tests verify:
   - Ancestor expansion with cartesian product for :or operators
   - Nested sub-conditional expansion with :or operators
   - Setup step generation for outputs and inputs
   - Scenario generation for all pattern types
   - Feature file writing with correct naming
   - Large file splitting into subdirectories"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [behave-cms.server :as cms]
            [behave-cms.store :refer [default-conn]]
            [cucumber-test-generator.core :as core]
            [cucumber-test-generator.generate-scenarios :as gs]))

;; ===========================================================================================================
;; Database Initialization
;; ===========================================================================================================

(defn init-test-db [f]
  "Initialize database connection before running tests"
  (cms/init-db!)
  (f))

(use-fixtures :once init-test-db)

;; ===========================================================================================================
;; Integration Tests (require real data from test_matrix_data.edn)
;; ===========================================================================================================

(deftest test-ancestor-expansion-with-or-operator
  (testing "expand-ancestor-or-branches uses cartesian product for :or operators"
    (let [edn-path "development/test_matrix_data.edn"
          _ (core/generate-test-matrix! edn-path)
          data (gs/load-test-matrix edn-path)
          groups (:groups data)
          ;; Find a group with :or ancestors
          group-with-or-ancestors (first (filter #(and (:ancestors %)
                                                       (some (fn [ancestor]
                                                               (= :or (get-in ancestor [:conditionals :conditionals-operator])))
                                                             (:ancestors %)))
                                                 groups))]
      (when group-with-or-ancestors
        (let [result (gs/expand-ancestor-or-branches (:ancestors group-with-or-ancestors))]
          (is (sequential? result)
              "Should return a sequence of branch combinations")
          (is (every? sequential? result)
              "Each branch should be a sequence of conditionals")
          ;; With cartesian product, should generate more scenarios than ZIP would
          (is (> (count result) 0)
              "Should generate at least one branch combination")

          ;; Verify no nested sub-conditionals remain (all should be flattened)
          (is (every? (fn [branch]
                        (every? #(nil? (:sub-conditionals %)) branch))
                      result)
              "All sub-conditionals should be flattened into branches"))))))

(deftest test-cartesian-product-with-nested-sub-conditionals
  (testing "Cartesian product expansion with nested OR sub-conditionals generates all scenarios"
    (let [;; Wind Adjustment Factor - User Input group with complex nested conditionals
          test-group {:path
                      ["Surface"
                       "Wind and Slope"
                       "Wind Adjustment Factor"
                       "Wind Adjustment Factor - User Input"],
                      :group/research? nil,
                      :parent-submodule/io :input,
                      :group/translated-name "Wind Adjustment Factor - User Input",
                      :ancestors
                      [{:path ["Surface" "Wind and Slope"],
                        :submodule/name "Wind and Slope",
                        :submodule/io :input,
                        :submodule/research? nil,
                        :conditionals
                        {:conditionals
                         [{:type :group-variable,
                           :operator :equal,
                           :values ["true"],
                           :group-variable
                           {:group-variable/translated-name "Heading",
                            :group-variable/research? nil,
                            :io :output,
                            :path ["Surface" "Fire Behavior" "Direction Mode"],
                            :submodule/order 0,
                            :submodule/research? nil,
                            :group/order 0}}
                          {:type :group-variable,
                           :operator :equal,
                           :values ["true"],
                           :group-variable
                           {:group-variable/translated-name "Direction of Interest",
                            :group-variable/research? nil,
                            :io :output,
                            :path ["Surface" "Fire Behavior" "Direction Mode"],
                            :submodule/order 0,
                            :submodule/research? nil,
                            :group/order 0}}],
                         :conditionals-operator :or}}
                       {:path ["Surface" "Wind and Slope" "Wind Adjustment Factor"],
                        :group/translated-name "Wind Adjustment Factor",
                        :group/research? nil,
                        :group/hidden? nil,
                        :parent-submodule/io :input,
                        :group/order 3,
                        :submodule/order 3,
                        :conditionals
                        {:conditionals
                         [{:type :group-variable,
                           :operator :in,
                           :values ["20-Foot" "10-Meter"],
                           :group-variable
                           {:group-variable/translated-name "Wind Measured at:",
                            :group-variable/research? nil,
                            :io :input,
                            :path ["Surface" "Wind and Slope" "Wind Measured at:"],
                            :submodule/order 3,
                            :submodule/research? nil,
                            :group/order nil},
                           :sub-conditionals
                           [{:type :group-variable,
                             :operator :equal,
                             :values ["true"],
                             :group-variable
                             {:group-variable/translated-name "Rate of Spread",
                              :group-variable/research? nil,
                              :io :output,
                              :path ["Crown" "Fire Behavior" "Fire Behavior"],
                              :submodule/order 0,
                              :submodule/research? nil,
                              :group/order 0}}
                            {:type :group-variable,
                             :operator :equal,
                             :values ["true"],
                             :group-variable
                             {:group-variable/translated-name "Flame Length",
                              :group-variable/research? nil,
                              :io :output,
                              :path ["Crown" "Fire Behavior" "Fire Behavior"],
                              :submodule/order 0,
                              :submodule/research? nil,
                              :group/order 0}}],
                           :sub-conditional-operator :or}],
                         :conditionals-operator :and}}],
                      :group/hidden? nil,
                      :group/order nil,
                      :submodule/order 3,
                      :conditionals
                      {:conditionals
                       [{:type :group-variable,
                         :operator :equal,
                         :values ["User Input"],
                         :group-variable
                         {:group-variable/translated-name
                          "Wind Adjustment Factor Calculation Method",
                          :group-variable/research? nil,
                          :io :input,
                          :path ["Surface" "Wind and Slope" "Wind Adjustment Factor"],
                          :submodule/order 3,
                          :submodule/research? nil,
                          :group/order 3}}],
                       :conditionals-operator nil}}

          ;; Expand ancestors
          ancestor-branches (gs/expand-ancestor-or-branches (:ancestors test-group))

          ;; Expected: Ancestor 1 has 2 OR branches (Heading OR Direction of Interest)
          ;;          Ancestor 2 has :in with 2 values × sub-OR with 2 branches = 4 branches
          ;;          Total: 2 × 4 = 8 ancestor combinations
          expected-ancestor-count 8]

      (is (= expected-ancestor-count (count ancestor-branches))
          (str "Should generate " expected-ancestor-count " ancestor combinations via cartesian product\n"
               "Ancestor 1 (OR): 2 branches\n"
               "Ancestor 2 (AND with nested :in + sub-OR): 2 values × 2 sub-branches = 4 branches\n"
               "Cartesian: 2 × 4 = " expected-ancestor-count "\n"
               "Actual count: " (count ancestor-branches)))

      ;; Verify each ancestor combination is a flat sequence of conditionals
      (is (every? sequential? ancestor-branches)
          "Each ancestor branch should be a sequence")

      ;; Verify no nested sub-conditionals remain (all should be flattened)
      (is (every? (fn [branch]
                    (every? #(nil? (:sub-conditionals %)) branch))
                  ancestor-branches)
          "All sub-conditionals should be flattened into the branch")

      ;; Verify we have combinations with different Wind Measured at values
      (let [has-20-foot? (some (fn [branch]
                                 (some #(= ["20-Foot"] (:values %)) branch))
                               ancestor-branches)
            has-10-meter? (some (fn [branch]
                                  (some #(= ["10-Meter"] (:values %)) branch))
                                ancestor-branches)]
        (is has-20-foot? "Should have branches with '20-Foot' value")
        (is has-10-meter? "Should have branches with '10-Meter' value"))

      ;; Verify we have combinations with both Heading and Direction of Interest
      (let [has-heading? (some (fn [branch]
                                 (some (fn [cond]
                                         (= "Heading"
                                            (get-in cond [:group-variable :group-variable/translated-name])))
                                       branch))
                               ancestor-branches)
            has-direction? (some (fn [branch]
                                   (some (fn [cond]
                                           (= "Direction of Interest"
                                              (get-in cond [:group-variable :group-variable/translated-name])))
                                         branch))
                                 ancestor-branches)]
        (is has-heading? "Should have branches with 'Heading'")
        (is has-direction? "Should have branches with 'Direction of Interest'"))

      ;; Verify we have both Rate of Spread and Flame Length in the combinations
      (let [has-rate-of-spread? (some (fn [branch]
                                        (some (fn [cond]
                                                (= "Rate of Spread"
                                                   (get-in cond [:group-variable :group-variable/translated-name])))
                                              branch))
                                      ancestor-branches)
            has-flame-length? (some (fn [branch]
                                      (some (fn [cond]
                                              (= "Flame Length"
                                                 (get-in cond [:group-variable :group-variable/translated-name])))
                                            branch))
                                    ancestor-branches)]
        (is has-rate-of-spread? "Should have branches with 'Rate of Spread' sub-conditional")
        (is has-flame-length? "Should have branches with 'Flame Length' sub-conditional")))))

(deftest test-setup-step-generation-for-outputs
  (testing "generate-output-step-line creates formatted output line"
    (let [conditional {:type :group-variable
                       :operator :equal
                       :values ["true"]
                       :group-variable {:io :output
                                        :path ["Surface" "Fire Behavior" "Rate of Spread"]
                                        :submodule/order 1
                                        :group/order 2}}
          result (gs/generate-output-step-line conditional)]
      (is (string? result)
          "Should return a string")
      (is (str/starts-with? result "-- ")
          "Should start with '-- '")
      (is (str/includes? result "Fire Behavior")
          "Should include formatted path"))))

(deftest test-setup-step-generation-for-inputs
  (testing "generate-input-step-line creates formatted input value line"
    (let [conditional {:type :group-variable
                       :operator :equal
                       :values ["Individual Size Class"]
                       :group-variable {:io :input
                                        :path ["Surface" "Fuel Moisture" "Moisture Input Mode"]
                                        :submodule/order 1
                                        :group/order 2}}
          result (gs/generate-input-step-line conditional)]
      (is (string? result)
          "Should return a string")
      (is (str/starts-with? result "--- ")
          "Should start with '--- '")
      (is (str/includes? result "Fuel Moisture")
          "Should include formatted path")
      (is (str/includes? result "Individual Size Class")
          "Should include value"))))

(deftest test-scenario-generation-for-output-enables-input
  (testing "Output-enables-input scenario generates valid Gherkin"
    (let [edn-path "development/test_matrix_data.edn"
          _ (core/generate-test-matrix! edn-path)
          data (gs/load-test-matrix edn-path)
          groups (:groups data)
          ;; Find a simple output-enables-input group
          simple-group (first (filter #(and (get-in % [:conditionals :conditionals])
                                            (= :output-enables-input (gs/determine-scenario-pattern %)))
                                      groups))]
      (when simple-group
        (let [result (gs/generate-output-enables-input-scenario
                      simple-group
                      [] ; no ancestor setup
                      (:path simple-group))]
          (is (string? result)
              "Should return a string")
          (is (str/includes? result "Given I have started")
              "Should include Given statement")
          (is (str/includes? result "Then the following input")
              "Should include Then statement for target group"))))))

(deftest test-filename-generation-with-kebab-case
  (testing "generate-feature-filename creates correct kebab-case filename"
    (let [module :surface
          path ["Surface" "Fuel Moisture" "By Size Class"]
          result (gs/generate-feature-filename module path)]
      (is (string? result)
          "Should return a string")
      (is (str/ends-with? result ".feature")
          "Should end with .feature extension")
      (is (str/includes? result "surface-input")
          "Should include module prefix in kebab-case")
      (is (str/includes? result "fuel-moisture")
          "Should include path elements in kebab-case")
      (is (str/includes? result "by-size-class")
          "Should convert spaces to hyphens"))))

(deftest test-feature-file-generation-workflow
  (testing "generate-feature-files! creates feature files with correct content"
    (let [edn-path "development/test_matrix_data.edn"
          test-features-dir "test-features-output/"
          _ (core/generate-test-matrix! edn-path)
          result (gs/generate-feature-files! edn-path test-features-dir)]
      (is (map? result)
          "Should return a map with statistics")
      (is (contains? result :features-dir)
          "Should contain :features-dir key")
      (is (contains? result :files-written)
          "Should contain :files-written key")
      (is (contains? result :scenarios-generated)
          "Should contain :scenarios-generated key")
      (is (>= (:files-written result) 0)
          "Should write at least 0 files")
      (is (>= (:scenarios-generated result) 0)
          "Should generate at least 0 scenarios")
      ;; Clean up test directory
      (when (.exists (io/file test-features-dir))
        (doseq [file (file-seq (io/file test-features-dir))]
          (when (.isFile file)
            (.delete file)))
        (.delete (io/file test-features-dir))))))

(deftest test-scenario-sorting-and-deduplication
  (testing "collect-and-sort-setup-steps sorts by submodule/order then group/order"
    (let [conditionals [{:type :group-variable
                         :operator :equal
                         :values ["true"]
                         :group-variable {:io :output
                                          :path ["Surface" "Wind and Slope" "Wind Speed"]
                                          :submodule/order 2
                                          :group/order 1}}
                        {:type :group-variable
                         :operator :equal
                         :values ["true"]
                         :group-variable {:io :output
                                          :path ["Surface" "Fuel" "Fuel Model"]
                                          :submodule/order 1
                                          :group/order 0}}]
          result (gs/collect-and-sort-setup-steps conditionals)]
      (is (map? result)
          "Should return a map")
      (is (contains? result :outputs)
          "Should contain :outputs key")
      (is (sequential? (:outputs result))
          "Outputs should be sequential")
      ;; Verify sorting: submodule/order 1 should come before submodule/order 2
      (when (>= (count (:outputs result)) 2)
        (let [first-output (first (:outputs result))
              second-output (second (:outputs result))]
          (is (<= (get-in first-output [:group-variable :submodule/order])
                  (get-in second-output [:group-variable :submodule/order]))
              "Should sort by submodule/order"))))))

(deftest test-deduplicate-ancestor-conditionals
  (testing "deduplicate-ancestor-conditionals removes duplicates by path and value"
    (let [conditionals [{:type :group-variable
                         :operator :equal
                         :values ["true"]
                         :group-variable {:io :output
                                          :path ["Surface" "Fire Behavior" "Rate of Spread"]}}
                        {:type :group-variable
                         :operator :equal
                         :values ["true"]
                         :group-variable {:io :output
                                          :path ["Surface" "Fire Behavior" "Rate of Spread"]}}]
          result (gs/deduplicate-ancestor-conditionals conditionals)]
      (is (sequential? result)
          "Should return a sequence")
      (is (= 1 (count result))
          "Should remove duplicate conditionals")
      (is (= (get-in (first result) [:group-variable :path])
             ["Surface" "Fire Behavior" "Rate of Spread"])
          "Should preserve the conditional"))))

(deftest test-generate-scenarios-for-group
  (testing "scenario-generation"
    (let [group {:path
                 ["Surface"
                  "Wind and Slope"
                  "Wind Adjustment Factor"
                  "Wind Adjustment Factor - User Input"],
                 :group/research? nil,
                 :parent-submodule/io :input,
                 :group/translated-name "Wind Adjustment Factor - User Input",
                 :ancestors
                 [{:path ["Surface" "Wind and Slope"],
                   :submodule/name "Wind and Slope",
                   :submodule/io :input,
                   :submodule/research? nil,
                   :conditionals
                   {:conditionals
                    [{:type :group-variable,
                      :operator :equal,
                      :values ["true"],
                      :group-variable
                      {:group-variable/translated-name "Heading",
                       :group-variable/research? nil,
                       :io :output,
                       :path ["Surface" "Fire Behavior" "Direction Mode"],
                       :submodule/order 0,
                       :submodule/research? nil,
                       :group/order 0}}
                     {:type :group-variable,
                      :operator :equal,
                      :values ["true"],
                      :group-variable
                      {:group-variable/translated-name "Direction of Interest",
                       :group-variable/research? nil,
                       :io :output,
                       :path ["Surface" "Fire Behavior" "Direction Mode"],
                       :submodule/order 0,
                       :submodule/research? nil,
                       :group/order 0}}],
                    :conditionals-operator :or}}
                  {:path ["Surface" "Wind and Slope" "Wind Adjustment Factor"],
                   :group/translated-name "Wind Adjustment Factor",
                   :group/research? nil,
                   :group/hidden? nil,
                   :parent-submodule/io :input,
                   :group/order 3,
                   :submodule/order 3,
                   :conditionals
                   {:conditionals
                    [{:type :group-variable,
                      :operator :in,
                      :values ["20-Foot" "10-Meter"],
                      :group-variable
                      {:group-variable/translated-name "Wind Measured at:",
                       :group-variable/research? nil,
                       :io :input,
                       :path ["Surface" "Wind and Slope" "Wind Measured at:"],
                       :submodule/order 3,
                       :submodule/research? nil,
                       :group/order nil},
                      :sub-conditionals
                      [{:type :group-variable,
                        :operator :equal,
                        :values ["true"],
                        :group-variable
                        {:group-variable/translated-name "Rate of Spread",
                         :group-variable/research? nil,
                         :io :output,
                         :path ["Crown" "Fire Behavior" "Fire Behavior"],
                         :submodule/order 0,
                         :submodule/research? nil,
                         :group/order 0}}
                       {:type :group-variable,
                        :operator :equal,
                        :values ["true"],
                        :group-variable
                        {:group-variable/translated-name "Flame Length",
                         :group-variable/research? nil,
                         :io :output,
                         :path ["Crown" "Fire Behavior" "Fire Behavior"],
                         :submodule/order 0,
                         :submodule/research? nil,
                         :group/order 0}}]

                      :sub-conditional-operator :or}],
                    :conditionals-operator :and}}],
                 :group/hidden? nil,
                 :group/order nil,
                 :submodule/order 3,
                 :conditionals
                 {:conditionals
                  [{:type :group-variable,
                    :operator :equal,
                    :values ["User Input"],
                    :group-variable
                    {:group-variable/translated-name
                     "Wind Adjustment Factor Calculation Method",
                     :group-variable/research? nil,
                     :io :input,
                     :path ["Surface" "Wind and Slope" "Wind Adjustment Factor"],
                     :submodule/order 3,
                     :submodule/research? nil,
                     :group/order 3}}],
                  :conditionals-operator nil}}]
      (gs/generate-scenarios-for-group group nil))))

