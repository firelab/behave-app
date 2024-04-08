(ns behave.components.results.inputs.subs
  (:require [clojure.walk :refer [prewalk]]
            [re-frame.core :refer [reg-sub subscribe] :as rf]
            [datascript.core        :as d]
            [behave.vms.store       :refer [vms-conn]]
            [behave.wizard.subs :refer [all-conditionals-pass?]]
            [datascript.impl.entity]))

(defn- export-entity
  [entity]
  (prewalk
   (fn [x]
     (if (instance? datascript.impl.entity/Entity x)
       (into {} x)
       x))
   entity))

(defn- is-collection-of-groups [x]
  (and (set? x) (contains? (first x) :group/name)))

(reg-sub
 :result.inputs/submodules
 (fn [[_ ws-uuid]]
   (subscribe [:worksheet ws-uuid]))

 (fn [worksheet [_ _ws-uuid module-id]]
   (->> module-id
        (d/entity @@vms-conn)
        (:module/submodules)
        (mapv d/touch)
        (export-entity) ;; ensures prewalk works properly
        (filterv #(= (:submodule/io %) :input))
        (filterv #(not (:submodule/research? %)))
        (filterv (fn [{conditionals-op :submodule/conditionals-operator
                       conditionals    :submodule/conditionals}]
                   (all-conditionals-pass? worksheet
                                           conditionals-op
                                           conditionals)))
        (prewalk (fn [node]
                   (if (is-collection-of-groups node)
                     (remove (fn [group]
                               (or (not (all-conditionals-pass? worksheet
                                                                (:group/conditional-operator group)
                                                                (:group/conditionals group)))
                                   (:group/research? group)))
                             node)
                     node)))
        (sort-by :submodule/order))))
