(ns migrations.2026-03-25-add-torching-trees-subgroups
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Add "Downwind Canopy Fuel" group under Crown > Input > Spotting 
;; move Downwind Canopy Cover, Downwind Canopy Height into it.
;; Reorder the remaining Torching Trees children:
;;   1. Torching Tree Species
;;   2. DBH
;;   3. Number of Torching Trees
;;   4. Torching Tree Height

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
(def spotting-submodule-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def torching-trees-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def downwind-canopy-cover-group-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:downwind-canopy-cover"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def downwind-canopy-height-group-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:downwind-canopy-height"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def torching-tree-species-group-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:torching_tree_species"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def dbh-group-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:dbh_diameter_at_breast_height"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def number-of-torching-trees-group-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:number_of_torching_trees_numeric_value"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def torching-tree-height-group-eid
  (sm/t-key->eid conn "behaveplus:crown:input:spotting:torching_trees:torching_tree_height"))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [;; 1. Create new Downwind Canopy Fuel group
   (sm/postwalk-insert
    {:db/id                        -1
     :submodule/_groups            spotting-submodule-eid
     :group/name                   "Downwind Canopy Fuel"
     :group/order                  1
     :group/translation-key        "behaveplus:crown:input:spotting:downwind_canopy_fuel"
     :group/result-translation-key "behaveplus:crown:result:spotting:downwind_canopy_fuel"
     :group/help-key               "behaveplus:crown:input:spotting:downwind_canopy_fuel:help"})

   ;; 2. Move Downwind Canopy Cover and Height into the new group
   {:db/id           downwind-canopy-cover-group-eid
    :group/_children -1
    :group/order     0}

   {:db/id           downwind-canopy-height-group-eid
    :group/_children -1
    :group/order     1}

   ;; 3. Retract old child references from torching_trees
   [:db/retract torching-trees-eid :group/children downwind-canopy-cover-group-eid]
   [:db/retract torching-trees-eid :group/children downwind-canopy-height-group-eid]

   ;; 4. Reorder remaining Torching Trees children
   {:db/id torching-tree-species-group-eid      :group/order 0}
   {:db/id dbh-group-eid                        :group/order 1}
   {:db/id number-of-torching-trees-group-eid   :group/order 2}
   {:db/id torching-tree-height-group-eid       :group/order 3}

   ;; 5. Reorder groups within the Spotting submodule
   {:db/id torching-trees-eid                                             :group/order 0}
   ;; Downwind Canopy Fuel (-1) is created above with :group/order 1
   {:db/id (sm/t-key->eid conn "behaveplus:crown:input:spotting:fire_behavior") :group/order 2}
   {:db/id (sm/t-key->eid conn "behaveplus:crown:input:spotting:canopy_fuel")   :group/order 3}
   {:db/id (sm/t-key->eid conn "behaveplus:crown:input:spotting:topography")    :group/order 4}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def translation-payload
  (sm/build-translations-payload
   conn 100
   {"behaveplus:crown:input:spotting:downwind_canopy_fuel" "Downwind Canopy Fuel"}))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn (concat payload translation-payload)))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
