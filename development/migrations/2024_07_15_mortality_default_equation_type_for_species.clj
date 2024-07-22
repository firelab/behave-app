(ns migrations.2024-07-15-mortality-default-equation-type-for-species
  (:require [schema-migrate.interface :as sm]
            [behave.schema.rules :refer [vms-rules]]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [nano-id.core :refer [nano-id]]
            [datascript.core :refer [squuid]]
            [clojure.walk :as walk]))

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))


;; ===========================================================================================================
;; Workspace
;; ===========================================================================================================

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
(defn name->eid [attr name]
  (d/q '[:find ?e .
         :in $ ?attr ?name
         :where [?e ?attr ?name]]
       (d/db conn)
       attr
       name))

;; ===========================================================================================================
;; Payload and Transact
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def english-language-eid (d/q '[:find ?e .
                                 :in $
                                 :where
                                 [?e :language/name "English"]]
                               (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id                 (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality")
    :group/group-variables (map postwalk-assoc-uuid+nid
                                [{:db/id                                 -1
                                  :group-variable/order                  4
                                  :group-variable/cpp-namespace          (cpp-ns->uuid "global")
                                  :group-variable/cpp-class              (cpp-class->uuid "SIGMortality")
                                  :group-variable/cpp-function           (cpp-fn->uuid "getEquationType")
                                  :group-variable/translation-key        "behaveplus:mortality:output:tree_mortality:tree_mortality:mortality-equation"
                                  :group-variable/result-translation-key "behaveplus:mortality:result:tree_mortality:tree_mortality:mortality-equation"
                                  :group-variable/help-key               "behaveplus:mortality:output:tree_mortality:tree_mortality:mortality-equation:help"
                                  :group-variable/conditionally-set?     true
                                  :group-variable/actions                [{:action/name                  "Always enable when Mortality is ran"
                                                                           :action/type                  :select
                                                                           :action/conditionals          [{:conditional/type     :module
                                                                                                           :conditional/operator :in
                                                                                                           :conditional/values   #{"mortality"}}
                                                                                                          {:conditional/type     :module
                                                                                                           :conditional/operator :in
                                                                                                           :conditional/values   #{"mortality" "surface"}}]
                                                                           :action/conditionals-operator :or}]
                                  }])}

   {:db/id                    (name->eid :variable/name "Mortality Equation")
    :variable/group-variables [-1]}

   {:db/id                   -2
    :translation/key         "behaveplus:mortality:output:tree_mortality:tree_mortality:mortality-equation"
    :translation/translation "Equation Type"}

   {:db/id                english-language-eid
    :language/translation [-2]}])


(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment (sm/rollback-tx! conn @tx-data))
