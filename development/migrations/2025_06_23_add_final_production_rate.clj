(ns migrations.2025-06-23-add-final-production-rate
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn ]]
            [cms-import :refer [add-export-file-to-conn]]
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

(add-export-file-to-conn "cms-exports/SIGContainAdapter.edn" conn)

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat
   [(sm/->group-variable
     conn
     {:parent-group-eid   (sm/t-key->eid conn "behaveplus:contain:output:fire:containment")
      :order              0
      :variable-eid       (sm/name->eid conn :variable/name "Resource Line Production Rate")
      :cpp-namespace      "global"
      :cpp-class          "SIGContainAdapter"
      :cpp-function       "getFinalProductionRate"
      :translation-key    "behaveplus:contain:output:fire:final_produciton_rate"
      :actions            [{:nname        "Set to true when Contain module is ran"
                            :ttype        :select
                            :target-value "true"
                            :conditionals [{:ttype    :module
                                            :operator :equal
                                            :values   ["contain" "surface"]}]}]
      :conditionally-set? true})]
   (sm/build-translations-payload conn {"behaveplus:contain:output:fire:final_produciton_rate" "Final Production Rate"})))

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
