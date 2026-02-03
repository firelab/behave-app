(ns migrations.2026-02-03-add-results-order-attr
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Adds order attr to module, submodule, and group order entities, (copied over from original order)

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(def order-attrs
  [[:module/order :module/results-order]
   [:submodule/order :submodule/results-order]
   [:group/order :group/results-order]])

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (mapcat
   (fn [[order-attr result-order-attr]]
     (map
      (fn [[eid order]]
        {:db/id            eid
         result-order-attr order})
      (d/q '[:find ?e ?order
             :in $ ?order-attr
             :where
             [?e ?order-attr ?order]]
           (d/db conn)
           order-attr)))
   order-attrs))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e  (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
