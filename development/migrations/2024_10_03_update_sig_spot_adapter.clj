(ns migrations.2024-10-03-update-sig-spot-adapter
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [cms-import :refer [add-export-file-to-conn]]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;;  Add a new group variable under Mortality > Mortality (output) > Tree Mortality

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(add-export-file-to-conn "./cms-exports/SIGSpot.edn" conn)

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def payload
  [{:db/id (sm/t-key->eid conn "behaveplus:crown:input:spotting")
    :submodule/groups
    (sm/postwalk-insert
     [{:group/name                  "Linked Inputs (hidden)"
       :group/conditionals          [{:conditional/type     :module
                                      :conditional/values   #{"surface" "crown" "mortality" "contain"}
                                      :conditional/operator :equal}]
       :group/conditionals-operator :and
       :group/group-variables
       [{:db/id                                 -1
         :variable/_group-variables             (sm/name->eid conn :variable/name "Fire Type")
         :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
         :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGSpot")
         :group-variable/cpp-function           (sm/cpp-fn->uuid conn "setFireType")
         :group-variable/cpp-parameter          (sm/cpp-param->uuid conn "setFireType" "fireType")
         :group-variable/translation-key        "behaveplus:crown:input:spotting:linked_inputs:fire_type"
         :group-variable/result-translation-key "behaveplus:crown:result:spotting:linked_inputs:fire_type"
         :group-variable/help-key               "behaveplus:crown:input:spotting:linked_inputs:fire_type:help"}]
       :group/translation-key       "behaveplus:crown:input:spotting:linked_inputs"
       :group/help-key              "behaveplus:crown:input:spotting:linked_inputs:help"}])}

   (sm/postwalk-insert
    {:link/source      (sm/t-key->eid conn "behaveplus:crown:output:fire_type:fire_type:fire_type_required")
     :link/destination -1})

   {:db/id                       (sm/t-key->eid conn "behaveplus:crown:output:spotting_active_crown_fire:maximum_spotting_distance:maximum_spotting_distance")
    :group-variable/cpp-function (sm/cpp-fn->uuid conn "getMaxMountainousTerrainSpottingDistanceFromActiveCrown")}

   ])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
