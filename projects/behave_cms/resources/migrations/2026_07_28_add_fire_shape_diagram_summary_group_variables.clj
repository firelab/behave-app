(ns migrations.2026-07-28-add-fire-shape-diagram-summary-group-variables
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Wires up the Fire Shape diagram's summary table (rendered by
;; behave.components.results.diagrams/construct-summary-table). That component
;; reads :diagram/output-group-variables and :diagram/input-group-variables off
;; the diagram entity and shows each GV's value from the worksheet results.
;;
;; We add:
;;   - outputs: Direction of Heading (= direction of maximum spread), Direction of
;;              Flanking, Direction of Backing -> :diagram/output-group-variables
;;   - input:   Wind Direction (degrees)       -> :diagram/input-group-variables
;;
;; The three spread-direction outputs are force-included at output-selection time
;; (see :worksheet/enable-diagram-summary-outputs in behave.worksheet.events), so
;; they are always present in the results when the Fire Shape Diagram output is on.
;;
;; The group-variables live in the base VMS seed and are resolved by their
;; translation-key via sm/t-key->eid (which matches both
;; :group-variable/translation-key and :group-variable/result-translation-key):
;;   - the three direction OUTPUTS carry a :result-translation-key
;;     (behaveplus:surface:output:fire_behavior:surface_fire:direction_of_*)
;;   - the wind-direction INPUTS carry a :translation-key; we include both Surface
;;     wind-direction contexts so whichever the worksheet populates is shown.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Single-step migration: the runner calls (payload-fn db) at startup and
;; transacts the returned vector.

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [diagram-eid (d/q '[:find ?e . :in $ :where [?e :diagram/type :fire-shape]] db)]
    [{:db/id                                                                                                              diagram-eid
      :diagram/output-group-variables
      [(sm/t-key->eid db "behaveplus:surface:output:fire_behavior:surface_fire:direction_of_heading")
       (sm/t-key->eid db "behaveplus:surface:output:fire_behavior:surface_fire:direction_of_flanking")
       (sm/t-key->eid db "behaveplus:surface:output:fire_behavior:surface_fire:direction_of_backing")]
      :diagram/input-group-variables
      [(sm/t-key->eid db "behaveplus:surface:input:wind_speed:wind-direction:wind-direction-degrees")
       (sm/t-key->eid db "behaveplus:surface:input:wind_speed:wind_and_slope_are:wind-direction:wind-direction-degrees")]}]))

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
