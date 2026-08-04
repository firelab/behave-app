(ns migrations.2026-08-03-hide-fire-shape-axis-numbers
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Hides the numeric tick labels on the Fire Shape diagram's axes (the axis titles
;; — x title, Upslope/Downslope — are kept). Sets :diagram/hide-axis-numbers? true;
;; the app's diagram renderer maps it to Vega-Lite :axis {:labels false}.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Single-step migration: the runner calls (payload-fn db) at startup and
;; transacts the returned vector.

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [diagram-eid (d/q '[:find ?e . :in $ :where [?e :diagram/type :fire-shape]] db)]
    [{:db/id                      diagram-eid
      :diagram/hide-axis-numbers? true}]))

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
