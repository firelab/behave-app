(ns cucumber-test
  (:require [behave-cms.store                                   :refer [default-conn]]
            [cucumber-test-generator.conditional-outputs        :as co]
            [cucumber-test-generator.core                       :as core]
            [cucumber-test-generator.generate-results-scenarios :as grs]
            [cucumber-test-generator.generate-scenarios         :as gs]
            [cucumber.runner                                    :refer [run-cucumber-tests]]
            [datomic.api                                        :as d]))

(comment

  (do
    (require '[cucumber-test-generator.core                       :as core] :reload)
    (require '[cucumber-test-generator.conditional-outputs        :as co] :reload)
    (require '[cucumber-test-generator.generate-scenarios         :as gs] :reload)
    (require '[cucumber-test-generator.generate-results-scenarios :as grs] :reload))

  ;; ── Recommended: generate both sections of the combined matrix in one call ──
  (core/generate-all-matrix! (d/db (default-conn)))

  ;; ── Or regenerate sections individually ──
  ;; :input-visibility section only
  (core/generate-test-matrix! (d/db (default-conn)))

  ;; :results-visibility section only
  (co/generate-conditional-outputs-matrix! (d/db (default-conn)))

  ;; ── Generate feature files from the combined matrix ──
  ;; Input-visibility scenarios → features/
  (gs/generate-feature-files!)

  ;; Results-page scenarios → features/results-page/
  (grs/generate-results-feature-files!)

  ;; Run a SINGLE feature file (fast iteration)
  (defn run-feature
    "Run one feature file by path. Optional opts override defaults."
    [feature-path & [opts]]
    (run-cucumber-tests
     (merge {:debug?       false
             :headless?    false
             :features     feature-path
             :steps        "steps"
             :stop         false
             :query-string '(and "core" (not "extended"))
             :browser      :chrome
             :url          "http://localhost:8081/worksheets"}
            opts)))

  ;(run-feature "features/surface-input_wind-and-slope_wind-adjustment-factor.feature")
  )
