(ns migrations.2026-03-21-hide-waf-for-spot-only
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Hide the Wind Adjustment Factor (WAF) input group when only "Max Spotting Distance
;; from Wind-Driven Surface Fire" is selected (remove it from sub-conditional).
;; Also add "Midflame Wind Speed" as a new sub-conditional so that WAF shows
;; when that output is enabled.

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
  (sm/t-key->uuid conn "behaveplus:surface:output:spot:maximum_spotting_distance:wind_driven_surface_fire"))

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

#_{:clj-kondo/ignore [:missing-docstring]}
(def midflame-uuid
  (sm/t-key->uuid conn "behaveplus:surface:output:wind-and-fuel:wind:midflame-eye-level-wind-speed"))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [;; Remove wind_driven_surface_fire from sub-conditionals
   [:db/retract waf-cond-eid
    :conditional/sub-conditionals wind-driven-sub-cond-eid]
   ;; Add midflame wind speed as a new sub-conditional
   {:db/id                          waf-cond-eid
    :conditional/sub-conditionals
    [(sm/postwalk-insert
      {:conditional/group-variable-uuid midflame-uuid
       :conditional/type                :group-variable
       :conditional/operator            :equal
       :conditional/values              "true"})]}])

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
