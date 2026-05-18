(ns migrations.2026-05-18-set-calculator-variable-min-max
  (:require [behave-cms.server :as cms]
            [behave-cms.store :refer [default-conn]]
            [datomic.api :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================
;; Sets :variable/minimum and :variable/maximum for calculator (tool) variables.
;;
;; All values are in native (English) units. The app handles unit conversion
;; for display when the user selects metric units.
;;
;; Probability of Ignition:
;;   1-h Fuel Moisture:          1 - 60
;;   Air Temperature:            -40 - 120 (F)
;;   Fuel Shading from the Sun:  0 - 100
;;
;; Vapor Pressure Deficit:
;;   Air Temperature:            -40 - 120 (F)
;;   Relative Humidity:          0 - 100
;;
;; Relative Humidity:
;;   Dry Bulb Temperature:       -40 - 120 (F)
;;   Wet Bulb Temperature:       -40 - 120 (F)
;;   Site Elevation:             0 - 10,000
;;
;; SSD / SZS:
;;   Vegetation Height:          0 - 300 (ft)

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id             (sm/name->eid conn :variable/name "1-h Fuel Moisture")
    :variable/minimum  1.0
    :variable/maximum  60.0}

   {:db/id             (sm/name->eid conn :variable/name "Air Temperature")
    :variable/minimum  -40.0
    :variable/maximum  120.0}

   {:db/id             (sm/name->eid conn :variable/name "Fuel Shading from the Sun")
    :variable/minimum  0.0
    :variable/maximum  100.0}

   {:db/id             (sm/name->eid conn :variable/name "Relative Humidity")
    :variable/minimum  0.0
    :variable/maximum  100.0}

   {:db/id             (sm/name->eid conn :variable/name "Dry Bulb Temperature")
    :variable/minimum  -40.0
    :variable/maximum  120.0}

   {:db/id             (sm/name->eid conn :variable/name "Wet Bulb Temperature")
    :variable/minimum  -40.0
    :variable/maximum  120.0}

   {:db/id             (sm/name->eid conn :variable/name "Site Elevation")
    :variable/minimum  0.0
    :variable/maximum  10000.0}

   {:db/id             (sm/name->eid conn :variable/name "Vegetation Height")
    :variable/minimum  0.0
    :variable/maximum  300.0}])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Verify
;; ===========================================================================================================

(comment
  (let [variable-names ["1-h Fuel Moisture"
                        "Air Temperature"
                        "Fuel Shading from the Sun"
                        "Relative Humidity"
                        "Dry Bulb Temperature"
                        "Wet Bulb Temperature"
                        "Site Elevation"
                        "Vegetation Height"]]
    (doseq [vname variable-names]
      (let [eid (sm/name->eid conn :variable/name vname)
            e   (d/entity (d/db conn) eid)]
        (println (format "%-30s min: %-8s max: %s"
                         vname
                         (:variable/minimum e)
                         (:variable/maximum e)))))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
