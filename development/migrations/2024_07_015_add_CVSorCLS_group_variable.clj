(ns migrations.2024-07-015-add-CVSorCLS-group-variable
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [cms-import :refer [add-export-file-to-conn]]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;;  Add a new group variable under Mortality > Mortality (output) > Tree Mortality

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(add-export-file-to-conn "./cms-exports/SIGMortality.edn" conn)

;; ===========================================================================================================
;; Build Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-entities
  [{:variable/name            "CVSorCLS"
    :variable/kind            :text
    :variable/group-variables [-1]}

   {:db/id                                 -1
    :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
    :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGMortality")
    :group-variable/cpp-function           (sm/cpp-fn->uuid conn "getTreeCrownVolumeScorchedBacking")
    :group-variable/conditionally-set?     true
    :group-variable/actions                [{:action/name                  "Enable whenever mortality is ran"
                                             :action/type                  :select
                                             :action/conditionals-operator :or
                                             :action/conditionals          #{{:conditional/type     :module
                                                                              :conditional/operator :equal
                                                                              :conditional/values   #{"mortality"}}
                                                                             {:conditional/type     :module
                                                                              :conditional/operator :equal
                                                                              :conditional/values   #{"surface" "mortality"}}}}]
    :group-variable/translation-key        "behaveplus:mortality:output:tree_mortality:tree_mortality:cvsorcls"
    :group-variable/result-translation-key "behaveplus:mortality:result:tree_mortality:tree_mortality:cvsorcls"
    :group-variable/help-key               "behaveplus:mortality:output:tree_mortality:tree_mortality:cvsorcls:help"}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-refs
  [{:db/id                 (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality")
   :group/group-variables [-1]}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-translations
  (sm/build-translations-payload
   conn
   100
   {"behaveplus:mortality:output:tree_mortality:tree_mortality:cvsorcls" "CVSorCLS"}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def final-payload (concat (sm/postwalk-insert new-entities)
                           new-refs
                           new-translations))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn final-payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
