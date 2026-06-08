(ns migrations.template
  (:require [datomic.api :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Describe what this migration does and why.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Option A: Single-step migration
;; The runner calls (payload-fn db) at startup.
;; Return a vector of transaction data.

#_{:clj-kondo/ignore [:missing-docstring :unused-binding]}
(defn payload-fn [db]
  [])

;; Option B: Multi-step migration (uncomment and remove payload-fn above)
;; Each step is (fn [db] tx-data). The runner passes a fresh db snapshot
;; taken after the prior step commits, so step N sees step N-1's writes.
;; If any step fails, all previously completed steps are rolled back.
;;
;; #_{:clj-kondo/ignore [:missing-docstring]}
;; (def payload-steps
;;   [(fn [db] [{:db/id (sm/t-key->eid db "behaveplus:...") :group/order 0}])
;;    (fn [db] [[:db/retractEntity (d/q '[:find ?e . :where ...] db)]])])

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server :as cms])
  (cms/init-db!)

  (def conn (behave-cms.store/default-conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
