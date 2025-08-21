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

(def search-table-to-update-eid
  (->> "Minimum Fireline Production Required for Containment"
       (sm/name->eid conn :search-table/name)))

(def search-table-column-id-to-update
  (->> "Minimum Fireline Production Required for Containment"
       (sm/name->eid conn :search-table/name)
       (d/entity (d/db conn))
       :search-table/columns
       (filter #(= (:search-table-column/translation-key %) "behaveplus:contain:search-table:minimum-fireline-production-rate-summary:minimum-production-rate-for-containment"))
       first
       :db/id))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat
   [;; add new list option for contain mode with values Default and compute with optimized resource line production rate
    (sm/postwalk-insert
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

    ;; add new variable for Contain Mode
    (sm/->variable
     conn {:db/id    -2
           :nname    "Contain Mode"
           :list-eid -1
           :kind     :discrete})

    ;; Add new group and group variable for contain mode
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

    ;; Update order and add conditional to only show when contain mode is default for Resources group
    {:db/id              (sm/t-key->eid conn "behaveplus:contain:input:suppression:resources")
     :group/order        4
     :group/conditionals [(sm/->conditional conn {:ttype               :group-variable
                                                  :operator            :equal
                                                  :values              "0"
                                                  :group-variable-uuid contain-mode-gv-uuid})]}

    ;; add new group "Resource" and subgroups with group variables for Resource Arrival Time and Resource Duration
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
                                                 :translation-key "behaveplus:contain:input:suppression:resource:resource_duration:resource_duration"}]}]})


    ;; Add actions to conditionally set outputs needed by the search table "Minimum Fireline Production Required for Containment"
    {:db/id                                   (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:fireline_constructed")
     :group-variable/hide-result-conditionals [(sm/->conditional conn {:ttype               :group-variable
                                                                       :operator            :equal
                                                                       :values              "1"
                                                                       :group-variable-uuid contain-mode-gv-uuid})]}

    {:db/id                                   (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:final-production-rate")
     :group-variable/hide-result-conditionals [(sm/->conditional conn {:ttype               :group-variable
                                                                       :operator            :equal
                                                                       :values              "1"
                                                                       :group-variable-uuid contain-mode-gv-uuid})]}

    {:db/id                                   (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:time_from_report")
     :group-variable/hide-result-conditionals [(sm/->conditional conn {:ttype               :group-variable
                                                                       :operator            :equal
                                                                       :values              "1"
                                                                       :group-variable-uuid contain-mode-gv-uuid})]}

    {:db/id                                   (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:contained_area")
     :group-variable/hide-result-conditionals [(sm/->conditional conn {:ttype               :group-variable
                                                                       :operator            :equal
                                                                       :values              "1"
                                                                       :group-variable-uuid contain-mode-gv-uuid})]}

    {:db/id                                   (sm/t-key->eid conn "behaveplus:contain:output:fire:fire_size___at_resource_arrival_time:fire_area___at_resource_arrival_time")
     :group-variable/hide-result-conditionals [(sm/->conditional conn {:ttype               :group-variable
                                                                       :operator            :equal
                                                                       :values              "1"
                                                                       :group-variable-uuid contain-mode-gv-uuid})]}


    ;; add new group variable to get the autocomputed resource line production rate
    (sm/->group-variable
     conn
     {:db/id              -3
      :parent-group-eid   (sm/t-key->eid conn "behaveplus:contain:output:fire:containment")
      :variable-eid       (sm/name->eid conn :variable/name "Resource Line Production Rate")
      :order              6
      :cpp-namespace      "global"
      :cpp-class          "SIGContainAdapter"
      :cpp-function       "getAutoComputedResourceProductionRate"
      :translation-key    "behaveplus:contain:output:fire:containment:autocomputed_resource_production_rate"
      :conditionally-set? true
      :actions            [{:nname        "Set to True when Contain Mode is Compute with Optiomal Resource"
                            :ttype        :select
                            :target-value "true"
                            :conditionals [{:ttype               :group-variable
                                            :operator            :equal
                                            :values              "1"
                                            :group-variable-uuid contain-mode-gv-uuid}]}]
      :hide-result?       true})

    ;; Add second search table that ueses the optimized Resource Line Production rate
    {:db/id (sm/t-key->eid conn "behaveplus:contain")
     :module/search-tables
     [{:search-table/name                  "Minimum Fireline Production Required for Containment Optimized Resource"
       :search-table/order                 1
       :search-table/group-variable        (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:final-production-rate")
       :search-table/operator              :min
       :search-table/error-translation-key "behaveplus:contain:search-table:error:minimum-fireline-production-required-for-containment-optimized-resource"
       :search-table/translation-key       "behaveplus:contain:search-table:minimum-fireline-production-required-for-containment-optimized-resource"
       :search-table/filters               [{:search-table-filter/group-variable (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:contain_status")
                                             :search-table-filter/operator       :equal
                                             :search-table-filter/value          "3"}]

       :search-table/columns [{:search-table-column/name            "Minimum Production Rate for Containment"
                               :search-table-column/group-variable  -3
                               :search-table-column/translation-key "behaveplus:contain:search-table:minimum-fireline-production-required-for-containment-optimized-resource:minimum-production-rate-for-containment"
                               :search-table-column/order           0}

                              {:search-table-column/name            "Minimum Time to Containment"
                               :search-table-column/group-variable  (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:time_from_report")
                               :search-table-column/translation-key "behaveplus:contain:search-table:minimum-fireline-production-required-for-containment-optimized-resource:minimum-time-to-containment"
                               :search-table-column/order           1}

                              {:search-table-column/name            "Minimum Fireline Constructed"
                               :search-table-column/group-variable  (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:fireline_constructed")
                               :search-table-column/translation-key "behaveplus:contain:search-table:minimum-fireline-production-required-for-containment-optimized-resource:minimum-fireline-constructed"
                               :search-table-column/order           2}

                              {:search-table-column/name            "Minimum Contained Area"
                               :search-table-column/group-variable  (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:contained_area")
                               :search-table-column/translation-key "behaveplus:contain:search-table:minimum-fireline-production-required-for-containment-optimized-resource:minimum-contained-area"
                               :search-table-column/order           3}

                              {:search-table-column/name            "Fire Area at Start of Containment"
                               :search-table-column/group-variable  (sm/t-key->eid conn "behaveplus:contain:output:fire:fire_size___at_resource_arrival_time:fire_area___at_resource_arrival_time")
                               :search-table-column/translation-key "behaveplus:contain:search-table:minimum-fireline-production-required-for-containment-optimized-resource:fire-area-at-start-of-containment"
                               :search-table-column/order           4}]

       :search-table/conditionals-operator :and
       :search-table/conditionals          [(sm/->conditional conn {:ttype               :group-variable
                                                                    :operator            :equal
                                                                    :values              "1"
                                                                    :group-variable-uuid contain-mode-gv-uuid})]}]}

    ;; update existing search table
    {:db/id                              (sm/name->eid conn :search-table/name "Minimum Fireline Production Required for Containment")
     :search-table/order                 0
     :search-table/conditionals-operator :and
     :search-table/conditionals          [(sm/->conditional conn {:ttype               :group-variable
                                                                  :operator            :equal
                                                                  :values              "0"
                                                                  :group-variable-uuid contain-mode-gv-uuid})]
     :search-table/error-translation-key "behaveplus:contain:search-table:error:minimum-fireline-production-rate-summary"}

    {:db/id                              search-table-column-id-to-update
     :search-table-column/group-variable (sm/t-key->eid conn "behaveplus:contain:input:suppression:resources:resource_line_production_rate")}]


   (sm/build-translations-payload conn 100 {"behaveplus:contain:input:suppression:contain_mode"                                                                                               "Contain Mode"
                                            "behaveplus:contain:input:suppression:resource"                                                                                                   "Estimated Resource Arrival Time and Duration"
                                            "behaveplus:list-option:contain-mode:default"                                                                                                     "Add Resources"
                                            "behaveplus:list-option:contain-mode:compute-with-optimal-resource"                                                                               "Calculate Minimum Production Rate Only"
                                            "behaveplus:contain:input:suppression:resource:resource_arrival_time"                                                                             "Resource Arrival Time"
                                            "behaveplus:contain:input:suppression:resource:resource_arrival_time:resource_arrival_time"                                                       "Resource Arrival Time"
                                            "behaveplus:contain:input:suppression:resource:resource_duration"                                                                                 "Resource Duration"
                                            "behaveplus:contain:output:fire:containment:autocomputed_resource_production_rate"                                                                "Minimal Resource Production Rate for Containment"
                                            "behaveplus:contain:output:fire:containment:minimum-production-rate-for-containment-from-input"                                                   "Minimal Resource Production Rate for Containment"
                                            "behaveplus:contain:search-table:error:minimum-fireline-production-required-for-containment-optimized-resource"                                   "Uncontainable under these conditions"
                                            "behaveplus:contain:search-table:error:minimum-fireline-production-rate-summary"                                                                  "Uncontainable under these conditions"
                                            "behaveplus:contain:search-table:minimum-fireline-production-required-for-containment-optimized-resource"                                         "Minimum Fireline Production Rate Summary"
                                            "behaveplus:contain:search-table:minimum-fireline-production-required-for-containment-optimized-resource:minimum-production-rate-for-containment" "Minimum Production Rate for Containment"
                                            "behaveplus:contain:search-table:minimum-fireline-production-required-for-containment-optimized-resource:minimum-time-to-containment"             "Minimum Time to Containment"
                                            "behaveplus:contain:search-table:minimum-fireline-production-required-for-containment-optimized-resource:minimum-fireline-constructed"            "Minimum Fireline Constructed"
                                            "behaveplus:contain:search-table:minimum-fireline-production-required-for-containment-optimized-resource:minimum-contained-area"                  "Minimum Contained Area"
                                            "behaveplus:contain:search-table:minimum-fireline-production-required-for-containment-optimized-resource:fire-area-at-start-of-containment"       "Fire Area at Start of Containment"
                                            })))







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
