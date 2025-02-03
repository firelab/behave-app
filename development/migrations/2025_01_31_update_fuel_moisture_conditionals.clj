(ns migrations.2025-01-31-update-fuel-moisture-conditionals
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; 1. Enable Mediteranean, Chapparal, and Coastal Sage Brush Fuel Model codes

;; 2. Add values to existing conditionals to show fuel moisture size classes based on what is necessary for that fuel
;; model code. See excel sheet linked in Jira ticket.

;; 3. Add a new conditional to Surface > Inputs > Fuel Moisture > Dead Live Herb and Live Woody
;; Categories > Dead Fuel Moisture. This includes all fuel moistures from standard, mediterranean,
;; chapparal, and coastal sage brush fuel models

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

;; Individual Size Class Conditionals
#_{:clj-kondo/ignore [:missing-docstring]}
(def individual-size-class-ten-h-fuel-moisture-conditional (d/entity (d/db conn) 4611681620380881711))

#_{:clj-kondo/ignore [:missing-docstring]}
(def individual-size-class-hundred-h-fuel-moisture-conditional (d/entity (d/db conn) 4611681620380881712))

#_{:clj-kondo/ignore [:missing-docstring]}
(def individual-size-class-live-herbacheous-fuel-moisture-conditional (d/entity (d/db conn) 4611681620380881713))

#_{:clj-kondo/ignore [:missing-docstring]}
(def individual-size-class-live-woody-fuel-moisture-conditional (d/entity (d/db conn) 4611681620380881767))

;; Dead, Live Hearb, and Live Woody Categories Conditionals
(def live-herbacheous-fuel-moisture-conditional (d/entity (d/db conn) 4611681620380881714))

(def live-woody-fuel-moisture-conditional (d/entity (d/db conn) 4611681620380881714))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn add-values-to-conditional [conditional-entity values-to-add]
  {:db/id              (:db/id conditional-entity)
   :conditional/values (set (into (:conditional/values conditional-entity)
                                  values-to-add))})

#_{:clj-kondo/ignore [:missing-docstring]}
(def add-new-conditional-payload
  [{:db/id                      (sm/t-key->eid conn
                                               "behaveplus:surface:input:fuel_moisture:dead-live-herb-and-live-woody-categories:dead-fuel-moisture")
    :group/conditionals         (sm/postwalk-insert
                                 [{:conditional/values (->> [1 2 3 4 5 6 7 8 9 10 11 12 13 101 102
                                                             103 104 105 106 107 108 109 111 121 122
                                                             123 124 141 142 143 144 145 146 147 148
                                                             149 150 151 152 153 154 155 156 157 158
                                                             159 161 162 163 164 165 166 167 168 169
                                                             170 171 172 181 182 183 184 185 186 187
                                                             188 189 190 191 192 193 201 202 203
                                                             204]
                                                            (map str)
                                                            set)
                                   :conditional/operator :in
                                   :conditional/group-variable-uuid (sm/t-key->uuid conn
                                                                                    "behaveplus:surface:input:fuel_models:standard:fuel_model:fuel_model")
                                   :conditional/type   :group-variable}])
    :group/conditionals-operator :and}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [(add-values-to-conditional individual-size-class-ten-h-fuel-moisture-conditional ten-h)
   (add-values-to-conditional individual-size-class-hundred-h-fuel-moisture-conditional hundred-h)
   (add-values-to-conditional individual-size-class-live-herbacheous-fuel-moisture-conditional live-herbacheous)
   (add-values-to-conditional individual-size-class-live-woody-fuel-moisture-conditional live-woody)
   (add-values-to-conditional live-herbacheous-fuel-moisture-conditional live-herbacheous)
   (add-values-to-conditional live-woody-fuel-moisture-conditional live-woody)])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn (concat payload enable-fuel-model-codes-payload add-new-conditional-payload))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
