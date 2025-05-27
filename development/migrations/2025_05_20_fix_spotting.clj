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
  [(sm/->group
    conn
    {:parent-submodule-eid (sm/t-key->eid conn "behaveplus:surface:input:spot")
     :order                4
     :group-name           "Linked Inputs (Hidden)"
     :hidden?              true
     :translation-key      "behaveplus:surface:input:spot:linked-inputs-hidden"
     :subgroups            [{:group-name      "Wind Measured at"
                             :order           0
                             :translation-key "behaveplus:surface:input:spot:linked-inputs-hidden:wind-measured-at"
                             :group-variables [{:db/id           -1
                                                :order           0
                                                :translation-key "behaveplus:surface:input:spot:linked-inputs-hidden:wind-measured-at:wind-measured-at"
                                                :variable-eid    (sm/name->eid conn :variable/name "Wind Measured at")
                                                :cpp-namespace   "global"
                                                :cpp-class       "SIGSpot"
                                                :cpp-function    "setWindHeightInputMode"
                                                :cpp-parameter   "windHeightInputMode"}]}
                            {:group-name      "Wind Speed"
                             :order           1
                             :translation-key "behaveplus:surface:input:spot:linked-inputs-hidden:wind-speed"
                             :group-variables [{:db/id           -2
                                                :order           0
                                                :translation-key "behaveplus:surface:input:spot:linked-inputs-hidden:wind-speed:wind-speed"
                                                :variable-eid    (sm/name->eid conn :variable/name "Wind Speed")
                                                :cpp-namespace   "global"
                                                :cpp-class       "SIGSpot"
                                                :cpp-function    "setWindSpeed"
                                                :cpp-parameter   "windSpeed"}]}]})
   (sm/->group
    conn
    {:parent-group-eid (sm/t-key->eid conn "behaveplus:crown:input:spotting:fire_behavior")
     :order            2
     :group-name       "Active Crown Fireline Intensity (Hidden)"
     :translation-key  "behaveplus:crown:input:spotting:fire_behavior:active-crown-fireline-intensity-hidden"
     :hidden?          true
     :group-variables  [{:db/id           -3
                         :order           0
                         :translation-key "behaveplus:crown:input:spotting:fire_behavior:active-crown-fireline-intensity-hidden:heading-fireline-intensity"
                         :variable-eid    (sm/name->eid conn :variable/name "Heading Fireline Intensity")
                         :cpp-namespace   "global"
                         :cpp-class       "SIGSpot"
                         :cpp-function    "setFirelineIntensity"
                         :cpp-parameter   "firelineIntensity"}]})

   (sm/->link (sm/t-key->eid conn "behaveplus:surface:input:wind_speed:wind_height:wind_height") -1)
   (sm/->link (sm/t-key->eid conn "behaveplus:surface:input:wind_speed:wind_speed:wind_speed") -2)
   (sm/->link (sm/t-key->eid conn "behaveplus:crown:output:fire_type:fire_behavior:active-crown-fireline-intensity") -3)])

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
