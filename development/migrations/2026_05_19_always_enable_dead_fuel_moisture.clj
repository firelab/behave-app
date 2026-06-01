(ns migrations.2026-05-19-always-enable-dead-fuel-moisture
  (:require [datomic.api :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; BHP1-1566: When a user selects Dead, Live Herb, and Live Woody Categories
;; as their Fuel Moisture Input Mode, always show the Dead Fuel Moisture
;; category whenever the Max Spotting Distance from Wind-Driven Surface Fire
;; output is selected.
;;
;; The Dead Fuel Moisture group currently has an `:or`'d conditional gated on
;; the wind-driven fuel-model code (`:in ["103" "108" "109"]`), which leaves
;; the input hidden for other wind-driven fuels (FB1/1, FB3/3, GR1/101,
;; GR2/102). We retract that conditional and replace it with one keyed on the
;; WDSF output checkbox so the input shows whenever that output is requested.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def ^:private group-t-key
  "behaveplus:surface:input:fuel_moisture:dead-live-herb-and-live-woody-categories:dead-fuel-moisture")

(def ^:private wind-driven-fuel-model-gv-t-key
  "behaveplus:surface:input:fuel_models:standard:wind-driven-fuel-model:wind-driven-fuel-model")

(def ^:private wdsf-output-gv-t-key
  "behaveplus:surface:output:spot:maximum_spotting_distance:wind_driven_surface_fire")

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [group-eid (d/q '[:find ?g .
                         :in $ ?tk
                         :where [?g :group/translation-key ?tk]]
                       db group-t-key)
        old-cond  (d/q '[:find ?c .
                         :in $ ?group ?wd-tk
                         :where
                         [?group :group/conditionals ?c]
                         [?wd :group-variable/translation-key ?wd-tk]
                         [?wd :bp/uuid ?wd-uuid]
                         [?c :conditional/group-variable-uuid ?wd-uuid]]
                       db group-eid wind-driven-fuel-model-gv-t-key)
        wdsf-uuid (d/q '[:find ?u .
                         :in $ ?tk
                         :where
                         [?gv :group-variable/translation-key ?tk]
                         [?gv :bp/uuid ?u]]
                       db wdsf-output-gv-t-key)]
    (cond-> []
      old-cond  (conj [:db/retractEntity old-cond])
      wdsf-uuid (into (let [tempid   -1
                            new-cond (-> (sm/->conditional
                                          db
                                          {:ttype               :group-variable
                                           :operator            :equal
                                           :values              ["true"]
                                           :group-variable-uuid wdsf-uuid})
                                         (assoc :db/id tempid))]
                        [new-cond
                         [:db/add group-eid :group/conditionals tempid]])))))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

(comment
  (require '[behave-cms.server :as cms]
           '[behave-cms.store  :as store])

  (cms/init-db!)

  #_{:clj-kondo/ignore [:missing-docstring]}
  (def conn (store/default-conn))

  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
