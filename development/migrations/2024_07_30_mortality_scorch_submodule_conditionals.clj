(ns migrations.2024-07-30-mortality-scorch-submodule-conditionals
  (:require
   [schema-migrate.interface :as sm]
   [datomic.api :as d]
   [behave-cms.store :refer [default-conn]]
   [behave-cms.server :as cms]
   [csv-parser.interface :refer [fetch-csv]]))


;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Scorch Submodule should be conditioned on whether or not any tree species that have been selected uses
;; crown scorch as the equation type

;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

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

(def payload
  [{:db/id (sm/t-key->eid conn "behaveplus:mortality:input:scorch")
    :submodule/conditionals
    [{:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
      :conditional/type     :group-variable
      :conditional/operator :in
      :conditional/values   (set crown-scorch-species-codes)}]}])

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
