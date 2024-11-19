(ns migrations.2024-08-22-remove-canopy-cover)

;; Removes Canopy Cover Group & Group Variable

#_{:clj-kondo/ignore [:missing-docstring]}
(do
  (require '[behave-cms.server        :as cms]
           '[datomic.api              :as d]
           '[datomic-store.main       :as ds]
           '[schema-migrate.interface :as sm])

  (cms/init-db!)

  (def db (d/db @ds/datomic-conn))

  ;; Canopy Cover

  (def canopy-cover-group-eid
    (sm/t-key->eid db "behaveplus:crown:input:canopy_fuel:canopy-cover"))

  (sm/t-key->eid db "behaveplus:crown:input:canopy_fuel:canopy-cover:canopy-cover")

  (def remove-tx [:db/retractEntity canopy-cover-group-eid])

  (def remove-i18ns-tx (sm/remove-nested-i18ns-tx @ds/datomic-conn "behaveplus:crown:input:canopy_fuel:canopy-cover"))

  (comment
    (def tx (d/transact @ds/datomic-conn (concat [remove-tx] remove-i18ns-tx)))
    )

  (comment
    (sm/rollback-tx! @ds/datomic-conn tx)
    )
  )
