(ns behave.headless-test-runner
  "Headless CI runner: figwheel launches headless Chrome and calls `-main`, which
  bootstraps the env and runs the suite via `run-tests-async`. figwheel exits
  with a pass/fail code when the run finishes."
  (:require [behave.contain-test]
            [behave.crown-test]
            [behave.diagram-test]
            [behave.events]
            [behave.help.subs]
            [behave.mortality-test]
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
            [figwheel.main.testing :refer-macros [run-tests-async]]))

(defn -main [& _]
  (ts/ensure-test-env!
   (fn []
     (run-tests-async 60000
                      'behave.crown-test
                      'behave.contain-test
                      'behave.mortality-test
                      'behave.diagram-test
                      'behave.surface-test
                      'behave.solver-test
                      'behave.tests-used-in-fixtures
                      'behave.test-solver-generators
                      'behave.test-solver-queries
                      'behave.utils-test
                      'behave.worksheet-events-test
                      'behave.worksheet-subs-test)))
  ;; Return the wait signal so figwheel blocks until the async run finishes
  ;; (run-tests-async fires in the callback above, not as -main's last form).
  [:figwheel.main.async-result/wait 90000])
