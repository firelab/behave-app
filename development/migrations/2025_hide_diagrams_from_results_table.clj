(ns migrations.2025-hide-diagrams-from-results-table
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

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
(def diagram-eids
  (d/q '[:find [?gv ...]
        :in $
        :where
        [?e :diagram/group-variable ?gv]
        [?e :diagram/type ?type]]
      (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload (map (fn [eid]{:db/id                       eid
                            :group-variable/hide-result? true})
                  diagram-eids))

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
