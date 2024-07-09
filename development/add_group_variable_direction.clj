(ns add-group-variable-direction
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))


;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Adds an attribute to group variables that are specified for a spread direction one of
;; #{:heading :backing :flanking}

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Build Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def gv-t-keys-to-process
  [["behaveplus:surface:output:fire_behavior:surface_fire:heading_fireline_intensity":heading]
   ["behaveplus:surface:output:fire_behavior:surface_fire:backing_fireline_intensity" :backing]
   ["behaveplus:surface:output:fire_behavior:surface_fire:flanking_fireline_intensity" :flanking]

   ["behaveplus:surface:output:fire_behavior:surface_fire:heading_flame_length":heading]
   ["behaveplus:surface:output:fire_behavior:surface_fire:backing_flame_length" :backing]
   ["behaveplus:surface:output:fire_behavior:surface_fire:flanking_flame_length" :flanking]

   ["behaveplus:surface:output:fire_behavior:surface_fire:rate_of_spread":heading]
   ["behaveplus:surface:output:fire_behavior:surface_fire:backing_rate_of_spread" :backing]
   ["behaveplus:surface:output:fire_behavior:surface_fire:flanking_rate_of_spread" :flanking]

   ["behaveplus:mortality:output:tree_mortality:tree_mortality:probability_of_mortality" :heading]
   ["behaveplus:mortality:output:tree_mortality:tree_mortality:probability-of-mortality-backing" :backing]
   ["behaveplus:mortality:output:tree_mortality:tree_mortality:probability-of-mortality-flanking" :flanking]

   ["behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height" :heading]
   ["behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height-backing" :backing]
   ["behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height-flanking" :flanking]

   ["behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched" :heading]
   ["behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_backing" :backing]
   ["behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_flanking" :flanking]

   ["behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched" :heading]
   ["behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_backing" :backing]
   ["behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_flanking" :flanking]])

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (map (fn [[t-key direction]]
         {:db/id                    (sm/t-key->eid conn t-key)
          :group-variable/direction direction})
       gv-t-keys-to-process))

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
