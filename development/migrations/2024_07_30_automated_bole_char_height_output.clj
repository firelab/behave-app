(ns migrations.2024-07-30-automated-bole-char-height-output
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [cms-import :refer [add-export-file-to-conn]]
            [csv-parser.interface :refer [fetch-csv]]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Bole Char Height needs to be an automated output in all directions

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(add-export-file-to-conn "./cms-exports/SIGMortality.edn" conn)

;; ===========================================================================================================
;; Build Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def species-master-table
  (fetch-csv "projects/behave_cms/resources/public/csv/mortality_tree_species_master_table.csv"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def bole-char-species-codes
  (->> species-master-table
       (filter #(= (:equationType %) "EquationType::bole_char"))
       (map :speciesCode)))

(def payload
  [(sm/postwalk-insert
    {:db/id                -1,
     :variable/name        "Bole Char Height Backing",
     :variable/domain-uuid (sm/name->uuid conn :domain/name "Tree & Canopy Height"),
     :variable/kind        :continuous})
   (sm/postwalk-insert
    {:db/id                -2,
     :variable/name        "Bole Char Height Flanking",
     :variable/domain-uuid (sm/name->uuid conn :domain/name "Tree & Canopy Height"),
     :variable/kind        :continuous})
   {:db/id (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality")
    :group/group-variables
    (sm/postwalk-insert
     [{:variable/_group-variables             (sm/name->eid conn :variable/name "Bole Char Height")
       :group-variable/order                  15
       :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
       :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGMortality")
       :group-variable/cpp-function           (sm/cpp-fn->uuid conn "getBoleCharHeight")
       :group-variable/conditionally-set?     true
       :group-variable/direction              :heading
       :group-variable/actions                [{:action/name                  "Enable whenever mortality is ran and tree species uses bole char as the equation type"
                                                :action/type                  :select
                                                :action/conditionals-operator :and
                                                :action/conditionals          #{{:conditional/type     :module
                                                                                 :conditional/operator :equal
                                                                                 :conditional/values   #{"mortality"}}
                                                                                {:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
                                                                                 :conditional/type                :group-variable
                                                                                 :conditional/operator            :in
                                                                                 :conditional/values              (set bole-char-species-codes)}}}
                                               {:action/name                  "Enable whenever surface and mortality is ran and tree species uses bole char as the equation type"
                                                :action/type                  :select
                                                :action/conditionals-operator :and
                                                :action/conditionals          #{{:conditional/type     :module
                                                                                 :conditional/operator :equal
                                                                                 :conditional/values   #{"mortality" "surface"}}
                                                                                {:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
                                                                                 :conditional/type                :group-variable
                                                                                 :conditional/operator            :in
                                                                                 :conditional/values              (set bole-char-species-codes)}}}]
       :group-variable/translation-key        "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height"
       :group-variable/result-translation-key "behaveplus:mortality:result:tree_mortality:tree_mortality:bole_char_height"
       :group-variable/help-key               "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height:help"}

      {:variable/_group-variables             -1
       :group-variable/order                  16
       :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
       :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGMortality")
       :group-variable/cpp-function           (sm/cpp-fn->uuid conn "getBoleCharHeightBacking")
       :group-variable/conditionally-set?     true
       :group-variable/direction              :backing
       :group-variable/actions                [{:action/name                  "Enable whenever surface and mortality is ran and tree species uses bole char as the equation type and spread direciton is heading,backing,flanking"
                                                :action/type                  :select
                                                :action/conditionals-operator :and
                                                :action/conditionals          #{{:conditional/type     :module
                                                                                 :conditional/operator :equal
                                                                                 :conditional/values   #{"mortality" "surface"}}
                                                                                {:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
                                                                                 :conditional/type                :group-variable
                                                                                 :conditional/operator            :in
                                                                                 :conditional/values              (set bole-char-species-codes)}
                                                                                {:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading_backing_flanking")
                                                                                 :conditional/type                :group-variable
                                                                                 :conditional/operator            :equal
                                                                                 :conditional/values              "true"}}}]
       :group-variable/translation-key        "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_backing"
       :group-variable/result-translation-key "behaveplus:mortality:result:tree_mortality:tree_mortality:bole_char_height_backing"
       :group-variable/help-key               "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_backing:help"}

      {:variable/_group-variables             -2
       :group-variable/order                  17
       :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
       :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGMortality")
       :group-variable/cpp-function           (sm/cpp-fn->uuid conn "getBoleCharHeightFlanking")
       :group-variable/conditionally-set?     true
       :group-variable/direction              :flanking
       :group-variable/actions                [{:action/name                  "Enable whenever surface and mortality is ran and tree species uses bole char as the equation type and spread direciton is heading,backing,flanking"
                                                :action/type                  :select
                                                :action/conditionals-operator :and
                                                :action/conditionals          #{{:conditional/type     :module
                                                                                 :conditional/operator :equal
                                                                                 :conditional/values   #{"mortality" "surface"}}
                                                                                {:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
                                                                                 :conditional/type                :group-variable
                                                                                 :conditional/operator            :in
                                                                                 :conditional/values              (set bole-char-species-codes)}
                                                                                {:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading_backing_flanking")
                                                                                 :conditional/type                :group-variable
                                                                                 :conditional/operator            :equal
                                                                                 :conditional/values              "true"}}}]
       :group-variable/translation-key        "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_flanking"
       :group-variable/result-translation-key "behaveplus:mortality:result:tree_mortality:tree_mortality:bole_char_height_flanking"
       :group-variable/help-key               "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_flanking:help"}])}])

(def translations
  (sm/build-translations-payload conn 100 {
                                           "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height"          "Bole Char Height"
                                           "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_backing"  "Bole Char Height Backing"
                                           "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_flanking" "Bole Char Height Flanking"
                                           }))

(def final-payload (concat payload translations))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn final-payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
