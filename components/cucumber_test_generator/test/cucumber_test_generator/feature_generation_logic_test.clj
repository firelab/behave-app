(ns cucumber-test-generator.feature-generation-logic-test
  "Tests for feature file generation logic.

   IMPORTANT: Integration tests are REPL-only and require the full behave-cms project.

   To run these tests:

   clojure -M:dev:behave/cms:test -e \"
   (require '[behave-cms.server :as cms])
   (cms/init-db!)
   (require '[cucumber-test-generator.feature-generation-logic-test])
   (clojure.test/run-tests 'cucumber-test-generator.feature-generation-logic-test)
   \"

   Tests verify:
   - Loading test matrix EDN file returns valid data structure
   - Path formatting converts paths to Gherkin format (skipping module)
   - Conditional categorization identifies pattern types
   - Module determination produces correct worksheet types
   - Research filtering identifies research dependencies
   - Module compatibility filtering prevents incompatible outputs"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [datomic.api :as d]
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

(deftest test-load-test-matrix-returns-valid-data
  (testing "load-test-matrix reads and parses test_matrix_data.edn file"
    ;; First generate the EDN file
    (let [db (d/db (default-conn))
          edn-path "development/test_matrix_data.edn"
          _ (core/generate-test-matrix! db edn-path)
          result (gs/load-test-matrix edn-path)]
      (is (map? result)
          "Should return a map")
      (is (contains? result :groups)
          "Should contain :groups key")
      (is (contains? result :submodules)
          "Should contain :submodules key")
      (is (vector? (:groups result))
          "Groups should be a vector")
      (is (vector? (:submodules result))
          "Submodules should be a vector"))))

(deftest test-path-formatting-skips-module-and-uses-separator
  (testing "format-path-for-gherkin converts path vector to Gherkin format"
    (let [path ["Surface" "Fuel Moisture" "By Size Class"]
          result (gs/format-path-for-gherkin path)]
      (is (= "Fuel Moisture > By Size Class" result)
          "Should skip first element (module) and use ' > ' separator"))

    (let [path ["Crown" "Spot" "Maximum Spotting Distance"]
          result (gs/format-path-for-gherkin path)]
      (is (= "Spot > Maximum Spotting Distance" result)
          "Should work for Crown module"))

    (let [path ["Surface" "Wind and Slope"]
          result (gs/format-path-for-gherkin path)]
      (is (= "Wind and Slope" result)
          "Should handle path with only module and submodule"))))

(deftest test-output-line-formatting-adds-prefix
  (testing "format-output-line prefixes path with '-- '"
    (let [path ["Surface" "Fire Behavior" "Direction Mode"]
          result (gs/format-output-line path)]
      (is (= "-- Fire Behavior > Direction Mode" result)
          "Should prefix with '-- ' for output display"))))

(deftest test-input-value-line-formatting-adds-prefix-and-value
  (testing "format-input-value-line prefixes path with '--- ' and appends value"
    (let [path ["Surface" "Fuel Model" "Standard" "Fuel Model"]
          value "FB2/2 - Timber grass"
          result (gs/format-input-value-line path value)]
      (is (= "--- Fuel Model > Standard > Fuel Model > FB2/2 - Timber grass" result)
          "Should prefix with '--- ' and append value"))))

(deftest test-categorize-output-selected-conditional
  (testing "categorize-conditional identifies output-selected pattern"
    (let [conditional {:type :group-variable
                       :operator :equal
                       :values ["true"]
                       :group-variable {:io :output
                                        :path ["Surface" "Fire Behavior" "Rate of Spread"]}}
          result (gs/categorize-conditional conditional)]
      (is (= :output-selected result)
          "Should identify output selected pattern (values=[\"true\"], io=:output)"))))

(deftest test-categorize-output-not-selected-conditional
  (testing "categorize-conditional identifies output-not-selected pattern"
    (let [conditional {:type :group-variable
                       :operator :equal
                       :values ["false"]
                       :group-variable {:io :output
                                        :path ["Surface" "Fire Behavior" "Direction Mode"]}}
          result (gs/categorize-conditional conditional)]
      (is (= :output-not-selected result)
          "Should identify output NOT selected pattern (values=[\"false\"], io=:output)"))))

(deftest test-categorize-input-value-conditional
  (testing "categorize-conditional identifies input-value pattern"
    (let [conditional {:type :group-variable
                       :operator :equal
                       :values ["Individual Size Class"]
                       :group-variable {:io :input
                                        :path ["Surface" "Fuel Moisture" "Moisture Input Mode"]}}
          result (gs/categorize-conditional conditional)]
      (is (= :input-value result)
          "Should identify input value pattern (io=:input)"))))

(deftest test-categorize-module-enabled-conditional
  (testing "categorize-conditional identifies module-enabled pattern"
    (let [conditional {:type :module
                       :operator :equal
                       :values ["contain"]}
          result (gs/categorize-conditional conditional)]
      (is (= :module-enabled result)
          "Should identify module enabled pattern (type=:module)"))))

(deftest test-module-detection-from-real-data
  (testing "Module detection works with real data from test_matrix_data.edn"
    (let [db (d/db (default-conn))
          edn-path "development/test_matrix_data.edn"
          _ (core/generate-test-matrix! db edn-path)
          data (gs/load-test-matrix edn-path)
          all-entities (vals data)
          groups (filter :group/translated-name all-entities)]
      (when (seq groups)
        (let [first-group (first groups)
              paths (gs/collect-all-paths-from-conditionals data first-group)
              modules (gs/extract-modules-from-paths paths)]
          (is (set? modules)
              "Should return a set of module keywords")
          (is (every? keyword? modules)
              "All modules should be keywords"))))))

(deftest test-module-combination-determination
  (testing "determine-module-combination maps module sets to module combination sets"
    (is (= #{:surface} (gs/determine-module-combination #{:surface}))
        "Single surface module should map to #{:surface}")

    (is (= #{:surface :crown} (gs/determine-module-combination #{:surface :crown}))
        ":surface and :crown should map to #{:surface :crown}")

    (is (= #{:surface :mortality} (gs/determine-module-combination #{:surface :mortality}))
        ":surface and :mortality should map to #{:surface :mortality}")

    (is (= #{:surface :contain} (gs/determine-module-combination #{:surface :contain}))
        ":surface and :contain should map to #{:surface :contain}")

    (is (= :unsupported (gs/determine-module-combination #{:surface :crown :mortality}))
        "3+ modules should map to :unsupported")))

(deftest test-module-to-given-statement
  (testing "module-to-given-statement generates correct Given step text"
    (is (= "Given I have started a new Surface Worksheet in Guided Mode"
           (gs/module-to-given-statement #{:surface}))
        "Surface module should generate correct Given statement")

    (is (= "Given I have started a new Surface & Crown Worksheet in Guided Mode"
           (gs/module-to-given-statement #{:surface :crown}))
        "Surface & Crown should generate correct Given statement")

    (is (= "Given I have started a new Surface & Mortality Worksheet in Guided Mode"
           (gs/module-to-given-statement #{:surface :mortality}))
        "Surface & Mortality should generate correct Given statement")))

(deftest test-research-dependency-detection
  (testing "has-research-dependency? checks research flags"
    (let [research-conditional {:type :group-variable
                                :operator :equal
                                :values ["true"]
                                :group-variable {:io :output
                                                 :group-variable/research? true
                                                 :path ["Surface" "Fire Behavior" "Rate of Spread"]}}
          result (gs/has-research-dependency? research-conditional)]
      (is (true? result)
          "Should return true when group-variable/research? is true"))

    (let [non-research-conditional {:type :group-variable
                                    :operator :equal
                                    :values ["true"]
                                    :group-variable {:io :output
                                                     :group-variable/research? nil
                                                     :path ["Surface" "Fire Behavior" "Rate of Spread"]}}
          result (gs/has-research-dependency? non-research-conditional)]
      (is (not result)
          "Should return falsy when research? is nil"))))

(deftest test-should-skip-group-with-real-data
  (testing "should-skip-group? identifies groups to exclude from generation"
    (let [db (d/db (default-conn))
          edn-path "development/test_matrix_data.edn"
          _ (core/generate-test-matrix! db edn-path)
          data (gs/load-test-matrix edn-path)
          groups (:groups data)]
      (when (seq groups)
        ;; All groups in the EDN should NOT be skipped (they've already been filtered)
        ;; But we can verify the function works correctly
        (let [first-group (first groups)
              should-skip (gs/should-skip-group? first-group)]
          (is (boolean? should-skip)
              "should-skip-group? should return a boolean"))))))

(deftest test-path-compatibility-with-modules
  (testing "is-path-compatible-with-modules? checks module compatibility"
    (let [surface-path ["Surface" "Fuel" "Standard"]
          active-modules #{:surface}]
      (is (true? (gs/is-path-compatible-with-modules? surface-path active-modules))
          "Surface path should be compatible with surface module"))

    (let [crown-path ["Crown" "Canopy" "Bulk Density"]
          active-modules #{:surface}]
      (is (false? (gs/is-path-compatible-with-modules? crown-path active-modules))
          "Crown path should not be compatible with surface-only modules"))

    (let [crown-path ["Crown" "Canopy" "Bulk Density"]
          active-modules #{:surface :crown}]
      (is (true? (gs/is-path-compatible-with-modules? crown-path active-modules))
          "Crown path should be compatible with surface-crown modules"))))
