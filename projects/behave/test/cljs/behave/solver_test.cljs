(ns behave.solver-test
  (:require [clojure.string       :as str]
            [cljs.test            :refer [deftest is join-fixtures testing use-fixtures are] :include-macros true]
            [csv-parser.interface :refer [parse-csv]]
            [datascript.core      :as d]
            [behave.fixtures      :as fx]
            [behave.lib.enums     :as enums]
            [behave.solver.core   :refer [solve-worksheet]]
            [behave.vms.store     :refer [vms-conn]]
            [behave.vms.rules     :refer [rules]])
  (:require-macros [behave.macros :refer [inline-resource]]))

;;; Helpers

(defn within? [precision a b]
  (> precision (Math/abs (- a b))))

(def within-four-percent? (partial within? 4.0))

(def within-one-percent? (partial within? 0.1))

(def within-millionth? (partial within? 1e-06))

(defn- clean-values [row]
  (into {}
        (map (fn remove-quotes[[key val]]
               (if (string? val)
                 [key (str/replace val "\"" "")]
                 [key val])))
        row))

(defn class+fn->gv-uuid [class-name fn-name]
  (d/q '[:find ?gv-uuid .
         :in $ % ?class-name ?f-name
         :where
         [?c :cpp.class/name ?class-name]
         [?c :bp/uuid ?c-uuid]
         [?c :cpp.class/function ?f]
         [?f :cpp.function/name ?f-name]
         [?f :bp/uuid ?f-uuid]
         [?gv :group-variable/cpp-class ?c-uuid]
         [?gv :group-variable/cpp-function ?f-uuid]
         [?gv :bp/uuid ?gv-uuid]]
       @@vms-conn rules class-name fn-name))

(def ws-output class+fn->gv-uuid)

(defn class+fn+param->group+gv-uuid [class-name fn-name param-name]
  (d/q '[:find [?g-uuid ?gv-uuid]
         :in $ % ?class-name ?fn-name ?param-name
         :where
         [?c :cpp.class/name ?class-name]
         [?c :cpp.class/function ?f]
         [?f :cpp.function/name ?fn-name]
         [?f :cpp.function/parameter ?p]
         [?p :cpp.parameter/name ?param-name]

         [?c :bp/uuid ?c-uuid]
         [?f :bp/uuid ?f-uuid]

         [?gv :group-variable/cpp-class ?c-uuid]
         [?gv :group-variable/cpp-function ?f-uuid]
         [?gv :bp/uuid ?gv-uuid]
         (variable ?g ?gv)
         [?g  :bp/uuid ?g-uuid]]
       @@vms-conn rules class-name fn-name param-name))

(defn ws-input [class-name fn-name param-name value]
  (let [[group-uuid gv-uuid] (class+fn+param->group+gv-uuid class-name fn-name param-name)]
    (if (and group-uuid gv-uuid)
      [group-uuid 0 gv-uuid (str value)]
      [fn-name 0 param-name (str value)])))

(defn outputs-exist? [class-name & fn-names]
  (doseq [fn-name fn-names]
    (is (some? (class+fn->gv-uuid class-name fn-name)))))

(defn inputs-exist? [class-name & fn-names]
  (doseq [fn-name fn-names]
    (is (some? (class+fn->gv-uuid class-name fn-name)))))

;;; Fixtures

(use-fixtures :each
  {:before (join-fixtures [fx/setup-empty-db fx/with-new-worksheet fx/log-rf-events])
   :after  (join-fixtures [fx/teardown-db fx/stop-logging-rf-events])})

;;; Tests

(deftest contain-fn-mappings-exist
  (let [class-name "SIGContainAdapter"]
    (testing "Contain Output Variables Function Mappings"
      (outputs-exist? class-name
                      "getContainmentStatus"
                      "getFinalContainmentArea"
                      "getFinalFireLineLength"
                      "getFinalTimeSinceReport"
                      "getFireSizeAtInitialAttack"
                      "getPerimeterAtInitialAttack"
                      "getResourcesUsed"))

    (testing "Contain Input Variables Function Mappings"
      (are [fn-name p-name] (some? (class+fn+param->group+gv-uuid class-name fn-name p-name))
        "setReportSize"     "reportSize"
        "setAttackDistance" "attackDistance"
        "setTactic"         "tactic"
        "setReportRate"     "reportRate"
        "setLwRatio"        "lwRatio"
        "addResource"       "duration"
        "addResource"       "arrival"
        "addResource"       "description"
        "addResource"       "productionRate"))))

