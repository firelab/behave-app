(ns behave.test-runner
  "Browser test runner (figwheel test page at /api/test).

  The suite is defined once in [[behave.test-namespaces]]; `run-tests`
  is invoked with no namespace arguments so it expands to every loaded
  test namespace. See that namespace to add tests."
  (:require [behave.test-namespaces]
            [behave.test-support :as ts]
            [cljs-test-display.core]
            [figwheel.main.testing :refer [run-tests]]))

(defn run-the-tests []
  (run-tests (cljs-test-display.core/init! "app-testing")))

(defn ^:after-load init []
  (ts/ensure-test-env! run-the-tests))

(init)
