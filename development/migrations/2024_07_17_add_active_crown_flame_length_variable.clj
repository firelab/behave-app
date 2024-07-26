(ns migrations.2024-07-17-add-active-crown-flame-length-variable)

#_{:clj-kondo/ignore [:missing-docstring]}
(do
  (require '[behave-cms.server        :as cms]
           '[datomic.api              :as d]
           '[datomic-store.main       :as ds]
           '[schema-migrate.interface :as sm]
           '[cms-import               :refer [add-export-file-to-conn]])

  (cms/init-db!)

  (def conn @ds/datomic-conn)
  (def db (d/db @ds/datomic-conn))

  ;; Import new SIGSpot::setActiveCrownFlameLength Setter

  (add-export-file-to-conn "cms-exports/SIGSpot.edn" conn)

  ;; Add new Hidden Subgroup

  (def fire-behavior-input-group
    (sm/t-key->eid db "behaveplus:crown:input:spotting:fire_behavior"))

  (def new-subgroup-tx
    (-> (sm/->subgroup fire-behavior-input-group
                       "Active Crown Flame Length (Hidden)"
                       "behaveplus:crown:input:spotting:fire_behavior:active_crown_flame_length_hidden")
        (merge {:db/id                       -1
                :group/conditionals-operator :or
                :group/conditionals          [(sm/->module-conditional :equal ["surface" "crown"])]})))

  ;; Add Group Variable to 'Active Crown Flame Length (Hidden)' with new `SIGSpot::setActiveCrownLength` fn

  (def active-crown-flame-length-variable
    (sm/name->eid conn :variable/name "Active Crown Flame Length"))

  (def new-group-variable-tx
    (-> (sm/->group-variable (:db/id new-subgroup-tx)
                             active-crown-flame-length-variable
                             (str (:group/translation-key new-subgroup-tx) ":active_crown_flame_length"))
        (merge {:db/id                        -2
                :group-variable/cpp-namespace (sm/cpp-ns->uuid conn "global")
                :group-variable/cpp-class     (sm/cpp-class->uuid conn "SIGSpot")
                :group-variable/cpp-function  (sm/cpp-fn->uuid conn "setActiveCrownFlameLength")
                :group-variable/cpp-parameter (sm/cpp-param->uuid conn "setActiveCrownFlameLength" "flameLength")})))

  ;; Link Final Flame Length with Active Crown Flame Length (Hidden)
  (def crown-final-flame-length
    (sm/t-key->eid db "behaveplus:crown:output:fire_type:fire_behavior:flame_length"))

  (def link-tx
    (sm/->link crown-final-flame-length (:db/id new-group-variable-tx)))

  (comment
    (def tx (d/transact @ds/datomic-conn [new-subgroup-tx new-group-variable-tx link-tx]))
    )

  (comment
    (sm/rollback-tx! @ds/datomic-conn tx)
    )

  )
