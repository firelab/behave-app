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

(comment
  (do
    (def module (surface/init (fuel-models/init)))

    (surface/setFuelModelNumber module 124)
    (surface/getFuelModelNumber module)))

(deftest surface-simple-test
  (let [row            (->> (inline-resource "public/csv/surface.csv")
                            (parse-csv)
                            (first)
                            (clean-values))
        module         (surface/init (fuel-models/init))
        moisture-units (enums/moisture-units (get row "moistureUnits"))]

    ;; Arrange
    #_(doto module
      (surface/setFuelModelNumber (get row "fuelModelNumber"))
      (surface/setMoistureOneHour (get row "moistureOneHour") moisture-units)
      (surface/setMoistureTenHour (get row "moistureTenHour") moisture-units)
      (surface/setMoistureHundredHour (get row "moistureHundredHour") moisture-units)
      (surface/setMoistureLiveHerbaceous (get row "moistureLiveHerbaceous") moisture-units)
      (surface/setMoistureLiveWoody (get row "moistureLiveWoody") moisture-units)

      ;;NOTE This is not setting the correct value
      (surface/setWindSpeed (get row "windSpeed")
                            (enums/speed-units (get row "windSpeedUnits"))
                            (enums/wind-height-input-mode (get row "windHeightInputMode")))

      (surface/setWindHeightInputMode (enums/wind-height-input-mode (get row "windHeightInputMode")))
      (surface/setWindDirection (get row "windDirection"))
      (surface/setWindAndSpreadOrientationMode (enums/wind-and-spread-orientation-mode (get row "windAndSpreadOrientationMode")))
      (surface/setSlope (get row "slope") (enums/slope-units (get row "slopeUnits")))
      (surface/setAspect (get row "aspect"))
      (surface/setCanopyCover (get row "canopyCover") (enums/cover-units (get row "canopyCoverUnits")))
      (surface/setCanopyHeight (get row "canopyHeight") (enums/length-units(get row "canopyHeightUnits")))
      (surface/setCrownRatio (get row "crownRatio")))
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

    (testing "values are properly set"
      (let [expected (surface/getFuelModelNumber module)
            observed (get row "fuelModelNumber")]
        (is (within-millionth? expected observed)
            (str "fuelModelNumber Expected: " expected " Observed: " observed) ))

      (let [expected (get row "moistureOneHour")
            observed (surface/getMoistureOneHour module moisture-units)]
        (is (within-millionth? expected observed)
            (str "MoistureOneHour Expected: " expected " Observed: " observed)))

      (let [expected (get row "moistureTenHour")
            observed (surface/getMoistureTenHour module moisture-units)]
        (is (within-millionth? expected observed)
            (str "moistureTenHour Expected: " expected " Observed: " observed)))

      (let [expected (get row "moistureHundredHour")
            observed (surface/getMoistureHundredHour module moisture-units)]
        (is (within-millionth? expected observed)
            (str "moistureHundredHour Expected: " expected " Observed: " observed)))

      (let [expected (get row "moistureLiveHerbaceous")
            observed (surface/getMoistureLiveHerbaceous module moisture-units)]
        (is (within-millionth? expected observed)
            (str "moistureLiveHerbaceous Expected: " expected " Observed: " observed)))

      (let [expected (get row "moistureLiveWoody")
            observed (surface/getMoistureLiveWoody module moisture-units)]
        (is (within-millionth? expected observed)
            (str "moistureLiveWoody Expected: " expected " Observed: " observed)))

      (let [expected (get row "windSpeed")
            observed (surface/getWindSpeed module
                                           (enums/speed-units (get row "windSpeedUnits"))
                                           (enums/wind-height-input-mode (get row "windHeightInputMode")))]
        (is (within-millionth? expected observed)
            (str "windSpeed Expected: " expected " Observed: " observed)))

      (let [expected (enums/wind-height-input-mode (get row "windHeightInputMode"))
            observed (surface/getWindHeightInputMode module)]
        (is (within-millionth? expected observed)
            (str "windHeightInputMode Expected: " expected " Observed: " observed)))

      (let [expected (get row "windDirection")
            observed (surface/getWindDirection module)]
        (is (within-millionth? expected observed)
            (str "windDirection Expected: " expected " Observed: " observed)))

      (let [expected (-> (get row "windAndSpreadOrientationMode") enums/wind-and-spread-orientation-mode)
            observed (surface/getWindAndSpreadOrientationMode module)]
        (is (within-millionth? expected observed)
            (str "windAndSpreadOrientationMode Expected: " expected " Observed: " observed)))

      (let [expected (get row "slope")
            observed (surface/getSlope module (enums/slope-units (get row "slopeUnits")))]
        (is (within-millionth? expected observed)
            (str "slopeUnits Expected: " expected " Observed: " observed)))

      (let [expected (get row "aspect")
            observed (surface/getAspect module)]
        (is (within-millionth? expected observed)
            (str "aspect Expected: " expected " Observed: " observed)))

      (let [expected (get row "canopyCover")
            observed (surface/getCanopyCover module (enums/cover-units (get row "canopyCoverUnits")))]
        (is (within-millionth? expected observed)
            (str "canopyCover Expected: " expected " Observed: " observed)))

      (let [expected (get row "canopyHeight")
            observed (surface/getCanopyHeight module (enums/length-units(get row "canopyHeightUnits")))]
        (is (within-millionth? expected observed)
            (str "canopyHeight Expected: " expected " Observed: " observed))))

    ;; Act
    (surface/doSurfaceRunInDirectionOfMaxSpread module)

    ;; Assert
    (testing "Spread Rate Result"
      (let [expected (get row "spreadRate")
            observed (surface/getSpreadRate module 1 #_(get-unit "ch/h"))]
        (is (within-millionth? expected observed)
            (str "spreadRate Expected: " expected "  Observed: " observed))))))
