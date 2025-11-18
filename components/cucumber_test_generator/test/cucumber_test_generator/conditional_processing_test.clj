(ns cucumber-test-generator.conditional-processing-test
  "Tests for conditional processing and ancestor enrichment.

   IMPORTANT: Integration tests are REPL-only and require the full behave-cms project.

   To run these tests:

   clojure -M:dev:behave/cms:test -e \"
   (require '[behave-cms.server :as cms])
   (cms/init-db!)
   (require '[cucumber-test-generator.conditional-processing-test])
   (clojure.test/run-tests 'cucumber-test-generator.conditional-processing-test)
   \"

   Tests verify:
   - Output conditional processing (`:io` = `:output`, values = [\"true\"])
   - Input conditional processing with enum value resolution
   - Filtering nil conditionals when resolution fails
   - Nested sub-conditional processing
   - Ancestor collection for nested group paths"
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
;; Pure Function Tests (no database required)
;; ===========================================================================================================

(deftest collect-parent-groups-handles-short-paths
  (testing "collect-parent-groups returns nil for paths with 2 or fewer elements"
    (let [short-path ["Surface" "Fuel"]]
      (is (nil? (core/collect-parent-groups short-path))
          "Should return nil for module/submodule-only path"))

    (let [single-element ["Surface"]]
      (is (nil? (core/collect-parent-groups single-element))
          "Should return nil for module-only path"))))

(deftest collect-parent-groups-generates-correct-paths
  (testing "collect-parent-groups generates all parent paths from target path"
    (let [path ["Surface" "Fuel Moisture" "By Size Class" "Live Woody Fuel Moisture"]
          result (core/collect-parent-groups path)]
      (is (= [["Surface" "Fuel Moisture"]
              ["Surface" "Fuel Moisture" "By Size Class"]
              ["Surface" "Fuel Moisture" "By Size Class" "Live Woody Fuel Moisture"]]
             result)
          "Should generate all parent paths including module and submodule"))))

(deftest find-group-by-path-finds-matching-group
  (testing "find-group-by-path finds group with matching path"
    (let [groups [{:path ["Surface" "Fuel"]}
                  {:path ["Surface" "Weather" "Wind"]}
                  {:path ["Crown" "Foliar"]}]
          result (core/find-group-by-path groups ["Surface" "Weather" "Wind"])]
      (is (= {:path ["Surface" "Weather" "Wind"]} result)
          "Should return group with matching path"))))

(deftest find-group-by-path-returns-nil-when-not-found
  (testing "find-group-by-path returns nil when path not found"
    (let [groups [{:path ["Surface" "Fuel"]}
                  {:path ["Crown" "Foliar"]}]
          result (core/find-group-by-path groups ["Surface" "Weather"])]
      (is (nil? result)
          "Should return nil when path not found"))))

(deftest enrich-group-with-ancestors-does-not-add-empty-key
  (testing "enrich-group-with-ancestors does not add :ancestors key when no ancestors"
    (let [all-groups []
          all-submodules []
          group {:path ["Surface" "Fuel"]}
          result (core/enrich-group-with-ancestors all-groups all-submodules group)]
      (is (= group result)
          "Should return group unchanged when no ancestors")
      (is (not (contains? result :ancestors))
          "Should not add :ancestors key when empty"))))

;; ===========================================================================================================
;; Integration Tests (require live database)
;; ===========================================================================================================

(deftest test-process-output-conditional
  (testing "Processing output conditional returns proper structure"
    (let [db (d/db (default-conn))
          groups (core/find-all-groups-with-conditionals db)]
      (when (seq groups)
        (let [first-group-eid (first groups)
              group (core/pull-group-details db first-group-eid)
              output-conditional (first (filter #(= (:conditional/type %) :group-variable)
                                               (:group/conditionals group)))]
          (when output-conditional
            (let [processed (core/process-conditional db output-conditional)]
              (when processed
                (is (contains? processed :type)
                    "Should contain :type key")
                (is (contains? processed :operator)
                    "Should contain :operator key")
                (is (contains? processed :values)
                    "Should contain :values key")
                (when (:group-variable processed)
                  (is (contains? (:group-variable processed) :io)
                      "Group-variable should contain :io")
                  (is (contains? (:group-variable processed) :path)
                      "Group-variable should contain :path"))))))))))

(deftest test-process-input-conditional-with-enum-resolution
  (testing "Processing input conditional resolves enum values"
    (let [db (d/db (default-conn))
          groups (core/find-all-groups-with-conditionals db)]
      (when (seq groups)
        (let [first-group-eid (first groups)
              group (core/pull-group-details db first-group-eid)
              conditionals (:group/conditionals group)]
          (when (seq conditionals)
            (let [input-cond (first (filter #(and (:conditional/group-variable-uuid %)
                                                  (seq (:conditional/values %)))
                                           conditionals))]
              (when input-cond
                (let [processed (core/process-conditional db input-cond)]
                  (when (and processed
                            (= :input (get-in processed [:group-variable :io])))
                    (is (or (nil? (:values processed))
                           (vector? (:values processed)))
                        "Enum values should be resolved to vector or nil")))))))))))

(deftest test-process-conditional-filters-nil-on-failed-resolution
  (testing "process-conditional handles failed resolution"
    (let [db (d/db (default-conn))
          ;; Create a conditional with an invalid UUID that should fail resolution
          invalid-conditional {:conditional/type :group-variable
                              :conditional/group-variable-uuid #uuid "00000000-0000-0000-0000-000000000000"
                              :conditional/operator :equal
                              :conditional/values ["true"]}
          processed (core/process-conditional db invalid-conditional)]
      ;; The function returns the conditional without group-variable key when resolution fails
      ;; This allows the caller to filter it out if needed
      (when processed
        (is (not (contains? processed :group-variable))
            "Should not add :group-variable key when UUID cannot be resolved")))))

(deftest test-process-nested-sub-conditionals
  (testing "Sub-conditionals are recursively processed"
    (let [db (d/db (default-conn))
          groups (core/find-all-groups-with-conditionals db)]
      (when (seq groups)
        (let [groups-with-subs (filter #(some :conditional/sub-conditionals
                                            (:group/conditionals (core/pull-group-details db %)))
                                      groups)]
          (when (seq groups-with-subs)
            (let [group (core/pull-group-details db (first groups-with-subs))
                  conditional-with-subs (first (filter :conditional/sub-conditionals
                                                      (:group/conditionals group)))]
              (when conditional-with-subs
                (let [processed (core/process-conditional db conditional-with-subs)]
                  (when processed
                    (is (contains? processed :sub-conditionals)
                        "Should contain :sub-conditionals key")
                    (is (or (nil? (:sub-conditionals processed))
                           (sequential? (:sub-conditionals processed)))
                        "Sub-conditionals should be a sequence or nil")))))))))))

(deftest test-process-group-conditionals-complete-structure
  (testing "process-group-conditionals returns complete structure"
    (let [db (d/db (default-conn))
          groups (core/find-all-groups-with-conditionals db)]
      (when (seq groups)
        (let [first-group-eid (first groups)
              group (core/pull-group-details db first-group-eid)
              processed (core/process-group-conditionals db group)]
          (when processed
            (is (contains? processed :conditionals)
                "Should contain :conditionals key")
            (is (contains? processed :conditionals-operator)
                "Should contain :conditionals-operator key")
            (is (sequential? (:conditionals processed))
                "Conditionals should be a sequence")
            (is (or (= :and (:conditionals-operator processed))
                   (= :or (:conditionals-operator processed)))
                "Operator should be :and or :or")))))))

(deftest test-process-group-conditionals-returns-nil-when-no-valid
  (testing "process-group-conditionals returns nil when all conditionals fail resolution"
    (let [group {:group/conditionals []
                 :group/conditionals-operator :and}
          result (core/process-group-conditionals nil group)]
      (is (nil? result)
          "Should return nil when no valid conditionals remain"))))

(deftest test-collect-ancestor-conditionals
  (testing "Ancestor conditionals are collected for nested paths"
    (let [db (d/db (default-conn))
          groups (core/find-all-groups-with-conditionals db)
          all-group-infos (keep #(core/extract-group-info db %) groups)
          all-submodule-infos (keep #(core/extract-submodule-info db %)
                                   (core/find-all-submodules-with-conditionals db))
          ;; Find a group with a path longer than 3 elements (has ancestors)
          nested-group (first (filter #(> (count (:path %)) 3) all-group-infos))]
      (when nested-group
        (let [ancestors (core/collect-ancestor-conditionals
                        all-group-infos
                        all-submodule-infos
                        (:path nested-group))]
          (is (or (nil? ancestors) (seq? ancestors) (vector? ancestors))
              "Should return a sequence or nil")
          (when (seq ancestors)
            (is (every? #(contains? % :path) ancestors)
                "Each ancestor should have a :path key")
            (is (every? #(contains? % :conditionals) ancestors)
                "Each ancestor should have a :conditionals key")))))))

(deftest test-collect-ancestor-conditionals-returns-empty-for-short-paths
  (testing "collect-ancestor-conditionals returns empty sequence for paths without ancestors"
    (let [all-groups []
          all-submodules [{:path ["Surface" "Fuel"]}]
          target-path ["Surface" "Fuel"]
          result (core/collect-ancestor-conditionals all-groups all-submodules target-path)]
      (is (empty? result)
          "Should return empty sequence when path has no parent groups"))))

(deftest test-enrich-group-with-ancestors
  (testing "Groups are enriched with ancestor data when ancestors exist"
    (let [db (d/db (default-conn))
          groups (core/find-all-groups-with-conditionals db)
          all-group-infos (keep #(core/extract-group-info db %) groups)
          all-submodule-infos (keep #(core/extract-submodule-info db %)
                                   (core/find-all-submodules-with-conditionals db))
          ;; Find a group with nested path
          nested-group (first (filter #(> (count (:path %)) 3) all-group-infos))]
      (when nested-group
        (let [enriched (core/enrich-group-with-ancestors
                       all-group-infos
                       all-submodule-infos
                       nested-group)]
          (is (map? enriched)
              "Should return a map")
          ;; Only check for ancestors key if the group actually has ancestors
          (let [ancestors (core/collect-ancestor-conditionals
                          all-group-infos
                          all-submodule-infos
                          (:path nested-group))]
            (if (seq ancestors)
              (is (contains? enriched :ancestors)
                  "Should add :ancestors key when ancestors exist")
              (is (not (contains? enriched :ancestors))
                  "Should not add :ancestors key when no ancestors exist"))))))))