(deftest surface-fn-mappings-exist
  (let [class-name "SIGSurface"
        spot       "SIGSpot"]
    (testing "Surface Output Variables Function Mappings"
      (outputs-exist? class-name
                      "getAspenLoadDeadOneHour"
                      "getAspenLoadDeadOneHour"
                      "getAspenLoadLiveHerbaceous"
                      "getAspenSavrDeadOneHour"
                      "getAspenSavrLiveWoody"
                      "getBackingFirelineIntensity"
                      "getBackingFlameLength"
                      "getBackingSpreadDistance"
                      "getBackingSpreadRate"
                      "getBulkDensity"
                      "getChaparralLoadDeadHalfInchToLessThanOneInch"
                      "getChaparralLoadDeadLessThanQuarterInch"
                      "getChaparralLoadDeadOneInchToThreeInch"
                      "getChaparralLoadDeadQuarterInchToLessThanHalfInch"
                      "getChaparralLoadLiveHalfInchToLessThanOneInch"
                      "getChaparralLoadLiveLeaves"
                      "getChaparralLoadLiveOneInchToThreeInch"
                      "getChaparralLoadLiveQuarterInchToLessThanHalfInch"
                      "getChaparralLoadLiveStemsLessThanQuaterInch"
                      "getChaparralTotalDeadFuelLoad"
                      "getChaparralTotalLiveFuelLoad"
                      "getCharacteristicMoistureByLifeState"
                      "getCharacteristicMoistureByLifeState"
                      "getCharacteristicSAVR"
                      "getDirectionOfMaxSpread"
                      "getFireArea"
                      "getFirePerimeter"
                      "getFirelineIntensity"
                      "getFlameLength"
                      "getFlankingFirelineIntensity"
                      "getFlankingFlameLength"
                      "getFlankingSpreadDistance"
                      "getFlankingSpreadRate"
                      "getHeatPerUnitArea"
                      "getHeatSink"
                      "getHeatSource"
                      "getLiveFuelMoistureOfExtinction"
                      "getPackingRatio"
                      "getPalmettoGallberyDeadFineFuelLoad"
                      "getPalmettoGallberyDeadFoliageLoad"
                      "getPalmettoGallberyDeadMediumFuelLoad"
                      "getPalmettoGallberyFuelBedDepth"
                      "getPalmettoGallberyLitterLoad"
                      "getPalmettoGallberyLiveFineFuelLoad"
                      "getPalmettoGallberyLiveFoliageLoad"
                      "getPalmettoGallberyLiveMediumFuelLoad"
                      "getReactionIntensity"
                      "getRelativePackingRatio"
                      "getResidenceTime"
                      "getSlopeFactor"
                      "getSpreadDistance"
                      "getSpreadRate"
                      "getSurfaceFireReactionIntensityForLifeState"
                      "getSurfaceFireReactionIntensityForLifeState"
                      "getWindAdjustmentFactor")
      (outputs-exist? spot
                      "getMaxMountainousTerrainSpottingDistanceFromTorchingTrees"
                      "getMaxMountainousTerrainSpottingDistanceFromBurningPile"
                      "getMaxMountainousTerrainSpottingDistanceFromSurfaceFire"
                      "getMaxFirebrandHeightFromBurningPile"))

    (testing "Surface Input Variables Function Mappings"
      (are [fn-name p-name] (some? (class+fn+param->group+gv-uuid class-name fn-name p-name))
        "setAgeOfRough"                    "ageOfRough"
        "setAspect"                        "aspect"
        "setAspenFuelModelNumber"          "aspenFuelModelNumber"
        "setCanopyCover"                   "canopyCover"
        "setCanopyHeight"                  "canopyHeight"
        "setChaparralFuelBedDepth"         "chaparralFuelBedDepth"
        "setChaparralFuelDeadLoadFraction" "chaparralFuelDeadLoadFraction"
        "setChaparralFuelLoadInputMode"    "fuelLoadInputMode"
        "setChaparralFuelType"             "chaparralFuelType"
        "setChaparralTotalFuelLoad"        "chaparralTotalFuelLoad"
        "setCrownRatio"                    "crownRatio"
        "setElapsedTime"                   "elapsedTime"
        "setFuelModelNumber"               "fuelModelNumber"
        "setHeightOfUnderstory"            "heightOfUnderstory"
        "setMoistureHundredHour"           "moistureHundredHour"
        "setMoistureInputMode"             "moistureInputMode"
        "setMoistureLiveHerbaceous"        "moistureLiveHerbaceous"
        "setMoistureLiveWoody"             "moistureLiveWoody"
        "setMoistureOneHour"               "moistureOneHour"
        "setMoistureTenHour"               "moistureTenHour"
        "setOverstoryBasalArea"            "overstoryBasalArea"
        "setPalmettoCoverage"              "palmettoCoverage"
        "setSlope"                         "slope"
        "setSurfaceRunInDirectionOf"       "surfaceRunInDirectionOf"
        "setSurfaceRunInDirectionOf"       "surfaceRunInDirectionOf"
        "setTwoFuelModelsMethod"           "twoFuelModelsMethod"
        "setWindAndSpreadOrientationMode"  "windAndSpreadOrientationMode"
        "setWindDirection"                 "windDirection"
        "setWindHeightInputMode"           "windHeightInputMode"
        "setWindSpeed"                     "windSpeed"
        )

      (are [fn-name p-name] (some? (class+fn+param->group+gv-uuid spot fn-name p-name))
        "setDownwindCoverHeight"    "downwindCoverHeight"
        "setDownwindCanopyMode"     "downwindCanopyMode"
        "setTreeHeight"             "treeHeight"
        "setTreeSpecies"            "treeSpecies"
        "setDBH"                    "DBH"
        "setTorchingTrees"          "torchingTrees"
        "setRidgeToValleyElevation" "ridgeToValleyElevation"
        "setRidgeToValleyDistance"  "ridgeToValleyDistance"
        "setLocation"               "location"))))

