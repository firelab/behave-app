(ns migrations.2026-03-21-hide-waf-for-spot-only
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Hide the Wind Adjustment Factor (WAF) input group when only "Max Spotting Distance
;; from Wind-Driven Surface Fire" is selected (remove it from sub-conditional)

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Helpers
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def wind-driven-uuid
  (:bp/uuid (sm/t-key->entity conn
              "behaveplus:surface:output:spot:maximum_spotting_distance:wind_driven_surface_fire")))

#_{:clj-kondo/ignore [:missing-docstring]}
(def waf-cond-eid
  (->> (sm/t-key->entity conn "behaveplus:surface:input:wind_speed:wind-adjustment-factor")
       :group/conditionals
       (filter #(:conditional/sub-conditionals (d/entity (d/db conn) (:db/id %))))
       first
       :db/id))

;; sub-conditional that references wind_driven_surface_fire
#_{:clj-kondo/ignore [:missing-docstring]}
(def wind-driven-sub-cond-eid
  (->> (d/entity (d/db conn) waf-cond-eid)
       :conditional/sub-conditionals
       (filter #(= (:conditional/group-variable-uuid %) wind-driven-uuid))
       first
       :db/id))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [[:db/retract waf-cond-eid
    :conditional/sub-conditionals wind-driven-sub-cond-eid]])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
