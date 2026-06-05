(ns migrations.2026-05-28-add-diagram-legend-id-translations
  (:require [behave-cms.server        :as cms]
            [behave-cms.store         :refer [default-conn]]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;;
;; Seeds English translations for all diagram legend-id translation keys introduced when the
;; ellipse/legend-id, arrow/legend-id, and scatter-plot/legend-id values were converted from
;; literal display strings to i18n keys.
;;
;; Keys added (13 total):
;;   contain (3): fire_perimeter_at_report, fire_perimeter_at_attack, fireline_constructed
;;   optimized_contain (1): production_rate_vs_containment_area
;;   surface_fire_shape (4): surface_fire, wind, max_spread, slope
;;   wind_slope_spread_direction (5): max_spread, flanking_1, flanking_2, backing, wind, interest
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
(def payload
  (sm/build-translations-payload
   conn
   {"behaveplus:diagram:contain:legend_id:fire_perimeter_at_report"                      "Fire Perimeter at Report"
    "behaveplus:diagram:contain:legend_id:fire_perimeter_at_attack"                      "Fire Perimeter at Attack"
    "behaveplus:diagram:contain:legend_id:fireline_constructed"                          "Fireline Constructed"
    "behaveplus:diagram:optimized_contain:legend_id:production_rate_vs_containment_area" "Production Rate vs Containment Area"
    "behaveplus:diagram:surface_fire_shape:legend_id:surface_fire"                       "Surface Fire"
    "behaveplus:diagram:surface_fire_shape:legend_id:wind"                               "Wind"
    "behaveplus:diagram:surface_fire_shape:legend_id:max_spread"                         "Max Spread"
    "behaveplus:diagram:surface_fire_shape:legend_id:slope"                              "Slope"
    "behaveplus:diagram:wind_slope_spread_direction:legend_id:max_spread"                "Max Spread"
    "behaveplus:diagram:wind_slope_spread_direction:legend_id:flanking_1"                "Flanking 1"
    "behaveplus:diagram:wind_slope_spread_direction:legend_id:flanking_2"                "Flanking 2"
    "behaveplus:diagram:wind_slope_spread_direction:legend_id:backing"                   "Backing"
    "behaveplus:diagram:wind_slope_spread_direction:legend_id:wind"                      "Wind"
    "behaveplus:diagram:wind_slope_spread_direction:legend_id:interest"                  "Interest"}))

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
