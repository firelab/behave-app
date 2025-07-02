(ns behave.lib.contain)

;; Initializer
(defn init []
  (js/Module.SIGContainAdapter.))

;; Run function
(defn doContainRun [self]
  (.doContainRun self))

;; Inputs
(defn addResource [self arrival arrivalTimeUnit duration durationTimeUnit productionRate productionRateUnits description]
  (.addResource self arrival arrivalTimeUnit duration durationTimeUnit productionRate productionRateUnits description))

(defn setAttackDistance [self attackDistance lengthUnits]
  (.setAttackDistance self attackDistance lengthUnits))

(defn setFireStartTime [self fireStartTime]
  (.setFireStartTime self fireStartTime))

(defn setLwRatio [self lwRatio]
  (.setLwRatio self lwRatio))

(defn setMaxFireSize [self maxFireSize]
  (.setMaxFireSize self maxFireSize))

(defn setMaxFireTime [self maxFireTime]
  (.setMaxFireTime self maxFireTime))

(defn setMaxSteps [self maxSteps]
  (.setMaxSteps self maxSteps))

(defn setMinSteps [self minSteps]
  (.setMinSteps self minSteps))

(defn setReportRate [self reportRate speedUnits]
  (.setReportRate self reportRate speedUnits))

(defn setReportSize [self reportSize areaUnits]
  (.setReportSize self reportSize areaUnits))

(defn setRetry [self retry]
  (.setRetry self retry))

(defn setTactic [self tactic]
  (.setTactic self tactic))

;; outputs
(defn getAttackDistance [self lengthUnits]
  (.getAttackDistance self lengthUnits))

(defn getContainmentStatus [self]
  (.getContainmentStatus self))

(defn getFinalContainmentArea [self areaUnits]
  (.getFinalContainmentArea self areaUnits))

(defn getFinalCost [self]
  (.getFinalCost self))

(defn getFinalFireLineLength [self lengthUnits]
  (.getFinalFireLineLength self lengthUnits))

(defn getFinalFireSize [self areaUnits]
  (.getFinalFireSize self areaUnits))

(defn getFinalTimeSinceReport [self timeUnits]
  (.getFinalTimeSinceReport self timeUnits))

(defn getFinalProductionRate [self productionRateUnits]
  (.getFinalProductionRate self productionRateUnits))

(defn getFireBackAtAttack [self]
  (.getFireBackAtAttack self))

(defn getFireBackAtReport [self]
  (.getFireBackAtReport self))

(defn getFireHeadAtAttack [self]
  (.getFireHeadAtAttack self))

(defn getFireHeadAtReport [self]
  (.getFireHeadAtReport self))

(defn getFirePerimeterPointCount [self]
  (.getFirePoints self))

(defn getFirePerimeterX [self]
  (.getFirePerimeterX self))

(defn getFirePerimeterY [self]
  (.getFirePerimeterY self))

(defn getFireSizeAtInitialAttack [self areaUnits]
  (.getFireSizeAtInitialAttack self areaUnits))

(defn getLengthToWidthRatio [self]
  (.getLengthToWidthRatio self))

(defn getPerimeterAtContainment [self lengthUnits]
  (.getPerimeterAtContainment self lengthUnits))

(defn getPerimeterAtInitialAttack [self lengthUnits]
  (.getPerimeterAtInitialAttack self lengthUnits))

(defn getResourcesUsed [self]
  (.getResourcesUsed self))

(defn getReportRate [self speedUnits]
  (.getReportRate self speedUnits))

(defn getReportSize [self areaUnits]
  (.getReportSize self areaUnits))

(defn getTactic [self]
  (.getTactic self))

(defn removeAllResources [self]
  (.removeAllResources self))

(defn removeAllResourcesWithThisDesc [self desc]
  (.removeAllResourcesWithThisDesc self desc))

(defn removeResourceAt [self index]
  (.removeResourceAt self index))

(defn removeResourceWithThisDesc [self desc]
  (.removeResourceWithThisDesc self desc))
