(ns migrations.2025-06-25-add-moisture-scenario-export-translations
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
(def moisture-scenarios-list-options
  (d/q '[:find [(pull ?o [:db/id :list-option/name :list-option/result-translation-key]) ...]
         :in $ ?list-name
         :where
         [?l :list/name ?list-name]
         [?l :list/options ?o]]
       (d/db conn) "MoistureScenarios"))

(defn- remove-non-alphanumeric
  [s]
  (apply str (filter #(re-matches #"[a-zA-Z0-9\-:]" (str %)) s)))

(defn- export-translation-key [option]
  (-> (:list-option/result-translation-key option)
      (str/replace-first #"result" "export")
      (remove-non-alphanumeric)
      (str/replace #"-\d+$" "")))

(defn- add-export-translation-key [option]
  (assoc option :list-option/export-translation-key (export-translation-key option)))

(defn- ->export-translation [option]
  (let [option-name    (:list-option/name option)
        [_ short-code] (re-matches #"^([A-Z0-9]+) .*" option-name)]
    [(:list-option/export-translation-key option) short-code]))

#_{:clj-kondo/ignore [:missing-docstring]}
(def list-options-with-export-translation-keys
  (map add-export-translation-key moisture-scenarios-list-options))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def export-translation-keys-payload
  (map #(select-keys % [:db/id :list-option/export-translation-key]) list-options-with-export-translation-keys))

#_{:clj-kondo/ignore [:missing-docstring]}
(def create-export-translations-payload
  (sm/build-translations-payload
   conn 
   100
   (into {} (map ->export-translation list-options-with-export-translation-keys))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload (concat [] export-translation-keys-payload create-export-translations-payload))

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
