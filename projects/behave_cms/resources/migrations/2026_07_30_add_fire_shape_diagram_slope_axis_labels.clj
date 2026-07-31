(ns migrations.2026-07-30-add-fire-shape-diagram-slope-axis-labels
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Labels the Fire Shape diagram's y-axis ends: the positive (top) end reads
;; "Upslope" and the negative (bottom) end reads "Downslope" (slope is measured
;; from upslope). These are stored on the diagram entity via the new
;; :diagram/y-axis-positive-label-translation-key / -negative- attributes (schema:
;; behave.schema.diagrams) and read by construct-summary-table's diagram renderer,
;; mirroring :diagram/y-axis-title. Translation keys use the diagram's existing
;; namespace (matches :diagram/title-translation-key).

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Single-step migration: the runner calls (payload-fn db) at startup and
;; transacts the returned vector.

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [diagram-eid (d/q '[:find ?e . :in $ :where [?e :diagram/type :fire-shape]] db)]
    (concat
     [{:db/id                                         diagram-eid
       :diagram/y-axis-positive-label-translation-key "behaveplus:surface:diagrams:fire-shape:y-axis-positive-label"
       :diagram/y-axis-negative-label-translation-key "behaveplus:surface:diagrams:fire-shape:y-axis-negative-label"}]
     (sm/build-translations-payload
      db
      {"behaveplus:surface:diagrams:fire-shape:y-axis-positive-label" "Upslope"
       "behaveplus:surface:diagrams:fire-shape:y-axis-negative-label" "Downslope"}))))

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
