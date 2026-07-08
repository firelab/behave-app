(ns ^{:migrate/ignore? true} migrations.2026-07-01-rename-mortality-region-to-gacc
  (:require [behave-cms.server :as cms]
            [behave-cms.store :refer [default-conn]]
            [datomic.api :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; SIGMortality's region API was renamed in the C++/hatchet source: the
;; functions gained a `GACC` infix (`Region` -> `GACCRegion`) and the region
;; enum type was renamed (`RegionCode` -> `GACC`). `behave.lib.mortality` (the
;; generated wrapper) and `cms-exports/SIGMortality.edn` already use the new
;; names, but the VMS was never updated -- it still carries `setRegion`,
;; `getRegion`, `checkIsInRegion*` and `getSpeciesRecordVectorForRegion*` with
;; `RegionCode` params. Because the solver resolves a group-variable to its
;; cpp-function by `:bp/uuid` and only then reads `:cpp.function/name`
;; (see behave.solver.queries/group-variable->fn), `((symbol "setRegion")
;; (ns-publics 'behave.lib.mortality))` returns nil at run time and the
;; mortality worksheet blows up.
;;
;; We reconcile the VMS with the export by RENAMING the existing entities in
;; place: updating `:cpp.function/name` (and the stale `:cpp.parameter/type` /
;; `:cpp.function/return-type`) keeps each entity's `:bp/uuid`, so every
;; existing group-variable link is preserved automatically. This is why an
;; in-place rename is used rather than `cms-import/add-export-file-to-conn`,
;; which is additive-only (`remove existing-fn?`) and would mint new uuids
;; without re-pointing the group-variables.
;;
;; NOTE: assumes the VMS still has the `Region`-named functions and none of the
;; `GACCRegion` targets yet (verified: the class has exactly the six Region
;; functions below and no GACC duplicates). After transacting, regenerate
;; layout.msgpack so the behave app picks up the rename.

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

(def conn (default-conn))

(def class-name "SIGMortality")

;; ===========================================================================================================
;; 1. Rename functions: Region -> GACCRegion
;; ===========================================================================================================

(def fn-renames
  {"setRegion"                                      "setGACCRegion"
   "getRegion"                                      "getGACCRegion"
   "checkIsInRegionAtSpeciesTableIndex"             "checkIsInGACCRegionAtSpeciesTableIndex"
   "checkIsInRegionFromSpeciesCode"                 "checkIsInGACCRegionFromSpeciesCode"
   "getSpeciesRecordVectorForRegion"                "getSpeciesRecordVectorForGACCRegion"
   "getSpeciesRecordVectorForRegionAndEquationType" "getSpeciesRecordVectorForGACCRegionAndEquationType"})

(defn fn-eid
  "Entity id of the `SIGMortality` cpp-function named `fn-name`, or nil."
  [fn-name]
  (d/q '[:find ?f .
         :in $ ?class ?fn
         :where
         [?c :cpp.class/name ?class]
         [?c :cpp.class/function ?f]
         [?f :cpp.function/name ?fn]]
       (d/db conn) class-name fn-name))

(def rename-fn-payload
  (for [[old new] fn-renames
        :let      [eid (fn-eid old)]
        :when     eid]
    {:db/id eid :cpp.function/name new}))

;; ===========================================================================================================
;; 2. Retype the region enum: RegionCode -> GACC
;; ===========================================================================================================

(def region-param-eids
  (d/q '[:find [?p ...]
         :in $ ?class
         :where
         [?c :cpp.class/name ?class]
         [?c :cpp.class/function ?f]
         [?f :cpp.function/parameter ?p]
         [?p :cpp.parameter/type "RegionCode"]]
       (d/db conn) class-name))

(def retype-param-payload
  (for [eid region-param-eids]
    {:db/id eid :cpp.parameter/type "GACC"}))

(def region-return-fn-eids
  (d/q '[:find [?f ...]
         :in $ ?class
         :where
         [?c :cpp.class/name ?class]
         [?c :cpp.class/function ?f]
         [?f :cpp.function/return-type "RegionCode"]]
       (d/db conn) class-name))

(def retype-return-payload
  (for [eid region-return-fn-eids]
    {:db/id eid :cpp.function/return-type "GACC"}))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def payload
  (concat rename-fn-payload
          retype-param-payload
          retype-return-payload))

(comment
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
