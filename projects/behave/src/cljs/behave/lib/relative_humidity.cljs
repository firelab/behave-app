(ns behave.lib.relative-humidity)

(defn init []
  (js/Module.RelativeHumidityTool.))

;; Calculate
(defn calculate [self]
  (.calculate self))

;; Getters
(defn getDewPointTemperature [self temperatureUnits]
  (.getDewPointTemperature self temperatureUnits))

(defn getDryBulbTemperature [self temperatureUnits]
  (.getDryBulbTemperature self temperatureUnits))

(defn getSiteElevation [self lengthUnits]
  (.getSiteElevation self lengthUnits))

(defn getRelativeHumidity [self fractionUnits]
  (.getRelativeHumidity self fractionUnits))

(defn getWetBulbDepression [self temperatureUnits]
  (.getWetBulbDepression self temperatureUnits))

(defn getWetBulbTemperature [self temperatureUnits]
  (.getWetBulbTemperature self temperatureUnits))

;; Setters
(defn setDryBulbTemperature [self dryBulbTemperature temperatureUnits]
  (.setDryBulbTemperature self dryBulbTemperature temperatureUnits))

(defn setSiteElevation [self siteElevation lengthUnits]
  (.setSiteElevation self siteElevation lengthUnits))

(defn setWetBulbTemperature [self wetBulbTemperature temperatureUnits]
  (.setWetBulbTemperature self wetBulbTemperature temperatureUnits))

(def ^:export ns-public-fns (update-keys (ns-publics 'behave.lib.relative-humidity) name))
