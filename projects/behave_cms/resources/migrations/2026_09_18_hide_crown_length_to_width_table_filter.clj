(ns migrations.2026-09-18-hide-crown-length-to-width-table-filter
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; BHP1-1637 follow-up — Length-to-Width Ratio has two output group variables,
;; one under Surface and one under Crown. The 2026-08-26 migration flagged only
;; the Surface one, but a Surface & Crown worksheet shows the Crown one, so its
;; Table Shading Filter row was still there.
;;
;; After this runs, re-export layout.msgpack.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Single-step migration: the runner calls (payload-fn db) at startup and
;; transacts the returned vector.

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  [{:db/id                             (sm/t-key->eid db "behaveplus:crown:output:size:length_to_width_ratio:length_to_width_ratio")
    :group-variable/hide-table-filter? true}])

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
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
