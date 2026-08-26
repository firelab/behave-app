(ns ^{:migrate/ignore? true} migrations.2026-08-26-hide-table-shading-filters
  (:require [behave-cms.server :as cms]
            [behave-cms.store :refer [default-conn]]
            [datomic.api :as d]
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
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

#_{:clj-kondo/ignore [:missing-docstring]}
(def translation-keys
  ["behaveplus:surface:output:size:surface___fire_size:length-to-width-ratio"
   "behaveplus:surface:output:wind-and-fuel:wind:midflame-eye-level-wind-speed"
   "behaveplus:surface:output:wind-and-fuel:fuel:total-live-fuel-load"
   "behaveplus:surface:output:wind-and-fuel:fuel:total-dead-fuel-load"
   "behaveplus:surface:output:wind-and-fuel:fuel:total-dead-herbaceous-fuel-load"
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
  [t-key]
  (d/q '[:find [?e ...]
         :in $ ?t-key
         :where
         [?e :group-variable/translation-key ?t-key]]
       (d/db conn)
       t-key))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (->> translation-keys
       (mapcat t-key->group-variable-eids)
       (distinct)
       (mapv (fn [eid] {:db/id eid :group-variable/hide-table-filter? true}))))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data @(d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
