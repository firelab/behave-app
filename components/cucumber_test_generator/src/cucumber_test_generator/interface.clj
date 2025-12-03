(ns cucumber-test-generator.interface
  "Public API for the cucumber_test_generator component.

   This component generates Cucumber feature files from a Datomic database (behave-cms),
   automating the creation of comprehensive conditional visibility testing scenarios
   for the BehavePlus application.

   Usage:
   (require '[cucumber-test-generator.interface :as ctg])
   (ctg/generate-test-matrix!)
   (ctg/generate-feature-files!)"
  (:require [cucumber-test-generator.core :as c]
            [cucumber-test-generator.generate-scenarios :as gs]))

(def ^{:arglists '([db] [db edn-path])
       :doc "Generate test_matrix_data.edn from Datomic database.

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
       :doc "Generate Cucumber feature files from test_matrix_data.edn.

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
