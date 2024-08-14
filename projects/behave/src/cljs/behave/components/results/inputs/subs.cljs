(ns behave.components.results.inputs.subs
  (:require [behave.schema.core  :refer [rules]]
            [behave.translate    :refer [<t]]
            [behave.vms.store    :refer [vms-conn]]
            [behave.wizard.subs  :refer [all-conditionals-pass?]]
            [clojure.walk        :refer [prewalk]]
            [datascript.core     :as d]
            [datascript.impl.entity]
            [map-utils.interface :refer [index-by]]
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
  (pos?
   (d/q '[:find  (count ?gv) .
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

(defn- create-formatter [variable]
  (let [v-kind (:variable/kind variable)]
    (if (or (= v-kind :discrete))
      (let [{llist :variable/list}  (d/pull @@vms-conn
                                            '[{:variable/list [* {:list/options [*]}]}]
                                            (:db/id variable))
            {options :list/options} llist
            options                 (index-by :list-option/value options)]
        (fn discrete-fmt [value]
          (if-let [option (get options value)]
            @(<t (:list-option/translation-key option))
            value)))
      identity)))

(reg-sub
 :result.inputs/table-formatters
 (fn [_ [_ gv-uuids]]
   (let [results (d/q '[:find ?gv-uuid (pull ?v [*])
                        :in $ % [?gv-uuid ...]
                        :where
                        (lookup ?gv-uuid ?gv)
                        (group-variable _ ?gv ?v)]
                       @@vms-conn rules gv-uuids)]
     (into {} (map
               (fn [[gv-uuid variable]]
                 [gv-uuid (create-formatter variable)])
               results)))))
