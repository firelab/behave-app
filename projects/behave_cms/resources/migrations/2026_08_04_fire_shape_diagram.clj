(ns migrations.2026-08-04-fire-shape-diagram
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Consolidated migration for the Fire Shape diagram rework (this branch). Replaces
;; the five per-step migrations (2026_07_27 … 2026_08_03). It:
;;
;;   1. Removes the standalone :wind-slope-spread-direction diagram (folded into
;;      Fire Shape): the diagram entity, its output group-variable, and any
;;      submodule conditionals keyed on that group-variable.
;;   2. Configures the Fire Shape diagram entity:
;;        - summary table GVs: outputs = Direction of Heading/Flanking/Backing,
;;          input = Wind Direction (degrees) (both Surface wind-direction contexts);
;;          resolved by translation-key via sm/t-key->eid (matches both
;;          :group-variable/translation-key and :result-translation-key).
;;        - y-axis end-label translation keys (Upslope / Downslope).
;;        - hide-axis-numbers? true.
;;   3. Upserts the en-US translations for the y-axis end labels and the renamed
;;      legend labels.
;;
;; Idempotent: guarded retractions, re-asserted attribute values are no-ops, and
;; translations are updated-if-present / created-if-absent — safe to run on a fresh
;; DB or on a partially-migrated one. Schema attributes themselves are installed
;; separately via `s/migrate! all-schemas` (behave.schema.diagrams / .worksheet).

;; ===========================================================================================================
;; Helpers
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- translation-exists? [db t-key]
  (boolean (d/q '[:find ?e . :in $ ?k :where [?e :translation/key ?k]] db t-key)))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- upsert-translations
  "Update existing translations (by shortcode) and create any that don't exist yet."
  [db shortcode t-key->text]
  (let [{to-update true to-create false} (group-by (comp #(translation-exists? db %) key) t-key->text)]
    (concat (when (seq to-create) (sm/build-translations-payload db (into {} to-create)))
            (when (seq to-update) (sm/update-translations-payload db shortcode (into {} to-update))))))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Single-step migration: the runner calls (payload-fn db) at startup and
;; transacts the returned vector.

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [fire-shape-eid (d/q '[:find ?e . :in $ :where [?e :diagram/type :fire-shape]] db)
        ;; wind/slope diagram to remove, plus its output GV + conditionals
        ws-eid         (d/q '[:find ?e . :in $ :where [?e :diagram/type :wind-slope-spread-direction]] db)
        ws-gv-eid      (when ws-eid
                         (d/q '[:find ?gv . :in $ ?d :where [?d :diagram/group-variable ?gv]] db ws-eid))
        ws-gv-uuid     (when ws-gv-eid
                         (d/q '[:find ?uuid . :in $ ?gv :where [?gv :bp/uuid ?uuid]] db ws-gv-eid))
        ws-cond-eids   (when ws-gv-uuid
                         (d/q '[:find [?c ...] :in $ ?uuid
                                :where [?c :conditional/group-variable-uuid ?uuid]]
                              db ws-gv-uuid))]
    (concat
     ;; (1) Remove the wind/slope/spread-direction diagram, its GV, and conditionals.
     (mapv (fn [eid] [:db.fn/retractEntity eid])
           (concat (when ws-eid [ws-eid])
                   (when ws-gv-eid [ws-gv-eid])
                   ws-cond-eids))

     ;; (2) Configure the Fire Shape diagram entity.
     [{:db/id                                         fire-shape-eid
       :diagram/output-group-variables
       [(sm/t-key->eid db "behaveplus:surface:output:fire_behavior:surface_fire:direction_of_heading")
        (sm/t-key->eid db "behaveplus:surface:output:fire_behavior:surface_fire:direction_of_flanking")
        (sm/t-key->eid db "behaveplus:surface:output:fire_behavior:surface_fire:direction_of_backing")]
       :diagram/input-group-variables
       [(sm/t-key->eid db "behaveplus:surface:input:wind_speed:wind-direction:wind-direction-degrees")
        (sm/t-key->eid db "behaveplus:surface:input:wind_speed:wind_and_slope_are:wind-direction:wind-direction-degrees")]
       :diagram/y-axis-positive-label-translation-key "behaveplus:surface:diagrams:fire-shape:y-axis-positive-label"
       :diagram/y-axis-negative-label-translation-key "behaveplus:surface:diagrams:fire-shape:y-axis-negative-label"
       :diagram/hide-axis-numbers?                    true}]

     ;; (3) y-axis end labels + renamed legend labels (create-if-absent / update-if-present).
     (upsert-translations
      db "en-US"
      {"behaveplus:surface:diagrams:fire-shape:y-axis-positive-label"        "Upslope"
       "behaveplus:surface:diagrams:fire-shape:y-axis-negative-label"        "Downslope"
       "behaveplus:diagram:surface_fire_shape:legend_id:surface_fire"        "Fire Perimeter"
       "behaveplus:diagram:surface_fire_shape:legend_id:max_spread"          "Heading Fire"
       "behaveplus:diagram:wind_slope_spread_direction:legend_id:flanking_1" "Flanking 1 Fire"
       "behaveplus:diagram:wind_slope_spread_direction:legend_id:flanking_2" "Flanking 2 Fire"
       "behaveplus:diagram:wind_slope_spread_direction:legend_id:backing"    "Backing Fire"
       "behaveplus:diagram:surface_fire_shape:legend_id:slope"               "Slope Vector"
       "behaveplus:diagram:surface_fire_shape:legend_id:wind"                "Wind Vector"}))))

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
