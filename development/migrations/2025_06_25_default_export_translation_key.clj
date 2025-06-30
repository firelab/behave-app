(ns migrations.2025-06-25-default-export-translation-key
  (:require [clojure.string :as str]
            [schema-migrate.interface :as sm]
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

#_{:clj-kondo/ignore [:missing-docstring]}
(def list-options
  (d/q '[:find [(pull ?o [:db/id :list-option/result-translation-key]) ...]
         :in $ ?moisture-list-name
         :where
         [?l :list/name ?list-name]
         [(not= ?list-name ?moisture-list-name)]
         [?l :list/options ?o]
         [?o :list-option/result-translation-key ?k]]
       (d/db conn) "MoistureScenarios"))

(defn- remove-non-alphanumeric
  [s]
  (apply str (filter #(re-matches #"[a-zA-Z0-9\-:]" (str %)) s)))

(defn- export-translation-key [option]
  (-> (:list-option/result-translation-key option)
      (str/replace-first #"result" "export")
      (remove-non-alphanumeric)))

(defn- add-export-translation-key [option]
  (assoc option :list-option/export-translation-key (export-translation-key option)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def list-options-with-export-translation-keys
  (map add-export-translation-key list-options))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload 
  (map #(select-keys % [:db/id :list-option/export-translation-key]) list-options-with-export-translation-keys))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e  (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
