(ns migrations.2025-05-02-remove-duplicate-output-variable-translation
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; Remove duplicate "behaveplus:output_variable" translation key
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
(def duplicate-translation-eid
  (d/q '[:find ?e .
         :where
         [?e :translation/key "behaveplus:output_variable"]
         [?e :translation/translation "output variable"]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload [[:db/retractEntity duplicate-translation-eid]])

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
