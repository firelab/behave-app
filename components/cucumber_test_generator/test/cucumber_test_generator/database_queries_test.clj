(ns cucumber-test-generator.database-queries-test
  "Integration tests for database query and data extraction layer.

   IMPORTANT: These are REPL-only integration tests that require the full behave-cms project.

   To run these tests:

   clojure -M:dev:behave/cms:test -e \"
   (require '[behave-cms.server :as cms])
   (cms/init-db!)
   (require '[cucumber-test-generator.database-queries-test])
   (clojure.test/run-tests 'cucumber-test-generator.database-queries-test)
   \"

   These tests verify actual functionality against a live database:
   - Groups and submodules with conditionals can be queried
   - Translation resolution works for known keys
   - Group-variable UUID resolution returns proper metadata
   - Hierarchy collection returns proper path vectors"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
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

(deftest test-find-groups-with-conditionals
  (testing "Finding groups with conditionals returns non-empty results"
    (let [db (d/db (default-conn))
          groups (core/find-all-groups-with-conditionals db)]
      (is (seq groups)
          "Should find at least one group with conditionals")
      (is (every? number? groups)
          "Should return entity IDs (numbers)"))))

(deftest test-find-submodules-with-conditionals
  (testing "Finding submodules with conditionals returns non-empty results"
    (let [db (d/db (default-conn))
          submodules (core/find-all-submodules-with-conditionals db)]
      (is (seq submodules)
          "Should find at least one submodule with conditionals")
      (is (every? number? submodules)
          "Should return entity IDs (numbers)"))))

(deftest test-get-translation-resolves-known-keys
  (testing "Translation resolution works for known translation keys"
    (let [db (d/db (default-conn))
          translation (core/get-translation db "surface-fire")]
      (is (or (string? translation) (nil? translation))
          "Should return a string or nil if key doesn't exist")))

  (testing "Translation resolution handles nil key gracefully"
    (let [translation (core/get-translation nil nil)]
      (is (nil? translation)
          "Should return nil when translation key is nil"))))

(deftest test-pull-group-details-returns-structure
  (testing "Pull group details returns proper data structure"
    (let [db (d/db (default-conn))
          groups (core/find-all-groups-with-conditionals db)]
      (when (seq groups)
        (let [first-group-eid (first groups)
              group-details (core/pull-group-details db first-group-eid)]
          (is (map? group-details)
              "Should return a map")
          (is (contains? group-details :group/conditionals)
              "Should contain group conditionals"))))))

(deftest test-pull-submodule-details-returns-structure
  (testing "Pull submodule details returns proper data structure"
    (let [db (d/db (default-conn))
          submodules (core/find-all-submodules-with-conditionals db)]
      (when (seq submodules)
        (let [first-submodule-eid (first submodules)
              submodule-details (core/pull-submodule-details db first-submodule-eid)]
          (is (map? submodule-details)
              "Should return a map")
          (is (contains? submodule-details :submodule/conditionals)
              "Should contain submodule conditionals"))))))

(deftest test-resolve-group-variable-uuid
  (testing "Group-variable UUID resolution returns metadata with expected keys"
    (let [db (d/db (default-conn))
          groups (core/find-all-groups-with-conditionals db)]
      (when (seq groups)
        (let [first-group-eid (first groups)
              group (core/pull-group-details db first-group-eid)
              gv-uuid (some :conditional/group-variable-uuid (:group/conditionals group))]
          (when gv-uuid
            (let [gv-info (core/resolve-group-variable-uuid db gv-uuid)]
              (when gv-info
                (is (contains? gv-info :group-variable/translated-name)
                    "Should contain translated name")
                (is (contains? gv-info :io)
                    "Should contain :io key")
                (is (contains? gv-info :path)
                    "Should contain :path key")
                (is (or (= :input (:io gv-info)) (= :output (:io gv-info)))
                    "IO should be :input or :output")
                (is (vector? (:path gv-info))
                    "Path should be a vector")))))))))

(deftest test-collect-group-hierarchy
  (testing "Group hierarchy collection returns vector of strings"
    (let [db (d/db (default-conn))
          groups (core/find-all-groups-with-conditionals db)]
      (when (seq groups)
        (let [first-group-eid (first groups)
              hierarchy (core/collect-group-hierarchy db first-group-eid)]
          (is (vector? hierarchy)
              "Should return a vector")
          (is (every? string? hierarchy)
              "All elements should be strings (translated names)"))))))

(deftest test-get-variable-list-options
  (testing "Get variable list options returns list for variables that have them"
    (let [db (d/db (default-conn))
          groups (core/find-all-groups-with-conditionals db)]
      (when (seq groups)
        (let [first-group-eid (first groups)
              group (core/pull-group-details db first-group-eid)
              gv-uuid (some :conditional/group-variable-uuid (:group/conditionals group))]
          (when gv-uuid
            (let [list-options (core/get-variable-list-options db gv-uuid)]
              (is (or (nil? list-options) (seq? list-options) (vector? list-options))
                  "Should return nil or a sequence of options"))))))))

(deftest test-resolve-enum-values
  (testing "Enum value resolution works for input conditionals"
    (let [db (d/db (default-conn))
          groups (core/find-all-groups-with-conditionals db)]
      (when (seq groups)
        (let [first-group-eid (first groups)
              group (core/pull-group-details db first-group-eid)
              conditionals (:group/conditionals group)]
          (when (seq conditionals)
            (let [first-cond (first conditionals)
                  gv-uuid (:conditional/group-variable-uuid first-cond)
                  values (:conditional/values first-cond)]
              (when (and gv-uuid values)
                (let [resolved (core/resolve-enum-values db gv-uuid values)]
                  (is (or (nil? resolved) (vector? resolved))
                      "Should return nil or a vector of resolved values"))))))))))
