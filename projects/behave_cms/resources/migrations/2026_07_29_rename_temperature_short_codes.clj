(ns migrations.2026-07-29-rename-temperature-short-codes
  (:require [datomic.api :as d]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; BHP1-1592 — Users read the temperature short codes "oF" / "oC" as the word
;; "of". Rename them to "°F" / "°C" (the ticket allows the degree symbol).
;; Kelvin's "K" is intentionally left as-is (SI style: no degree on kelvin).
;;
;; Every display site (Settings selector, wizard "Units Used", results/CSV
;; headers, diagrams, tools) reads :unit/short-code live from the VMS by
;; unit-uuid, so this rename propagates with no app-code changes. Conversions
;; resolve through behave.lib.units, which gains "°F"/"°C" entries and keeps
;; "oF"/"oC" as legacy aliases for result headers stored in old worksheets.
;;
;; Runs after 2026_07_29_add_missing_units (alphabetical order) but does not
;; depend on it. After this runs, re-export layout.msgpack.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(defn- temperature-unit-eid
  "Entity id of the Temperature-dimension unit with `short-code`, or nil."
  [db short-code]
  (d/q '[:find ?u . :in $ ?sc
         :where
         [?d :dimension/name "Temperature"]
         [?d :dimension/units ?u]
         [?u :unit/short-code ?sc]]
       db short-code))

#_{:clj-kondo/ignore [:missing-docstring :unused-binding]}
(defn payload-fn [db]
  (vec
   (for [[old-sc new-sc new-name] [["oF" "°F" "Fahrenheit (°F)"]
                                   ["oC" "°C" "Celsius (°C)"]]
         :let                     [eid (temperature-unit-eid db old-sc)]
         :when                    eid]
     {:db/id           eid
      :unit/short-code new-sc
      :unit/name       new-name})))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server :as cms]
           '[behave-cms.store  :as store])
  (cms/init-db!)

  (def conn (store/default-conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (require '[schema-migrate.interface :as sm])
  (sm/rollback-tx! conn tx-data))
