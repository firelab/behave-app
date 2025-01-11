(ns migrations.2025-01-10-fix-equals-conditionals
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; 1. Modify all Condiitionals where the operator is `:equals` -> `:equal`

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
(def equals-conditional-ids 
  (d/q '[:find [?e ...]
         :where
         [?e :conditional/operator :equals]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (mapv (fn [eid] {:db/id eid :conditional/operator :equal}) equals-conditional-ids))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn payload))
  )

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data)
  )
