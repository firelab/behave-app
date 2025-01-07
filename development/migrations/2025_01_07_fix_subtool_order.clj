(ns migrations.2025-01-07-fix-subtool-order
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

(defn build-reset-order-payload
  [eid group-attr order-attr]
  (let [eids (map :db/id (->> (d/entity (d/db conn) eid)
                              group-attr
                              (sort-by order-attr)))]
    (map-indexed  (fn [index v]
                    {:db/id     v
                     order-attr index})
                  eids)))

(def update-function-return-type-payload
  (->> "1-HR Fuel Moisture"
       (sm/name->eid conn :subtool/name)
       (d/entity (d/db conn))
       :subtool/variables
       (filter #(= (:subtool-variable/io %) :output))
       (map #(d/entity (d/db conn) (:db/id %)))
       (map :subtool-variable/cpp-function-uuid)
       (map #(d/entity (d/db conn) [:bp/uuid %]))
       (map (fn [{id :db/id}] {:db/id                    id
                               :cpp.function/return-type "ProbabilityUnits::ProbabilityUnitsEnum"
                               :cpp.function/parameter   [{:cpp.parameter/name "desiredUnits",
                                                           :cpp.parameter/type "ProbabilityUnits::ProbabilityUnitsEnum"}]}))))

(->> "Ignite"
     (sm/name->eid conn :subtool/name)
     (d/entity (d/db conn))
     :subtool/variables
     (filter #(= (:subtool-variable/io %) :output))
     (map #(d/entity (d/db conn) (:db/id %)))
     (map :subtool-variable/cpp-function-uuid)
     (map #(d/entity (d/db conn) [:bp/uuid %]))
     (map d/touch))



#_{:clj-kondo/ignore [:missing-docstring]}
(def update-order-payload
  (build-reset-order-payload (sm/name->eid conn :subtool/name "1-HR Fuel Moisture")
                             :subtool/variables
                             :subtool-variable/order))

(def final-payload (concat update-function-return-type-payload update-order-payload))

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
