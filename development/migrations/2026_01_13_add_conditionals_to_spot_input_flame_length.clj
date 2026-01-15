(ns migrations.2026-01-13-add-conditionals-to-spot-input-flame-length
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def conditionals-to-add
  (map
   #(sm/->conditional conn
                      {:ttype               :group-variable
                       :operator            :equal
                       :values              #{"false"}
                       :group-variable-uuid %})
   (map #(sm/t-key->uuid conn %)
        ["behaveplus:surface:output:fire_behavior:surface_fire:rate_of_spread"
         "behaveplus:surface:output:fire_behavior:surface_fire:flame_length"
         "behaveplus:surface:output:fire_behavior:surface_fire:fireline_intensity"
         "behaveplus:surface:output:fire_behavior:ignition"
         "behaveplus:surface:output:size:surface___fire_size:fire_perimeter"
         "behaveplus:surface:output:size:surface___fire_size:fire_area"
         "behaveplus:surface:output:size:surface___fire_size:length-to-width-ratio"
         "behaveplus:surface:output:size:surface___fire_size:spread-distance"
         "behaveplus:surface:output:size:surface___fire_size:fire-shape-diagram"
         "behaveplus:surface:output:size:surface___fire_size:fire_area"
         "behaveplus:surface:output:spot:burning_pile"
         "behaveplus:surface:output:wind-and-fuel:fuel:total-dead-herbaceous-fuel-load"
         "behaveplus:surface:output:wind-and-fuel:fuel:total-dead-fuel-load"
         "behaveplus:surface:output:wind-and-fuel:fuel:total-live-fuel-load"
         "behaveplus:surface:output:wind-and-fuel:wind:midflame-eye-level-wind-speed"])))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id              (sm/t-key->eid conn "behaveplus:surface:input:spot:surface-fire-flame-length")
    :group/conditionals conditionals-to-add}])

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
