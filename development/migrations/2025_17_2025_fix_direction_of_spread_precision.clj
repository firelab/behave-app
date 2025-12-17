(ns migrations.2025-17-2025-fix-direction-of-spread-precision
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
(def direction-dimension-entity
  (d/entity (d/db conn) (sm/name->eid conn :dimension/name "Direction")))

#_{:clj-kondo/ignore [:missing-docstring]}
(def direction-dimension-uuid
  (:bp/uuid direction-dimension-entity))

#_{:clj-kondo/ignore [:missing-docstring]}
(def degree-unit-uuid
  (:bp/uuid (first (:dimension/units direction-dimension-entity))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-domain
  (sm/postwalk-insert
   {:domain/name              "Spread Direction"
    :domain-set/_domains      (sm/name->eid conn :domain-set/name "Fire & Effects")
    :domain/dimension-uuid    direction-dimension-uuid
    :domain/decimals          0
    :domain/native-unit-uuid  degree-unit-uuid
    :domain/metric-unit-uuid  degree-unit-uuid
    :domain/english-unit-uuid degree-unit-uuid}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def update-domain-for-var-direction-of-heading
  {:db/id                (sm/name->eid conn :variable/name "Direction of Heading")
   :variable/domain-uuid (:bp/uuid new-domain)})

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [new-domain

   ;; remove dimension from variables
   [:db/retract (sm/name->eid conn :variable/name "Direction of Heading") :variable/dimension-uuid]
   [:db/retract (sm/name->eid conn :variable/name "Direction of Backing") :variable/dimension-uuid]
   [:db/retract (sm/name->eid conn :variable/name "Direction of Flanking") :variable/dimension-uuid]

   ;; add domain to variables
   {:db/id                (sm/name->eid conn :variable/name "Direction of Heading")
    :variable/domain-uuid (:bp/uuid new-domain)}
   {:db/id                (sm/name->eid conn :variable/name "Direction of Backing")
    :variable/domain-uuid (:bp/uuid new-domain)}
   {:db/id                (sm/name->eid conn :variable/name "Direction of Flanking")
    :variable/domain-uuid (:bp/uuid new-domain)}])

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
