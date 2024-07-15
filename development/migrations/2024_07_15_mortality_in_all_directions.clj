(ns migrations.2024-07-15-mortality-in-all-directions
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

(add-export-file-to-conn "./cms-exports/SIGMortality.edn" conn)

;; ===========================================================================================================
;; Workspace
;; ===========================================================================================================

(d/q '[:find ?e ?doc
       :in $ %
       :where
       [?e :db/doc ?doc]]
     (d/db conn)
     vms-rules)

#_{:clj-kondo/ignore [:missing-docstring]}
(defn insert-bp-uuid [x]
  (if (map? x)
    (assoc x :bp/uuid (str (squuid)))
    x))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn insert-bp-nid [x]
  (if (map? x)
    (assoc x :bp/nid (nano-id))
    x))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn postwalk-assoc-uuid+nid [data]
  (->> data
       (walk/postwalk insert-bp-uuid)
       (walk/postwalk insert-bp-nid)))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn name->eid [attr name]
  (d/q '[:find ?e .
         :in $ ?attr ?name
         :where [?e ?attr ?name]]
       (d/db conn)
       attr
       name))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn name->uuid [attr name]
  (d/q '[:find ?uuid .
         :in $ ?attr ?name
         :where
         [?e ?attr ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       attr
       name))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn name->nid [attr name]
  (d/q '[:find ?nid .
         :in $ ?attr ?name
         :where
         [?e ?attr ?name]
         [?e :bp/nid ?nid]]
       (d/db conn)
       attr
       name))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn cpp-ns->uuid [name]
  (d/q '[:find ?uuid .
         :in $ % ?name
         :where
         [?e :cpp.namespace/name ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       vms-rules
       name))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn cpp-class->uuid [name]
  (d/q '[:find ?uuid .
         :in $ % ?name
         :where
         [?e :cpp.class/name ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       vms-rules
       name))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn cpp-fn->uuid [name]
  (d/q '[:find ?uuid .
         :in $ % ?name
         :where
         [?e :cpp.function/name ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       vms-rules
       name))

