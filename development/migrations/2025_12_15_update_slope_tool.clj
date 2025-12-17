(ns migrations.2025-12-15-update-slope-tool
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

(def existing-slope-steepness-deg-variable-ied
  (sm/name->eid conn :variable/name "Slope Steepness Deg"))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat
   [;; Add new slope variable
    (sm/->variable conn {:db/id            -1
                         :nname            "Slope Steepness Percent"
                         :dimension-uuid   (:bp/uuid (d/entity (d/db conn) (sm/name->eid conn :dimension/name "Slope")))
                         :native-unit-uuid (:bp/uuid (d/entity (d/db conn) (sm/name->eid conn :unit/name "Percent (%)")))
                         :kind             :continuous})


    ;; update existing slope variable
    [:db/retract existing-slope-steepness-deg-variable-ied :variable/domain-uuid]
    {:db/id                     existing-slope-steepness-deg-variable-ied
     :variable/dimension-uuid   (:bp/uuid (d/entity (d/db conn) (sm/name->eid conn :dimension/name "Slope")))
     :variable/native-unit-uuid (:bp/uuid (d/entity (d/db conn) (sm/name->eid conn :unit/name "Degrees (deg)")))}

    ;; delete existing slope tool variable
    [:db/retractEntity (sm/t-key->eid conn "behaveplus:slope-tool:slope-from-map-measurements:slope-steepness-")]

    ;; add new slope tool variables
    {:db/id             (sm/name->eid conn :subtool/name "Slope from Map Measurements")
     :subtool/variables [(sm/postwalk-insert
                          {:subtool-variable/io                 :output
                           :variable/_subtool-variables         -1
                           :subtool-variable/order              4
                           :subtool-variable/cpp-namespace-uuid (sm/cpp-ns->uuid conn "global")
                           :subtool-variable/cpp-class-uuid     (sm/cpp-class->uuid conn "global" "SIGSlopeTool")
                           :subtool-variable/cpp-function-uuid  (sm/cpp-fn->uuid conn "global" "SIGSlopeTool" "getSlopeFromMapMeasurementsInPercent")
                           :subtool-variable/translation-key    "behaveplus:slope-tool:slope-from-map-measurements:slope-steepness-percent"
                           :subtool-variable/help-key           "behaveplus:slope-tool:slope-from-map-measurements:slope-steepness-percent:help"})

                         (sm/postwalk-insert
                          {:subtool-variable/io                 :output
                           :variable/_subtool-variables         existing-slope-steepness-deg-variable-ied
                           :subtool-variable/order              5
                           :subtool-variable/cpp-namespace-uuid (sm/cpp-ns->uuid conn "global")
                           :subtool-variable/cpp-class-uuid     (sm/cpp-class->uuid conn "global" "SIGSlopeTool")
                           :subtool-variable/cpp-function-uuid  (sm/cpp-fn->uuid conn "global" "SIGSlopeTool" "getSlopeFromMapMeasurementsInDegrees")
                           :subtool-variable/translation-key    "behaveplus:slope-tool:slope-from-map-measurements:slope-steepness-deg"
                           :subtool-variable/help-key           "behaveplus:slope-tool:slope-from-map-measurements:slope-steepness-deg:help"})]}

    ;;reorder existing subtool variables
    {:db/id                  (sm/t-key->eid conn "behaveplus:slope-tool:slope-from-map-measurements:slope-elevation-change")
     :subtool-variable/order 6}
    {:db/id                  (sm/t-key->eid conn "behaveplus:slope-tool:slope-from-map-measurements:slope-horizontal-distance")
     :subtool-variable/order 7}]

   ;; add translations
   (sm/build-translations-payload conn {"behaveplus:slope-tool:slope-from-map-measurements:slope-steepness-percent" "Slope"
                                        "behaveplus:slope-tool:slope-from-map-measurements:slope-steepness-deg"     "Slope"})))

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
