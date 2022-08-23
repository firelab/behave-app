(ns behave.lib.contain
  (:require [behave.lib.enums :as enum]))

;; Initializer
(defn init []
  (js/Module.SIGContainAdapter.))

;; Run function
(defn doContainRun [self]
  (.doContainRun self))

;; Inputs
;(defn addResource [self arrival duration timeUnit productionRate productionRateUnits description]
;  (.addResource self arrival duration timeUnit productionRate productionRateUnits description))
;
;(defn setAttackDistance [self attackDistance lengthUnits]
;  (.setAttackDistance self attackDistance lengthUnits))
;
;(defn setFireStartTime [self fireStartTime]
;  (.setFireStartTime self fireStartTime))
;
;(defn setLwRatio [self lwRatio]
;  (.setLwRatio self lwRatio))
;
;(defn setMaxFireSize [self maxFireSize]
;  (.setMaxFireSize self maxFireSize))
;
;(defn setMaxFireTime [self maxFireTime]
;  (.setMaxFireTime self maxFireTime))
;
;(defn setMaxSteps [self maxSteps]
;  (.setMaxSteps self maxSteps))
;
;(defn setMinSteps [self minSteps]
;  (.setMinSteps self minSteps))
;
;(defn setReportRate [self reportRate speedUnits]
;  (.setReportRate self reportRate speedUnits))
;
;(defn setReportSize [self reportSize areaUnits]
;  (.setReportSize self reportSize areaUnits))
;
;(defn setRetry [self retry]
;  (.setRetry self retry))
;
;(defn setTactic [self tactic]
;  (.setTactic self tactic))
;
;(defn removeAllResources [self]
;  (.removeAllResources self))
;
;(defn removeResourceWithThisDesc [self desc]
;  (.removeResourceWithThisDesc self desc))
;
;(defn removeResourceAt [self index]
;  (.removeResourceAt self index))
;
;(defn removeAllResourcesWithThisDesc [self desc]
;  (.removeAllResourcesWithThisDesc self desc))


;; Outputs
;(defn getContainmentStatus [self]
;  (.getContainmentStatus self))
;
;(defn getFinalContainmentArea [self areaUnits]
;  (.getFinalContainmentArea self areaUnits))
;
;(defn getFinalCost [self]
;  (.getFinalCost self))
;
;(defn getFinalFireLineLength [self LengthUnits_LengthUnitsEnum lengthUnits]
;  (.getFinalFireLineLength self LengthUnits_LengthUnitsEnum lengthUnits))
;
;(defn getFinalFireSize [self areaUnits]
;  (.getFinalFireSize self areaUnits))
;
;(defn getFinalTimeSinceReport [self TimeUnits_TimeUnitsEnum timeUnits]
;  (.getFinalTimeSinceReport self TimeUnits_TimeUnitsEnum timeUnits))
;
;(defn getFireSizeAtInitialAttack [self areaUnits]
;  (.getFireSizeAtInitialAttack self areaUnits))
;
;(defn getPerimeterAtContainment [self LengthUnits_LengthUnitsEnum lengthUnits]
;  (.getPerimeterAtContainment self LengthUnits_LengthUnitsEnum lengthUnits))
;
;(defn getPerimiterAtInitialAttack [self LengthUnits_LengthUnitsEnum lengthUnits]
;  (.getPerimiterAtInitialAttack self LengthUnits_LengthUnitsEnum lengthUnits))


(comment

  (def contain (init))

  (println js/LengthUnits_Chains)

  (.setAttackDistance contain 0 (get enum/length-units "Chains"))
  (.setReportRate contain 5 (get enum/speed-units "ChainsPerHour"))
  (.setLwRatio contain 3.0)
  (.setReportRate contain 5 (get enum/speed-units "ChainsPerHour"))
  (.setReportSize contain 1 (get enum/area-units "Acres"))
  (.setTactic contain (get enum/contain-tactic "HeadAttack"))
  (.addResource contain 2 8 (get enum/time-units "Hours") 20 (get enum/speed-units "ChainsPerHour") "test")

  (.doContainRun contain)

  (.getFinalFireLineLength contain (get enum/length-units "Chains"))

  ; FIXME Not working
  (.getPerimeterAtInitialAttack contain (get enum/length-units "Chains"))

  (.getFinalFireSize contain (get enum/area-units "Acres"))

  (.getFinalContainmentArea contain (get enum/area-units "Acres"))

  (.getFinalTimeSinceReport contain (get enum/time-units "Hours"))

  (.getContainmentStatus contain)

  (.getResourcesUsed contain) ; TODO: Not working

  )
