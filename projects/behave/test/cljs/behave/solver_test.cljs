(ns behave.solver-test
  (:require [behave.fixtures      :as fx]
            [behave.lib.enums     :as enums]
            [behave.schema.core   :refer [rules]]
            [behave.solver.core   :refer [solve-worksheet]]
            [behave.solver.queries :as q]
            [behave.vms.store     :refer [vms-conn]]
            [cljs.test            :refer [deftest is join-fixtures testing use-fixtures are] :include-macros true]
            [clojure.string       :as str]
            [csv-parser.interface :refer [parse-csv]]
            [data-utils.interface :refer [parse-float parse-int]]
            [datascript.core      :as d]
            [re-frame.core        :as rf])
  (:require-macros [behave.macros :refer [inline-resource]]))

;;; Helpers

(defn within? [precision a b]
  (> precision (Math/abs (- a b))))

(def within-four-percent? (partial within? 4.0))

(def within-one-percent? (partial within? 0.1))

(def within-millionth? (partial within? 1e-06))

(defn slope-degrees [percent]
  (* (/ 180 js/Math.PI) (js/Math.atan (/ percent 100.0))))

(defn- clean-values [row]
  (into {}
        (map (fn remove-quotes [[k v]]
               (if (string? v)
                 [k (str/replace v "\"" "")]
                 [k v])))
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

(defn class+fn+param->group+gv-uuid
  "Resolve `[group-uuid gv-uuid]` for a class/fn/param.

  Some setters map to more than one group-variable (e.g. surface
  `setFuelModelNumber` has both a standard and a wind-driven fuel-model
  variable). Pass `tk-fragment` to disambiguate by translation key so the
  solver's surface->crown links match the intended source variable."
  ([class-name fn-name param-name]
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
          (group-variable ?g ?gv)
          [?g  :bp/uuid ?g-uuid]]
        @@vms-conn rules class-name fn-name param-name))
  ([class-name fn-name param-name tk-fragment]
   (let [matches (d/q '[:find ?g-uuid ?gv-uuid ?tk
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
                        [?gv :group-variable/translation-key ?tk]
                        [?gv :bp/uuid ?gv-uuid]
                        (group-variable ?g ?gv)
                        [?g  :bp/uuid ?g-uuid]]
                      @@vms-conn rules class-name fn-name param-name)
         chosen  (first (filter (fn [[_ _ tk]] (str/includes? tk tk-fragment)) matches))]
     (when chosen [(first chosen) (second chosen)]))))

(defn ws-input
  ([class-name fn-name param-name value]
   (ws-input class-name fn-name param-name value nil))
  ([class-name fn-name param-name value tk-fragment]
   (let [[group-uuid gv-uuid] (if tk-fragment
                                (class+fn+param->group+gv-uuid class-name fn-name param-name tk-fragment)
                                (class+fn+param->group+gv-uuid class-name fn-name param-name))]
     (if (and group-uuid gv-uuid)
       [group-uuid 0 gv-uuid (str value) :none]
       [fn-name 0 param-name (str value)]))))

(defn outputs-exist? [class-name & fn-names]
  (doseq [fn-name fn-names]
    (is (some? (class+fn->gv-uuid class-name fn-name))
        (str "Unable to find:" class-name "::" fn-name))))

;;; Worksheet-backed solving
;;;
;;; Rather than hand-build an input vector and call the 4-arity `solve-worksheet`
;;; (which bypasses input-links), these helpers populate a real worksheet in the
;;; DataScript fixture DB and solve it through the same path the app uses, so the
;;; VMS surface->crown input-links fire. Each test builds a fresh worksheet uuid
;;; and the DB is torn down by `teardown-db`.

(defn new-solver-worksheet!
  "Create a fresh worksheet `ws-uuid` with `modules` (e.g. [:surface :crown])."
  [ws-uuid modules]
  (rf/dispatch-sync [:worksheet/new {:uuid ws-uuid :name ws-uuid :modules modules}]))

(defn unit-uuid
  "VMS units-uuid for a unit `short-code` (e.g. \"mi/h\", \"fraction\", \"ft\"),
  or nil. Used so worksheet inputs carry real units the solver can convert."
  [short-code]
  (d/q '[:find ?u .
         :in $ ?sc
         :where
         [?e :unit/short-code ?sc]
         [?e :bp/uuid ?u]]
       @@vms-conn short-code))

