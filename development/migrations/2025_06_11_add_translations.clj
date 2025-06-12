(ns migrations.2025-06-11-add-translations
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

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (sm/build-translations-payload
   conn
   {(bp "new_worksheet")                                    "New Worksheet"
    (bp "create_a_new-worksheet_or_import_an_existing_one") "Create a new worksheet or import an existing one."
    (bp "create_a_new_worksheet")                           "Create a New Worksheet"
    (bp "open_using_guided_workflow")                       "Open using Guided Workflow"
    (bp "recommended_for_students")                         "Recommended for students."
    (bp "open_using_standard_workflow")                     "Open using Standard Workflow"
    (bp "recommended_for_intermediate_users")               "Recommended for intermediate users."
    (bp "please_select_a_workflow")                         "Please select a workflow."
    (bp "note_that_that_you_can_open_using_any_workflow")   "Note that you can open using any workflow."
    (bp "home")                                             "Home"
    (bp "workflow")                                         "Workflow"}))

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