(deftest crown-fn-mappings-exist
  (let [class-name "SIGCrown"
        spot       "SIGSpot"]
    (testing "Crown Output Variables Function Mappings"
      (outputs-exist? class-name
                      "getFireType"
                      "getCrownCriticalFireSpreadRate"
                      "getCrownCriticalFireSpreadRate"
                      "getCrownCriticalSurfaceFirelineIntensity"
                      "getCrownCriticalSurfaceFlameLength"
                      "getCrownFireActiveRatio"
                      "getCrownFireArea"
                      "getCrownFireLengthToWidthRatio"
                      "getCrownFireLengthToWidthRatio"
                      "getCrownFirePerimeter"
                      "getCrownFirelineIntensity"
                      "getCrownFireSpreadDistance"
                      "getCrownFireSpreadRate"
                      "getCrownFlameLength"
                      "getCrownFlameLength"
                      "getCrownTransitionRatio")
      (outputs-exist? spot
                      "getMaxMountainousTerrainSpottingDistanceFromTorchingTrees"
                      "getFlameHeightForTorchingTrees"))


    (testing "Crown Input Variables Function Mappings"
      (are [fn-name p-name] (some? (class+fn+param->group+gv-uuid class-name fn-name p-name))
        "setAspect"                                "aspect"
        "setCanopyBaseHeight"                      "canopyBaseHeight"
        "setCanopyBulkDensity"                     "canopyBulkDensity"
        "setCanopyCover"                           "canopyCover"
        "setCanopyHeight"                          "canopyHeight"
        "setCrownFireCalculationMethod"            "CrownFireCalculationMethod"
        "setCrownRatio"                            "crownRatio"
        "setFuelModelNumber"                       "fuelModelNumber"
        "setMoistureFoliar"                        "foliarMoisture"
        "setMoistureHundredHour"                   "moistureHundredHour"
        "setMoistureInputMode"                     "moistureInputMode"
        "setMoistureLiveHerbaceous"                "moistureLiveHerbaceous"
        "setMoistureLiveWoody"                     "moistureLiveWoody"
        "setMoistureOneHour"                       "moistureOneHour"
        "setMoistureTenHour"                       "moistureTenHour"
        "setSlope"                                 "slope"
        "setWindAdjustmentFactorCalculationMethod" "windAdjustmentFactorCalculationMethod"
        "setWindAndSpreadOrientationMode"          "windAndSpreadAngleMode"
        "setWindDirection"                         "windDirection"
        "setWindHeightInputMode"                   "windHeightInputMode"
        "setWindSpeed"                             "windSpeed")

      (are [fn-name p-name] (some? (class+fn+param->group+gv-uuid spot fn-name p-name))
        "setTreeHeight"             "treeHeight"
        "setWindSpeedAtTwentyFeet"  "windSpeedAtTwentyFeet"
        "setRidgeToValleyElevation" "ridgeToValleyElevation"
        "setRidgeToValleyDistance"  "ridgeToValleyDistance"
        "setLocation"               "location"
        "setFlameLength"            "flameLength"))))

