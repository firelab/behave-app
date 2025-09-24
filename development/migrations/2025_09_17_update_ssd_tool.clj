(ns migrations.2025-09-17-update-ssd-tool
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
(def ssd-tool
  (d/entity (d/db conn) 
            (d/q '[:find ?e .
                   :in $ ?name
                   :where [?e :tool/name ?name]]
                 (d/db conn) "Safe Separation Distance")))

#_{:clj-kondo/ignore [:missing-docstring]}
(def ssd-subtool (first (:tool/subtools ssd-tool)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def ssd-name "Safe Separation Distance & Safety Zone Size")

#_{:clj-kondo/ignore [:missing-docstring]}
(def ssd-translations-payload
  (sm/update-translations-payload
   conn
   "en-US"
   {(:tool/translation-key ssd-tool)       ssd-name
    (:subtool/translation-key ssd-subtool) ssd-name}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def name-payload
  [{:db/id     (:db/id ssd-tool)
    :tool/name ssd-name}
   {:db/id        (:db/id ssd-subtool)
    :subtool/name ssd-name}])

;; ===========================================================================================================
;; Update Slope List Options
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def slope-translations-payload
  (sm/update-translations-payload
   conn
   "en-US"
   {"behaveplus:list-option:slope-class:steep"    "Steep (>45%)"
    "behaveplus:list-option:slope-class:low"      "Flat (<25%)"
    "behaveplus:list-option:slope-class:moderate" "Moderate (25% - 45%)"}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload (concat name-payload ssd-translations-payload slope-translations-payload))

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

