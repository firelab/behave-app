(ns cucumber-test-generator.interface
  "Public API for the cucumber_test_generator component.

   This component generates Cucumber feature files from a Datomic database (behave-cms),
   automating the creation of comprehensive conditional visibility testing scenarios
   for the BehavePlus application.

   Usage:
   (require '[cucumber-test-generator.interface :as ctg])
   ;; Generate both sections of the combined matrix in one call (recommended):
   (ctg/generate-all-matrix! db)
   ;; Then generate feature files for each test type:
   (ctg/generate-feature-files!)
   (ctg/generate-results-feature-files!)
   ;; Or regenerate sections individually:
   (ctg/generate-test-matrix! db)
   (ctg/generate-conditional-outputs-matrix! db)"
  (:require [cucumber-test-generator.conditional-outputs        :as co]
            [cucumber-test-generator.core                       :as c]
            [cucumber-test-generator.generate-results-scenarios :as grs]
            [cucumber-test-generator.generate-scenarios         :as gs]))

(def ^{:arglists '([db] [db edn-path])
       :doc      "Generate test_matrix_data.edn from Datomic database.

                  Queries the database to find all groups and submodules
                  with conditionals, processes them with ancestor enrichment, and
                  writes structured EDN data for feature file generation.

                  Arguments:
                  - db: Datomic database value (from d/db)
                  - edn-path (optional): Path to output EDN file
                    Default: 'development/test_matrix_data.edn'

                  Returns:
                  Map with :edn-path, :groups-count, :submodules-count"}
  generate-test-matrix! c/generate-test-matrix!)

(def ^{:arglists '([] [edn-path] [edn-path features-dir])
       :doc      "Generate Cucumber feature files from test_matrix_data.edn.

                  Reads the test matrix EDN file and generates comprehensive Cucumber
                  feature files with Gherkin scenarios for testing conditional
                  visibility logic.

                  Arguments:
                  - edn-path (optional): Path to input EDN file
                    Default: 'development/test_matrix_data.edn'
                  - features-dir (optional): Directory for output feature files
                    Default: 'features/'

                  Returns:
                  Map with :features-dir, :files-written, :scenarios-generated"}
  generate-feature-files! gs/generate-feature-files!)

(def ^{:arglists '([db] [db edn-path])
       :doc      "Generate conditional_outputs_matrix.edn from Datomic database.

                  Finds every output group-variable with
                  :group-variable/conditionally-set? true and at least one
                  :select action, resolves the full transitive chain of required
                  inputs, and writes structured EDN data for results-page
                  feature file generation.

                  Arguments:
                  - db:       Datomic database value (from d/db)
                  - edn-path: (optional) output path
                    Default: 'development/conditional_outputs_matrix.edn'

                  Returns:
                  Map with :edn-path and :entries-count"}
  generate-conditional-outputs-matrix! co/generate-conditional-outputs-matrix!)

(def ^{:arglists '([] [edn-path] [edn-path features-dir])
       :doc      "Generate results-page Cucumber feature files from
                  conditional_outputs_matrix.edn.

                  For each test case, writes one feature file asserting that
                  the conditionally-set output appears on the results page when
                  its required inputs are set.

                  Arguments:
                  - edn-path:     (optional) input EDN
                    Default: 'development/conditional_outputs_matrix.edn'
                  - features-dir: (optional) output directory
                    Default: 'features/'

                  Returns:
                  Map with :features-dir and :files-written"}
  generate-results-feature-files! grs/generate-results-feature-files!)

(def ^{:arglists '([db] [db edn-path])
       :doc      "Generate the combined test_matrix_data.edn in one call.
                  Writes both :input-visibility and :results-page sections
                  to a single file, eliminating the need to keep two EDN
                  files in sync.

                  Arguments:
                  - db:       Datomic database value (from d/db)
                  - edn-path: (optional) output path
                    Default: 'development/test_matrix_data.edn'

                  Returns:
                  Map with :edn-path, :groups-count, :submodules-count,
                  :results-page-count"}
  generate-all-matrix! c/generate-all-matrix!)
