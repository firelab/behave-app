(ns cucumber-test-generator.edn-generation-test
  "Tests for EDN data structure generation.

   IMPORTANT: Integration tests are REPL-only and require the full behave-cms project.

   To run these tests:

   clojure -M:dev:behave/cms:test -e \"
   (require '[behave-cms.server :as cms])
   (cms/init-db!)
   (require '[cucumber-test-generator.edn-generation-test])
   (clojure.test/run-tests 'cucumber-test-generator.edn-generation-test)
   \"

   Tests verify:
   - EDN file is created at specified path
   - EDN contains required top-level keys
   - Group structure contains expected fields
   - Research groups are excluded
   - Groups with nil conditionals are filtered out"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [datomic.api :as d]
            [behave-cms.server :as cms]
            [behave-cms.store :refer [default-conn]]
            [cucumber-test-generator.core :as core]))

;; ===========================================================================================================
;; Database Initialization
;; ===========================================================================================================

(defn init-test-db [f]
  "Initialize database connection before running tests"
  (cms/init-db!)
  (f))

(use-fixtures :once init-test-db)

;; ===========================================================================================================
;; Integration Tests
;; ===========================================================================================================

(deftest test-edn-file-is-created
  (testing "EDN file is created at specified path"
    (let [db (d/db (default-conn))
          test-path "development/test_edn_generation_test.edn"
          _ (core/generate-test-matrix! db test-path)
          file (io/file test-path)]
      (is (.exists file)
          "EDN file should be created")
      ;; Clean up
      (when (.exists file)
        (.delete file)))))

(deftest test-edn-contains-required-keys
  (testing "Generated EDN contains required top-level keys"
    (let [db (d/db (default-conn))
          test-path "development/test_edn_structure_test.edn"
          _ (core/generate-test-matrix! db test-path)
          edn-data (edn/read-string (slurp test-path))]
      (is (contains? edn-data :generated-at)
          "Should contain :generated-at timestamp")
      (is (contains? edn-data :summary)
          "Should contain :summary")
      (is (contains? edn-data :groups)
          "Should contain :groups")
      (is (contains? edn-data :submodules)
          "Should contain :submodules")
      ;; Clean up
      (.delete (io/file test-path)))))

(deftest test-group-contains-expected-fields
  (testing "Group entries contain expected fields"
    (let [db (d/db (default-conn))
          test-path "development/test_group_fields_test.edn"
          _ (core/generate-test-matrix! db test-path)
          edn-data (edn/read-string (slurp test-path))
          groups (:groups edn-data)]
      (when (seq groups)
        (let [first-group (first groups)]
          (is (contains? first-group :path)
              "Group should have :path")
          (is (vector? (:path first-group))
              "Path should be a vector")
          (is (contains? first-group :conditionals)
              "Group should have :conditionals")
          ;; Ancestors may or may not be present depending on group depth
          (when (contains? first-group :ancestors)
            (is (vector? (:ancestors first-group))
                "Ancestors should be a vector when present"))))
      ;; Clean up
      (.delete (io/file test-path)))))

(deftest test-research-groups-are-excluded
  (testing "Groups with :group/research? true are excluded from EDN"
    (let [db (d/db (default-conn))
          all-groups (core/find-all-groups-with-conditionals db)
          research-groups (filter #(:group/research? (core/pull-group-details db %)) all-groups)]
      (when (seq research-groups)
        (let [test-path "development/test_research_filter_test.edn"
              _ (core/generate-test-matrix! db test-path)
              edn-data (edn/read-string (slurp test-path))
              generated-groups (:groups edn-data)
              ;; Check if any generated group has research? true
              has-research-groups (some #(:group/research? %) generated-groups)]
          (is (not has-research-groups)
              "No research groups should appear in generated EDN")
          ;; Clean up
          (.delete (io/file test-path)))))))

(deftest test-groups-with-nil-conditionals-filtered-out
  (testing "Groups that fail conditional processing are filtered out"
    (let [db (d/db (default-conn))
          test-path "development/test_nil_filter_test.edn"
          _ (core/generate-test-matrix! db test-path)
          edn-data (edn/read-string (slurp test-path))
          groups (:groups edn-data)]
      ;; All groups in the EDN should have non-nil conditionals
      (is (every? #(some? (:conditionals %)) groups)
          "All groups should have non-nil conditionals")
      (is (every? #(seq (get-in % [:conditionals :conditionals])) groups)
          "All groups should have at least one conditional in the conditionals list")
      ;; Clean up
      (.delete (io/file test-path)))))

(deftest test-summary-contains-counts
  (testing "Summary section contains group and submodule counts"
    (let [db (d/db (default-conn))
          test-path "development/test_summary_test.edn"
          _ (core/generate-test-matrix! db test-path)
          edn-data (edn/read-string (slurp test-path))
          summary (:summary edn-data)]
      (is (contains? summary :total-groups)
          "Summary should contain :total-groups")
      (is (contains? summary :total-submodules)
          "Summary should contain :total-submodules")
      (is (number? (:total-groups summary))
          "Total groups should be a number")
      (is (number? (:total-submodules summary))
          "Total submodules should be a number")
      ;; Verify counts match actual data
      (is (= (:total-groups summary) (count (:groups edn-data)))
          "Summary count should match actual groups count")
      (is (= (:total-submodules summary) (count (:submodules edn-data)))
          "Summary count should match actual submodules count")
      ;; Clean up
      (.delete (io/file test-path)))))

(deftest test-ancestors-are-pre-computed
  (testing "Groups with nested paths have ancestors pre-computed"
    (let [db (d/db (default-conn))
          test-path "development/test_ancestors_test.edn"
          _ (core/generate-test-matrix! db test-path)
          edn-data (edn/read-string (slurp test-path))
          groups (:groups edn-data)
          ;; Find groups with paths longer than 3 (should have potential ancestors)
          nested-groups (filter #(> (count (:path %)) 3) groups)]
      (when (seq nested-groups)
        (let [first-nested (first nested-groups)]
          ;; Not all nested groups have ancestors (depends on whether parent has conditionals)
          ;; But if ancestors key exists, it should be valid
          (when (contains? first-nested :ancestors)
            (is (vector? (:ancestors first-nested))
                "Ancestors should be a vector")
            (is (every? #(contains? % :path) (:ancestors first-nested))
                "Each ancestor should have :path")
            (is (every? #(contains? % :conditionals) (:ancestors first-nested))
                "Each ancestor should have :conditionals"))))
      ;; Clean up
      (.delete (io/file test-path)))))

(deftest test-generate-test-matrix-returns-summary
  (testing "generate-test-matrix! returns summary map with counts"
    (let [db (d/db (default-conn))
          test-path "development/test_return_value_test.edn"
          result (core/generate-test-matrix! db test-path)]
      (is (contains? result :edn-path)
          "Result should contain :edn-path")
      (is (contains? result :groups-count)
          "Result should contain :groups-count")
      (is (contains? result :submodules-count)
          "Result should contain :submodules-count")
      (is (= test-path (:edn-path result))
          "EDN path should match input")
      (is (number? (:groups-count result))
          "Groups count should be a number")
      (is (number? (:submodules-count result))
          "Submodules count should be a number")
      ;; Clean up
      (.delete (io/file test-path)))))
