(ns migrations.2025-06-23-disable-surface-wind-directions-in-csv
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;;  Disable csv column for Surface Run in Direction Of
;;  Disable csv column Wind Type
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
(def payload
  [;; Wind Direction
   {:db/id                    (sm/t-key->eid conn "behaveplus:surface:input:wind_speed:wind_height:wind_height")
    :group-variable/hide-csv? true}
   ;; Surface Spread
   {:db/id                    (sm/t-key->eid conn "behaveplus:surface:input:directions_of_surface_spread__wind:surface_fire_wind__spread:surface-run-in-direction-of")
    :group-variable/hide-csv? true}])

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
  (sm/rollback-tx! conn @tx-data))
