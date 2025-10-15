(ns migrations.2025-10-14-set-vpd-dynamic-units
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; 1. Find Required UUIDs and Entity IDs
;; 2. Create Vapor Pressure Deficit domain
;; 3. Associate domain with Weather domain-set
;; 4. Update Vapor Pressure Deficit variable and remove old attributes
;; 5. Set :subtool-variable/dynamic-units? to true for Vapor Pressure Deficit output variable
;; 6. Update unit names for pressure units

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; 1. Find Required UUIDs and Entity IDs
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def pressure-dimension-uuid
  (d/q '[:find ?uuid .
         :where
         [?e :dimension/name "Pressure"]
         [?e :bp/uuid ?uuid]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def hectopascal-unit-uuid
  (d/q '[:find ?uuid .
         :where
         [?u :unit/short-code "hPa"]
         [?u :bp/uuid ?uuid]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def kilopascal-unit-uuid
  (d/q '[:find ?uuid .
         :where
         [?u :unit/short-code "kPa"]
         [?u :bp/uuid ?uuid]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def psi-unit-uuid
  (d/q '[:find ?uuid .
         :where
         [?u :unit/short-code "psi"]
         [?u :bp/uuid ?uuid]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def weather-domain-set-eid
  (d/q '[:find ?e .
         :where
         [?e :domain-set/name "Weather"]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def vpd-variable-eid
  (d/q '[:find ?e .
         :where
         [?e :variable/name "Vapor Pressure Deficit"]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def vpd-output-eid (sm/t-key->eid conn "behaveplus:vapor-pressure-deficit:vapor-pressure-deficit:vapor-pressure-deficit"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def hectopascal-unit-eid
  (d/q '[:find ?e .
         :where
         [?e :unit/short-code "hPa"]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def kilopascal-unit-eid
  (d/q '[:find ?e .
         :where
         [?e :unit/short-code "kPa"]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def psi-unit-eid
  (d/q '[:find ?e .
         :where
         [?e :unit/short-code "psi"]]
       (d/db conn)))

;; ===========================================================================================================
;; 2. Create Vapor Pressure Deficit Domain
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def vpd-domain-uuid (str (d/squuid)))

(def vpd-domain-payload
  [{:db/id                      -1
    :bp/uuid                    vpd-domain-uuid
    :domain/name                "Vapor Pressure Deficit"
    :domain/decimals            2
    :domain/dimension-uuid      pressure-dimension-uuid
    :domain/native-unit-uuid    psi-unit-uuid
    :domain/english-unit-uuid   psi-unit-uuid
    :domain/metric-unit-uuid    hectopascal-unit-uuid
    :domain/filtered-unit-uuids [hectopascal-unit-uuid
                                 kilopascal-unit-uuid
                                 psi-unit-uuid]}])

;; ===========================================================================================================
;; 3. Associate Domain with Weather Domain-Set
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def associate-domain-set-payload
  [{:db/id              weather-domain-set-eid
    :domain-set/domains -1}])

;; ===========================================================================================================
;; 4. Update Vapor Pressure Deficit variable and remove old attributes
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def update-variable-payload
  [{:db/id                vpd-variable-eid
    :variable/domain-uuid vpd-domain-uuid}
   [:db/retract vpd-variable-eid :variable/dimension-uuid]
   [:db/retract vpd-variable-eid :variable/english-unit-uuid]
   [:db/retract vpd-variable-eid :variable/metric-unit-uuid]
   [:db/retract vpd-variable-eid :variable/native-unit-uuid]])

;; ===========================================================================================================
;; 5. Set Dynamic Units for VPD Output
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def vpd-dynamic-units-payload
  [{:db/id                           vpd-output-eid
    :subtool-variable/dynamic-units? true}])

;; ===========================================================================================================
;; 6. Update Unit Names
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def update-unit-names-payload
  [{:db/id     hectopascal-unit-eid
    :unit/name "Hectopascal"}
   {:db/id     kilopascal-unit-eid
    :unit/name "Kilopascal"}
   {:db/id     psi-unit-eid
    :unit/name "Pounds Per Square Inch"}])

;; ===========================================================================================================
;; Combined Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat (sm/postwalk-insert vpd-domain-payload)
          associate-domain-set-payload
          update-variable-payload
          vpd-dynamic-units-payload
          update-unit-names-payload))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
