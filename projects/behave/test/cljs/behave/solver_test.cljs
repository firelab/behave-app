(ns behave.solver-test
  (:require [cljs.test          :refer [are is deftest testing]]
            [datascript.core    :as d]
            [re-frame.core      :as rf]
            [behave.lib.contain :as contain]
            [behave.lib.enums   :as enums]
            [behave.lib.units   :refer [get-unit]]
            [behave.solver.core :as solver]
            [behave.vms.store :refer [vms-conn]]
            [behave.vms.rules :refer [rules]]))

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

(defn outputs-exist? [class-name & fn-names]
  (doseq [fn-name fn-names]
    (is (some? (class+fn->gv-uuid class-name fn-name)))))

(defn inputs-exist? [class-name & fn-names]
  (doseq [fn-name fn-names]
    (is (some? (class+fn->gv-uuid class-name fn-name)))))

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
        "setWindDirection"                 "windDirection"
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
        "setDownwindCoverHeight"           "downwindCoverHeight"
        "setDownwindCanopyMode"            "downwindCanopyMode"
        "setTreeHeight"                    "treeHeight"
        "setTreeSpecies"                   "treeSpecies"
        "setDBH"                           "DBH"
        "setTorchingTrees"                 "torchingTrees"
        "setRidgeToValleyElevation"        "ridgeToValleyElevation"
        "setRidgeToValleyDistance"         "ridgeToValleyDistance"
        "setLocation"                      "location"))))

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
        "setTreeHeight"             "treeHeight"
        "setCrownRatio"             "crownRatio"
        "setSpeciesCode"            "speciesCode"
        "setDBH"                    "dbh"
        "setSurfaceFireFlameLength" "value"
        "setSurfaceFireFlameLength" "value"
        "setFirelineIntensity"      "firelineIntensity"
        "setMidFlameWindSpeed"      "midFlameWindSpeed"
        "setAirTemperature"         "airTemperature"))))
