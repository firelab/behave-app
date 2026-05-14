(ns migrations.2026-05-14-fix-direction-of-interest-vms
  (:require [behave-cms.server        :as cms]
            [behave-cms.store         :refer [default-conn]]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Linking Group Variable Direciton of Interest to the variable direction interest

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def direction-of-interest-eid
  (sm/t-key->eid conn "behaveplus:surface:input:directions_of_surface_spread__wind:surface_fire_wind__spread:direction-of-interest:direction-of-interest"))

(def direction-of-interest-variable-eid
  (sm/name->eid conn :variable/name "Direction of Interest"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload [{:db/id                    direction-of-interest-variable-eid
               :variable/group-variables [direction-of-interest-eid]}])

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
