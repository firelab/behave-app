(ns migrations.2026-08-26-hide-table-shading-filters
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; BHP1-1637 — Table Shading Filters do not make sense for outputs users never
;; base a decision on. Flag those outputs with
;; `:group-variable/hide-table-filter?` so the app drops their row from the
;; Table Shading Filters page and stops marking their result cells.
;;
;; After this runs, re-export layout.msgpack.

;; ===========================================================================================================
;; Helpers
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def translation-keys
  ["behaveplus:surface:output:size:surface___fire_size:length_to_width_ratio"
   "behaveplus:surface:output:wind_and_fuel:wind:midflame_eye_level_wind_speed"
   "behaveplus:surface:output:wind_and_fuel:fuel:total_live_fuel_load"
   "behaveplus:surface:output:wind_and_fuel:fuel:total_dead_fuel_load"
   "behaveplus:surface:output:wind_and_fuel:fuel:total_dead_herbaceous_fuel_load"
   "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched"
   "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_backing"
   "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_flanking"
   "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched"
   "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_backing"
   "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_flanking"
   "behaveplus:contain:output:fire:fire_size___at_resource_arrival_time:fire_perimeter___at_resource_arrival_time"
   "behaveplus:contain:output:fire:fire_size___at_resource_arrival_time:fire_area___at_resource_arrival_time"])

(defn- t-key->group-variable-eids
  "All group-variable eids carrying `t-key`. The directional refactor left a
  parent and its Heading child sharing one translation key, so `sm/t-key->eid`
  would only reach one of them."
  [db t-key]
  (d/q '[:find [?e ...]
         :in $ ?t-key
         :where
         [?e :group-variable/translation-key ?t-key]]
       db
       t-key))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Single-step migration: the runner calls (payload-fn db) at startup and
;; transacts the returned vector.

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (->> translation-keys
       (mapcat #(t-key->group-variable-eids db %))
       (distinct)
       (mapv (fn [eid] {:db/id eid :group-variable/hide-table-filter? true}))))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server        :as cms]
           '[behave-cms.store         :as store])
  (cms/init-db!)

  (def conn (store/default-conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
