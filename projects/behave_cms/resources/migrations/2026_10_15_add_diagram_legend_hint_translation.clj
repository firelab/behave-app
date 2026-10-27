(ns migrations.2026-10-15-add-diagram-legend-hint-translation
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Every results diagram has a legend whose entries toggle their series on click, but nothing tells the
;; user so. Add the translation for the hint rendered as the diagram's subtitle:
;; "Click items in Legend to view or hide".

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (sm/build-translations-payload
   db
   {"behaveplus:diagram:legend_hint" "Click items in Legend to view or hide"}))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server        :as cms])
  (cms/init-db!)

  (def conn (behave-cms.store/default-conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
