(ns migrations.2026-03-24-add-conditionals-to-default-heading-if-missing
  (:require [behave-cms.server        :as cms]
            [behave-cms.store         :refer [default-conn]]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Adds conditionals for when a user forgets to select a direction mode

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(defn t-key->conditional [conn t-key]
  {:ttype               :group-variable
   :operator            :equal
   :values              #{"true"}
   :group-variable-uuid (sm/t-key->bp-uuid conn t-key)})

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id                  (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading")
    :group-variable/actions [(sm/->action
                              conn
                              {:nname                 "Enable if no direction mode is selected and at least one of these outputs are selected"
                               :ttype                 :select
                               :conditionals-operator :and
                               :conditionals          [{:ttype               :group-variable
                                                        :operator            :equal
                                                        :values              #{"false"}
                                                        :group-variable-uuid (sm/t-key->bp-uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:direction_of_interest")}

                                                       {:ttype               :group-variable
                                                        :operator            :equal
                                                        :values              #{"false"}
                                                        :group-variable-uuid (sm/t-key->bp-uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading_backing_flanking")}

                                                       {:ttype                    :group-variable
                                                        :operator                 :equal
                                                        :values                   #{"false"}
                                                        :group-variable-uuid      (sm/t-key->bp-uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading")
                                                        :sub-conditional-operator :or
                                                        :sub-conditionals         [(t-key->conditional conn "behaveplus:surface:output:fire_behavior:surface_fire:rate_of_spread")
                                                                                   (t-key->conditional conn "behaveplus:surface:output:fire_behavior:surface_fire:fireline_intensity")
                                                                                   (t-key->conditional conn "behaveplus:surface:output:fire_behavior:surface_fire:flame_length")
                                                                                   (t-key->conditional conn "behaveplus:surface:output:size:surface___fire_size:fire_perimeter")
                                                                                   (t-key->conditional conn "behaveplus:surface:output:size:surface___fire_size:fire_area")
                                                                                   (t-key->conditional conn "behaveplus:surface:output:size:surface___fire_size:length-to-width-ratio")
                                                                                   (t-key->conditional conn "behaveplus:surface:output:size:surface___fire_size:spread-distance")]}]})]}])

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
