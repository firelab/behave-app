(ns migrations.2026-04-27-convert-direction-to-entity
  (:require [behave-cms.server        :as cms]
            [behave-cms.store         :refer [default-conn]]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; 1. Create three Direction entities (:heading, :flanking, :backing) with translation keys and hex colors.
;; 2. Seed English translations for each direction.
;; 3. Link all existing group-variables that have :group-variable/direction to the new Direction entity
;;    via the new :group-variable/direction-ref attribute.
;;
;; The old :group-variable/direction keyword attribute is NOT retracted — it remains as a deprecated
;; fallback. Remove it in a future migration once all code paths have migrated.

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
(def direction-entities
  [{:bp/nid                    "direction-heading"
    :bp/uuid                   (sm/rand-uuid)
    :direction/id              :heading
    :direction/translation-key "behaveplus:heading"
    :direction/color           "#13486a"
    :direction/order           0}
   {:bp/nid                    "direction-flanking"
    :bp/uuid                   (sm/rand-uuid)
    :direction/id              :flanking
    :direction/translation-key "behaveplus:flanking"
    :direction/color           "#4a7086"
    :direction/order           1}
   {:bp/nid                    "direction-backing"
    :bp/uuid                   (sm/rand-uuid)
    :direction/id              :backing
    :direction/translation-key "behaveplus:backing"
    :direction/color           "#8297a3"
    :direction/order           2}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def translations-payload
  (sm/build-translations-payload
   conn
   {"behaveplus:heading"  "Heading"
    "behaveplus:flanking" "Flanking"
    "behaveplus:backing"  "Backing"}))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  ;; Step 1: create direction entities and seed translations
  (def tx1 @(d/transact conn (concat direction-entities translations-payload)))

  ;; Step 2: link existing group-variables to their Direction entity
  (def gv-direction-pairs
    (d/q '[:find ?gv-eid ?direction
           :where [?gv-eid :group-variable/direction ?direction]]
         (d/db conn)))

  (def link-payload
    (mapv (fn [[gv-eid direction]]
            [:db/add gv-eid :group-variable/direction-ref [:direction/id direction]])
          gv-direction-pairs))

  (def tx2 @(d/transact conn link-payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx2)
  (sm/rollback-tx! conn @tx1))
