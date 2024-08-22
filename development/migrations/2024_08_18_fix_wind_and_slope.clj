(ns migrations.2024-08-18-fix-wind-and-slope)

;; Adds Size Inputs from Surface and Crown to Wind and Slope

#_{:clj-kondo/ignore [:missing-docstring]}
(do
  (require '[clojure.set              :as set]
           '[behave-cms.server        :as cms]
           '[datomic.api              :as d]
           '[datomic-store.main       :as ds]
           '[schema-migrate.interface :as sm])

  (cms/init-db!)

  (def db (d/db @ds/datomic-conn))

  ;; Helpers
  (defn sets-to-vector [sets]
    (vec (apply set/union sets)))

  ;; Wind and Slope Groups

  (def wind-and-slope-are-group-eid
    (sm/t-key->eid db "behaveplus:surface:input:wind_speed:wind_and_slope_are"))

  (def slope-group-eid
    (sm/t-key->eid db "behaveplus:surface:input:wind_speed:slope"))

  ;; Surface Fire Size Outputs

  (def surface-size-outputs
    (->>
     [;; Size
      "behaveplus:surface:output:size:surface___fire_size:spread-distance"
      "behaveplus:surface:output:size:surface___fire_size:fire_area"
      "behaveplus:surface:output:size:surface___fire_size:fire_perimeter"
      "behaveplus:surface:output:size:surface___fire_size:length-to-width-ratio"]
     (map (partial sm/t-key->uuid db))))

  ;; Crown Fire Size Outputs

  (def crown-fire-size-outputs
    (->>
     ["behaveplus:crown:output:size:fire_area:fire_area"
      "behaveplus:crown:output:size:fire_perimeter:fire_perimeter"
      "behaveplus:crown:output:size:length_to_width_ratio:length_to_width_ratio"
      "behaveplus:crown:output:size:spread_distance:spread_distance"]
     (map (partial sm/t-key->uuid db))))

  ;; Crown Fire Type Outputs

  (def crown-fire-type-groups
    [(sm/t-key->eid db "behaveplus:crown:output:fire_type:active_or_independent_crown_fire")
     (sm/t-key->eid db "behaveplus:crown:output:fire_type:transition_to_crown_fire")])

  (def crown-fire-type-outputs
    (->> (map (partial d/entity db) crown-fire-type-groups)
         (map :group/group-variables)
         (flatten)
         (sets-to-vector)
         (map :bp/uuid)))

  ;; Add Conditionals
  (def wind-and-slope-are-conds-tx
    {:db/id
     wind-and-slope-are-group-eid

     :group/conditionals
     (mapv #(sm/->gv-conditional % :equal "true") (concat surface-size-outputs crown-fire-size-outputs crown-fire-type-outputs))})

  (def slope-conds-tx
    {:db/id
    slope-group-eid

     :group/conditionals
     (mapv #(sm/->gv-conditional % :equal "true") (concat surface-size-outputs crown-fire-size-outputs crown-fire-type-outputs))})

  (comment
    (def tx (d/transact @ds/datomic-conn [wind-and-slope-are-conds-tx slope-conds-tx]))
    )

  (comment
    (sm/rollback-tx! @ds/datomic-conn tx)
    )
  )
