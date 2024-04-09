(ns behave.components.results.inputs.subs
  (:require [behave.vms.store    :refer [vms-conn]]
            [behave.wizard.subs  :refer [all-conditionals-pass?]]
            [clojure.walk        :refer [prewalk]]
            [datascript.core     :as d]
            [datascript.impl.entity]
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
       (remove (fn [{conditional-op :group/conditional-operator
                     conditionals   :group/conditionals
                     research?      :group/research?}]
                 (or (not (all-conditionals-pass? worksheet
                                                  conditional-op
                                                  conditionals))
                     research?)))))
   form))

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
        (filter #(= (:submodule/io %) :input))
        (filter #(not (:submodule/research? %)))
        (filter (fn [{conditionals-op :submodule/conditionals-operator
                      conditionals    :submodule/conditionals}]
                  (all-conditionals-pass? worksheet
                                          conditionals-op
                                          conditionals)))
        (filter-group-variables worksheet)
        (sort-by :submodule/order))))
