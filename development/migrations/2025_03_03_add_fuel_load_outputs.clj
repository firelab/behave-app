(ns migrations.2025-03-03-add-fuel-load-outputs
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [cms-import :refer [add-export-file-to-conn]]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(def cms-import-tx
  (add-export-file-to-conn "./cms-exports/SIGSurface.edn" conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id (sm/t-key->eid conn "behaveplus:surface:output:wind-and-fuel")
    :submodule/groups
    (sm/postwalk-insert
     [{:group/name                   "Fuel"
       :group/group-variables        [{:group-variable/order                  0
                                       :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
                                       :variable/_group-variables             (sm/name->eid conn :variable/name "Total Live Fuel Load")
                                       :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGSurface")
                                       :group-variable/cpp-function           (sm/cpp-fn->uuid conn "getTotalLiveFuelLoad")
                                       :group-variable/translation-key        "behaveplus:surface:output:wind-and-fuel:fuel:total-live-fuel-load"
                                       :group-variable/result-translation-key "behaveplus:surface:result:wind-and-fuel:fuel:total-live-fuel-load"
                                       :group-variable/help-key               "behaveplus:surface:output:wind-and-fuel:fuel:total-live-fuel-load:help"}
                                      {:group-variable/order                  1
                                       :variable/_group-variables             (sm/name->eid conn :variable/name "Total Dead Fuel Load")
                                       :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
                                       :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGSurface")
                                       :group-variable/cpp-function           (sm/cpp-fn->uuid conn "getTotalDeadFuelLoad")
                                       :group-variable/translation-key        "behaveplus:surface:output:wind-and-fuel:fuel:total-dead-fuel-load"
                                       :group-variable/result-translation-key "behaveplus:surface:result:wind-and-fuel:fuel:total-dead-fuel-load"
                                       :group-variable/help-key               "behaveplus:surface:output:wind-and-fuel:fuel:total-dead-fuel-load:help"}
                                      {:group-variable/order                  2
                                       :variable/_group-variables             (sm/name->eid conn :variable/name "Dead Herbaceous Fuel Load")
                                       :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
                                       :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGSurface")
                                       :group-variable/cpp-function           (sm/cpp-fn->uuid conn "getTotalDeadHerbaceousFuelLoad")
                                       :group-variable/translation-key        "behaveplus:surface:output:wind-and-fuel:fuel:total-dead-herbaceous-fuel-load"
                                       :group-variable/result-translation-key "behaveplus:surface:result:wind-and-fuel:fuel:total-dead-herbaceous-fuel-load"
                                       :group-variable/help-key               "behaveplus:surface:output:wind-and-fuel:fuel:total-dead-herbaceous-fuel-load:help"}]
       :group/order                  1
       :group/translation-key        "behaveplus:surface:output:wind-and-fuel:fuel"
       :group/help-key               "behaveplus:surface:output:wind-and-fuel:fuel:help"
       :group/result-translation-key "behaveplus:surface:result:wind-and-fuel:fuel"}])}]

  )

(def add-new-translations-payload
  (sm/build-translations-payload conn 100
                                 {"behaveplus:surface:output:wind-and-fuel:fuel"                                 "Fuel"
                                  "behaveplus:surface:output:wind-and-fuel:fuel:total-live-fuel-load"            "Total Live Fuel Load"
                                  "behaveplus:surface:output:wind-and-fuel:fuel:total-dead-fuel-load"            "Total Dead Fuel Load"
                                  "behaveplus:surface:output:wind-and-fuel:fuel:total-dead-herbaceous-fuel-load" "Total Dead Herbaceous Fuel Load"}))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn (concat payload add-new-translations-payload))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (do
    (sm/rollback-tx! conn @cms-import-tx)
    (sm/rollback-tx! conn @tx-data)))
