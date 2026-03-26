(ns migrations.template
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]))

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
;; Each step is a function of conn (or a raw payload vector).
;; If any step fails, all previously completed steps are rolled back.
;;
;; #_{:clj-kondo/ignore [:missing-docstring]}
;; (def payload-steps
;;   [(fn [conn] [{:db/id (sm/t-key->eid conn "behaveplus:...") :group/order 0}])
;;    (fn [conn] [[:db/retractEntity (sm/t-key->eid conn "behaveplus:...")]])])

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
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