(deftest mortality-fn-mappings-exist
  (let [class-name "SIGMortality"]
    (testing "Mortality Output Variables Function Mappings"
      (outputs-exist? class-name
                      "getFlameLengthOrScorchHeightValue"
                      "getProbabilityOfMortality"
                      "getBarkThickness"
                      "getTreeCrownLengthScorched"
                      "getTreeCrownVolumeScorched"))

    (testing "Mortality Input Variables Function Mappings"
      (are [fn-name p-name] (some? (class+fn+param->group+gv-uuid class-name fn-name p-name))
        "setAirTemperature"          "airTemperature"
        "setBeetleDamage"            "beetleDamage"
        "setBoleCharHeight"          "boleCharHeight"
        "setCambiumKillRating"       "cambiumKillRating"
        "setCrownDamage"             "crownDamage"
        "setCrownRatio"              "crownRatio"
        "setDBH"                     "dbh"
        "setEquationType"            "equationType"
        "setFirelineIntensity"       "firelineIntensity"
        "setMidFlameWindSpeed"       "midFlameWindSpeed"
        "setRegion"                  "region"
        "setSpeciesCode"             "speciesCode"
        "setSurfaceFireFlameLength"  "value"
        "setSurfaceFireScorchHeight" "value"
        "setTreeHeight"              "treeHeight"
        "setTreeDensityPerUnitArea"  "numberOfTrees"))))

(def equation-type-lookup
  {"CRNSCH" "crown_scorch"
   "CRCABE" "crown_damage"
   "BOLCHR" "bole_char"})

(def not-blank? (comp not str/blank?))

