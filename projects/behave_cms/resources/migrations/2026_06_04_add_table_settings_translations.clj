(ns migrations.2026-06-04-add-table-settings-translations
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Seeds English translations for the table-settings UI keys introduced with
;; the results settings modal (BHP1-1583).

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring :unused-binding]}
(defn payload-fn [db]
  (sm/build-translations-payload
   db
   {"behaveplus:table-settings"     "Table Settings"
    "behaveplus:row-variable"       "Row variable"
    "behaveplus:column-variable"    "Column variable"
    "behaveplus:sub-table-variable" "Sub-table variable"}))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server        :as cms]
           '[behave-cms.store         :as store])
  (cms/init-db!)

  (def conn (store/default-conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
