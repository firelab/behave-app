(ns migrations.2024-07-16-torching-trees-conditionals)

;; Removes the following Submodules if only Spot Outputs are selected
;; in Surface + Crown 
;; 
;; Crown
;; - Calculation Options
;; - Canopy Fuel
;; - Fuel Moisture

#_{:clj-kondo/ignore [:missing-docstring]}
(do
  (require '[behave-cms.server        :as cms]
           '[datomic.api              :as d]
           '[datomic-store.main       :as ds]
           '[schema-migrate.interface :as sm])

  (cms/init-db!)

  (def db (d/db @ds/datomic-conn))

  ;; Helpers

  (defn ->submodule-conditional-tx [sm-eid gv-uuids]
    {:db/id                           sm-eid
     :submodule/conditionals-operator :or
     :submodule/conditionals          (mapv #(sm/->gv-conditional % :equal "true") gv-uuids)})

  ;; Surface Submodules

  (def surface-submodules
    [(sm/t-key->eid db "behaveplus:surface:input:fuel_models")
     (sm/t-key->eid db "behaveplus:surface:input:fuel_moisture")])

  (def surface-submodules-existing-conds
    (->> (d/pull-many db '[{:submodule/conditionals [:db/id]}] surface-submodules)
         (map :submodule/conditionals)
         (flatten)
         (map :db/id)))

  ;; Remove existing Surface conditionals

  (def remove-existing-conds-tx (map (fn [eid] [:db/retractEntity eid]) surface-submodules-existing-conds))

  ;; Surface Fire Behavior/Size Outputs

  (def surface-outputs
    (->>
     [;; Fire Behavior
      "behaveplus:surface:output:fire_behavior:surface_fire:flame_length"
      "behaveplus:surface:output:fire_behavior:surface_fire:fireline_intensity"
      "behaveplus:surface:output:fire_behavior:surface_fire:rate_of_spread"
      ;; Size
      "behaveplus:surface:output:size:surface___fire_size:spread-distance"
      "behaveplus:surface:output:size:surface___fire_size:fire_area"
      "behaveplus:surface:output:size:surface___fire_size:fire_perimeter"
      "behaveplus:surface:output:size:surface___fire_size:length-to-width-ratio"]
     (map (partial sm/t-key->uuid db))))

  ;; Crown Fire Behavior Outputs
  (def crown-fire-behavior 
    (sm/t-key->eid db "behaveplus:crown:output:fire_type:fire_behavior"))

  (def crown-fire-behavior-outputs
    (->> (d/pull db '[{:group/group-variables [:bp/uuid]}] crown-fire-behavior)
         (:group/group-variables)
         (map :bp/uuid)))

  ;; Crown Fire Size Outputs
  (def crown-fire-size
    (sm/t-key->eid db "behaveplus:surface:output:size"))

  (def crown-fire-size-outputs
    (->> (d/pull db '[{:submodule/groups [{:group/group-variables [:bp/uuid]}]}] crown-fire-size)
         (:submodule/groups)
         (map #(->> % (:group/group-variables) (map :bp/uuid)))
         (flatten)))

  ;; Crown Fire Type Outputs
  
  (def crown-fire-type
    (sm/t-key->eid db "behaveplus:crown:output:fire_type"))

  (def crown-fire-type-outputs
    (->> (d/pull db '[{:submodule/groups [{:group/group-variables [:bp/uuid]}]}] crown-fire-type)
         (:submodule/groups)
         (map #(->> % (:group/group-variables) (map :bp/uuid)))
         (flatten)))

  (def crown-submodule-conditionals (concat crown-fire-behavior-outputs crown-fire-size-outputs crown-fire-type-outputs))

  (def surface-submodule-conditionals (concat surface-outputs crown-submodule-conditionals))

  ;; Crown Submodules to Enable
  (def enable-crown-submodules
    (map (partial sm/t-key->eid db)
         ["behaveplus:crown:input:canopy_fuel"
          "behaveplus:crown:input:fuel_moisture"
          "behaveplus:crown:input:calculation_options"]))

  (def enable-crown-submodules-tx (map #(->submodule-conditional-tx % crown-submodule-conditionals) enable-crown-submodules))

  (def enable-surface-submodules-tx (map #(->submodule-conditional-tx % surface-submodule-conditionals) surface-submodules))

  (comment
    (def tx (d/transact @ds/datomic-conn (concat remove-existing-conds-tx enable-crown-submodules-tx enable-surface-submodules-tx)))
    )

  (def db-after (d/db @ds/datomic-conn))

  (d/pull db-after '[:submodule/conditionals :submodule/conditionals-operator] (sm/t-key->eid db-after "behaveplus:crown:input:canopy_fuel"))

  ;; Rollback
  #_(sm/rollback-tx! @ds/datomic-conn tx)

  )
