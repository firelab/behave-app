(ns migrations.2024-08-19-fix-surface-input-downwind-canopy-cover
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))


(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id (sm/name->eid conn :variable/name "Downwind Canopy Cover")
    :variable/group-variables [(sm/t-key->eid conn "behaveplus:surface:input:spot:canopy_fuel:downwind_canopy_cover:downwind_canopy_cover")]}])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
