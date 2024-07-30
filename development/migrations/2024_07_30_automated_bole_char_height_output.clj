(ns migrations.2024-07-30-automated-bole-char-height-output
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [cms-import :refer [add-export-file-to-conn]]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Bole Char Height needs to be an automated output in all directions

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

(def payload
  [(sm/postwalk-insert
    {:db/id                -1,
     :variable/name        "Bole Char Height Backing",
     :variable/domain-uuid (sm/name->uuid conn :domain/name "Tree & Canopy Height"),
     :variable/kind        :continuous})
   (sm/postwalk-insert
    {:db/id                -2,
     :variable/name        "Bole Char Height Flanking",
     :variable/domain-uuid (sm/name->uuid conn :domain/name "Tree & Canopy Height"),
     :variable/kind        :continuous})
   {:db/id (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality")
    :group/group-variables
    (sm/postwalk-insert
     [{:variable/_group-variables             (sm/name->eid conn :variable/name "Bole Char Height")
       :group-variable/order                  15
       :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
       :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGMortality")
       :group-variable/cpp-function           (sm/cpp-fn->uuid conn "getBoleCharHeight")
       :group-variable/conditionally-set?     true
       :group-variable/direction              :heading
       :group-variable/actions                [{:action/name                  "Enable whenever mortality is ran"
                                                :action/type                  :select
                                                :action/conditionals-operator :or
                                                :action/conditionals          #{{:conditional/type     :module
                                                                                 :conditional/operator :equal
                                                                                 :conditional/values   #{"mortality"}}
                                                                                {:conditional/type     :module
                                                                                 :conditional/operator :equal
                                                                                 :conditional/values   #{"surface" "mortality"}}}}]
       :group-variable/translation-key        "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height"
       :group-variable/result-translation-key "behaveplus:mortality:result:tree_mortality:tree_mortality:bole_char_height"
       :group-variable/help-key               "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height:help"}

      {:variable/_group-variables             -1
       :group-variable/order                  16
       :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
       :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGMortality")
       :group-variable/cpp-function           (sm/cpp-fn->uuid conn "getBoleCharHeightBacking")
       :group-variable/conditionally-set?     true
       :group-variable/direction              :backing
       :group-variable/actions                [{:action/name                  "Enable whenever mortality is ran"
                                                :action/type                  :select
                                                :action/conditionals-operator :or
                                                :action/conditionals          #{{:conditional/type     :module
                                                                                 :conditional/operator :equal
                                                                                 :conditional/values   #{"mortality"}}
                                                                                {:conditional/type     :module
                                                                                 :conditional/operator :equal
                                                                                 :conditional/values   #{"surface" "mortality"}}}}]
       :group-variable/translation-key        "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_backing"
       :group-variable/result-translation-key "behaveplus:mortality:result:tree_mortality:tree_mortality:bole_char_height_backing"
       :group-variable/help-key               "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_backing:help"}

      {:variable/_group-variables             -2
       :group-variable/order                  17
       :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
       :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGMortality")
       :group-variable/cpp-function           (sm/cpp-fn->uuid conn "getBoleCharHeightFlanking")
       :group-variable/conditionally-set?     true
       :group-variable/direction              :flanking
       :group-variable/actions                [{:action/name                  "Enable whenever mortality is ran"
                                                :action/type                  :select
                                                :action/conditionals-operator :or
                                                :action/conditionals          #{{:conditional/type     :module
                                                                                 :conditional/operator :equal
                                                                                 :conditional/values   #{"mortality"}}
                                                                                {:conditional/type     :module
                                                                                 :conditional/operator :equal
                                                                                 :conditional/values   #{"surface" "mortality"}}}}]
       :group-variable/translation-key        "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_flanking"
       :group-variable/result-translation-key "behaveplus:mortality:result:tree_mortality:tree_mortality:bole_char_height_flanking"
       :group-variable/help-key               "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_flanking:help"}])}])

(def translations
  (sm/build-translations-payload conn 100 {
                                       "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height" "Bole Char Height"
                                       "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_backing" "Bole Char Height Backing"
                                       "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_flanking" "Bole Char Height Flanking"
                                       }))

(def final-payload (concat payload translations))

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