(defn add-ws-input!
  "Resolve `class-name`/`fn-name`/`param-name` to a worksheet group +
  group-variable and add `value`. Options: `:tk` disambiguates the
  group-variable by translation key; `:unit` is a unit short-code stored as
  the input's `:input/units-uuid` (via `:worksheet/update-input-units`, as
  the wizard does) so the solver passes the matching WASM units enum.
  Without `:unit`, the solver falls back to the variable/domain native unit,
  so `value` must already be in that unit."
  [ws-uuid class-name fn-name param-name value & {:keys [tk unit]}]
  (when-let [[group-uuid gv-uuid]
             (if tk
               (class+fn+param->group+gv-uuid class-name fn-name param-name tk)
               (class+fn+param->group+gv-uuid class-name fn-name param-name))]
    (rf/dispatch-sync [:worksheet/add-input-group ws-uuid group-uuid 0])
    (rf/dispatch-sync [:worksheet/upsert-input-variable
                       ws-uuid group-uuid 0 gv-uuid (str value)])
    (when-let [units-uuid (and unit (unit-uuid unit))]
      (rf/dispatch-sync [:worksheet/update-input-units
                         ws-uuid group-uuid 0 gv-uuid units-uuid]))))

(defn enable-ws-output!
  "Enable the `class-name`/`fn-name` output on the worksheet; returns its gv-uuid."
  [ws-uuid class-name fn-name]
  (when-let [gv-uuid (class+fn->gv-uuid class-name fn-name)]
    (rf/dispatch-sync [:worksheet/upsert-output ws-uuid gv-uuid true])
    gv-uuid))

(defn solve-ws-outputs
  "Solve `ws-uuid` through the real (link-preserving) path; returns the first
  run's `{gv-uuid [value unit]}` output map."
  [ws-uuid]
  (let [modules     (set (q/worksheet-modules ws-uuid))
        all-inputs  @(rf/subscribe [:worksheet/all-inputs+units-vector ws-uuid])
        all-outputs @(rf/subscribe [:worksheet/all-output-uuids ws-uuid])]
    (-> (solve-worksheet ws-uuid modules all-inputs all-outputs)
        first
        :outputs)))

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
                      "getPerimeterAtInitialAttack"))

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
                      ;; getCharacteristicMoistureByLifeState is read via direct
                      ;; wrapper calls, not a CMS group-variable mapping.
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
        "setWindSpeed"                     "windSpeed")

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
      ;; NOTE: the crown-fire outputs (getCrownFireArea, getCrownFirePerimeter,
      ;; getCrownFireSpreadDistance, getCrownFireSpreadRate,
      ;; getCrownFirelineIntensity, getCrownFlameLength) are intentionally not
      ;; exposed as CMS group-variables -- they are read via direct wrapper
      ;; calls (see behave.crown-test), so they are not asserted here.
      (outputs-exist? class-name
                      "getFireType"
                      "getCrownCriticalFireSpreadRate"
                      "getCrownCriticalFireSpreadRate"
                      "getCrownCriticalSurfaceFirelineIntensity"
                      "getCrownCriticalSurfaceFlameLength"
                      "getCrownFireActiveRatio"
                      "getCrownFireLengthToWidthRatio"
                      "getCrownFireLengthToWidthRatio"
                      "getCrownTransitionRatio")
      (outputs-exist? spot
                      "getMaxMountainousTerrainSpottingDistanceFromTorchingTrees"
                      "getFlameHeightForTorchingTrees"))

    (testing "Crown Input Variables Function Mappings"
      (are [fn-name p-name] (some? (class+fn+param->group+gv-uuid class-name fn-name p-name))
        "setAspect"                                "aspect"
        "setCanopyBaseHeight"                      "canopyBaseHeight"
        "setCanopyBulkDensity"                     "canopyBulkDensity"
        "setCanopyHeight"                          "canopyHeight"
        "setCrownFireCalculationMethod"            "CrownFireCalculationMethod"
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
        "setRidgeToValleyElevation" "ridgeToValleyElevation"
        "setRidgeToValleyDistance"  "ridgeToValleyDistance"
        "setLocation"               "location"
        "setFlameLength"            "flameLength"))))

