(ns migrations.2025-03-18-update-mortality-tree-species-tags
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [clojure.string :as str]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Update tags for list options under  SurfaceFuelModel and MortalitySpeciesMasterList. Replace "-" with "_"

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
(defn update-tags [tags]
  (for [tag   tags
        :when (str/includes? (name tag) "-")]
    (if (= tag :Rockey-Mountain)
      :Rocky_Mountain
      (keyword (str/replace (name tag) "-" "_")))))

(defn remove-existing-tags [eid tags]
  (for [tag   tags
        :when (str/includes? (name tag) "-")]
    [:db/retract eid :list-option/tags tag]))

(defn build-payload [list-name]
  (->> (sm/name->eid conn :list/name list-name)
       (d/entity (d/db conn))
       (:list/options)
       (mapcat (fn [option]
                 (let [option-entity (d/touch (d/entity (d/db conn) (:db/id option)))]
                   (concat (remove-existing-tags (:db/id option-entity) (:list-option/tags option-entity))
                           [{:db/id            (:db/id option-entity)
                             :list-option/tags (update-tags (:list-option/tags option-entity))}]))))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload (concat (build-payload "SurfaceFuelModels")
                     (build-payload "MortalitySpeciesMasterList")))

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
