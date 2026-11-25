(ns migrations.2026-11-25-remove-degrees-from-wind-direction-name
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; The Wind Direction input is labeled "Wind Direction (degrees)". The "(degrees)" suffix only existed to
;; distinguish it from "Wind Direction (compass)", which is no longer attached to any group variable. Now that
;; the unit is shown next to the value (e.g. "Wind Direction: 90 deg"), the suffix is redundant.
;;
;; This migration:
;;   1. Renames the variable "Wind Direction (degrees)" -> "Wind Direction".
;;   2. Updates the en-US translations of the Surface and Crown "Wind and slope are > Wind Direction" group
;;      variables to "Wind Direction". Their result-translation-keys have no translation, so results and
;;      diagram summary tables fall back to these labels.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def ^:private new-name "Wind Direction")

(def ^:private translation-keys
  ["behaveplus:surface:input:wind_speed:wind_and_slope_are:wind_direction:wind_direction_degrees"
   "behaveplus:crown:input:weather:wind_and_slope_are:wind_direction:wind_direction_degrees"])

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [variable-eid (d/q '[:find ?v .
                            :where [?v :variable/name "Wind Direction (degrees)"]]
                          db)]
    (concat
     (when variable-eid
       [{:db/id variable-eid :variable/name new-name}])
     (->> (sm/update-translations-payload db "en-US" (zipmap translation-keys (repeat new-name)))
          ;; skip keys without an existing translation (nil :db/id would create a dangling entity)
          (filter :db/id)))))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server        :as cms])
  (cms/init-db!)

  (def conn (behave-cms.store/default-conn))

  (payload-fn (d/db conn))

  (try (def tx-data @(d/transact conn (vec (payload-fn (d/db conn)))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
