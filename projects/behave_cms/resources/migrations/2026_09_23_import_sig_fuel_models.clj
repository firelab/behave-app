(ns migrations.2026-09-23-import-sig-fuel-models
  (:require [clojure.edn              :as edn]
            [clojure.java.io          :as io]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; Imports the SIGFuelModels C++ class into the CMS registry so record types can bind to it.
;;
;; Custom fuel models are configured by calling SIGFuelModels::defineCustomFuelModel before a run, but the
;; class was never imported — `global` held every other SIG class and not this one — so the record type
;; editor's Namespace -> Class -> Function dropdowns had nothing to offer.
;;
;; The resource read below is a frozen copy of cms-exports/SIGFuelModels.edn, generated from the
;; `interface SIGFuelModels` block of behave-lib/include/idl/behave.idl. It must not be edited: a migration
;; has to produce the same result on a fresh database forever. If the IDL gains a method later, add a new
;; migration with its own frozen resource.
;; ===========================================================================================================

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [namespace-eid (d/q '[:find ?e . :where [?e :cpp.namespace/name "global"]] db)
        exported      (-> "cms_exports/2026_09_23_sig_fuel_models.edn" io/resource slurp edn/read-string)]
    (when (nil? namespace-eid)
      (throw (ex-info "No `global` cpp namespace to attach SIGFuelModels to." {})))
    ;; sm/->cpp-class is idempotent: it reuses an existing class's :db/id and omits functions already
    ;; attached to it, so re-running this migration transacts nothing.
    [{:db/id               namespace-eid
      :cpp.namespace/class [(sm/->cpp-class db (first (:global exported)))]}]))

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
