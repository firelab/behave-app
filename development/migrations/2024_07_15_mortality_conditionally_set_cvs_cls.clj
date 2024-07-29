(ns migrations.2024_07_15_mortality_conditionally_set_cvs_cls
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [cms-import :refer [add-export-file-to-conn]]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; - Fix getTreeCrownLengthScorched units from fractionUnits to LengthUnits
;; - Create 2 new variables for the Crown Volume Scorched in the Backing and Flanking direction
;; - Create 3 new conditionally set group variables for Crown Volume Scorched one for each direction.
;;   - Condition: Enable only when selected tree species is in the list of speices that uses crown
;;     scorch as the equation type.

;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; We need to remove these functions because they have the wrong units.
;; getTreeCrownLengthScorched should have length units not MortalityRateUnits
;; getTreeCrownVolumeScorched should have fractionUnits not MortalityRateUnits
#_{:clj-kondo/ignore [:missing-docstring]}
(def cms-remove-tx
  (d/transact conn [[:db/retractEntity [:bp/uuid (sm/cpp-fn->uuid conn "getTreeCrownLengthScorched")]]
                    [:db/retractEntity [:bp/uuid (sm/cpp-fn->uuid conn "getTreeCrownVolumeScorched")]]]))

#_{:clj-kondo/ignore [:missing-docstring]}
(def cms-import-tx
  (add-export-file-to-conn "./cms-exports/SIGMortality.edn" conn))

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

(defn- csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data)
            (map keyword)
            repeat)
       (rest csv-data)))

