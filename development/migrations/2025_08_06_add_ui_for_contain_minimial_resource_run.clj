(ns migrations.2025-08-06-add-ui-for-contain-minimial-resource-run
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [datascript.core    :refer [squuid]]
            [behave-cms.store :refer [default-conn]]
            [cms-import :refer [add-export-file-to-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))


(add-export-file-to-conn "./cms-exports/SIGContainAdapter.edn" conn)

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================


(def
  ^{:doc "Random UUID in string format."}
  rand-uuid (comp str squuid))

(def contain-mode-gv-uuid (rand-uuid))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat
   [(sm/postwalk-insert
     {:db/id        -1
      :list/name    "ContainMode"
      :list/options [{:list-option/name                   "Default"
                      :list-option/value                  "0"
                      :list-option/order                  0
                      :list-option/translation-key        "behaveplus:list-option:contain-mode:default"
                      :list-option/result-translation-key "behaveplus:list-option:result:contain-mode:default"
                      :list-option/export-translation-key "behaveplus:list-option:export:contain-mode:default"}
                     {:list-option/name                   "Compute with Optiomal Resource"
                      :list-option/value                  "1"
                      :list-option/order                  1
                      :list-option/translation-key        "behaveplus:list-option:contain-mode:compute-with-optimal-resource"
                      :list-option/result-translation-key "behaveplus:list-option:result:contain-mode:compute-with-optimal-resource"
                      :list-option/export-translation-key "behaveplus:list-option:export:contain-mode:compute-with-optimal-resource"}]})

    (sm/->variable
     conn {:db/id    -2
           :nname    "Contain Mode"
           :list-eid -1
           :kind     :discrete})

    (sm/->group
     conn
     {:parent-submodule-eid (sm/t-key->eid conn "behaveplus:contain:input:suppression")
      :group-name           "Contain Mode"
      :order                3
      :translation-key      "behaveplus:contain:input:suppression:contain_mode"
      :group-variables      [{:bp/uuid         contain-mode-gv-uuid
                              :variable-eid    -2
                              :translation-key "behaveplus:contain:input:suppression:contain_mode:contain_mode"
                              :order           0
                              :cpp-namespace   "global"
                              :cpp-class       "SIGContainAdapter"
                              :cpp-function    "setContainMode"
                              :cpp-parameter   "containMode"}]})

    {:db/id              (sm/t-key->eid conn "behaveplus:contain:input:suppression:resources")
     :group/order        4
     :group/conditionals [(sm/->conditional conn {:ttype               :group-variable
                                                  :operator            :equal
                                                  :values              "0"
                                                  :group-variable-uuid contain-mode-gv-uuid})]}

    (sm/->group
     conn
     {:parent-submodule-eid (sm/t-key->eid conn "behaveplus:contain:input:suppression")
      :group-name           "Resource"
      :order                5
      :translation-key      "behaveplus:contain:input:suppression:resource"
      :conditionals         [{:ttype               :group-variable
                              :operator            :equal
                              :values              "1"
                              :group-variable-uuid contain-mode-gv-uuid}]
      :subgroups            [{:group-name      "Resource Arrival Time"
                              :order           0
                              :translation-key "behaveplus:contain:input:suppression:resource:resource_arrival_time"
                              :group-variables [{:variable-eid    (sm/name->eid conn :variable/name "Resource Arrival Time")
                                                 :order           0
                                                 :cpp-namespace   "global"
                                                 :cpp-class       "SIGContainAdapter"
                                                 :cpp-function    "setResourceArrivalTime"
                                                 :cpp-parameter   "arrivalTime"
                                                 :translation-key "behaveplus:contain:input:suppression:resource:resource_arrival_time:resource_arrival_time"}]}
                             {:group-name      "Resource Duration"
                              :order           1
                              :translation-key "behaveplus:contain:input:suppression:resource:resource_duration"
                              :group-variables [{:variable-eid    (sm/name->eid conn :variable/name "Resource Duration")
                                                 :order           0
                                                 :cpp-namespace   "global"
                                                 :cpp-class       "SIGContainAdapter"
                                                 :cpp-function    "setResourceDuration"
                                                 :cpp-parameter   "duration"
                                                 :translation-key "behaveplus:contain:input:suppression:resource:resource_duration:resource_duration"}]}]})]
   (sm/build-translations-payload conn 100 {"behaveplus:contain:input:suppression:contain_mode"                                         "Contain Mode"
                                            "behaveplus:contain:input:suppression:resource"                                             "Estimated Resource Arrival Time and Duration"
                                            "behaveplus:list-option:contain-mode:default"                                               "Add Resources"
                                            "behaveplus:list-option:contain-mode:compute-with-optimal-resource"                         "Calculate Minimum Production Rate Only"
                                            "behaveplus:contain:input:suppression:resource:resource_arrival_time"                       "Resource Arrival Time"
                                            "behaveplus:contain:input:suppression:resource:resource_arrival_time:resource_arrival_time" "Resource Arrival Time"
                                            "behaveplus:contain:input:suppression:resource:resource_duration"                           "Resource Duration"})
   ))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e  (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
