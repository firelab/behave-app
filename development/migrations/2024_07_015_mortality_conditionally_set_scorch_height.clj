(ns migrations.2024-07-015-mortality-conditionally-set-scorch-height
  (:require
   [schema-migrate.interface :as sm]
   [datomic.api :as d]
   [behave-cms.store :refer [default-conn]]
   [behave-cms.server :as cms]
   [cms-import :refer [add-export-file-to-conn]]
   [clojure.data.csv :as csv]
   [csv-parser.interface :refer [fetch-csv]]
   [clojure.java.io :as io]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; - Create 2 new variables for the Scorch Height in the Backing and Flanking direction
;; - Create 3 new conditionally set group variables for Scorch Height one for each direction.
;;   - Condition: Enable only when selected tree species is in the list of speices that uses crown
;;     scorch as the equation type.

;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(add-export-file-to-conn "./cms-exports/SIGMortality.edn" conn)

;; ===========================================================================================================
;; Helpers
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def domain-name->uuid
  (->> (d/q '[:find ?name ?uuid
              :in $
              :where
              [?e :domain/name ?name]
              [?e :bp/uuid ?uuid]]
             (d/db conn))
       (sort-by first)
       (into (sorted-map))))

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
(def new-entities
  [{:variable/name            "Scorch Height Backing"
    :variable/kind            :continuous
    :variable/domain-uuid     (domain-name->uuid "Flame Length & Scorch Ht")
    :variable/group-variables [-2]}

   {:variable/name            "Scorch Height Flanking"
    :variable/kind            :continuous
    :variable/domain-uuid     (domain-name->uuid "Flame Length & Scorch Ht")
    :variable/group-variables [-3]}

   ;; Scorch Height (output)
   {:db/id                             -1
    :group-variable/cpp-namespace      (sm/cpp-ns->uuid conn "global")
    :group-variable/cpp-class          (sm/cpp-class->uuid conn "SIGMortality")
    :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getScorchHeight")
    :group-variable/conditionally-set? true
    :group-variable/actions
    [{:action/name                  "Enable if selected tree species uses crown scorch equation type"
      :action/type                  :select
      :action/conditionals
      #{{:conditional/group-variable-uuid
         (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
         :conditional/type     :group-variable
         :conditional/operator :in
         :conditional/values   (set crown-scorch-species-codes)}}
      :action/conditionals-operator :and}]
    :group-variable/translation-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height"
    :group-variable/result-translation-key
    "behaveplus:mortality:result:tree_mortality:tree_mortality:scorch-height"
    :group-variable/help-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height:help"}

   ;; Scorch Height Backing (output)
   {:db/id                             -2
    :group-variable/cpp-namespace      (sm/cpp-ns->uuid conn "global")
    :group-variable/cpp-class          (sm/cpp-class->uuid conn "SIGMortality")
    :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getScorchHeightBacking")
    :group-variable/conditionally-set? true
    :group-variable/actions
    [{:action/name "Enable if selected tree species uses crown scorch equation type and Surface Spread Direction is HeadingBackingFlanking"
      :action/type :select
      :action/conditionals
      #{{:conditional/group-variable-uuid
         (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
         :conditional/type     :group-variable
         :conditional/operator :in
         :conditional/values   (set crown-scorch-species-codes)}

        {:conditional/type     :module
         :conditional/operator :equal
         :conditional/values   #{"surface" "mortality"}}

        {:conditional/group-variable-uuid
         (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading_backing_flanking")
         :conditional/type     :group-variable
         :conditional/operator :equal
         :conditional/values   "true"}}
      :action/conditionals-operator :and}]
    :group-variable/translation-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height-backing"
    :group-variable/result-translation-key
    "behaveplus:mortality:result:tree_mortality:tree_mortality:scorch-height-backing"
    :group-variable/help-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height-backing:help"}

   ;; Scorch Height Flanking (output)
   {:db/id                             -3
    :group-variable/cpp-namespace      (sm/cpp-ns->uuid conn "global")
    :group-variable/cpp-class          (sm/cpp-class->uuid conn "SIGMortality")
    :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getScorchHeightFlanking")
    :group-variable/conditionally-set? true
    :group-variable/actions
    [{:action/name "Enable if selected tree species uses crown scorch equation type and Surface Spread Direction is HeadingBackingFlanking"
      :action/type :select
      :action/conditionals
      #{{:conditional/group-variable-uuid
         (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
         :conditional/type     :group-variable
         :conditional/operator :in
         :conditional/values   (set crown-scorch-species-codes)}

        {:conditional/type     :module
         :conditional/operator :equal
         :conditional/values   #{"surface" "mortality"}}

        {:conditional/group-variable-uuid
         (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading_backing_flanking")
         :conditional/type     :group-variable
         :conditional/operator :equal
         :conditional/values   "true"}}
      :action/conditionals-operator :and}]
    :group-variable/translation-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height-flanking"
    :group-variable/result-translation-key
    "behaveplus:mortality:result:tree_mortality:tree_mortality:scorch-height-flanking"
    :group-variable/help-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height-flanking:help"}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-refs
  [{:db/id                 (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality")
    :group/group-variables [-1 -2 -3]}
   {:db/id                    (sm/name->eid conn :variable/name "Scorch Height")
    :variable/group-variables [-1]}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-translations
  (sm/build-translations-payload
   conn
   100
   {"behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height"          "Scorch Height"
    "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height-backing"  "Scorch Height Backing"
    "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height-flanking" "Scorch Height Flanking"}))

(def final-payload (concat (sm/postwalk-insert new-entities)
                           new-refs
                           new-translations))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  (def tx-data (d/transact conn final-payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
