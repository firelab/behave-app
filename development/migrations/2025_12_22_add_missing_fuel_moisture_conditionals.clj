(ns migrations.2025-12-22-add-missing-fuel-moisture-conditionals
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Add missing conditionals to show Fuel Moisture Groups when Wind Driven Fuel Model Codes are selected. Currently
;; these values exist in the conditionals for the "Fuel Model" Group but we actually need these values for the "Wind Driven Fuel Model Code" group

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
(def payload
  [;; Update 10-h Fuel Moisture related Group conditionals

   {:db/id                       (sm/t-key->eid conn "behaveplus:surface:input:fuel_moisture:by-size-class:10-h-fuel-moisture")
    :group/conditionals          [(sm/->conditional conn
                                                    {:ttype               :group-variable
                                                     :operator            :in
                                                     :values              ["103" "108" "109"]
                                                     :group-variable-uuid (sm/t-key->uuid conn "behaveplus:surface:input:fuel_models:standard:wind-driven-fuel-model:wind-driven-fuel-model")})]
    :group/conditionals-operator :or}

   {:db/id                       (sm/t-key->eid conn "behaveplus:surface:input:fuel_moisture:dead-live-herb-and-live-woody-categories:dead-fuel-moisture")
    :group/conditionals          [(sm/->conditional conn
                                                    {:ttype               :group-variable
                                                     :operator            :in
                                                     :values              ["103" "108" "109"]
                                                     :group-variable-uuid (sm/t-key->uuid conn "behaveplus:surface:input:fuel_models:standard:wind-driven-fuel-model:wind-driven-fuel-model")})]
    :group/conditionals-operator :or}

   ;; Update Live Herbaceous related Group Conditionals
   {:db/id                       (sm/t-key->eid conn "behaveplus:surface:input:fuel_moisture:by-size-class:live-herbaceous-fuel-moisture")
    :group/conditionals          [(sm/->conditional conn
                                                    {:ttype               :group-variable
                                                     :operator            :in
                                                     :values              ["101" "102" "103" "104" "105" "106" "107" "108" "109" "121"]
                                                     :group-variable-uuid (sm/t-key->uuid conn "behaveplus:surface:input:fuel_models:standard:wind-driven-fuel-model:wind-driven-fuel-model")})]
    :group/conditionals-operator :or}

   {:db/id                       (sm/t-key->eid conn "behaveplus:surface:input:fuel_moisture:dead-live-herb-and-live-woody-categories:live-herbaceous-fuel-moisture")
    :group/conditionals          [(sm/->conditional conn
                                                    {:ttype               :group-variable
                                                     :operator            :in
                                                     :values              ["101" "102" "103" "104" "105" "106" "107" "108" "109" "121"]
                                                     :group-variable-uuid (sm/t-key->uuid conn "behaveplus:surface:input:fuel_models:standard:wind-driven-fuel-model:wind-driven-fuel-model")})]
    :group/conditionals-operator :or}

   ;; Update Live Woody related Group Conditionals
   {:db/id              (sm/t-key->eid conn "behaveplus:surface:input:fuel_moisture:by-size-class:live-woody-fuel-moisture")
    :group/conditionals [(sm/->conditional conn
                                           {:ttype               :group-variable
                                            :operator            :in
                                            :values              ["121"]
                                            :group-variable-uuid (sm/t-key->uuid conn "behaveplus:surface:input:fuel_models:standard:wind-driven-fuel-model:wind-driven-fuel-model")})]}

   {:db/id                       (sm/t-key->eid conn "behaveplus:surface:input:fuel_moisture:dead-live-herb-and-live-woody-categories:live-woody-fuel-moisture")
    :group/conditionals          [(sm/->conditional conn
                                                    {:ttype               :group-variable
                                                     :operator            :in
                                                     :values              ["121"]
                                                     :group-variable-uuid (sm/t-key->uuid conn "behaveplus:surface:input:fuel_models:standard:wind-driven-fuel-model:wind-driven-fuel-model")})]
    :group/conditionals-operator :or}])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e  (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
