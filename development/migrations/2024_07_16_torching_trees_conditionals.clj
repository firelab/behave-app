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

  ;; Crown Fire Behavior Outputs
  (def crown-fire-behavior 
    (sm/t-key->eid db "behaveplus:crown:output:fire_type:fire_behavior"))

  (def crown-fire-behavior-outputs
    (->> (d/pull db '[{:group/group-variables [:bp/uuid]}] crown-fire-behavior)
         (:group/group-variables)
         (map :bp/uuid)))

  ;; Crown Fire Type Outputs
  
  (def crown-fire-type
    (sm/t-key->eid db "behaveplus:crown:output:fire_type"))

  (def crown-fire-type-outputs
    (->> (d/pull db '[{:submodule/groups [{:group/group-variables [:bp/uuid]}]}] crown-fire-type)
         (:submodule/groups)
         (map #(->> % (:group/group-variables) (map :bp/uuid)))
         (flatten)))

  (def outputs-for-conditionals (concat crown-fire-behavior-outputs crown-fire-type-outputs))

  ;; Submodules to Enable
  (def enable-crown-submodules
    (map (partial sm/t-key->eid db)
         ["behaveplus:crown:input:canopy_fuel"
          "behaveplus:crown:input:fuel_moisture"
          "behaveplus:crown:input:calculation_options"]))

  (defn new-conds [sm-eid]
    {:db/id                           sm-eid
     :submodule/conditionals-operator :or
     :submodule/conditionals
     (map #(sm/->gv-conditional % :equal "true") outputs-for-conditionals)})

  (def enable-crown-submodules-tx (map new-conds enable-crown-submodules))

  (d/transact @ds/datomic-conn enable-crown-submodules-tx)

  (def db-after (d/db @ds/datomic-conn))

  (d/pull db-after '[:submodule/conditionals :submodule/conditionals-operator] (sm/t-key->eid db-after "behaveplus:crown:input:canopy_fuel"))

  ;; Rollback
  #_(sm/rollback-tx! @ds/datomic-conn tx)

  )
