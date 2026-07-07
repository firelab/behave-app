(ns behave.test-runner
  (:require [behave.contain-test]
            [behave.crown-test]
            [behave.diagram-test]
            [behave.events]
            [behave.help.subs]
            [behave.mortality-test]
            [behave.results-table-test]
            [behave.shading-test]
            [behave.solver-test]
            [behave.subs]
            [behave.surface-test]
            [behave.test-solver-generators]
            [behave.test-solver-queries]
            [behave.test-support :as ts]
            [behave.tests-used-in-fixtures]
            [behave.utils-test]
            [behave.vms.subs]
            [behave.wizard.events]
            [behave.wizard.subs]
            [behave.worksheet-events-test]
            [behave.worksheet-subs-test]
            [behave.worksheet.events]
            [behave.worksheet.subs]
            [cljs-test-display.core]
            [figwheel.main.testing :refer [run-tests]]))

(defn run-the-tests []
  (run-tests (cljs-test-display.core/init! "app-testing")
             'behave.crown-test
             'behave.contain-test
             'behave.mortality-test
             'behave.results-table-test
             'behave.shading-test
             'behave.diagram-test
             'behave.surface-test
             'behave.solver-test
             'behave.tests-used-in-fixtures
             'behave.test-solver-generators
             'behave.test-solver-queries
             'behave.utils-test
             'behave.worksheet-events-test
             'behave.worksheet-subs-test))

(defn ^:after-load init []
  (ts/ensure-test-env! run-the-tests))

(init)
