(ns migrations.2025-01-31-update-fuel-moisture-conditionals
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; 1. Enable Mediteranean, Chapparal, and Coastal Sage Brush Fuel Model codes
;; 2. Add conditionals to show fuel moisture size classes based on what is necessary for that fuel
;; model code. See excel sheet linked in Jira ticket.

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
(def ten-h
  #{"111" "155" "156" "157" "158" "159" "166" "167" "168" "169" "170" "171" "172"
    "190" "191" "192" "193" "150" "151" "152" "153" "154"} )

#_{:clj-kondo/ignore [:missing-docstring]}
(def hundred-h
  #{"166" "168" "169" "170" "190" "191" "193" "150" "151" "152" "153" "154"} )

#_{:clj-kondo/ignore [:missing-docstring]}
(def live-herbacheous
  #{"167" "168" "150" "151" "152" "153" "154"})

#_{:clj-kondo/ignore [:missing-docstring]}
(def live-woody
  #{"111" "155" "156" "157" "158" "159" "166" "167" "168" "169" "170" "171" "172"
    "190" "191" "193" "150" "151" "152" "153" "154" })

#_{:clj-kondo/ignore [:missing-docstring]}
(def ten-h-fuel-moisture-conditional (d/entity (d/db conn) 4611681620380881711))

#_{:clj-kondo/ignore [:missing-docstring]}
(def hundred-h-fuel-moisture-conditional (d/entity (d/db conn) 4611681620380881712))

#_{:clj-kondo/ignore [:missing-docstring]}
(def live-herbacheous-fuel-moisture-conditional (d/entity (d/db conn) 4611681620380881713))

#_{:clj-kondo/ignore [:missing-docstring]}
(def live-woody-fuel-moisture-conditional (d/entity (d/db conn) 4611681620380881767))

#_{:clj-kondo/ignore [:missing-docstring]}
(def all-codes-to-unhide
  (set (concat ten-h hundred-h live-herbacheous live-woody)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def list-options
  (:list/options (d/entity (d/db conn) (sm/name->eid conn :list/name "SurfaceFuelModels"))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def enable-fuel-model-codes-payload
  (->> list-options
       (filter #(contains? all-codes-to-unhide (:list-option/value %)))
       (map (fn [{eid :db/id}]
              {:db/id             eid
               :list-option/hide? false}))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn add-values-to-conditional [conditional-entity values-to-add]
  {:db/id              (:db/id conditional-entity)
   :conditional/values (set (into (:conditional/values conditional-entity)
                                  values-to-add))})

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [(add-values-to-conditional ten-h-fuel-moisture-conditional ten-h)
   (add-values-to-conditional hundred-h-fuel-moisture-conditional hundred-h)
   (add-values-to-conditional live-herbacheous-fuel-moisture-conditional live-herbacheous)
   (add-values-to-conditional live-woody-fuel-moisture-conditional live-woody)])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn (concat payload enable-fuel-model-codes-payload))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
