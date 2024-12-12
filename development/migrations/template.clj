(ns migrations.template
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

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

{:conditional/group-variable-uuid (:bp/uuid
                                     (sm/t-key->entity conn
                                                       "behaveplus:surface:output:fire_behavior:surface_fire:rate_of_spread"))
   :conditional/type                :group-variable
   :conditional/operator            :equal
   :conditional/values              "true"}
{:conditional/group-variable-uuid (:bp/uuid
                                     (sm/t-key->entity conn
                                                       "behaveplus:surface:output:fire_behavior:surface_fire:fireline_intensity"))
   :conditional/type                :group-variable
   :conditional/operator            :equal
   :conditional/values              "true"}
{:conditional/group-variable-uuid (:bp/uuid
                                     (sm/t-key->entity conn
                                                       "behaveplus:surface:output:spot:maximum_spotting_distance:wind_driven_surface_fire"))
   :conditional/type                :group-variable
   :conditional/operator            :equal
   :conditional/values              "true"}

(def db-hist (d/history (d/db conn)))

(def waf-conditional-eid 4611681620380881131)

(d/touch (d/entity (d/db conn) 4611681620380881131))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id                                waf-conditional-eid
    :conditional/sub-conditionals         (sm/postwalk-insert
                                           [{:conditional/group-variable-uuid (:bp/uuid
                                                                               (sm/t-key->entity conn
                                                                                                 "behaveplus:surface:output:fire_behavior:surface_fire:rate_of_spread"))
                                             :conditional/type                :group-variable
                                             :conditional/operator            :equal
                                             :conditional/values              "true"}
                                            {:conditional/group-variable-uuid (:bp/uuid
                                                                               (sm/t-key->entity conn
                                                                                                 "behaveplus:surface:output:fire_behavior:surface_fire:fireline_intensity"))
                                             :conditional/type                :group-variable
                                             :conditional/operator            :equal
                                             :conditional/values              "true"}
                                            {:conditional/group-variable-uuid (:bp/uuid
                                                                               (sm/t-key->entity conn
                                                                                                 "behaveplus:surface:output:spot:maximum_spotting_distance:wind_driven_surface_fire"))
                                             :conditional/type                :group-variable
                                             :conditional/operator            :equal
                                             :conditional/values              "true"}


                                            {:conditional/group-variable-uuid (:bp/uuid
                                                                               (sm/t-key->entity conn
                                                                                                 "behaveplus:crown:output:fire_type:fire_behavior:rate_of_spread"))
                                             :conditional/type                :group-variable
                                             :conditional/operator            :equal
                                             :conditional/values              "true"}
                                            {:conditional/group-variable-uuid (:bp/uuid
                                                                               (sm/t-key->entity conn
                                                                                                 "behaveplus:crown:output:fire_type:fire_behavior:flame_length"))
                                             :conditional/type                :group-variable
                                             :conditional/operator            :equal
                                             :conditional/values              "true"}])
    :conditional/sub-conditional-operator :or}])

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
