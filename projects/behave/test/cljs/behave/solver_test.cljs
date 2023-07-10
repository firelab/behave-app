(ns behave.solver-test
  (:require [clojure.string       :as str]
            [cljs.test            :refer [deftest is join-fixtures testing use-fixtures are] :include-macros true]
            [csv-parser.interface :refer [parse-csv]]
            [datascript.core      :as d]
            [behave.fixtures :as fx]
            [behave.lib.enums     :as enums]
            [behave.lib.units     :refer [get-unit]]
            [behave.solver.core   :refer [solve-worksheet]]
            [behave.vms.store     :refer [vms-conn]]
            [behave.vms.rules     :refer [rules]])
  (:require-macros [behave.macros :refer [inline-resource]]))

;;; Helpers

(defn within? [precision a b]
  (> precision (Math/abs (- a b))))

(def within-four-percent? (partial within? 4.0))

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
        "setSurfaceRunInDirectionOf"       "surfaceRunInDirectionOf"
        "setWindAndSpreadOrientationMode"  "windAndSpreadOrientationMode"
        "setFuelModelNumber"               "fuelModelNumber"
        "setChaparralFuelLoadInputMode"    "fuelLoadInputMode"
        "setChaparralFuelDeadLoadFraction" "chaparralFuelDeadLoadFraction"
        "setChaparralFuelType"             "chaparralFuelType"
        "setChaparralFuelBedDepth"         "chaparralFuelBedDepth"
        "setChaparralTotalFuelLoad"        "chaparralTotalFuelLoad"
        "setAgeOfRough"                    "ageOfRough"
        "setHeightOfUnderstory"            "heightOfUnderstory"
        "setPalmettoCoverage"              "palmettoCoverage"
        "setOverstoryBasalArea"            "overstoryBasalArea"
        "setAspenFuelModelNumber"          "aspenFuelModelNumber"
        "setTwoFuelModelsMethod"           "twoFuelModelsMethod"
        "setMoistureInputMode"             "moistureInputMode"
        "setElapsedTime"                   "elapsedTime"
        "setWindSpeed"                     "windSpeed"
        "setWindHeightInputMode"           "windHeightInputMode"
        "setWindUpslopeAlignmentMode"      "windUpslopeAlignmentMode")

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
                      "getCrownTransitionRatio"
                      "getCrownCriticalSurfaceFlameLength"
                      "getCrownCriticalFireSpreadRate"
                      "getCrownCriticalSurfaceFirelineIntensity"
                      "getCrownFireActiveRatio"
                      "getCrownCriticalFireSpreadRate"
                      "getCrownFireSpreadRate"
                      "getCrownFlameLength"
                      "getCrownFireSpreadDistance"
                      "getCrownFireArea"
                      "getCrownFirePerimeter"
                      "getCrownFireLengthToWidthRatio")
      (outputs-exist? spot
                      "getMaxMountainousTerrainSpottingDistanceFromTorchingTrees"
                      "getFlameHeightForTorchingTrees"))


    (testing "Crown Input Variables Function Mappings"
      (are [fn-name p-name] (some? (class+fn+param->group+gv-uuid class-name fn-name p-name))
        "setCrownFireCalculationMethod"            "CrownFireCalculationMethod"
        "setMoistureFoliar"                        "foliarMoisture"
        "setCanopyHeight"                          "canopyHeight"
        "setCanopyBaseHeight"                      "canopyBaseHeight"
        "setCanopyBulkDensity"                     "canopyBulkDensity"
        "setWindSpeed"                             "windSpeed"
        "setWindAdjustmentFactorCalculationMethod" "windAdjustmentFactorCalculationMethod")

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
