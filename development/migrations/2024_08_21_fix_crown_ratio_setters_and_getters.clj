(ns migrations.2024-08-21-fix-crown-ratio-setters-and-getters
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [cms-import :refer [add-export-file-to-conn]]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(sm/make-attr-is-component! conn :cpp.function/parameter)

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(def get-crown-ratio-function-entities
  (d/q '[:find [?f ...]
         :in $
         :where
         [?f :cpp.function/name "getCrownRatio"]]
      (d/db conn)))

(def update-get-crown-ratio-payload
 (->> get-crown-ratio-function-entities
      (map (fn [id]
             {:db/id                  id
              :cpp.function/parameter [{:cpp.parameter/name  "crownRatioUnits"
                                        :cpp.parameter/order 0
                                        :cpp.parameter/type  "FractionUnits::FractionUnitsEnum"}]}))))

(def set-crown-ratio-function-entities
  (d/q '[:find [?f ...]
         :in $
         :where
         [?f :cpp.function/name "setCrownRatio"]]
       (d/db conn)))

(def update-set-crown-ratio-payload
 (->> set-crown-ratio-function-entities
      (map (fn [id]
             {:db/id                  id
              :cpp.function/parameter [{:cpp.parameter/name  "crownRatioUnits"
                                        :cpp.parameter/order 1
                                        :cpp.parameter/type  "FractionUnits::FractionUnitsEnum"}]}))))


(def payload (concat
              update-get-crown-ratio-payload
              update-set-crown-ratio-payload))


(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
