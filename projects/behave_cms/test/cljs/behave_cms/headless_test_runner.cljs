(ns behave-cms.headless-test-runner
  (:require
    [figwheel.main.testing :refer-macros [run-tests-async]]
    ;; require main test-runner ns
    [behave-cms.test-runner]))

(defn -main [& args]
  (run-tests-async 10000))
