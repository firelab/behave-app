(ns migrations.2026-10-13-apply-dev-cms-edits
  (:require [datomic.api :as d]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Carries the edits made through the CMS UI on Dev (2026-08-07 → 2026-10-08) over to Prod, so Dev can
;; be restored from Prod without losing them (BHP1-1681). Taken as the net change per entity/attribute
;; across every non-migration, non-schema Dev transaction after Dev forked from Prod (t 17582,
;; 2026-07-20). Edits that ended where they started (e.g. a list-option rename that was reverted) are
;; left out.
;;
;; Entities are resolved by `:bp/uuid`, translation key or diagram type — never by eid.

;; ===========================================================================================================
;; Data
;; ===========================================================================================================

(def ^:private list-option-orders
  "[list-option uuid, order] — drag-reordered fuel models."
  [;; SurfaceFuelModels
   ["6765c3fb-339a-4673-83d2-d4abff637468" 60]  ; V-Hb/110 - Short Grass, < 0.5 m
   ["6765c3fb-086c-4516-851a-2126f1bd5636" 61]  ; V-Ha/111 - Tall Grass, > 0.5 m
   ["6765c3fb-72d4-4512-af42-d35be4d7e57d" 62]  ; SCAL17/150
   ["6765c3fb-4df8-423e-8911-2c0fd9b19da4" 63]  ; SCAL15/151
   ["6765c3fb-022a-47f4-94b2-617b91380523" 64]  ; SCAL16/152
   ["6765c3fb-e9c1-44eb-a8a0-33163863e737" 65]  ; SCAL14/153
   ["6765c3fb-c67a-4654-b9c9-08a60e311f89" 66]  ; SCAL18/154
   ;; WindDrivenSurfaceFuelModelCodes
   ["662bc95f-370c-4697-a31f-d3c90b117f99" 2]   ; FB3/3 - Tall grass
   ["66295268-16d9-4778-bed0-47ee8a719aae" 13]  ; GR1/101
   ["66295268-31e6-4a94-b0ff-c99af0e96038" 14]  ; GR2/102
   ["66295268-eb0b-4f66-a15a-fa1db3a97b0f" 15]  ; GR3/103
   ["66295268-90e3-4e27-909d-97497bfd3196" 16]  ; GR4/104
   ["66295268-a32f-4d48-831e-be5931e1e84f" 17]  ; GR5/105
   ["66295268-8c2c-4ab1-b2b4-ee1c1339ce28" 18]  ; GR6/106
   ["66295268-325d-434a-8bdd-7402fe8053ac" 19]  ; GR7/107
   ["66295268-6112-494e-a123-36d466cddbd3" 20]  ; GR8/108
   ["66295268-1eb8-4cca-9bac-858fb0a7c764" 21]  ; GR9/109
   ["66295268-8f94-4b95-8b4f-de6eed836066" 22]]) ; GS1/121

(def ^:private domain-decimals
  "[domain uuid, decimals]"
  [["04521bf5-caa2-4587-96a8-eed43961e0f0" 2]   ; Flame Residence Time
   ["bfada064-f6fb-4844-a2df-ff3e4f2df01b" 1]   ; Map Distance
   ["4418dc9f-6fb9-47b9-be67-4f9a8bee3968" 1]]) ; P-G Age of Rough

(def ^:private domain-filtered-units
  "[domain uuid, short codes of the complete set of units it should offer]. Units are resolved by
   short code within the domain's dimension: some (e.g. m/s, from 2026_07_29_add_missing_units) were
   created by migrations with a random `:bp/uuid`, so their uuid differs between Dev and Prod."
  [["5b3471b0-e529-4e3f-9edf-10a22c73d61a" #{"cm" "in"}]                                ; Tree Dbh & Bark Thickness
   ["36579d1d-de73-4ee3-9adf-3cd29d587ded" #{"ch/h" "ft/min" "km/h" "m/min" "mi/h"}]     ; Surface Rate of Spread
   ["e22a9e77-bcdb-4957-82f3-0ce28a22a1b6" #{"ch/h" "ft/min" "km/h" "m/min" "m/s" "mi/h"}]]) ; Crown Rate of Spread

(def ^:private translations
  "[translation key, English text]"
  [["behaveplus:surface:input:fuel_models:standard" "Fuel Model Selection"]])

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(defn- translation-eid [db k]
  (d/q '[:find ?e . :in $ ?k :where [?e :translation/key ?k]] db k))

(defn- unit-uuid
  "uuid of the unit with `short-code` in dimension `dim-uuid`. Throws if missing, so the migration
   never writes a dangling uuid."
  [db dim-uuid short-code]
  (or (d/q '[:find ?u . :in $ ?dim ?c
             :where [?d :bp/uuid ?dim] [?d :dimension/units ?e] [?e :unit/short-code ?c] [?e :bp/uuid ?u]]
           db dim-uuid short-code)
      (throw (ex-info "Unit not found" {:dimension-uuid dim-uuid :short-code short-code}))))

(defn- filtered-units-tx
  "Make the domain's `:domain/filtered-unit-uuids` exactly the units with `short-codes`."
  [db [uuid short-codes]]
  (let [eid     (d/entid db [:bp/uuid uuid])
        dim     (:domain/dimension-uuid (d/entity db eid))
        units   (set (map #(unit-uuid db dim %) short-codes))
        current (set (:domain/filtered-unit-uuids (d/entity db eid)))]
    (concat
     (for [u (remove units current)] [:db/retract eid :domain/filtered-unit-uuids u])
     (for [u (remove current units)] [:db/add eid :domain/filtered-unit-uuids u]))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (vec
   (concat
    (for [[uuid order] list-option-orders]
      {:db/id [:bp/uuid uuid] :list-option/order order})
    (for [[uuid decimals] domain-decimals]
      {:db/id [:bp/uuid uuid] :domain/decimals decimals})
    (mapcat #(filtered-units-tx db %) domain-filtered-units)
    (for [[k text] translations]
      {:db/id (translation-eid db k) :translation/translation text})
    [{:db/id                      (d/q '[:find ?e . :where [?e :diagram/type :fire-shape]] db)
      :diagram/hide-axis-numbers? false}])))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server :as cms])
  (cms/init-db!)

  (def conn (behave-cms.store/default-conn))

  (payload-fn (d/db conn))
  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
