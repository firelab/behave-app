(ns behave.contain-test
  (:require [cljs.test            :refer [is deftest testing]]
            [csv-parser.interface :refer [parse-csv]]
            [behave.lib.contain   :as contain]
            [behave.lib.enums     :as enums]
            [behave.lib.units     :refer [get-unit]])
  (:require-macros [behave.macros :refer [inline-resource]]))

;; Helpers

(defn within? [a b precision]
  (> precision (abs (- a b))))

(defn within-millionth? [a b]
  (within? a b 1e-6))

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
      ;; addResource: [arrival arrivalTimeUnit duration durationTimeUnit
      ;;               productionRate productionRateUnits description]
      (contain/addResource (get row "resourceArrival")
                           (get-unit "h")
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

;; Optimal resource (minimum production rate) search

(defn- optimal-module
  "Contain module in ComputeWithOptimalResource mode for a fire of
  `report-size` ac spreading at `report-rate` ch/h."
  [report-size report-rate lw-ratio arrival-min duration-min]
  (doto (contain/init)
    (contain/setReportSize report-size (get-unit "ac"))
    (contain/setReportRate report-rate (get-unit "ch/h"))
    (contain/setLwRatio lw-ratio)
    (contain/setTactic (enums/contain-tactic "HeadAttack"))
    (contain/setAttackDistance 0 (get-unit "ch"))
    (contain/setRetry true)
    (contain/setMinSteps 250)
    (contain/setMaxSteps 1000)
    (contain/setMaxFireSize 1000)
    (contain/setMaxFireTime 1080)
    (contain/setContainMode (js/Module._emscripten_enum_ContainMode_ComputeWithOptimalResource))
    (contain/setResourceArrivalTime arrival-min (get-unit "min"))
    (contain/setResourceDuration duration-min (get-unit "min"))))

(deftest contain-optimal-resource-uncontained-rate-test
  (testing "a reused module reports 0 ch/h, not the previous run's rate, when uncontainable"
    (let [module (optimal-module 10 20 3 60 480)]
      (contain/doContainRun module)
      (is (= 41 (contain/getAutoComputedResourceProductionRate module (get-unit "ch/h"))))
      (doto module
        (contain/setReportSize 500 (get-unit "ac"))
        (contain/setReportRate 300 (get-unit "ch/h"))
        (contain/setResourceDuration 10 (get-unit "min"))
        (contain/doContainRun))
      (is (= (enums/contain-status "SizeLimitExceeded") (contain/getContainmentStatus module)))
      (is (zero? (contain/getAutoComputedResourceProductionRate module (get-unit "ch/h"))))
      (is (zero? (contain/getOptimizedContainPointCount module))))))

(deftest contain-optimal-resource-max-rate-test
  (testing "a fire contained only by a rate in (9999, 10000] ch/h reports 10000 ch/h"
    ;; Report rate found by bisection so that 10000 ch/h contains the fire
    ;; but 9999 ch/h does not (previously a null ContainSim was read).
    (let [module (optimal-module 1 281.855941305 2 30 480)]
      (contain/doContainRun module)
      (is (= (enums/contain-status "Contained") (contain/getContainmentStatus module)))
      (is (= 10000 (contain/getAutoComputedResourceProductionRate module (get-unit "ch/h"))))
      (is (= 15001 (contain/getOptimizedContainPointCount module))))))
