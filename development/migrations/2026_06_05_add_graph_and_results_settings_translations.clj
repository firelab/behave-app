(ns migrations.2026-06-05-add-graph-and-results-settings-translations
  (:require [datomic.api              :as d]
            [schema-migrate.interface :refer [bp] :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Adds translations for string literals in wizard-results-settings-page and graph-settings-modal
;; that were previously hard-coded English strings.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring :unused-binding]}
(defn payload-fn [db]
  (sm/build-translations-payload
   db
   {(bp "table_settings") "Table Settings"
    (bp "show_graphs")    "Show Graphs"
    (bp "graph_settings") "Graph Settings"}))

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