#_{:clj-kondo/ignore [:missing-docstring]}
(def english-language-eid (d/q '[:find ?e .
                                 :in $
                                 :where
                                 [?e :language/name "English"]]
                               (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def add-new-entities
  [;;New new variables
   {:db/id                    -1
    :variable/name            "Probability of Mortality Backing"
    :variable/domain-uuid     (name->uuid :domain/name "Probability of Mortality & Crown Vol Scorched")
    :variable/kind            :continuous
    :variable/group-variables [-3]}

   {:db/id                    -2
    :variable/name            "Probability of Mortality Flanking"
    :variable/domain-uuid     (name->uuid :domain/name "Probability of Mortality & Crown Vol Scorched")
    :variable/kind            :continuous
    :variable/group-variables [-4]}

   ;;New Directional Probability of Mortality Group Variables
   {:db/id                             -3
    :group-variable/cpp-namespace      (cpp-ns->uuid "global")
    :group-variable/cpp-class          (cpp-class->uuid "SIGMortality")
    :group-variable/cpp-function       (cpp-fn->uuid "getProbabilityOfMortalityBacking")
    :group-variable/actions
    #{{:action/name                  "Enable when Surface Spread Direction Mode is HeadingBackingFlanking"
       :action/type                  :select
       :action/conditionals
       #{{:conditional/group-variable-uuid
          (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading_backing_flanking")
          :conditional/type     :group-variable
          :conditional/operator :equal
          :conditional/values   #{"true"}}}
       :action/conditionals-operator :and}}
    :group-variable/translation-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:probability-of-mortality-backing"
    :group-variable/conditionally-set? true
    :group-variable/help-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:probability-of-mortality-backing:help"
    :group-variable/result-translation-key
    "behaveplus:mortality:result:tree_mortality:tree_mortality:probability-of-mortality-backing"
    :group-variable/order              4}

   {:db/id                             -4
    :group-variable/cpp-namespace      (cpp-ns->uuid "global")
    :group-variable/cpp-class          (cpp-class->uuid "SIGMortality")
    :group-variable/cpp-function       (cpp-fn->uuid "getProbabilityOfMortalityFlanking")
    :group-variable/actions
    #{{:action/name                  "Enable when Surface Spread Direction Mode is HeadingBackingFlanking"
       :action/type                  :select
       :action/conditionals
       #{{:conditional/group-variable-uuid
          (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading_backing_flanking")
          :conditional/type     :group-variable
          :conditional/operator :equal
          :conditional/values   #{"true"}}}
       :action/conditionals-operator :and}}
    :group-variable/conditionally-set? true
    :group-variable/translation-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:probability-of-mortality-flanking"
    :group-variable/help-key
    "behaveplus:mortality:output:tree_mortality:tree_mortality:probability-of-mortality-flanking:help"
    :group-variable/result-translation-key
    "behaveplus:mortality:result:tree_mortality:tree_mortality:probability-of-mortality-flanking"
    :group-variable/order              5}

   ;;New Groups and Group Variables for Directional Setters for Flame Length
   {:db/id                 -5
    :group/name            "Surface Fire Flame Length Backing"
    :group/translation-key "behaveplus:mortality:input:scorch:fire:surface-fire-flame-length-backing"
    :group/help-key        "behaveplus:mortality:input:scorch:fire:surface-fire-flame-length-backing:help"
    :group/research?       true
    :group/group-variables
    [{:db/id                        -6
      :group-variable/cpp-namespace (cpp-ns->uuid "global")
      :group-variable/cpp-class     (cpp-class->uuid "SIGMortality")
      :group-variable/cpp-function  (cpp-fn->uuid "setSurfaceFireFlameLengthBacking")
      :group-variable/translation-key
      "behaveplus:mortality:input:scorch:fire:surface-fire-flame-length-backing:surface-fire-flame-length-at-vector"
      :group-variable/result-translation-key
      "behaveplus:mortality:result:scorch:fire:surface-fire-flame-length-backing:surface-fire-flame-length-at-vector"
      :group-variable/help-key
      "behaveplus:mortality:input:scorch:fire:surface-fire-flame-length-backing:surface-fire-flame-length-at-vector:help"
      :group-variable/order         0}]}

   {:db/id                 -7
    :group/name            "Surface Fire Flame Length Flanking"
    :group/translation-key "behaveplus:mortality:input:scorch:fire:surface-fire-flame-length-flanking"
    :group/help-key        "behaveplus:mortality:input:scorch:fire:surface-fire-flame-length-flanking:help"
    :group/research?       true
    :group/group-variables
    [{:db/id                        -8
      :group-variable/cpp-namespace (cpp-ns->uuid "global")
      :group-variable/cpp-class     (cpp-class->uuid "SIGMortality")
      :group-variable/cpp-function  (cpp-fn->uuid "setSurfaceFireFlameLengthFlanking")
      :group-variable/translation-key
      "behaveplus:mortality:input:scorch:fire:surface-fire-flame-length-flanking:surface-fire-flame-length-at-vector"
      :group-variable/result-translation-key
      "behaveplus:mortality:result:scorch:fire:surface-fire-flame-length-flanking:surface-fire-flame-length-at-vector"
      :group-variable/help-key
      "behaveplus:mortality:input:scorch:fire:surface-fire-flame-length-flanking:surface-fire-flame-length-at-vector:help"
      :group-variable/order         0}]}

   ;; New links
   {:db/id            -9
    :link/source      (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:backing_flame_length")
    :link/destination -6}

   {:db/id            -10
    :link/source      (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:flanking_flame_length")
    :link/destination -8}

   ;; New translations
   {:db/id                   -11
    :translation/key         "behaveplus:mortality:output:tree_mortality:tree_mortality:probability-of-mortality-backing"
    :translation/translation "Probability of Mortality Backing"}

   {:db/id                   -12
    :translation/key         "behaveplus:mortality:output:tree_mortality:tree_mortality:probability-of-mortality-flanking"
    :translation/translation "Probability of Mortality Flanking"}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def add-refs-to-existing-entities
  [{:db/id                 (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality")
    :group/group-variables [-3 -4]}
   {:db/id          (sm/t-key->eid conn "behaveplus:mortality:input:scorch:fire")
    :group/children [-5 -7]}
   {:db/id                    (name->eid :variable/name "Surface Fire Flame Length at Vector")
    :variable/group-variables [-6 -8]}
   {:db/id                english-language-eid
    :language/translation [-11 -12]}])

;; ===========================================================================================================
;; Payload and Transact
;; ===========================================================================================================


#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat (-> add-new-entities postwalk-assoc-uuid+nid)
          add-refs-to-existing-entities))

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data @(d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment (sm/rollback-tx! conn tx-data))
