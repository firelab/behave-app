(ns migrations.2024-10-27-add-crown-moisture-scenario
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(do
  (def conn (default-conn))

  (def db (d/db conn))

  (def crown-fuel-moisture-submodule
    (d/entity db (d/q '[:find ?sm .
                        :where
                        [?m :module/name "Crown"]
                        [?m :module/submodules ?sm]
                        [?sm :submodule/name "Fuel Moisture"]] db)))

  (def moisture-scenario-variable
    (d/entity db (d/q '[:find ?v .
                        :where
                        [?v :variable/name "Moisture Scenario"]] db)))

  (def moisture-scenario-group-tx
    (-> (sm/->group
         (:db/id crown-fuel-moisture-submodule)
         "Moisture Scenario"
         (str (:submodule/translation-key crown-fuel-moisture-submodule) ":moisture_scenario"))
        (assoc :db/id -1)))

  (def moisture-scenario-gv-tx
    (merge
     (sm/->group-variable
      (:db/id moisture-scenario-group-tx)
      (:db/id moisture-scenario-variable)
      (str (:group/translation-key moisture-scenario-group-tx) ":moisture_scenario"))
     {:db/id -2
      :group-variable/cpp-namespace (sm/cpp-ns->uuid conn "global")
      :group-variable/cpp-class     (sm/cpp-class->uuid conn "SIGCrown")
      :group-variable/cpp-function  (sm/cpp-fn->uuid conn "SIGCrown" "setCurrentMoistureScenarioByIndex")
      :group-variable/cpp-parameter (sm/cpp-param->uuid conn "SIGCrown" "setCurrentMoistureScenarioByIndex" "moistureScenarioIndex")}))

  ;; Add Links
  (def surface-moisture-scenario-gv
    (sm/t-key->eid db "behaveplus:surface:input:fuel_moisture:moisture-scenario:moisture-scenario"))

  (def add-link-tx
    (sm/->link surface-moisture-scenario-gv (:db/id moisture-scenario-gv-tx)))

  (def new-translations-tx
    (sm/build-translations-payload
     conn
     100
     {(:group/translation-key moisture-scenario-group-tx) "Moisture Scenario"
      (:group-variable/translation-key moisture-scenario-gv-tx) "Moisture Scenario"})))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  (do (def tx (d/transact conn [moisture-scenario-group-tx
                                moisture-scenario-gv-tx
                                add-link-tx]))
      (def tx-2 (d/transact conn new-translations-tx))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (do
    (sm/rollback-tx! conn @tx-2)
    (sm/rollback-tx! conn @tx)))
