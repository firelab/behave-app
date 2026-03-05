(ns migrations.2025-10-01-update-ssd-colors
  (:require [datomic.api :as d]
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

(defn- find-tag-eid
  [conn tag-set-name tag-name]
  (d/q '[:find ?t .
         :in $ ?set-name ?tag-name
         :where
         [?e :tag-set/name ?set-name]
         [?e :tag-set/tags ?t]
         [?t :tag/name ?tag-name]]
       (d/db conn) tag-set-name tag-name))

#_{:clj-kondo/ignore [:missing-docstring]}
(def moderate-tag-eid (find-tag-eid conn "Safety Conditions" "Moderate"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def extreme-tag-eid (find-tag-eid conn "Safety Conditions" "Extreme"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id     moderate-tag-eid
    :tag/color "#FFDA0D"}
   {:db/id     extreme-tag-eid
    :tag/color "#CB5757"}])

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

