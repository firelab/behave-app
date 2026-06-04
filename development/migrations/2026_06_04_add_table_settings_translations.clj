(ns migrations.2026-06-04-add-table-settings-translations
  (:require [behave-cms.server        :as cms]
            [behave-cms.store         :refer [default-conn]]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

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
   {"behaveplus:table-settings"     "Table Settings"
    "behaveplus:row-variable"       "Row variable"
    "behaveplus:column-variable"    "Column variable"
    "behaveplus:sub-table-variable" "Sub-table variable"}))

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
