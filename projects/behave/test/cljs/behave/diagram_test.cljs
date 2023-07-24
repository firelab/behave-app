(ns behave.diagram-test
  (:require [cljs.test            :refer [is deftest testing]]
            [csv-parser.interface :refer [parse-csv]]
            [behave.lib.surface   :as surface]
            [behave.lib.contain   :as contain]
            [behave.lib.fuel-models   :as fuel-models]
            [behave.lib.enums     :as enums]
            [behave.lib.units     :refer [get-unit]]
            [clojure.string :as str])
  (:require-macros [behave.macros :refer [inline-resource]]))

;; Helpers
(defn- clean-values [row]
  (into {}
        (map (fn remove-quotes[[key val]]
               (if (string? val)
                 [key (str/replace val "\"" "")]
                 [key val])))
        row))

(deftest surface-diagram-getter-test
  (let [row    (->> (inline-resource "public/csv/surface.csv")
                    (parse-csv)
                    (map clean-values)
                    first)
        module (surface/init (fuel-models/init))]

    ;; Setup Run
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

    ;; Do Run
    (surface/doSurfaceRunInDirectionOfMaxSpread module)

    (testing "Getters for Fire Shape Diagram Parameters Exist"
      ;; Fire Spread Ellipse
      (is (some? (surface/getEllipticalA module (enums/length-units "Chains"))))
      (is (some? (surface/getEllipticalB module (enums/length-units "Chains"))))
      (is (some? (surface/getDirectionOfMaxSpread module)))

      ;; Fire Wind Arrow
      (is (some? (surface/getWindDirection module)))
      (is (some? (surface/getWindSpeed module
                                       (enums/speed-units "ChainsPerHour")
                                       (surface/getWindHeightInputMode module))))
      ;; Other info to display
      (is (some? (surface/getFireArea module (enums/area-units "Acres"))))

      ;;FIXME Not sure why This is failing
      (is (some? (surface/getElapsedTime module (enums/time-units "Minutes")))))

    (testing "Getters for Wind/Slope/FirespreadDirection Diagram Parameters Exist"
      ;; Heading Direction Arrow
      (is (some? (surface/getDirectionOfMaxSpread module)))
      (is (some? (surface/getSpreadRate module (enums/speed-units "ChainsPerHour"))))

      ;; Direction of Interest Arrow
      (is (some? (surface/getDirectionOfInterest module)))
      (is (some? (surface/getSpreadRateInDirectionOfInterest module
                                                             (enums/speed-units "ChainsPerHour"))))

      ;; Backing Direction Arrow
      (is (some? (surface/getDirectionOfBacking module)))
      (is (some? (surface/getBackingSpreadRate module
                                               (enums/speed-units "ChainsPerHour"))))

      ;; Flanking Direction Arrow
      (is (some? (surface/getDirectionOfFlanking module)))
      (is (some? (surface/getFlankingSpreadRate module
                                                (enums/speed-units "ChainsPerHour"))))

      ;; Wind Direction Arrow
      (is (some? (surface/getWindDirection module)))
      (is (some? (surface/getWindSpeed module
                                       (enums/speed-units "ChainsPerHour")
                                       (surface/getWindHeightInputMode module)))))))

(deftest contain-diagram-getter-test
  (let [row    (->> (inline-resource "public/csv/contain.csv")
                    (parse-csv)
                    (first))
        module (contain/init)]

    ;; Setup Run
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

    ;; Do Run
    (contain/doContainRun module)

    (testing "Getters for Contain Diagram Parameters Exist"

      ;; Fireline Constructed
      (is (some? (contain/getFirePerimeterX module)))
      (is (some? (contain/getFirePerimeterY module)))
      (is (some? (contain/getAttackDistance module (enums/length-units "Chains"))))

      ;; Perimter at Report Ellipse. The difference between these two number will give us the length
      ;; of the ellipse. Using this length and the length to width ratio we can derrive the width of
      ;; the ellipse.
      (is (some? (contain/getFireBackAtReport module)))
      (is (some? (contain/getFireHeadAtReport module)))

      ;; Perimter at Attack Ellipse
      (is (some? (contain/getFireBackAtAttack module)))
      (is (some? (contain/getFireHeadAtAttack module)))

      ;; Used for both Perimter at Report and Attack
      (is (some? (contain/getLengthToWidthRatio module))))))

(comment
  ;; To Get the fireline constructed coordinates This was used to massage the data.
  ;; The coordinates stored in the contain module was only for the top half (above the x-axis) so i
  ;; had to make a copy for the bottom half.
  (def plot-atom (atom nil))
  (reset! plot-atom
          (concat
           (map (fn [x y]
                  {"x" x "y" y})
                (map #(.get (contain/firePerimeterX module) %) (range 1001))
                (map #(.get (contain/firePerimeterY module) %) (range 1001)))
           (map (fn [x y]
                  {"x" x "y" (* -1 y)})
                (map #(.get (contain/firePerimeterX module) %) (range 1001))
                (map #(.get (contain/firePerimeterY module) %) (range 1001))))))
