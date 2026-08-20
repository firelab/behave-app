(ns migrations.2026-08-20-update-fire-shape-legend-labels
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Rename three Fire Shape diagram legend labels (en-US), originally set by
;; 2026_08_04_fire_shape_diagram.clj:
;;   - "Surface Fire Spread" -> "Resultant Vector"
;;   - "Flanking 1 Fire"     -> "Flanking (Right)"
;;   - "Flanking 2 Fire"     -> "Flanking (Left)"
;;
;; All three translation keys already exist, so sm/upsert-translations updates them
;; in place (update-if-present / create-if-absent), scoped to the en-US language.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (sm/upsert-translations
   db "en-US"
   {"behaveplus:diagram:surface_fire_shape:legend_id:surface_fire_spread" "Resultant Vector"
    "behaveplus:diagram:wind_slope_spread_direction:legend_id:flanking_1" "Flanking (Right)"
    "behaveplus:diagram:wind_slope_spread_direction:legend_id:flanking_2" "Flanking (Left)"}))

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
