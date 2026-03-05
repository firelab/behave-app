(ns run-cucumber
  (:require
   [cucumber.runner :refer [run-cucumber-tests]]))

(def ^:private default-opts
  {:debug?    false
   :headless? true
   :features  "features"
   :steps     "steps"
   :stop      true
   :browser   :chrome
   :url       "http://localhost:8081/worksheets"})

(defn run-core
  "Runs core cucumber tests (~30 min).

  Usage: `clj -X:dev:cucumber/core`

  Options:
    :url - URL to test against (default: http://localhost:8081/worksheets)"
  [opts]
  (run-cucumber-tests
   (merge default-opts
          (select-keys opts [:url])
          {:query-string '(and "core" (not "extended"))})))

(defn run-all
  "Runs all cucumber tests (~3 hr).

  Usage: `clj -X:dev:cucumber/all`

  Options:
    :url - URL to test against (default: http://localhost:8081/worksheets)"
  [opts]
  (run-cucumber-tests
   (merge default-opts
          (select-keys opts [:url]))))
