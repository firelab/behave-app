(ns behave.crown-test
  (:require [cljs.test            :refer [is deftest testing]]
            [csv-parser.interface :refer [parse-csv]]
            [behave.lib.crown   :as crown]
            [behave.lib.fuel-models   :as fuel-models]
            [behave.lib.enums     :as enums]
            [behave.lib.units     :refer [get-unit]]
            [clojure.string :as str])
  (:require-macros [behave.macros :refer [inline-resource]]))

;; Helpers

(defn within? [precision a b]
  (> precision (- a b)))

(def within-millionth? (partial within? 1e-06))

(defn- clean-values [row]
  (into {}
        (map (fn remove-quotes[[key val]]
               (if (string? val)
                 [key (str/replace val "\"" "")]
                 [key val])))
        row))

(defn- test-crown [row-idx row]
  (let [module (crown/init (fuel-models/init))]

    ;; Arrange
    (doto module
      ;; Fuel
      (crown/setFuelModelNumber        (get row "fuelModelNumber"))

      ;; Moisture
      (crown/setMoistureOneHour        (/ (get row "moistureOneHour") 100)        (enums/moisture-units "Fraction"))
      (crown/setMoistureTenHour        (/ (get row "moistureTenHour") 100)        (enums/moisture-units "Fraction"))
      (crown/setMoistureHundredHour    (/ (get row "moistureHundredHour") 100)    (enums/moisture-units "Fraction"))
      (crown/setMoistureLiveHerbaceous (/ (get row "moistureLiveHerbaceous") 100) (enums/moisture-units "Fraction"))
      (crown/setMoistureLiveWoody      (/ (get row "moistureLiveWoody") 100)      (enums/moisture-units "Fraction"))
      (crown/setMoistureFoliar         (/ (get row "moistureFoliar") 100)         (enums/moisture-units "Fraction"))

      ;; Wind
      (crown/setWindSpeed              (get row "windSpeed")              (enums/speed-units (get row "windSpeedUnits")))
      (crown/setWindHeightInputMode    (enums/wind-height-input-mode (get row "windHeightInputMode")))
      (crown/setWindDirection          (get row "windDirection"))
      (crown/setWindAndSpreadOrientationMode (enums/wind-and-spread-orientation-mode (get row "windAndSpreadOrientationMode")))

      ;; Topo
      (crown/setSlope (get row "slope") (enums/slope-units (get row "slopeUnits")))
      (crown/setAspect (get row "aspect"))

      ;; Canopy
      (crown/setCanopyCover       (/ (get row "canopyCover") 100)  (enums/cover-units "Fraction"))
      (crown/setCanopyHeight      (get row "canopyHeight")         (enums/length-units (get row "canopyHeightUnits")))
      (crown/setCanopyBaseHeight  (get row "canopyBaseHeight")     (enums/length-units (get row "canopyHeightUnits")))
      (crown/setCrownRatio        (get row "crownRatio"))
      (crown/setCanopyBulkDensity (get row "canopyBulkDensity")    (enums/density-units (get row "canopyBulkDensityUnits")))

      ;; Crown Method
      (crown/setCrownFireCalculationMethod (enums/crown-fire-calculation-method (get row "calculationMethod"))))

    ;; Act
    (crown/doCrownRun module)

    ;; Assert
    (testing (str "csv row idx:" row-idx)
      (testing "lengthToWidthRatio Result"
        (let [header   "lengthToWidthRatio"
              expected (js/parseFloat (get row header))]

          (is (contains? row header)
              (str "header not in csv: " header))

          (when (not (js/isNaN expected))
            (let [observed (crown/getCrownFireLengthToWidthRatio module)]
              (is (within-millionth? expected observed)
                  (str "Expected: " expected "  Observed: " observed))))))

      (testing "Spread Rate Result"
        (let [header   "crownFireSpreadRate"
              expected (js/parseFloat (get row header))]

          (is (contains? row header)
              (str "header not in csv: " header))

          (when (not (js/isNaN expected))
            (let [observed (crown/getCrownFireSpreadRate module (get-unit "ch/h"))]
              (is (within-millionth? expected observed)
                  (str "Expected: " expected "  Observed: " observed))))))

      (testing "Flame Length Result"
        (let [header   "crownFlameLength"
              expected (js/parseFloat (get row header))]

          (is (contains? row header)
              (str "header not in csv: " header))

          (when (not (js/isNaN expected))
            (let [observed (crown/getCrownFlameLength module (get-unit "ft"))]
              (is (within-millionth? expected observed)
                  (str "Expected: " expected "  Observed: " observed))))))

      (testing "Fire Line Intensity Result"
        (let [header   "crownFirelineIntensity"
              expected (js/parseFloat (get row header))]

          (is (contains? row header)
              (str "header not in csv: " header))

          (when (not (js/isNaN expected))
            (let [observed (crown/getCrownFirelineIntensity module (get-unit "Btu/ft/s"))]
              (is (within-millionth? expected observed)
                  (str "Expected: " expected "  Observed: " observed))))))

      (testing "Fire Type Result"
        (let [header   "fireType"
              expected (enums/fire-type (get row header))]

          (is (contains? row header)
              (str "header not in csv: " header))

          (when expected
            (let [observed (crown/getFireType module)]
              (is (= expected observed)
                  (str "Expected: " expected "  Observed: " observed)))))))))

(deftest crown-simple-test
  (let [rows (->> (inline-resource "public/csv/crown.csv")
                  (parse-csv)
                  (map clean-values))]
    (doall
     (map-indexed (fn [idx row-data]
                    (test-crown idx row-data))
                  rows))))
