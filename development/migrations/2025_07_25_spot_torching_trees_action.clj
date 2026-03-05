(ns migrations.2025-07-25-spot-torching-trees-action
  (:require
   [schema-migrate.interface :as sm]
   [datomic.api              :as d]
   [behave-cms.store         :refer [default-conn]]
   [behave-cms.server        :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================
;;
;; Automatically selects "Torching" Fire Type when no other inputs are selected.
;;
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Build Payload
;; ===========================================================================================================

;; Create conditional
(defn ->equal-cond [t-key value]
  (sm/->entity
   {:conditional/group-variable-uuid (sm/t-key->uuid conn t-key)
    :conditional/type                :group-variable
    :conditional/operator            :equal
    :conditional/values              value}))

;; Variables
(def active-crown "behaveplus:crown:output:spotting_active_crown_fire:maximum_spotting_distance:maximum_spotting_distance")
(def torching-trees "behaveplus:crown:output:spotting_active_crown_fire:maximum_spotting_distance:max-spot-dist-from-torching-trees")

(def crown (sm/t-key->entity conn "behaveplus:crown"))

(def crown-output-submodule-variables
  (->> (:module/submodules crown)
       (filter #(= :output (:submodule/io %)))
       (mapcat :submodule/groups)
       (mapcat :group/group-variables)
       (map :group-variable/translation-key)
       (set)))

(def payload
  {:db/id (sm/t-key->eid conn "behaveplus:crown:input:spotting:linked_inputs:fire_type")
   :group-variable/conditionally-set? true
   :group-variable/actions
   [;; Torching
    (sm/->entity {:action/name                  "Select 'Torching' Fire Type when 'Form Torching Trees' is selected and no other Crown outputs are selected."
                  :action/type                  :select
                  :action/target-value          "1"
                  :action/conditionals-operator :and
                  :action/conditionals
                  (concat [(->equal-cond torching-trees "true")]
                          (map #(->equal-cond % "false") (disj crown-output-submodule-variables torching-trees)))})

    ;; Crowning
    (sm/->entity {:action/name                  "Select 'Crowning' Fire Type when 'From Active Crown Fire' is selected and no other Crown outputs are selected."
                  :action/type                  :select
                  :action/target-value          "3"
                  :action/conditionals-operator :and
                  :action/conditionals
                  (concat [(->equal-cond active-crown "true")]
                          (map #(->equal-cond % "false") (disj crown-output-submodule-variables active-crown)))})]})

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
