(ns behave-cms.headless-test-runner
  (:require
   ;; require main test-runner ns
   [behave-cms.test-runner]
   [figwheel.main.testing :refer-macros [run-tests-async]]))

(defn -main [& args]
  (run-tests-async 10000))
