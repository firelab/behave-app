(ns migrations.2024-08-20-remove-waf-spot-only-outputs)

;; Removes Wind-Adjustment Factor in Surface + Crown
;; when only Spot outputs are selected

#_{:clj-kondo/ignore [:missing-docstring]}
(do
  (require '[behave-cms.server        :as cms]
           '[datomic.api              :as d]
           '[datomic-store.main       :as ds]
           '[schema-migrate.interface :as sm])

  (cms/init-db!)

  (def db (d/db @ds/datomic-conn))

  ;; Wind Adjustment Factor
  (def wind-adjustment-factor-group-eid
    (sm/t-key->eid db "behaveplus:surface:input:wind_speed:wind-adjustment-factor"))

  ;; Crown Spot Outputs

  (def crown-max-spot-distance 
    (sm/t-key->eid db "behaveplus:crown:output:spotting_active_crown_fire:maximum_spotting_distance"))

  (def crown-max-spot-distance-outputs
    (->> (d/entity db crown-max-spot-distance)
         (:group/group-variables)
         (map :bp/uuid)))

  ;; Add Conditionals
  (def wind-adjustment-factor-conds-tx
    {:db/id
     wind-adjustment-factor-group-eid

     :group/conditionals
     (mapv #(sm/->gv-conditional % :equal "false") crown-max-spot-distance-outputs)})

  (comment
    (def tx (d/transact @ds/datomic-conn [wind-adjustment-factor-conds-tx]))
    )

  (comment
    (sm/rollback-tx! @ds/datomic-conn tx)
    )
  )
