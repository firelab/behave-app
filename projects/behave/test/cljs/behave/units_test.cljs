(ns behave.units-test
  (:require [behave.helpers   :refer [within-hundredth?]]
            [behave.lib.units :as sut]
            [cljs.test :refer [deftest is] :include-macros true]))

(deftest units-conversion-test
  (is (= 1 (sut/convert 1 "ac" "ac")))

  (is (within-hundredth? 43560 (sut/convert 1 "ac" "ft2"))))

;; BHP1-1607 — units added from the Variables Spreadsheet
(deftest new-speed-units-conversion-test
  (is (within-hundredth? 196.85 (sut/convert 1 "m/s" "ft/min")))
  (is (within-hundredth? 1 (sut/convert 196.8503937 "ft/min" "m/s")))
  ;; 1 mi/h = 88 ft/min = 88 * (20160/660) fur/fortnight
  (is (within-hundredth? 2688 (sut/convert 1 "mi/h" "fur/fortnight")))
  (is (within-hundredth? 3.27 (sut/convert 100 "fur/fortnight" "ft/min"))))

(deftest new-loading-units-conversion-test
  (is (within-hundredth? 21.78 (sut/convert 1 "lb/ft2" "ton/ac")))
  (is (within-hundredth? 4.88 (sut/convert 1 "lb/ft2" "kg/m2"))))

(deftest new-savr-units-conversion-test
  (is (within-hundredth? 0.08 (sut/convert 1 "ft2/ft3" "in2/in3")))
  (is (within-hundredth? 1 (sut/convert 100 "m2/m3" "cm2/cm3"))))

(deftest kelvin-conversion-test
  (is (within-hundredth? 273.15 (sut/convert 32 "oF" "K")))
  (is (within-hundredth? 32 (sut/convert 273.15 "K" "oF"))))

(deftest new-heat-units-conversion-test
  (is (within-hundredth? 11.37 (sut/convert 1 "Btu/ft2" "kW-s/m2")))
  (is (within-hundredth? 3.46 (sut/convert 1 "Btu/ft/s" "kJ/m/s")))
  (is (within-hundredth? 207.85 (sut/convert 1 "Btu/ft/s" "kJ/m/min")))
  (is (within-hundredth? 11.36 (sut/convert 1 "Btu/ft2/sec" "kJ/m2/s")))
  (is (within-hundredth? 11.36 (sut/convert 1 "Btu/ft2/min" "kJ/m2/min"))))

(deftest time-units-conversion-test
  (is (within-hundredth? 365 (sut/convert 1 "years" "days")))
  (is (within-hundredth? 525600 (sut/convert 1 "years" "min"))))
