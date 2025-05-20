(ns migrations.2025-05-20-fix-spotting
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
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

(def cms-import-tx
  (add-export-file-to-conn "./cms-exports/SIGSpot.edn" conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (sm/postwalk-insert
   [{:submodule/_groups            (sm/t-key->eid conn "behaveplus:surface:input:spot")
     :group/order                  4
     :group/name                   "Linked Inputs (Hidden)"
     :group/translation-key        "behaveplus:surface:input:spot:linked-inputs-hidden"
     :group/result-translation-key "behaveplus:surface:result:spot:linked-inputs-hidden"
     :group/children               [{:group/name            "Wind Measured at"
                                     :group/order           0
                                     :group/translation-key "behaveplus:surface:input:spot:linked-inputs-hidden:wind-measured-at"
                                     :group/conditionals    [{:conditional/type     :module,
                                                              :conditional/operator :equal,
                                                              :conditional/values   #{"mortality" "crown" "surface" "contain"}}]
                                     :group/group-variables
                                     [{:db/id                                 -1
                                       :variable/_group-variables             (sm/name->eid conn :variable/name "Wind Measured at")
                                       :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
                                       :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGSpot")
                                       :group-variable/cpp-function           (sm/cpp-fn->uuid conn "setWindHeightInputMode")
                                       :group-variable/cpp-parameter          (sm/cpp-param->uuid conn "SIGSpot" "setWindHeightInputMode" "windHeightInputMode")
                                       :group-variable/translation-key        "behaveplus:surface:input:spot:linked-inputs-hidden:wind-measured-at:wind-measured-at"
                                       :group-variable/result-translation-key "behaveplus:surface:result:spot:linked-inputs-hidden:wind-measured-at:wind-measured-at"
                                       :group-variable/help-key               "behaveplus:surface:input:spot:linked-inputs-hidden:wind-measured-at:wind-measured-at:help"}]}
                                    {:group/name            "Wind Speed"
                                     :group/order           1
                                     :group/translation-key "behaveplus:surface:input:spot:linked-inputs-hidden:wind-speed"
                                     :group/conditionals    [{:conditional/type     :module,
                                                              :conditional/operator :equal,
                                                              :conditional/values   #{"mortality" "crown" "surface" "contain"}}]
                                     :group/group-variables
                                     [{:db/id                                 -2
                                       :variable/_group-variables             (sm/name->eid conn :variable/name "Wind Speed")
                                       :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
                                       :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGSpot")
                                       :group-variable/cpp-function           (sm/cpp-fn->uuid conn "setWindSpeed")
                                       :group-variable/cpp-parameter          (sm/cpp-param->uuid conn "SIGSpot" "setWindSpeed" "windSpeed")
                                       :group-variable/translation-key        "behaveplus:surface:input:spot:linked-inputs-hidden:wind-speed:wind-speed"
                                       :group-variable/result-translation-key "behaveplus:surface:result:spot:linked-inputs-hidden:wind-speed:wind-speed"
                                       :group-variable/help-key               "behaveplus:surface:input:spot:linked-inputs-hidden:wind-speed:wind-speed:help"}]}]}

    {:group/_children              (sm/t-key->eid conn "behaveplus:crown:input:spotting:fire_behavior")
     :group/order                  2
     :group/name                   "Active Crown Fireline Intensity (Hidden)"
     :group/translation-key        "behaveplus:crown:input:spotting:fire_behavior:active-crown-fireline-intensity-hidden"
     :group/result-translation-key "behaveplus:crown:result:spotting:fire_behavior:active-crown-fireline-intensity-hidden"
     :group/group-variables        [{:db/id                                 -3
                                     :variable/_group-variables             (sm/name->eid conn :variable/name "Heading Fireline Intensity")
                                     :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
                                     :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGSpot")
                                     :group-variable/cpp-function           (sm/cpp-fn->uuid conn "setFirelineIntensity")
                                     :group-variable/cpp-parameter          (sm/cpp-param->uuid conn "SIGSpot" "setFirelineIntensity" "firelineIntensity")
                                     :group-variable/translation-key        "behaveplus:crown:input:spotting:fire_behavior:active-crown-fireline-intensity-hidden:heading-fireline-intensity"
                                     :group-variable/result-translation-key "behaveplus:crown:result:spotting:fire_behavior:active-crown-fireline-intensity-hidden:heading-fireline-intensity"
                                     :group-variable/help-key               "behaveplus:crown:input:spotting:fire_behavior:active-crown-fireline-intensity-hidden:heading-fireline-intensity:help"}]
     :group/conditionals           [{:conditional/type     :module,
                                     :conditional/operator :equal,
                                     :conditional/values   #{"mortality" "crown" "surface" "contain"}}]}

    {:link/source      (sm/t-key->eid conn "behaveplus:surface:input:wind_speed:wind_height:wind_height")
     :link/destination -1}

    {:link/source      (sm/t-key->eid conn "behaveplus:surface:input:wind_speed:wind_speed:wind_speed")
     :link/destination -2}

    {:link/source      (sm/t-key->eid conn "behaveplus:crown:output:fire_type:fire_behavior:active-crown-fireline-intensity")
     :link/destination -3}
    ]))

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
