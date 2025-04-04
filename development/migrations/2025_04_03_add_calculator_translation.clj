(ns migrations.2025-04-03-add-calculator-translation
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (sm/build-translations-payload
   conn
   {"behaveplus:calculator" "Calculator"}))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; Rollback
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