(deftest mortality-worksheet

  (let [mortality-input     (fn [acc & args]
                              (conj acc (apply ws-input "SIGMortality" args)))
        row                 (->> (inline-resource "public/csv/mortality.csv")
                                 (parse-csv)
                                 (map clean-values)
                                 (first))
        equation-type       (if (get row "EquationType")
                              (->> (get row "EquationType")
                                   (get equation-type-lookup)
                                   (enums/equation-type))
                              -1)
        species-code        (get row "TreeSpecies")
        FS                  (get row "FS")
        FlLe-ScHt           (get row "FlLe/ScHt")
        TreeExpansionFactor (get row "TreeExpansionFactor")
        Diameter            (get row "Diameter")
        TreeHeight          (get row "TreeHeight")
        CrownRatio          (get row "CrownRatio")
        CrownScorch         (get row "CrownScorch%")
        CKR                 (get row "CKR")
        BeetleDamage        (get row "BeetleDamage")
        BoleCharHeight      (get row "BoleCharHeight")
        inputs
        (cond-> []
          :always
          (->
           (mortality-input "setRegion" "region" (enums/region-code "south_east"))
           (mortality-input "setEquationType" "equationType" equation-type)
           (mortality-input "setSpeciesCode" "speciesCode" species-code))

          (empty? FS)
          (mortality-input "setSurfaceFireFlameLength" "value" 4)

          (and (not-blank? FlLe-ScHt) (= FS "F"))
          (mortality-input "setSurfaceFireFlameLength" "value" FlLe-ScHt)

          (and (not-blank? FlLe-ScHt) (= FS "S"))
          (mortality-input "setSurfaceFireScorchHeight" "value" FlLe-ScHt)

          (not-blank? TreeExpansionFactor)
          (mortality-input "setTreeDensityPerUnitArea" "numberOfTrees" TreeExpansionFactor)

          (not-blank? Diameter)
          (mortality-input "setDBH" "dbh" Diameter)

          (not-blank? TreeHeight)
          (mortality-input "setTreeHeight" "treeHeight" TreeHeight)

          (not-blank? CrownRatio)
          (mortality-input "setCrownRatio" "crownRatio" (/ CrownRatio 100))

          (not-blank? CrownScorch)
          (mortality-input "setCrownDamage" "crownDamage" CrownScorch)

          (not-blank? CKR)
          (mortality-input "setCambiumKillRating" "cambiumKillRating" CKR)

          (not-blank? BeetleDamage)
          (mortality-input "setBeetleDamage" "beetleDamage" (enums/beetle-damage (str/lower-case BeetleDamage)))

          (not-blank? BoleCharHeight)
          (mortality-input "setBoleCharHeight" "boleCharHeight" BoleCharHeight))

        outputs  [(ws-output "SIGMortality" "getProbabilityOfMortality")]
        expected (get row "MortAvgPercent")

        observed (-> (solve-worksheet #{:mortality} inputs outputs)
                     (first)
                     (:outputs)
                     (vals)
                     (ffirst)
                     (* 100))]

    (is (within-four-percent? observed expected))))

(deftest surface-worksheet
  (let [surface-input  (fn [acc & args]
                         (conj acc (apply ws-input "SIGSurface" args)))
        surface-output (partial ws-output "SIGSurface")
        row            (->> (inline-resource "public/csv/surface.csv")
                            (parse-csv)
                            (map clean-values)
                            (first))
        _              (println row)
        outputs        [(surface-output "getSpreadRate")]

        inputs
        (-> []
            ;; Fuel
            (surface-input "setFuelModelNumber"         "fuelModelNumber"         (get row "fuelModelNumber"))

            ;; Moisture
            (surface-input "setMoistureOneHour"         "moistureOneHour"         (/ (get row "moistureOneHour") 100))
            (surface-input "setMoistureTenHour"         "moistureTenHour"         (/ (get row "moistureTenHour") 100))
            (surface-input "setMoistureHundredHour"     "moistureHundredHour"     (/ (get row "moistureHundredHour") 100))
            (surface-input "setMoistureLiveHerbaceous"  "moistureLiveHerbaceous"  (/ (get row "moistureLiveHerbaceous") 100))
            (surface-input "setMoistureLiveWoody"       "moistureLiveWoody"       (/ (get row "moistureLiveWoody") 100))

            (surface-input "setSurfaceRunInDirectionOf" "surfaceRunInDirectionOf" (enums/surface-run-in-direction-of "MaxSpread"))

            ;; Wind
            (surface-input "setWindAndSpreadOrientationMode" "windAndSpreadOrientationMode" (enums/wind-and-spread-orientation-mode (get row "windAndSpreadOrientationMode")))
            (surface-input "setWindSpeed"               "windSpeed"               (get row "windSpeed"))
            (surface-input "setWindHeightInputMode"     "windHeightInputMode"     (enums/wind-height-input-mode (get row "windHeightInputMode")))
            (surface-input "setWindDirection"           "windDirection"           (get row "windDirection"))

            ;; Topo
            (surface-input "setAspect"                  "aspect"                  (get row "aspect"))
            (surface-input "setSlope"                   "slope"                   (get row "slope"))

            ;; Canopy
            (surface-input "setCanopyCover"             "canopyCover"             (/ (get row "canopyCover") 100))
            (surface-input "setCanopyHeight"            "canopyHeight"            (get row "canopyHeight"))
            (surface-input "setCrownRatio"              "crownRatio"              (get row "crownRatio")))

        expected
        (get row "spreadRate")

        observed
        (-> (solve-worksheet #{:surface} inputs outputs)
            (first)
            (:outputs)
            (vals)
            (ffirst))]

    (println "SOLVER OUTPUT:" observed "EXPECTED:" expected)
    (is (within-one-percent? expected observed))))

(defn test-crown-worksheet [row]
  (let [module-input  (fn [class-name acc & args]
                         (conj acc (apply ws-input class-name args)))
        crown-input   (partial module-input "SIGCrown")
        surface-input (partial module-input "SIGSurface")
        crown-output  (partial ws-output "SIGCrown")
        _             (println row)

        inputs
        (-> []
            ;;; Surface Inputs

            ;; Fuel
            (surface-input "setFuelModelNumber"         "fuelModelNumber"         (get row "fuelModelNumber"))

            ;; Moisture
            (surface-input "setMoistureOneHour"         "moistureOneHour"         (/ (get row "moistureOneHour") 100))
            (surface-input "setMoistureTenHour"         "moistureTenHour"         (/ (get row "moistureTenHour") 100))
            (surface-input "setMoistureHundredHour"     "moistureHundredHour"     (/ (get row "moistureHundredHour") 100))
            (surface-input "setMoistureLiveHerbaceous"  "moistureLiveHerbaceous"  (/ (get row "moistureLiveHerbaceous") 100))
            (surface-input "setMoistureLiveWoody"       "moistureLiveWoody"       (/ (get row "moistureLiveWoody") 100))

            ;; Wind
            (surface-input "setWindAndSpreadOrientationMode" "windAndSpreadOrientationMode" (enums/wind-and-spread-orientation-mode (get row "windAndSpreadOrientationMode")))
            (surface-input "setWindSpeed"               "windSpeed"               (get row "windSpeed"))
            (surface-input "setWindHeightInputMode"     "windHeightInputMode"     (enums/wind-height-input-mode (get row "windHeightInputMode")))
            (surface-input "setWindDirection"           "windDirection"           (get row "windDirection"))

            ;; Topo
            (surface-input "setAspect"                  "aspect"                  (get row "aspect"))
            (surface-input "setSlope"                   "slope"                   (get row "slope"))

            ;; Canopy
            (surface-input "setCanopyCover"             "canopyCover"             (/ (get row "canopyCover") 100))
            (surface-input "setCanopyHeight"            "canopyHeight"            (get row "canopyHeight"))
            (surface-input "setCrownRatio"              "crownRatio"              (get row "crownRatio"))

            ;;; Crown Inputs
            (crown-input "setCanopyHeight"      "canopyHeight"      (get row "canopyHeight"))
            (crown-input "setCanopyBaseHeight"  "canopyBaseHeight"  (get row "canopyBaseHeight"))
            (crown-input "setCanopyBulkDensity" "canopyBulkDensity" (get row "canopyBulkDensity")))

        fire-type-output   (crown-output "getFireType")

        float-outputs
        {"lengthToWidthRatio"
         (crown-output "getCrownLengthToWidthRatio")
         "crownFireSpreadRate"
         (crown-output "getCrownFlameLength")
         "crownFlameLength"
         (crown-output "getCrownFireSpreadRate")
         "crownFirelineIntensity"
         (crown-output "getCrownFirelineIntensity")}

        outputs
        (conj (vals float-outputs) fire-type-output)

        observed
        (-> (solve-worksheet #{:surface} inputs outputs)
            (first)
            (:outputs))

        test-float (fn [[header gv-uuid]]
                     (testing (str "Crown Worksheet Testing:" header)
                       (if-let [expected-value (get row header)]
                         (let [_ (println [:CROWN header expected-value])
                               expected-value (parse-float expected-value)
                               observed-value (-> observed (get gv-uuid) (first))]
                           (when-not (js/isNaN expected-value)
                             (is (within-millionth? expected-value observed-value)
                                 (str "Expected value: " expected-value"  Observed: " observed-value))))
                         (str "header not in csv: " header))))]


    ;; Assert
    (doall (map test-float float-outputs))

    (let [header   "fireType"
          expected-value (enums/fire-type (get row header))
          observed-value (-> observed (get fire-type-output) (first))]
      (testing (str "Crown Worksheet Testing:" header)
        (is (= expected-value observed-value)
            (str "Expected value: " expected-value"  Observed: " observed-value))))))

(deftest crown-worksheet
  (let [rows (->> (inline-resource "public/csv/crown.csv")
                  (parse-csv)
                  (map clean-values))]

    (doall (map test-crown-worksheet rows))))
