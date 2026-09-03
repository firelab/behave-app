(ns migrations.2026-08-19-auto-enable-fire-shape-outputs
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; The Fire Shape diagram's summary table only renders an output row if that output
;; was enabled/computed on the worksheet. The consolidated rework migration
;; 2026_08_04_fire_shape_diagram.clj force-enables the directional spread-distance
;; outputs (via `spread-distance-force-actions`) so they appear whenever the Fire
;; Shape diagram is selected, but it never did the same for the pre-existing scalar
;; fire-size outputs. This migration adds the analogous force-actions for fire area,
;; fire perimeter, and length-to-width ratio so they auto-appear in the summary
;; table too.
;;
;; These three have been in the diagram's :diagram/output-group-variables since the
;; base seed, so no change to that list is needed. Because they are scalar values
;; (no drawn arrow), they are gated on the diagram-selected condition only — the
;; "heading" pattern, not the flanking/backing arrow gate.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [fs-cond {:conditional/group-variable-uuid (sm/t-key->uuid db "behaveplus:surface:output:size:surface___fire_size:fire_shape_diagram")
                 :conditional/type                :group-variable
                 :conditional/operator            :equal
                 :conditional/values              #{"true"}}
        specs   [["behaveplus:surface:output:size:surface___fire_size:fire_area"
                  "fire-shape-diagram: force fire area"]
                 ["behaveplus:surface:output:size:surface___fire_size:fire_perimeter"
                  "fire-shape-diagram: force fire perimeter"]
                 ["behaveplus:surface:output:size:surface___fire_size:length_to_width_ratio"
                  "fire-shape-diagram: force length-to-width ratio"]]]
    (mapv (fn [[k action-name]]
            {:db/id                  (sm/t-key->eid db k)
             :group-variable/actions [{:action/name                  action-name
                                       :action/type                  :select
                                       :action/conditionals-operator :and
                                       :action/conditionals          #{fs-cond}}]})
          specs)))

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
