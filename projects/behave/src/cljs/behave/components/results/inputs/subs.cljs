(ns behave.components.results.inputs.subs
  (:require [behave.vms.store    :refer [vms-conn]]
            [behave.wizard.subs  :refer [all-conditionals-pass?]]
            [clojure.walk        :refer [prewalk]]
            [datascript.core     :as d]
            [datascript.impl.entity]
            [behave.schema.core     :refer [rules]]
            [re-frame.core       :refer [reg-sub subscribe]]))

(defn- export-entity
  [entity]
  (prewalk
   (fn [x]
     (if (instance? datascript.impl.entity/Entity x)
       (into {} x)
       x))
   entity))

(defn- is-collection-of-groups
  [x]
  (and (coll? x) (contains? (first x) :group/name)))

(defn- filter-group-variables
  [worksheet form]
  (prewalk
   (fn [node]
     (cond->> node
       (is-collection-of-groups node)
       (filter (fn [{conditional-op :group/conditional-operator
                     conditionals   :group/conditionals
                     research?      :group/research?}]
                 (and (not research?)
                      (all-conditionals-pass? worksheet
                                              conditional-op
                                              conditionals))))))
   form))

(defn- has-conditionally-set-group-variables? [s-uuid]
  (seq
   (d/q '[:find  [?gv ...]
          :in    $ % ?s-uuid
          :where
          [?s :bp/uuid ?s-uuid]
          (group ?s ?g)
          (group-variable ?g ?gv ?v)
          [?gv :group-variable/conditionally-set? true]]
        @@vms-conn rules s-uuid)))

(reg-sub
 :result.inputs/submodules
 (fn [[_ ws-uuid]]
   (subscribe [:worksheet ws-uuid]))

 (fn [worksheet [_ _ws-uuid module-id]]
   (->> module-id
        (d/entity @@vms-conn)
        (:module/submodules)
        (map d/touch)
        (export-entity) ;; ensures prewalk works properly
        (filter (fn [{io              :submodule/io
                      research?       :submodule/research?
                      conditionals-op :submodule/conditionals-operator
                      conditionals    :submodule/conditionals
                      s-uuid          :bp/uuid}]
                  (and (= io :input)
                       (not research?)
                       (or (all-conditionals-pass? worksheet
                                                   conditionals-op
                                                   conditionals)
                           (has-conditionally-set-group-variables? s-uuid)))))
        (filter-group-variables worksheet)
        (sort-by :submodule/order))))
