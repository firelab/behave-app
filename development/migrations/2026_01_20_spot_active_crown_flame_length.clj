(ns migrations.2026-01-20-spot-active-crown-flame-length
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; Sets the Crown > Spot (Input) > Active Crown Flame Length variable to the new `setActiveCrownFlameLength` setter
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
(def new-active-crown-function-param-uuids
  (d/q '[:find [?f-uuid ?p-uuid]
         :where
         [?c :cpp.class/name "SIGSpot"]
         [?c :cpp.class/function ?f]
         [?f :cpp.function/name "setActiveCrownFlameLength"]
         [?f :bp/uuid ?f-uuid]
         [?f :cpp.function/parameter ?p]
         [?p :cpp.parameter/name "flameLength"]
         [?p :bp/uuid ?p-uuid]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload 
  [{:db/id                        (sm/t-key->eid conn "behaveplus:crown:input:spotting:fire_behavior:active_crown_flame_length:active_crown_flame_length")
    :group-variable/cpp-function  (first new-active-crown-function-param-uuids)
    :group-variable/cpp-parameter (second new-active-crown-function-param-uuids)}])
 
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
