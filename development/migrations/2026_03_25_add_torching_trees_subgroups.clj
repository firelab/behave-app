(ns migrations.2026-03-25-add-torching-trees-subgroups
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Add two new subgroups under Crown > Input > Spotting > Torching Trees:
;; 1. Downwind Canopy Fuel  — Downwind Canopy Cover, Downwind Canopy Height
;; 2. Torching Trees        — Number of Torching Trees, DBH, Species, Height
;;
;; Steps:
;; 1. Create two new intermediate subgroups under torching_trees
;; 2. Move existing child groups into the new subgroups
;; 3. Retract old child references from the parent
;; 4. Add translations only for the two brand-new subgroups

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Helpers
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def torching-trees-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees"))

;; Existing child group eids — Downwind Canopy Fuel
#_{:clj-kondo/ignore [:missing-docstring]}
(def downwind-canopy-cover-group-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:downwind-canopy-cover"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def downwind-canopy-height-group-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:downwind-canopy-height"))

;; Existing child group eids — Torching Trees
#_{:clj-kondo/ignore [:missing-docstring]}
(def number-of-torching-trees-group-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:number_of_torching_trees_numeric_value"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def dbh-group-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:dbh_diameter_at_breast_height"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def torching-tree-species-group-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:torching_tree_species"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def torching-tree-height-group-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:torching_tree_height"))

;; Existing group-variable eids
#_{:clj-kondo/ignore [:missing-docstring]}
(def downwind-canopy-cover-gv-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:downwind-canopy-cover:downwind-canopy-cover"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def downwind-canopy-height-gv-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:downwind-canopy-height:downwind-canopy-height"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def number-of-torching-trees-gv-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:number_of_torching_trees_numeric_value:number_of_torching_trees_numeric_value"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def dbh-gv-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:dbh_diameter_at_breast_height:dbh_diameter_at_breast_height"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def torching-tree-species-gv-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:torching_tree_species:torching_tree_species"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def torching-tree-height-gv-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:torching_tree_height:torching_tree_height"))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [;; 1. Create new intermediate subgroups
   (sm/postwalk-insert
    {:db/id                        -1
     :group/_children              torching-trees-eid
     :group/name                   "Downwind Canopy Fuel"
     :group/translation-key        "behaveplus:crown:input:spotting:torching_trees:downwind_canopy_fuel"
     :group/result-translation-key "behaveplus:crown:result:spotting:torching_trees:downwind_canopy_fuel"
     :group/help-key               "behaveplus:crown:input:spotting:torching_trees:downwind_canopy_fuel:help"
     :group/order                  0})

   (sm/postwalk-insert
    {:db/id                        -2
     :group/_children              torching-trees-eid
     :group/name                   "Torching Trees"
     :group/translation-key        "behaveplus:crown:input:spotting:torching_trees:torching_trees"
     :group/result-translation-key "behaveplus:crown:result:spotting:torching_trees:torching_trees"
     :group/help-key               "behaveplus:crown:input:spotting:torching_trees:torching_trees:help"
     :group/order                  1})

   ;; 2. Move existing groups as children of Downwind Canopy Fuel and update keys
   {:db/id                        downwind-canopy-cover-group-eid
    :group/_children              -1
    :group/order                  0}

   {:db/id                        downwind-canopy-height-group-eid
    :group/_children              -1
    :group/order                  1}

   ;; 3. Move existing groups as children of Torching Trees and update keys
   {:db/id                        number-of-torching-trees-group-eid
    :group/_children              -2
    :group/order                  0}

   {:db/id                        dbh-group-eid
    :group/_children              -2
    :group/order                  1}

   {:db/id                        torching-tree-species-group-eid
    :group/_children              -2
    :group/order                  2}

   {:db/id                        torching-tree-height-group-eid
    :group/_children              -2
    :group/order                  3}

   ;; 4. Retract old child references from torching_trees
   [:db/retract torching-trees-eid :group/children downwind-canopy-cover-group-eid]
   [:db/retract torching-trees-eid :group/children downwind-canopy-height-group-eid]
   [:db/retract torching-trees-eid :group/children number-of-torching-trees-group-eid]
   [:db/retract torching-trees-eid :group/children dbh-group-eid]
   [:db/retract torching-trees-eid :group/children torching-tree-species-group-eid]
   [:db/retract torching-trees-eid :group/children torching-tree-height-group-eid]])

;; New translations only needed for the two brand-new subgroups
#_{:clj-kondo/ignore [:missing-docstring]}
(def new-translations-payload
  (sm/build-translations-payload
   conn 100
   {"behaveplus:crown:input:spotting:torching_trees:downwind_canopy_fuel" "Downwind Canopy Fuel"
    "behaveplus:crown:input:spotting:torching_trees:torching_trees"       "Torching Trees"}))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn (concat payload
                                              new-translations-payload)))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
