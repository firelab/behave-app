(ns behave.surface-test
  (:require [clojure.core.async   :refer [go <!]]
            [cljs.test            :refer [is deftest testing]]
            [csv-parser.interface :refer [parse-csv]]
            [behave.lib.surface   :as surface]
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

(deftest surface-simple-test
  (let [row            (->> (inline-resource "public/csv/surface.csv")
                            (parse-csv)
                            (first)
                            (clean-values))
        module         (surface/init (fuel-models/init))
        moisture-units (enums/moisture-units (get row "moistureUnits"))]

    ;; Arrange
    (doto module
      (surface/updateSurfaceInputs (get row "fuelModelNumber")
                                   (get row "moistureOneHour")
                                   (get row "moistureTenHour")
                                   (get row "moistureHundredHour")
                                   (get row "moistureLiveHerbaceous")
                                   (get row "moistureLiveWoody")
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
                                   (enums/cover-units (get row "canopyCoverUnits"))
                                   (get row "canopyHeight")
                                   (enums/length-units(get row "canopyHeightUnits"))
                                   (get row "crownRatio")))

    ;; Act
    (surface/doSurfaceRunInDirectionOfMaxSpread module)

    ;; Assert
    (testing "Spread Rate Result"
      (let [expected (get row "spreadRate")
            observed (surface/getSpreadRate module (get-unit "ch/h"))]
        (is (within-millionth? expected observed)
            (str "spreadRate Expected: " expected "  Observed: " observed))))))
