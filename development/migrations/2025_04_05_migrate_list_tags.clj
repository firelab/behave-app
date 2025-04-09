(ns migrations.2025-04-05-migrate-list-tags
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; Converts all existing :list/tags / :color-tags to :tag-sets
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

#_{:clj-kondo/ignore [:missing-docstring]}
(def db (d/db conn))



#_{:clj-kondo/ignore [:missing-docstring]}
(def lists-w-tags
  (d/q '[:find [?l ...]
         :where
         [?lo :list-option/tags ?t]
         [?l :list/options ?lo]] db))

#_{:clj-kondo/ignore [:missing-docstring]}
(def lists-w-color-tags
  (d/q '[:find [?l ...]
         :where
         [?lo :list-option/color-tag ?c]
         [?l :list/options ?lo]] db))

(defn- gen-tag-set [tag-set-name tag-color-list]
  {:tag-set/name tag-set-name
   :tag-set/tags
   (map (fn [[tag-name order]]
          {:tag/name tag-name :tag/order order}) tag-color-list)})

(defn- gen-color-tag-set [tag-set-name tag-color-list]
  {:tag-set/name tag-set-name
   :tag-set/tags
   (map (fn [[tag-name order color]]
          {:tag/name tag-name :tag/order order :tag/color color}) tag-color-list)})

(gen-tag-set "Fuel Models" [["Shrub" 0]])
(gen-color-tag-set "Fuel Models" [["Standard" 0 "#FFE93C"]])

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload [])

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
