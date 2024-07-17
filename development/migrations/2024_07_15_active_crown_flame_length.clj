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

  ;; Fire Behavior Input Group
  (def fire-behavior-input-group
    (sm/t-key->eid db "behaveplus:crown:input:spotting:fire_behavior"))

  ;; Add Conditional

  (def new-cond-tx
    {:db/id                       fire-behavior-input-group
     :group/conditionals-operator :and
     :group/conditionals          (map
                                   (fn [uuid]
                                     (sm/->gv-conditional uuid :equal "false"))
                                   crown-fire-behavior-outputs)})

  (def tx (d/transact @ds/datomic-conn [new-cond-tx]))

  ;; Rollback
  #_(sm/rollback-tx! @ds/datomic-conn tx)

)
