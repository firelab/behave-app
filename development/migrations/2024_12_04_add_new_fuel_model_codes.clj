(ns migrations.2024-12-04-add-new-fuel-model-codes
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(defn- csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data)
            (map #(str/replace % #"\(.*" "")) ; remove units
            (map #(str/replace % #" " "")) ; remove spaces
            (map #(str/replace % #"\n" "")) ; remove new line
            (map keyword)
            repeat)
       (rest csv-data)
       #_(map #(map edn/read-string %) (rest csv-data))))

(def csv-data
  (with-open [reader (io/reader (io/resource "public/csv/2024_12_04_new_fuel_model_codes.csv"))]
    (csv-data->maps (doall (csv/read-csv reader)))))

(first csv-data)
#_{:FuelBedDepthft               4,
   :FuelModelCode                150,
   :DeadFuelHeatContent          8000,
   :FuelModelType                Static,
   :100-hFuelLoad                1,
   :LiveWoodySA/V                640,
   :LiveWoodyFuelLoad            2,
   :LiveherbaceousSA/V           2200,
   :LiveHerbaceousFuelLoad       2,
   :10-hFuelLoad                 1,
   :FuelModelDescription         Chamise,
   :1-hSA/V                      640,
   :FuelModelName                SCAL17,
   :1-hFuelLoad                  1.3,
   :DeadFuelMoistureofExtinction 20,
   :LiveFuelHeatContent          8000}

#_{:clj-kondo/ignore [:missing-docstring]}
(defn build-translation-key
  [data parent-translation-key]
  (str parent-translation-key
       ":"
       (str/lower-case (:FuelModelName data)) "-"
       (:FuelModelCode data)))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn build-fuel-model-name
  [data]
  (str (:FuelModelName data) "/"
       (:FuelModelCode data) " - "
       (:FuelModelDescription data)))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn build-fuel-short-name
  [data]
  (str (:FuelModelName data) "/" (:FuelModelCode data)))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn list-option-starting-order
  [conn list-eid idx]
  (let [highest-existing-order (->> list-eid
                                    (d/entity (d/db conn))
                                    :list/options
                                    (map :list-option/order)
                                    (apply max))]
    (+ (inc idx) highest-existing-order)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id        (sm/name->eid conn :list/name "SurfaceFuelModels")
    :list/options (sm/postwalk-insert
                   (map-indexed
                    (fn [idx d]
                      {:list-option/name                   (build-fuel-model-name d)
                       :list-option/value                  (:FuelModelCode d)
                       :list-option/translation-key        (build-translation-key
                                                            d "behaveplus:list-option:surface-fuel-models")
                       :list-option/result-translation-key (build-translation-key
                                                            d "behaveplus:list-option:result:surface-fuel-models")
                       :list-option/hide?                  true
                       :list-option/order                  (list-option-starting-order
                                                            conn
                                                            (sm/name->eid conn :list/name "SurfaceFuelModels")
                                                            idx)})
                    csv-data))}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def translations
  (reduce
   (fn [acc d]
     (merge acc
            (let [t-key        (build-translation-key
                                d
                                "behaveplus:list-option:surface-fuel-models")
                  result-t-key (build-translation-key
                                d
                                "behaveplus:list-option:result:surface-fuel-models")]
              {t-key        (build-fuel-model-name d)
               result-t-key (build-fuel-short-name d)})))
   {}
   csv-data))

#_{:clj-kondo/ignore [:missing-docstring]}
(def translations-payload
  (sm/build-translations-payload
   conn
   translations))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn (concat payload translations-payload))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
