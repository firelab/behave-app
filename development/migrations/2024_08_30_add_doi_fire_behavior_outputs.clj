(ns migrations.2024-08-30-add-doi-fire-behavior-outputs
  (:require [schema-migrate.interface :as sm]
            [behave.schema.rules :refer [vms-rules]]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [nano-id.core :refer [nano-id]]
            [cms-import :refer [add-export-file-to-conn]]
            [datascript.core :refer [squuid]]
            [clojure.walk :as walk]))

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(add-export-file-to-conn "./cms-exports/SIGSurface.edn" conn)

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def surface-fire-eid (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (sm/postwalk-insert
   [{:db/id                -1
     :variable/name        "Direction of Interest Flame Length"
     :variable/kind        :continuous
     :variable/domain-uuid (sm/name->uuid conn :domain/name "Flame Length & Scorch Ht")}

    {:db/id                -2
     :variable/name        "Direction of Interest Fireline Intensity"
     :variable/kind        :continuous
     :variable/domain-uuid (sm/name->uuid conn :domain/name "Fireline Intensity")}

    {:db/id                -3
     :variable/name        "Direction of Interest Rate of Spread"
     :variable/kind        :continuous
     :variable/domain-uuid (sm/name->uuid conn :domain/name "Surface Rate of Spread")}

    {:variable/_group-variables         -1
     :group/_group-variables            surface-fire-eid
     :group-variable/cpp-namespace      (sm/cpp-ns->uuid conn "global")
     :group-variable/cpp-class          (sm/cpp-class->uuid conn "SIGSurface")
     :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getFlameLengthInDirectionOfInterest")
     :group-variable/conditionally-set? true
     :group-variable/actions            [{:action/name                  "Enable when Direction Mode is Direction of Interest and Flame Length are enabled."
                                          :action/type                  :select
                                          :action/conditionals
                                          #{{:conditional/group-variable-uuid
                                             (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:direction_of_interest")
                                             :conditional/type     :group-variable
                                             :conditional/operator :equal
                                             :conditional/values   "true"}
                                            {:conditional/group-variable-uuid
                                             (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:flame_length")
                                             :conditional/type     :group-variable
                                             :conditional/operator :equal
                                             :conditional/values   "true"}}
                                          :action/conditionals-operator :and}]
     :group-variable/translation-key
     "behaveplus:surface:output:fire_behavior:surface_fire:direction_of_interest_flame_length"
     :group-variable/result-translation-key
     "behaveplus:surface:result:fire_behavior:surface_fire:direction_of_interest_flame_length"
     :group-variable/help-key
     "behaveplus:surface:output:fire_behavior:surface_fire:direction_of_interest_flame_length:help"}

    {:variable/_group-variables         -2
     :group/_group-variables            surface-fire-eid
     :group-variable/cpp-namespace      (sm/cpp-ns->uuid conn "global")
     :group-variable/cpp-class          (sm/cpp-class->uuid conn "SIGSurface")
     :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getFirelineIntensityInDirectionOfInterest")
     :group-variable/conditionally-set? true
     :group-variable/actions            [{:action/name                  "Enable when Direction Mode is Direction of Interest and Flame Length are enabled."
                                          :action/type                  :select
                                          :action/conditionals
                                          #{{:conditional/group-variable-uuid
                                             (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:direction_of_interest")
                                             :conditional/type     :group-variable
                                             :conditional/operator :equal
                                             :conditional/values   "true"}
                                            {:conditional/group-variable-uuid
                                             (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:fireline_intensity")
                                             :conditional/type     :group-variable
                                             :conditional/operator :equal
                                             :conditional/values   "true"}}
                                          :action/conditionals-operator :and}]
     :group-variable/translation-key
     "behaveplus:surface:output:fire_behavior:surface_fire:direction_of_interest_fireline_intensity"
     :group-variable/result-translation-key
     "behaveplus:surface:result:fire_behavior:surface_fire:direction_of_interest_fireline_intensity"
     :group-variable/help-key
     "behaveplus:surface:output:fire_behavior:surface_fire:direction_of_interest_fireline_intensity:help"}

    {:variable/_group-variables         -3
     :group/_group-variables            surface-fire-eid
     :group-variable/cpp-namespace      (sm/cpp-ns->uuid conn "global")
     :group-variable/cpp-class          (sm/cpp-class->uuid conn "SIGSurface")
     :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getSpreadRateInDirectionOfInterest")
     :group-variable/conditionally-set? true
     :group-variable/actions            [{:action/name                  "Enable when Direction Mode is Direction of Interest and Spread Rate are enabled."
                                          :action/type                  :select
                                          :action/conditionals
                                          #{{:conditional/group-variable-uuid
                                             (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:direction_of_interest")
                                             :conditional/type     :group-variable
                                             :conditional/operator :equal
                                             :conditional/values   "true"}
                                            {:conditional/group-variable-uuid
                                             (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:rate_of_spread")
                                             :conditional/type     :group-variable
                                             :conditional/operator :equal
                                             :conditional/values   "true"}}
                                          :action/conditionals-operator :and}]
     :group-variable/translation-key
     "behaveplus:surface:output:fire_behavior:surface_fire:direction_of_interest_spread_rate"
     :group-variable/result-translation-key
     "behaveplus:surface:result:fire_behavior:surface_fire:direction_of_interest_spread_rate"
     :group-variable/help-key
     "behaveplus:surface:output:fire_behavior:surface_fire:direction_of_interest_spread_rate:help"}]))

(def translation-payload
  (sm/build-translations-payload
   conn
   {"behaveplus:surface:result:fire_behavior:surface_fire:direction_of_interest_flame_length"       "Direction of Interest Flame Length"
    "behaveplus:surface:result:fire_behavior:surface_fire:direction_of_interest_fireline_intensity" "Direction of Interest Fireline Intensity"
    "behaveplus:surface:result:fire_behavior:surface_fire:direction_of_interest_spread_rate"        "Direction of Interest Rate of Spread"}))

;; ===========================================================================================================
;; Transact
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data @(d/transact conn (concat payload
                                         translation-payload))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment (sm/rollback-tx! conn tx-data))
