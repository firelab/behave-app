(ns migrations.2026-01-22-fix-erroneous-subgroup-downwind-canopy-height
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; The subgroup Downwind Canopy Height under the Topography Group has a referenece to a variable entity that erroneously also has group attributes. When deleting this group it also deleted the references to this variable from other group-variables.

;; Steps to fix:
;; 1. remove all group attributes from the variable entity
;; 2. remove reference to this erroneous subgroup from the group Topography

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))



;; ===========================================================================================================
;; Payload
;; ===========================================================================================================
(def variable-downwind-canopy-height-eid (sm/t-key->eid conn "behaveplus:crown:input:spotting:topography:downwind-canopy-height"))

(def submodule-topography-eid (sm/t-key->eid conn "behaveplus:crown:input:spotting:topography"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload [[:db/retract variable-downwind-canopy-height-eid :group/name]
              [:db/retract variable-downwind-canopy-height-eid :group/help-key]
              [:db/retract variable-downwind-canopy-height-eid :group/translation-key]
              [:db/retract variable-downwind-canopy-height-eid :group/result-translation-key]
              [:db/retract submodule-topography-eid :group/children variable-downwind-canopy-height-eid]])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e  (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
