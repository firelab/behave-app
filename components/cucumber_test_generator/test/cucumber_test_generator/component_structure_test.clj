(ns cucumber-test-generator.component-structure-test
  "Focused tests for cucumber_test_generator component structure.

   Tests verify that the component follows Polylith structure conventions:
   - Interface namespace exists and loads
   - Core namespace exists and loads
   - Public functions are properly exposed in interface"
  (:require [clojure.test :refer [deftest is testing]]))

(deftest interface-namespace-loads
  (testing "Interface namespace exists and can be required"
    (is (do
          (require '[cucumber-test-generator.interface :as ctg])
          true)
        "Should successfully require cucumber-test-generator.interface")))

(deftest core-namespace-loads
  (testing "Core namespace exists and can be required"
    (is (do
          (require '[cucumber-test-generator.core :as core])
          true)
        "Should successfully require cucumber-test-generator.core")))

(deftest public-functions-exposed
  (testing "Public functions are properly exposed in interface"
    (require '[cucumber-test-generator.interface :as ctg])

    (is (var? (resolve 'cucumber-test-generator.interface/generate-test-matrix!))
        "generate-test-matrix! should be exposed in interface")

    (is (var? (resolve 'cucumber-test-generator.interface/generate-feature-files!))
        "generate-feature-files! should be exposed in interface")))

(deftest function-metadata-present
  (testing "Public functions have proper metadata"
    (require '[cucumber-test-generator.interface :as ctg])

    (let [matrix-var (resolve 'cucumber-test-generator.interface/generate-test-matrix!)
          feature-var (resolve 'cucumber-test-generator.interface/generate-feature-files!)
          matrix-meta (when matrix-var (meta matrix-var))
          feature-meta (when feature-var (meta feature-var))]

      (is matrix-meta
          "generate-test-matrix! var should exist")

      (is (:arglists matrix-meta)
          "generate-test-matrix! should have :arglists metadata")

      (is (:doc matrix-meta)
          "generate-test-matrix! should have :doc metadata")

      (is feature-meta
          "generate-feature-files! var should exist")

      (is (:arglists feature-meta)
          "generate-feature-files! should have :arglists metadata")

      (is (:doc feature-meta)
          "generate-feature-files! should have :doc metadata"))))

(deftest interface-delegates-to-core
  (testing "Interface functions delegate to core implementation"
    (require '[cucumber-test-generator.interface :as ctg])
    (require '[cucumber-test-generator.core :as core])
    (require '[cucumber-test-generator.generate-scenarios :as gs])

    ;; Verify that the interface vars point to implementation functions
    ;; This ensures proper separation of concerns
    (let [interface-matrix-var (resolve 'cucumber-test-generator.interface/generate-test-matrix!)
          core-matrix-var (resolve 'cucumber-test-generator.core/generate-test-matrix!)
          interface-feature-var (resolve 'cucumber-test-generator.interface/generate-feature-files!)
          gs-feature-var (resolve 'cucumber-test-generator.generate-scenarios/generate-feature-files!)]

      (is (and interface-matrix-var core-matrix-var
               (= @interface-matrix-var @core-matrix-var))
          "Interface generate-test-matrix! should delegate to core")

      (is (and interface-feature-var gs-feature-var
               (= @interface-feature-var @gs-feature-var))
          "Interface generate-feature-files! should delegate to generate-scenarios"))))