(deftest mortality-fn-mappings-exist
  (let [class-name "SIGMortality"]
    (testing "Mortality Output Variables Function Mappings"
      (outputs-exist? class-name
                      "getCalculatedScorchHeight"
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
        "setGACCRegion"              "region"
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
  (let [row           (->> (inline-resource "public/csv/mortality.csv")
                           (parse-csv)
                           (map clean-values)
                           (first))
        ws-uuid       "mortality-ws"
        equation-type (if (get row "EquationType")
                        (->> (get row "EquationType")
                             (get equation-type-lookup)
                             (enums/equation-type))
                        -1)
        species-code  (get row "TreeSpecies")
        FS            (get row "FS")
        FlLe-ScHt     (get row "FlLe/ScHt")
        expected      (parse-float (get row "MortAvgPercent"))]
    (new-solver-worksheet! ws-uuid [:mortality])

    (add-ws-input! ws-uuid "SIGMortality" "setGACCRegion" "region" (enums/gacc "SouthernArea"))
    (add-ws-input! ws-uuid "SIGMortality" "setEquationType" "equationType" equation-type)
    (add-ws-input! ws-uuid "SIGMortality" "setSpeciesCode" "speciesCode" species-code)

    ;; Surface fire input -- scorch height (FS "S") or flame length (FS "F"/empty), in feet.
    (cond
      (empty? FS)
      (add-ws-input! ws-uuid "SIGMortality" "setSurfaceFireFlameLength" "value" 4 :unit "ft")

      (and (not-blank? FlLe-ScHt) (= FS "F"))
      (add-ws-input! ws-uuid "SIGMortality" "setSurfaceFireFlameLength" "value" FlLe-ScHt :unit "ft")

      (and (not-blank? FlLe-ScHt) (= FS "S"))
      (add-ws-input! ws-uuid "SIGMortality" "setSurfaceFireScorchHeight" "value" FlLe-ScHt :unit "ft"))

    (when (not-blank? (get row "TreeExpansionFactor"))
      (add-ws-input! ws-uuid "SIGMortality" "setTreeDensityPerUnitArea" "numberOfTrees" (get row "TreeExpansionFactor") :unit "ac"))
    (when (not-blank? (get row "Diameter"))
      (add-ws-input! ws-uuid "SIGMortality" "setDBH" "dbh" (get row "Diameter") :unit "in"))
    (when (not-blank? (get row "TreeHeight"))
      (add-ws-input! ws-uuid "SIGMortality" "setTreeHeight" "treeHeight" (get row "TreeHeight") :unit "ft"))
    (when (not-blank? (get row "CrownRatio"))
      (add-ws-input! ws-uuid "SIGMortality" "setCrownRatio" "crownRatio" (/ (parse-float (get row "CrownRatio")) 100) :unit "fraction"))
    (when (not-blank? (get row "CrownScorch%"))
      (add-ws-input! ws-uuid "SIGMortality" "setCrownDamage" "crownDamage" (get row "CrownScorch%")))
    (when (not-blank? (get row "CKR"))
      (add-ws-input! ws-uuid "SIGMortality" "setCambiumKillRating" "cambiumKillRating" (get row "CKR")))
    (when (not-blank? (get row "BeetleDamage"))
      (add-ws-input! ws-uuid "SIGMortality" "setBeetleDamage" "beetleDamage" (enums/beetle-damage (str/lower-case (get row "BeetleDamage")))))
    (when (not-blank? (get row "BoleCharHeight"))
      (add-ws-input! ws-uuid "SIGMortality" "setBoleCharHeight" "boleCharHeight" (get row "BoleCharHeight") :unit "ft"))

    (let [mort-gv  (enable-ws-output! ws-uuid "SIGMortality" "getProbabilityOfMortality")
          observed (-> (solve-ws-outputs ws-uuid) (get mort-gv) first parse-float)]
      (is (within-four-percent? observed expected)
          (str "Expected: " expected " Observed: " observed)))))

(deftest surface-worksheet
  (let [row     (->> (inline-resource "public/csv/surface.csv")
                     (parse-csv)
                     (map clean-values)
                     (first))
        ws-uuid "surface-ws"]
    (new-solver-worksheet! ws-uuid [:surface])

    ;; Raw CSV values are passed with the CSV's units (moisture/cover Percent,
    ;; slope/aspect Degrees, wind mi/h) so the WASM converts them, exactly as
    ;; surface-simple-test feeds them directly.
    ;; Fuel -- standard (not wind-driven) fuel-model variable.
    (add-ws-input! ws-uuid "SIGSurface" "setFuelModelNumber" "fuelModelNumber" (get row "fuelModelNumber") :tk "standard:fuel_model")

    ;; Moisture
    (add-ws-input! ws-uuid "SIGSurface" "setMoistureOneHour" "moistureOneHour" (get row "moistureOneHour") :unit "%")
    (add-ws-input! ws-uuid "SIGSurface" "setMoistureTenHour" "moistureTenHour" (get row "moistureTenHour") :unit "%")
    (add-ws-input! ws-uuid "SIGSurface" "setMoistureHundredHour" "moistureHundredHour" (get row "moistureHundredHour") :unit "%")
    (add-ws-input! ws-uuid "SIGSurface" "setMoistureLiveHerbaceous" "moistureLiveHerbaceous" (get row "moistureLiveHerbaceous") :unit "%")
    (add-ws-input! ws-uuid "SIGSurface" "setMoistureLiveWoody" "moistureLiveWoody" (get row "moistureLiveWoody") :unit "%")

    (add-ws-input! ws-uuid "SIGSurface" "setSurfaceRunInDirectionOf" "surfaceRunInDirectionOf" (enums/surface-run-in-direction-of "MaxSpread"))

    ;; Wind
    (add-ws-input! ws-uuid "SIGSurface" "setWindAndSpreadOrientationMode" "windAndSpreadOrientationMode" (enums/wind-and-spread-orientation-mode (get row "windAndSpreadOrientationMode")))
    (add-ws-input! ws-uuid "SIGSurface" "setWindSpeed" "windSpeed" (get row "windSpeed") :tk "wind_speed:wind_speed" :unit "mi/h")
    (add-ws-input! ws-uuid "SIGSurface" "setWindHeightInputMode" "windHeightInputMode" (enums/wind-height-input-mode (get row "windHeightInputMode")))
    (add-ws-input! ws-uuid "SIGSurface" "setWindDirection" "windDirection" (get row "windDirection") :tk "wind_and_slope_are:wind-direction" :unit "deg")

    ;; Topo -- row 1 of surface.csv carries slope in Degrees.
    (add-ws-input! ws-uuid "SIGSurface" "setAspect" "aspect" (get row "aspect") :unit "deg")
    (add-ws-input! ws-uuid "SIGSurface" "setSlope" "slope" (get row "slope") :unit "deg")

    ;; Canopy
    (add-ws-input! ws-uuid "SIGSurface" "setCanopyCover" "canopyCover" (get row "canopyCover") :unit "%")
    (add-ws-input! ws-uuid "SIGSurface" "setCanopyHeight" "canopyHeight" (get row "canopyHeight") :unit "ft")
    (add-ws-input! ws-uuid "SIGSurface" "setCrownRatio" "crownRatio" (get row "crownRatio") :unit "fraction")

    (let [sr-gv    (enable-ws-output! ws-uuid "SIGSurface" "getSpreadRate")
          expected (parse-float (get row "spreadRate"))
          observed (-> (solve-ws-outputs ws-uuid) (get sr-gv) first parse-float)]
      (is (within-one-percent? expected observed)
          (str "Expected: " expected " Observed: " observed)))))

(defn test-crown-worksheet [row-idx row]
  (let [ws-uuid (str "crown-ws-" row-idx)]
    (new-solver-worksheet! ws-uuid [:surface :crown])

    ;;; Surface inputs -- these reach the crown module via the VMS
    ;;; surface->crown input-links (preserved by solving a real worksheet).
    ;;; Raw CSV values are passed with real units so the solver converts them.
    ;; Fuel: standard (not wind-driven) fuel-model variable.
    (add-ws-input! ws-uuid "SIGSurface" "setFuelModelNumber" "fuelModelNumber" (get row "fuelModelNumber") :tk "standard:fuel_model")
    (add-ws-input! ws-uuid "SIGSurface" "setMoistureOneHour" "moistureOneHour" (/ (get row "moistureOneHour") 100) :unit "fraction")
    (add-ws-input! ws-uuid "SIGSurface" "setMoistureTenHour" "moistureTenHour" (/ (get row "moistureTenHour") 100) :unit "fraction")
    (add-ws-input! ws-uuid "SIGSurface" "setMoistureHundredHour" "moistureHundredHour" (/ (get row "moistureHundredHour") 100) :unit "fraction")
    (add-ws-input! ws-uuid "SIGSurface" "setMoistureLiveHerbaceous" "moistureLiveHerbaceous" (/ (get row "moistureLiveHerbaceous") 100) :unit "fraction")
    (add-ws-input! ws-uuid "SIGSurface" "setMoistureLiveWoody" "moistureLiveWoody" (/ (get row "moistureLiveWoody") 100) :unit "fraction")
    (add-ws-input! ws-uuid "SIGSurface" "setWindAndSpreadOrientationMode" "windAndSpreadOrientationMode" (enums/wind-and-spread-orientation-mode (get row "windAndSpreadOrientationMode")))
    (add-ws-input! ws-uuid "SIGSurface" "setWindSpeed" "windSpeed" (get row "windSpeed") :tk "wind_speed:wind_speed" :unit "mi/h")
    (add-ws-input! ws-uuid "SIGSurface" "setWindHeightInputMode" "windHeightInputMode" (enums/wind-height-input-mode (get row "windHeightInputMode")))
    (add-ws-input! ws-uuid "SIGSurface" "setWindDirection" "windDirection" (get row "windDirection") :tk "wind_and_slope_are:wind-direction" :unit "deg")
    (add-ws-input! ws-uuid "SIGSurface" "setAspect" "aspect" (get row "aspect") :unit "deg")
    ;; crown.csv carries slope in Percent; convert to degrees.
    (add-ws-input! ws-uuid "SIGSurface" "setSlope" "slope" (slope-degrees (get row "slope")) :unit "deg")

    ;;; Crown inputs (set directly on crown). Canopy cover and crown ratio are
    ;;; not crown group-variables; crown defaults them and they do not affect
    ;;; L/W or fireType (see test-crown.bp7, which also omits them).
    (add-ws-input! ws-uuid "SIGCrown" "setMoistureFoliar" "foliarMoisture" (/ (get row "moistureFoliar") 100) :unit "fraction")
    (add-ws-input! ws-uuid "SIGCrown" "setCanopyHeight" "canopyHeight" (get row "canopyHeight") :unit "ft")
    (add-ws-input! ws-uuid "SIGCrown" "setCanopyBaseHeight" "canopyBaseHeight" (get row "canopyBaseHeight") :unit "ft")
    (add-ws-input! ws-uuid "SIGCrown" "setCanopyBulkDensity" "canopyBulkDensity" (get row "canopyBulkDensity") :unit "lb/ft3")
    (add-ws-input! ws-uuid "SIGCrown" "setCrownFireCalculationMethod" "CrownFireCalculationMethod" (enums/crown-fire-calculation-method (get row "calculationMethod")))

    (let [lw-gv    (enable-ws-output! ws-uuid "SIGCrown" "getCrownFireLengthToWidthRatio")
          ft-gv    (enable-ws-output! ws-uuid "SIGCrown" "getFireType")
          observed (solve-ws-outputs ws-uuid)]

      ;; L/W ratio -- wind-driven; reaches crown via the surface->crown wind
      ;; links, so this verifies the link-preserving worksheet solve end to end.
      (testing (str "Crown Worksheet (#" (inc row-idx) "): lengthToWidthRatio")
        (let [expected (parse-float (get row "lengthToWidthRatio"))
              actual   (-> observed (get lw-gv) first parse-float)]
          (when-not (js/isNaN expected)
            (is (within-millionth? expected actual)
                (str "Expected: " expected " Observed: " actual)))))

      ;; fireType -- driven by the crown transition inputs (canopy base height,
      ;; canopy bulk density) plus the linked surface fire behavior.
      (testing (str "Crown Worksheet (#" (inc row-idx) "): fireType")
        (let [expected (enums/fire-type (get row "fireType"))
              actual   (some-> observed (get ft-gv) first parse-int)]
          (is (= expected actual)
              (str "Expected: " expected " Observed: " actual)))))))

(deftest crown-worksheet
  (let [rows (->> (inline-resource "public/csv/crown.csv")
                  (parse-csv)
                  (map clean-values))]

    (doall (map-indexed test-crown-worksheet rows))))
