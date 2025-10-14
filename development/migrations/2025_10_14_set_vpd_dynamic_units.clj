(ns migrations.2025-10-14-set-vpd-dynamic-units
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Set :subtool-variable/dynamic-units? to true for Vapor Pressure Deficit output variable

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Find VPD Output Subtool Variable
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def vpd-output-eid (sm/t-key->eid conn "behaveplus:vapor-pressure-deficit:vapor-pressure-deficit:vapor-pressure-deficit"))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id vpd-output-eid
    :subtool-variable/dynamic-units? true}])

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
