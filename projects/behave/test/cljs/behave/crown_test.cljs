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
      (crown/updateCrownInputs (get row "fuelModelNumber")
                               (get row "moistureOneHour")
                               (get row "moistureTenHour")
                               (get row "moistureHundredHour")
                               (get row "moistureLiveHerbaceous")
                               (get row "moistureLiveWoody")
                               (get row "moistureFoliar")
                               (enums/moisture-units (get row "moistureUnits"))
                               (get row "windSpeed")
                               (enums/speed-units (get row "windSpeedUnits"))
                               (enums/wind-height-input-mode (get row "windHeightInputMode"))
                               (get row "windDirection")
                               (enums/wind-and-spread-orientation-mode (get row "windAndSpreadOrientationMode"))
                               (get row "slope")
                               (enums/slope-units (get row "slopeUnits"))
                               (get row "aspect")
                               (get row "canopyCover")
                               (get row "coverUnits")
                               (get row "canopyHeight")
                               (get row "canopyBaseHeight")
                               (enums/length-units (get row "canopyHeightUnits"))
                               (get row "crownRatio")
                               (get row "canopyBulkDensity")
                               (enums/density-units (get row "canopyBulkDensityUnits"))))

    ;; Act
    (crown/doCrownRunRothermel module)

    ;; Assert
    (testing (str "csv row idx:" row-idx)
      (testing "lengthToWidthRatio Result"
        (let [header   "lengthToWidthRatio"
              expected (get row header)]

          (is (contains? row header)
              (str "header not in csv: " header))

          (when expected
            (let [observed (crown/getCrownFireLengthToWidthRatio module)]
              (is (within-millionth? expected observed)
                  (str "Expected: " expected "  Observed: " observed))))))

      (testing "Spread Rate Result"
        (let [header   "crownFireSpreadRate"
              expected (get row header)]

          (is (contains? row header)
              (str "header not in csv: " header))

          (when expected
            (let [observed (crown/getCrownFireSpreadRate module (get-unit "ch/h"))]
              (is (within-millionth? expected observed)
                  (str "Expected: " expected "  Observed: " observed))))))

      (testing "Flame Length Result"
        (let [header   "crownFlameLength"
              expected (get row header)]

          (is (contains? row header)
              (str "header not in csv: " header))

          (when expected
            (let [observed (crown/getCrownFlameLength module (get-unit "ft"))]
              (is (within-millionth? expected observed)
                  (str "Expected: " expected "  Observed: " observed))))))

      (testing "Fire Line Intensity Result"
        (let [header   "crownFirelineIntensity"
              expected (get row header)]

          (is (contains? row header)
              (str "header not in csv: " header))

          (when expected
            (let [observed (crown/getCrownFirelineIntensity module (get-unit "Btu/ft/s"))]
              (is (within-millionth? expected observed)
                  (str "Expected: " expected "  Observed: " observed)))))))))

(deftest crown-simple-test
  (let [rows (->> (inline-resource "public/csv/crown.csv")
                  (parse-csv)
                  (map clean-values))]
    (doall (map-indexed (fn [idx row-data]
                          (test-crown idx row-data))
                        rows)))) ;TODO process all rows
