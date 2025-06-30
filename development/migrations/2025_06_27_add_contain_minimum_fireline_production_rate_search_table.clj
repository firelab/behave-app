(ns migrations.2025-06-27-add-contain-minimum-fireline-production-rate-search-table
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [re-frame.core :as rf]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat [{:db/id (sm/t-key->eid conn "behaveplus:contain")
            :module/search-tables
            [{:search-table/name            "Minimum Fireline Produciton Rate Summary"
              :search-table/group-variable  (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:fireline_constructed")
              :search-table/op              :min
              :search-table/translation-key "behaveplus:contain:search_table:minimum_fireline_production_rate_summary"
              :search-table/filters         [{:search-table-filter/group-variable (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:contain_status")
                                              :search-table-filter/value          "3"}]

              :search-table/columns [{:search-table-column/name            "Minimum Production Rate for Containment"
                                      :search-table-column/group-variable  (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:fireline_constructed")
                                      :search-table-column/translation-key "behaveplus:contain:search-table:minimum-fireline-production-rate-summary:minimum-production-rate-for-containment"
                                      :search-table-column/order           0}

                                     {:search-table-column/name            "Minimum Time to Containment"
                                      :search-table-column/group-variable  (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:time_from_report")
                                      :search-table-column/translation-key "behaveplus:contain:search-table:minimum-fireline-production-rate-summary:minimum-time-to-containment"
                                      :search-table-column/order           1}

                                     {:search-table-column/name            "Minimum Fireline Constructed"
                                      :search-table-column/group-variable  (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:fireline_constructed")
                                      :search-table-column/translation-key "behaveplus:contain:search-table:minimum-fireline-production-rate-summary:minimum-fireline-constructed"
                                      :search-table-column/order           2}

                                     {:search-table-column/name            "Minimum Contained Area"
                                      :search-table-column/group-variable  (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:contained_area")
                                      :search-table-column/translation-key "behaveplus:contain:search-table:minimum-fireline-production-rate-summary:minimum-contained-area"
                                      :search-table-column/order           3}

                                     {:search-table-column/name            "Fire Area at Start of Containment"
                                      :search-table-column/group-variable  (sm/t-key->eid conn "behaveplus:contain:output:fire:fire_size___at_resource_arrival_time:fire_area___at_resource_arrival_time")
                                      :search-table-column/translation-key "behaveplus:contain:search-table:minimum-fireline-production-rate-summary:fire-area-at-start-of-containment"
                                      :search-table-column/order           4}
                                     ]}]}]
          (sm/build-translations-payload conn {"behaveplus:contain:search_table:minimum_fireline_production_rate_summary"                                         "Minimum Fireline Produciton Rate Summary"
                                               "behaveplus:contain:search-table:minimum-fireline-production-rate-summary:minimum-production-rate-for-containment" "Minimum Production Rate for Containment"
                                               "behaveplus:contain:search-table:minimum-fireline-production-rate-summary:minimum-time-to-containment"             "Minimum Time to Containment"
                                               "behaveplus:contain:search-table:minimum-fireline-production-rate-summary:minimum-fireline-constructed"            "Minimum Fireline Constructed"
                                               "behaveplus:contain:search-table:minimum-fireline-production-rate-summary:minimum-contained-area"                  "Minimum Contained Area"
                                               "behaveplus:contain:search-table:minimum-fireline-production-rate-summary:fire-area-at-start-of-containment"       "Fire Area at Start of Containment"} )))

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
