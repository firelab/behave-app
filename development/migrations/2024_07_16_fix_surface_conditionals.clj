(ns migrations.2024-07-16-fix-surface-conditionals)

#_{:clj-kondo/ignore [:missing-docstring]}
(do
  (require '[behave-cms.server        :as cms]
           '[datomic.api              :as d]
           '[datomic-store.main       :as ds]
           '[schema-migrate.interface :as sm])

  (cms/init-db!)

  (def db (d/db @ds/datomic-conn))

  ;; Surface Submodules

  (def surface-submodules
    [(sm/t-key->eid db "behaveplus:surface:input:fuel_models")
     (sm/t-key->eid db "behaveplus:surface:input:fuel_moisture")])

  (def surface-submodules-existing-conds 
    (->> (d/pull-many db '[{:submodule/conditionals [:db/id]}] surface-submodules)
         (map :submodule/conditionals)
         (flatten)
         (map :db/id)))

  (def remove-existing-conds-tx (map (fn [eid] [:db/retractEntity eid]) surface-submodules-existing-conds))

  (d/transact @ds/datomic-conn remove-existing-conds-tx)

  ;; Surface Fire Behavior Outputs

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

  ;; Add Conditional

  (defn ->new-surface-conditional-tx [sm-eid]
    {:db/id                           sm-eid
     :submodule/conditionals-operator :or
     :submodule/conditionals
     (map
      (fn [uuid]
        (sm/->gv-conditional uuid :equal "true"))
      surface-outputs)})

  (def new-conditionals-tx (map ->new-surface-conditional-tx surface-submodules))

  (def tx (d/transact @ds/datomic-conn new-conditionals-tx))

  ;; Rollback
  #_(sm/rollback-tx! @ds/datomic-conn tx)

)
