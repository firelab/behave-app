(ns migrations.2025-10-02-spot-enable-flame-length-only-active-crown
  (:require [schema-migrate.interface :refer [bp] :as sm]
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

#_{:clj-kondo/ignore [:missing-docstring]}
(def spot-fire-behavior-input (sm/t-key->entity (d/db conn) "behaveplus:crown:input:spotting:fire_behavior"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def active-crowning-max-spotting-distance-gv-uuid (sm/t-key->uuid (d/db conn) "behaveplus:crown:output:spotting_active_crown_fire:maximum_spotting_distance:maximum_spotting_distance"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-conditionals-payload
  [{:db/id              (:db/id spot-fire-behavior-input)
    :group/conditionals [(sm/->conditional conn
                                           {:ttype               :group-variable
                                            :operator            :equal
                                            :values              "true"
                                            :group-variable-uuid active-crowning-max-spotting-distance-gv-uuid})]}])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data @(d/transact conn new-conditionals-payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
