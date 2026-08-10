(ns behave.headless-test-runner
  "Headless CI runner: figwheel launches headless Chrome and calls `-main`, which
  bootstraps the env and runs the suite via `run-tests-async`. figwheel exits
  with a pass/fail code when the run finishes.

  The suite is defined once in [[behave.test-namespaces]]; `run-tests-async`
  is invoked with no namespace arguments so it expands to every loaded test
  namespace — identical to the browser runner by construction."
  (:require [behave.test-namespaces]
            [behave.test-support :as ts]
            [figwheel.main.testing :refer-macros [run-tests-async]]))

(defn -main [& _]
  (ts/ensure-test-env!
   (fn []
     (run-tests-async 60000)))
  ;; Return the wait signal so figwheel blocks until the async run finishes
  ;; (run-tests-async fires in the callback above, not as -main's last form).
  [:figwheel.main.async-result/wait 90000])
