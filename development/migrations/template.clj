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
;; The runner calls (payload-fn conn) at startup.
;; Return a vector of transaction data.

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [conn]
  [])

;; Option B: Multi-step migration (uncomment and remove payload-fn above)
;; Each step is (fn [conn db] tx-data). The runner passes a fresh db snapshot
;; taken after the prior step commits, so step N sees step N-1's writes.
;; Use conn for sm/* helpers; use db for direct d/q / d/entity calls.
;; If any step fails, all previously completed steps are rolled back.
;;
;; #_{:clj-kondo/ignore [:missing-docstring]}
;; (def payload-steps
;;   [(fn [conn db] [{:db/id (sm/t-key->eid conn "behaveplus:...") :group/order 0}])
;;    (fn [conn db] [[:db/retractEntity (d/q '[:find ?e . :where ...] db)]])])

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

(comment
  (require '[behave-cms.server :as cms])
  (cms/init-db!)

  #_{:clj-kondo/ignore [:missing-docstring]}
  (def conn (behave-cms.store/default-conn))

  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn (payload-fn conn)))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
