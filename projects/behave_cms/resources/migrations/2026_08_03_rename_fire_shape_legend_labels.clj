(ns migrations.2026-08-03-rename-fire-shape-legend-labels
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Renames the Fire Shape diagram's legend labels (the English translations behind
;; each arrow/ellipse legend-id key). Only the displayed text changes; the
;; legend-id keys are unchanged. The wind_slope_spread_direction:* keys are used
;; only by the Fire Shape diagram now (the wind/slope diagram was removed), so this
;; affects only that legend.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Single-step migration: the runner calls (payload-fn db) at startup and
;; transacts the returned vector.

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (sm/update-translations-payload
   db
   "en-US"
   {"behaveplus:diagram:surface_fire_shape:legend_id:surface_fire"        "Fire Perimeter"
    "behaveplus:diagram:surface_fire_shape:legend_id:max_spread"          "Heading Fire"
    "behaveplus:diagram:wind_slope_spread_direction:legend_id:flanking_1" "Flanking 1 Fire"
    "behaveplus:diagram:wind_slope_spread_direction:legend_id:flanking_2" "Flanking 2 Fire"
    "behaveplus:diagram:wind_slope_spread_direction:legend_id:backing"    "Backing Fire"
    "behaveplus:diagram:surface_fire_shape:legend_id:slope"               "Slope Vector"
    "behaveplus:diagram:surface_fire_shape:legend_id:wind"                "Wind Vector"}))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server        :as cms])
  (cms/init-db!)

  (def conn (behave-cms.store/default-conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
