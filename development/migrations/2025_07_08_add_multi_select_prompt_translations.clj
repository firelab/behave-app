(ns migrations.template
  (:require [schema-migrate.interface :as sm :refer [bp]]
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
(def payload
  (sm/build-translations-payload
   conn
   {(bp "behave-components:input:multi-select:no-search:prompt1")                       "Select from the following %s (you can select multiple)"
    (bp "behave-components:input:multi-select:no-search:prompt2")                        "Your %s Selections"
    (bp "behave-components:input:multi-select:no-search:prompt3")                        "View your %s selections"
    (bp "behave-components:input:multi-select:no-search:expand-options-button-label")   "Select More"
    (bp "behave-components:input:multi-select:no-search:collapse-options-button-label") "View"
    (bp "behave-components:input:multi-select:search:prompt1")                          "Select a %s. To make additional selections, press Enter after searching."
    (bp "behave-components:input:multi-select:search:expand-options-button-label")      "Show Options"
    (bp "behave-components:input:multi-select:search:collapse-options-button-label")    "Hide Options"}))



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