;; Table taken from behave-mirror/src/behave/species_master_table.cpp
#_{:clj-kondo/ignore [:missing-docstring]}
(def species-master-table
  (with-open [reader (io/reader "projects/behave_cms/resources/public/csv/mortality_tree_species_master_table.csv")]
    (csv-data->maps (doall (csv/read-csv reader)))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def crown-scorch-species-codes
  (->> species-master-table
       (filter #(= (:equationType %) "EquationType::crown_scorch"))
       (map :speciesCode)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def existing-entities
  [{:db/id                             (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched")
    :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getTreeCrownVolumeScorched")
    :group-variable/conditionally-set? true
    :group-variable/actions
    (sm/postwalk-insert
     [{:action/name                  "Enable if selected tree species uses crown scorch equation type"
       :action/type                  :select
       :action/conditionals
       #{{:conditional/group-variable-uuid
          (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
          :conditional/type     :group-variable
          :conditional/operator :in
          :conditional/values   (set crown-scorch-species-codes)}}
       :action/conditionals-operator :and}])}

   {:db/id                             (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched")
    :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getTreeCrownLengthScorched")
    :group-variable/conditionally-set? true
    :group-variable/actions
    (sm/postwalk-insert
     [{:action/name                  "Enable if selected tree species uses crown scorch equation type"
       :action/type                  :select
       :action/conditionals
       #{{:conditional/group-variable-uuid
          (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
          :conditional/type     :group-variable
          :conditional/operator :in
          :conditional/values   (set crown-scorch-species-codes)}}
       :action/conditionals-operator :and}])}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-entities-tree-crown-volume-scorched
  [{:variable/name            "Tree Crown Volume Scorched Backing"
    :variable/kind            :continuous
    :variable/domain-uuid     (domain-name->uuid "Probability of Mortality & Crown Vol Scorched")
    :variable/group-variables [-2]}

   {:variable/name            "Tree Crown Volume Scorched Flanking"
    :variable/kind            :continuous
    :variable/domain-uuid     (domain-name->uuid "Probability of Mortality & Crown Vol Scorched")
    :variable/group-variables [-3]}

   ;; Crown Volume Scorched Backing (output)
   {:db/id                             -2
    :group-variable/cpp-namespace      (sm/cpp-ns->uuid conn "global")
    :group-variable/cpp-class          (sm/cpp-class->uuid conn "SIGMortality")
    :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getTreeCrownVolumeScorchedBacking")
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
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_backing"
    :group-variable/result-translation-key
    "behaveplus:mortality:result:tree_mortality:tree_mortality:tree_crown_volume_scorched_backing"
    :group-variable/help-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_backing:help"}

   ;; Crown Volume Scorched Flanking (output)
   {:db/id                             -3
    :group-variable/cpp-namespace      (sm/cpp-ns->uuid conn "global")
    :group-variable/cpp-class          (sm/cpp-class->uuid conn "SIGMortality")
    :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getTreeCrownVolumeScorchedFlanking")
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
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_flanking"
    :group-variable/result-translation-key
    "behaveplus:mortality:result:tree_mortality:tree_mortality:tree_crown_volume_scorched_flanking"
    :group-variable/help-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_flanking:help"}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-entities-tree-crown-length-scorched
  [{:variable/name            "Tree Crown Length Scorched Backing"
    :variable/kind            :continuous
    :variable/domain-uuid     (domain-name->uuid "Tree & Canopy Height")
    :variable/group-variables [-4]}

   {:variable/name            "Tree Crown Length Scorched Flanking"
    :variable/kind            :continuous
    :variable/domain-uuid     (domain-name->uuid "Tree & Canopy Height")
    :variable/group-variables [-5]}

   ;; Crown Length Scorched Backing (output)
   {:db/id                             -4
    :group-variable/cpp-namespace      (sm/cpp-ns->uuid conn "global")
    :group-variable/cpp-class          (sm/cpp-class->uuid conn "SIGMortality")
    :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getTreeCrownLengthScorchedBacking")
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
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_backing"
    :group-variable/result-translation-key
    "behaveplus:mortality:result:tree_mortality:tree_mortality:tree_crown_length_scorched_backing"
    :group-variable/help-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_backing:help"}

   ;; Crown Length Scorched Flanking (output)
   {:db/id                             -5
    :group-variable/cpp-namespace      (sm/cpp-ns->uuid conn "global")
    :group-variable/cpp-class          (sm/cpp-class->uuid conn "SIGMortality")
    :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getTreeCrownLengthScorchedFlanking")
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
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_flanking"
    :group-variable/result-translation-key
    "behaveplus:mortality:result:tree_mortality:tree_mortality:tree_crown_length_scorched_flanking"
    :group-variable/help-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_flanking:help"}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-refs
  [{:db/id                 (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality")
    :group/group-variables [-2 -3 -4 -5]}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-translations
  (sm/build-translations-payload
   conn
   100
   {"behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched"          "Crown Volume Scorched"
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_backing"  "Crown Volume Scorched Backing"
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_flanking" "Crown Volume Scorched Flanking"
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched"          "Crown Length Scorched"
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_backing"  "Crown Length Scorched Backing"
    "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_flanking" "Crown Length Scorched Flanking"}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def final-payload (concat existing-entities
                           (sm/postwalk-insert new-entities-tree-crown-volume-scorched)
                           (sm/postwalk-insert new-entities-tree-crown-length-scorched)
                           new-refs
                           new-translations))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn build-reset-order-payload [eid group-attr order-attr]
  (let [eids (map :db/id (->> (d/entity (d/db conn) eid)
                              group-attr
                              (sort-by order-attr)))]
    (map-indexed  (fn [index v]
                    {:db/id     v
                     order-attr index})
                  eids)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def reset-gv-order-payload
  (build-reset-order-payload (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality")
                             :group/group-variables
                             :group-variable/order))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  (do
    #_{:clj-kondo/ignore [:missing-docstring]}
    (def tx-data (d/transact conn final-payload))
    #_{:clj-kondo/ignore [:missing-docstring]}
    (def tx-data-2 (d/transact conn reset-gv-order-payload))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (do
    (sm/rollback-tx! conn @tx-data-2)
    (sm/rollback-tx! conn @tx-data)
    (sm/rollback-tx! conn @cms-import-tx)
    (sm/rollback-tx! conn @cms-remove-tx)))
