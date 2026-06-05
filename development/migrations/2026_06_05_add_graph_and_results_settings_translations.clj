(ns migrations.2026-06-05-add-graph-and-results-settings-translations
  (:require [behave-cms.server        :as cms]
            [behave-cms.store         :refer [default-conn]]
            [datomic.api              :as d]
            [schema-migrate.interface :refer [bp] :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Adds translations for string literals in wizard-results-settings-page and graph-settings-modal
;; that were previously hard-coded English strings.

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
  (sm/build-translations-payload
   conn
   {(bp "table_settings") "Table Settings"
    (bp "show_graphs")    "Show Graphs"
    (bp "graph_settings") "Graph Settings"}))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
