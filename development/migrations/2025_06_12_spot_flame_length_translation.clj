(ns migrations.2025-06-12-spot-flame-length-translation
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (sm/update-translations-payload
   conn
   "en-US"
   {"behaveplus:crown:input:spotting:fire_behavior:active_crown_flame_length"                           "Flame Length"
    "behaveplus:crown:input:spotting:fire_behavior:active_crown_flame_length:active_crown_flame_length" "Flame Length"}))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; Rollback
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
