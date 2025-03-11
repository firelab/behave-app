(ns migrations.2025-03-11-update-contain-add-resource-edn
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def re-order-parameters-payload
  (->> (sm/name->eid conn :cpp.function/name "addResource")
       (d/entity (d/db conn))
       :cpp.function/parameter
       (sort-by :cpp.parameter/order)
       rest
       (map (fn [parameter-entity]
              {:db/id               (:db/id parameter-entity)
               :cpp.parameter/order (inc (:cpp.parameter/order parameter-entity))}))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-param-payload
  [(sm/postwalk-insert
    {:cpp.function/_parameter (sm/name->eid conn :cpp.function/name "addResource")
     :cpp.parameter/name      "arrivalTimeUnit"
     :cpp.parameter/order     1
     :cpp.parameter/type "TimeUnits::TimeUnitsEnum"})])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn (concat re-order-parameters-payload new-param-payload))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))


(comment
  (->> (sm/name->eid conn :cpp.function/name "addResource")
       (d/entity (d/db conn))
       :cpp.function/parameter
       (sort-by :cpp.parameter/order)
       (map d/touch))

  )
