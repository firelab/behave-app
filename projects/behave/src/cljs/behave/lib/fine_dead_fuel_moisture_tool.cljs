(ns behave.lib.fine-dead-fuel-moisture-tool)

(defn init []
  (js/Module.SIGFineDeadFuelMoistureTool.))

(defn calculate [self]
  (.calculate self))

(defn calculateByIndex [self aspectIndex dryBulbIndex elevationIndex monthIndex relativeHumidityIndex shadingIndex slopeIndex timeOfDayIndex]
  (.calculateByIndex self aspectIndex dryBulbIndex elevationIndex monthIndex relativeHumidityIndex shadingIndex slopeIndex timeOfDayIndex))

(defn getAspectIndexSize [self]
  (.getAspectIndexSize self))

(defn getAspectLabelAtIndex [self index]
  (.getAspectLabelAtIndex self index))

(defn getCorrectionMoisture [self units]
  (.getCorrectionMoisture self units))

(defn getDryBulbTemperatureIndexSize [self]
  (.getDryBulbTemperatureIndexSize self))

(defn getDryBulbTemperatureLabelAtIndex [self index]
  (.getDryBulbTemperatureLabelAtIndex self index))

(defn getElevationIndexSize [self]
  (.getElevationIndexSize self))

(defn getElevationLabelAtIndex [self index]
  (.getElevationLabelAtIndex self index))

(defn getFineDeadFuelMoisture [self units]
  (.getFineDeadFuelMoisture self units))

(defn getMonthIndexSize [self]
  (.getMonthIndexSize self))

(defn getMonthLabelAtIndex [self index]
  (.getMonthLabelAtIndex self index))

(defn getReferenceMoisture [self units]
  (.getReferenceMoisture self units))

(defn getRelativeHumidityIndexSize [self]
  (.getRelativeHumidityIndexSize self))

(defn getRelativeHumidityLabelAtIndex [self index]
  (.getRelativeHumidityLabelAtIndex self index))

(defn getShadingIndexSize [self]
  (.getShadingIndexSize self))

(defn getShadingLabelAtIndex [self index]
  (.getShadingLabelAtIndex self index))

(defn getSlopeIndexSize [self]
  (.getSlopeIndexSize self))

(defn getSlopeLabelAtIndex [self index]
  (.getSlopeLabelAtIndex self index))

(defn getTimeOfDayIndexSize [self]
  (.getTimeOfDayIndexSize self))

(defn getTimeOfDayLabelAtIndex [self index]
  (.getTimeOfDayLabelAtIndex self index))

(defn operator= [self rhs]
  (.operator= self rhs))

(defn setAspectIndex [self aspectIndex]
  (.setAspectIndex self aspectIndex))

(defn setDryBulbIndex [self dryBulbIndex]
  (.setDryBulbIndex self dryBulbIndex))

(defn setElevationIndex [self elevationIndex]
  (.setElevationIndex self elevationIndex))

(defn setMonthIndex [self monthIndex]
  (.setMonthIndex self monthIndex))

(defn setRHIndex [self relativeHumidityIndex]
  (.setRHIndex self relativeHumidityIndex))

(defn setShadingIndex [self shadingIndex]
  (.setShadingIndex self shadingIndex))

(defn setSlopeIndex [self slopeIndex]
  (.setSlopeIndex self slopeIndex))

(defn setTimeOfDayIndex [self timeOfDayIndex]
  (.setTimeOfDayIndex self timeOfDayIndex))

(def ^:export ns-public-fns (update-keys (ns-publics 'behave.lib.fine-dead-fuel-moisture-tool) name))
