(ns behave.contain-test
  (:require [clojure.core.async   :refer [go <!]]
            [cljs.test            :refer [is deftest testing]]
            [csv-parser.interface :refer [fetch-csv parse-csv]]
            [behave.lib.contain   :as contain]
            [behave.lib.enums     :as enums]
            [behave.lib.units     :refer [get-unit]])
  (:require-macros [behave.macros :refer [inline-resource]]))

;; Helpers

(defn within? [a b precision]
  (> precision (- a b)))

(def within-millionth? (partial within? 1e-06))

;; Tests

(defn- test-contain [row-idx row]
  (let [module (contain/init)]

    ;; Arrange
    (doto module
      (contain/setAttackDistance (get row "attackDistance") (get-unit "ch"))
      (contain/setLwRatio (get row "lwRatio"))
      (contain/setReportRate (get row "reportRate") (get-unit "ch/h"))
      (contain/setReportSize (get row "reportSize") (get-unit "ac"))
      (contain/setTactic (enums/contain-tactic (get row "tactic")))
      (contain/addResource (get row "resourceArrival")
                           (get row "resourceDuration")
                           (get-unit "h")
                           (get row "resourceProduction")
                           (get-unit "ch/h")
                           (get row "resourceDescription")))

    ;; Act
    (contain/doContainRun module)

    ;; Assert
    (testing (str "csv row idx:" row-idx)
      (is (within? (get row "fireLineLength")           (contain/getFinalFireLineLength module (get-unit "ch")) 1e-6))
      (is (within? (get row "perimeterAtInitialAttack") (contain/getPerimeterAtInitialAttack module (get-unit "ch")) 1e-6))
      (is (within? (get row "perimeterAtContainment")   (contain/getPerimeterAtContainment module (get-unit "ch")) 1e-6))
      (is (within? (get row "fireSizeAtInitialAttack")  (contain/getFireSizeAtInitialAttack module (get-unit "ac")) 1e-6))
      (is (within? (get row "fireSize")                 (contain/getFinalFireSize module (get-unit "ac")) 1e-6))
      (is (within? (get row "containmentArea")          (contain/getFinalContainmentArea module (get-unit "ac")) 1e-6))
      (is (within? (get row "timeSinceReport")          (contain/getFinalTimeSinceReport module (get-unit "min")) 1e-6))
      (is (= (enums/contain-status (get row "containmentStatus")) (contain/getContainmentStatus module))))))

(deftest contain-testing-simple
  (let [rows (->> (inline-resource "public/csv/contain.csv")
                  (parse-csv))]
    (doall
     (map-indexed (fn [idx row-data]
                    (test-contain idx row-data))
                  rows))))
