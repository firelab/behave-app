(ns migrations.2026-07-27-remove-wind-slope-spread-direction-diagram
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; The :wind-slope-spread-direction diagram has been folded into the :fire-shape
;; diagram (its flanking/backing/wind spread arrows now render on Fire Shape), so
;; the standalone diagram is no longer used. Its solver method, store event, and
;; the CMS diagram-type dropdown option were removed in code; this migration
;; retracts the CMS data so the diagram stops appearing as an output.
;;
;; We reverse the same wiring the add-fire-shape migration created for its
;; diagram (see 2025_11_12_add_fire_shape_diagram.clj):
;;   1. the diagram entity itself (:diagram/type :wind-slope-spread-direction)
;;   2. its output group-variable (:diagram/group-variable) -- retracting this
;;      removes it as a selectable/active output
;;   3. any submodule conditional keyed on that group-variable's uuid
;;      (:conditional/group-variable-uuid), which gated the output's visibility
;;
;; retractEntity also retracts inbound references, so the parent group's link to
;; the group-variable and the submodule's link to the conditional are cleaned up
;; automatically.
;;
;; The wind_slope_spread_direction:legend_id:{flanking_1,flanking_2,backing}
;; translations are intentionally LEFT IN PLACE -- the reworked Fire Shape arrows
;; reuse those legend keys.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Single-step migration: the runner calls (payload-fn db) at startup and
;; transacts the returned vector.

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [diagram-eid    (d/q '[:find ?e .
                              :in $
                              :where
                              [?e :diagram/type :wind-slope-spread-direction]]
                            db)
        gv-eid         (when diagram-eid
                         (d/q '[:find ?gv .
                                :in $ ?d
                                :where
                                [?d :diagram/group-variable ?gv]]
                              db diagram-eid))
        gv-uuid        (when gv-eid
                         (d/q '[:find ?uuid .
                                :in $ ?gv
                                :where
                                [?gv :bp/uuid ?uuid]]
                              db gv-eid))
        conditional-es (when gv-uuid
                         (d/q '[:find [?c ...]
                                :in $ ?uuid
                                :where
                                [?c :conditional/group-variable-uuid ?uuid]]
                              db gv-uuid))]
    (->> (concat (when diagram-eid [diagram-eid])
                 (when gv-eid [gv-eid])
                 conditional-es)
         (mapv (fn [eid] [:db.fn/retractEntity eid])))))

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
