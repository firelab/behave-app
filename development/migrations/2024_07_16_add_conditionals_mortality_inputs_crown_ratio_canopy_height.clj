(ns migrations.2024-07-16-add-conditionals-mortality-inputs-crown-ratio-canopy-height
  (:require
   [schema-migrate.interface :as sm]
   [datomic.api :as d]
   [behave-cms.store :refer [default-conn]]
   [behave-cms.server :as cms]
   [cms-import :refer [add-export-file-to-conn]]
   [csv-parser.interface :refer [fetch-csv]]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Adds conditionals to 2 input groups so that so only when certain tree speices are chosen will
;; this group be a required input.
;; Groups:
;; 1. Mortality > Tree Characteristics (input) > Crown Ratio
;; 2. Mortality > Tree Characteristics (input) > Canopy Height

;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(add-export-file-to-conn "./cms-exports/SIGMortality.edn" conn)

;; ===========================================================================================================
;; Helpers
;; ===========================================================================================================

;; ===========================================================================================================
;; Build Payload
;; ===========================================================================================================

;; Table taken from behave-mirror/src/behave/species_master_table.cpp
#_{:clj-kondo/ignore [:missing-docstring]}
(def species-master-table
  (fetch-csv "projects/behave_cms/resources/public/csv/mortality_tree_species_master_table.csv"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def crown-scorch-species-codes
  (->> species-master-table
       (filter #(= (:equationType %) "EquationType::crown_scorch"))
       (map :speciesCode)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id              (sm/t-key->eid conn "behaveplus:mortality:input:fuelvegetation_overstory:crown_ratio")
    :group/conditionals (sm/postwalk-insert
                         [{:conditional/group-variable-uuid
                           (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
                           :conditional/type     :group-variable
                           :conditional/operator :in
                           :conditional/values   (set crown-scorch-species-codes)}])}
   {:db/id              (sm/t-key->eid conn "behaveplus:mortality:input:fuelvegetation_overstory:canopy_height")
    :group/conditionals (sm/postwalk-insert
                         [{:conditional/group-variable-uuid
                           (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
                           :conditional/type     :group-variable
                           :conditional/operator :in
                           :conditional/values   (set crown-scorch-species-codes)}])}])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
