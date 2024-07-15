(ns migrations.2024-07-15-active-crown-flame-length)

#_{:clj-kondo/ignore [:missing-docstring]}
(do
  (require '[behave-cms.server        :as cms]
           '[datomic.api              :as d]
           '[datomic-store.main       :as ds]
           '[schema-migrate.interface :as sm])

  (cms/init-db!)

  (def db (d/db @ds/datomic-conn))

  ;; Crown Fire Behavior
  (def crown-fire-behavior
    (sm/t-key->eid db "behaveplus:crown:output:fire_type:fire_behavior"))

  (def crown-fire-behavior-outputs
    (->> (d/pull db '[{:group/group-variables [:bp/uuid]}] crown-fire-behavior)
         (:group/group-variables)
         (map :bp/uuid)))

  ;; Active Crown Fire Length Input
  (def active-crown-fire-length-group
    (sm/t-key->eid db "behaveplus:crown:input:spotting:fire_behavior:active_crown_flame_length"))

  ;; Add Conditional

  (def active-crown-fire-length-tx
    {:db/id                       active-crown-fire-length-group
     :group/conditionals-operator :and
     :group/conditionals          (map
                                   (fn [uuid]
                                     (sm/->gv-conditional uuid :equal "false"))
                                   crown-fire-behavior-outputs)})

  (def tx (d/transact @ds/datomic-conn [active-crown-fire-length-tx]))

  ;; Rollback
  #_(sm/rollback-tx! @ds/datomic-conn tx)

)
