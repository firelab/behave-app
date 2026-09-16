(ns migrations.2026-09-15-update-settings-units
  (:require [clojure.set              :as set]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; BHP1-1653 — Updated units in Settings (extension of BHP1-1607).
;;
;; Updates Custom Unit Settings to:
;;   Contain
;;     Line Production Rate     ch/h, m/h
;;     Containment Distances    ch, m
;;   Fire & Effects
;;     Fire Area                ac, ha
;;     Fire Perimeter           ch, m
;;     Spread Distance          ch, m, ft, mi, km
;;   Fuel & Vegetation
;;     Fuel Bulk Density        lb/ft3, kg/m3
;;     Fuel Heat Content        Btu/lb, kJ/kg  (repoint to "Heat of Combustion" "domain)
;;     Ground Fuel Depth        REMOVED
;;     Surface Fuel Depth       ft, m, in, cm
;;   Terrain & Spotting
;;     Firebrand & Cover Height ft, m
;;     Spotting Distance        mi, km
;;   Time & Map
;;     Flame Residence Time     min, h, s
;;

;; ===========================================================================================================
;; Helpers
;; ===========================================================================================================

(defn- domain-eid [db domain-name]
  (d/q '[:find ?e . :in $ ?n :where [?e :domain/name ?n]] db domain-name))

(defn- domain-set-eid [db set-name]
  (d/q '[:find ?e . :in $ ?n :where [?e :domain-set/name ?n]] db set-name))

(defn- dimension-uuid [db dim-name]
  (d/q '[:find ?uuid . :in $ ?n
         :where [?e :dimension/name ?n] [?e :bp/uuid ?uuid]]
       db dim-name))

(defn- dimension-unit-uuid [db dim-name short-code]
  (d/q '[:find ?uuid . :in $ ?dn ?sc
         :where
         [?d :dimension/name ?dn]
         [?d :dimension/units ?u]
         [?u :unit/short-code ?sc]
         [?u :bp/uuid ?uuid]]
       db dim-name short-code))

(defn- domain-filtered-unit-uuids [db domain-name]
  (set (d/q '[:find [?uuid ...] :in $ ?n
              :where [?e :domain/name ?n] [?e :domain/filtered-unit-uuids ?uuid]]
            db domain-name)))

(defn- set-filter-tx
  "Tx data making `short-codes` (units of `dim-name`) the exact filter list of
  domain `domain-name`, retracting any previously filtered unit not in it."
  [db domain-name dim-name short-codes]
  (let [eid       (domain-eid db domain-name)
        new-uuids (mapv #(dimension-unit-uuid db dim-name %) short-codes)
        stale     (set/difference (domain-filtered-unit-uuids db domain-name)
                                  (set new-uuids))]
    (into [{:db/id eid :domain/filtered-unit-uuids new-uuids}]
          (map (fn [uuid] [:db/retract eid :domain/filtered-unit-uuids uuid]) stale))))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring :unused-binding]}
(defn payload-fn [db]
  (let [ground-fuel-depth (domain-eid db "Ground Fuel Depth")
        duff-litter-depth (d/q '[:find ?e . :where [?e :variable/name "Duff & Litter Depth"]] db)]
    (concat
     ;; Contain
     (set-filter-tx db "Line Production Rate" "Speed" ["ch/h" "m/h"])
     [{:db/id                   (domain-eid db "Line Production Rate")
       :domain/metric-unit-uuid (dimension-unit-uuid db "Speed" "m/h")}]
     (set-filter-tx db "Containment Distances" "Length" ["ch" "m"])

     ;; Fire & Effects
     (set-filter-tx db "Fire Area" "Area" ["ac" "ha"])
     (set-filter-tx db "Fire Perimeter" "Length" ["ch" "m"])
     (set-filter-tx db "Spread Distance" "Length" ["ch" "m" "ft" "mi" "km"])

     ;; Fuel & Vegetation
     (set-filter-tx db "Fuel Bulk Density" "Density" ["lb/ft3" "kg/m3"])
     [{:db/id                 (domain-eid db "Fuel Heat Content")
       :domain/dimension-uuid (dimension-uuid db "Heat of Combustion")}]
     (set-filter-tx db "Fuel Heat Content" "Heat of Combustion" ["Btu/lb" "kJ/kg"])
     [[:db/retract (domain-set-eid db "Fuel & Vegetation") :domain-set/domains ground-fuel-depth]
      [:db/retract duff-litter-depth :variable/domain-uuid
       (:bp/uuid (d/pull db [:bp/uuid] ground-fuel-depth))]
      [:db/retractEntity ground-fuel-depth]]
     (set-filter-tx db "Surface Fuel Depth" "Length" ["ft" "m" "in" "cm"])

     ;; Terrain & Spotting
     (set-filter-tx db "Firebrand & Cover Height" "Length" ["ft" "m"])
     (set-filter-tx db "Spotting Distance" "Length" ["mi" "km"])

     ;; Time & Map
     (set-filter-tx db "Flame Residence Time" "Time" ["min" "h" "s"]))))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server :as cms]
           '[behave-cms.store  :as store])
  (cms/init-db!)

  (def conn (store/default-conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
