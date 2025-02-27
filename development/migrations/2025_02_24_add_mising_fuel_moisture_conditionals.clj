(ns migrations.2025-02-24-add-mising-fuel-moisture-conditionals
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

(def ten-h
  #{"110"} )

#_{:clj-kondo/ignore [:missing-docstring]}
(def live-herbacheous
  #{"111" "110" "155"})

#_{:clj-kondo/ignore [:missing-docstring]}
(def list-options
  (:list/options (d/entity (d/db conn) (sm/name->eid conn :list/name "SurfaceFuelModels"))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def individual-size-class-ten-h-fuel-moisture-conditional (d/entity (d/db conn) 4611681620380881711))

#_{:clj-kondo/ignore [:missing-docstring]}
(def individual-size-class-live-herbacheous-fuel-moisture-conditional (d/entity (d/db conn) 4611681620380881713))


#_{:clj-kondo/ignore [:missing-docstring]}
(defn add-values-to-conditional [conditional-entity values-to-add]
  {:db/id              (:db/id conditional-entity)
   :conditional/values (set (into (:conditional/values conditional-entity)
                                  values-to-add))})

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [(add-values-to-conditional individual-size-class-ten-h-fuel-moisture-conditional ten-h)
   (add-values-to-conditional individual-size-class-live-herbacheous-fuel-moisture-conditional live-herbacheous)])

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
