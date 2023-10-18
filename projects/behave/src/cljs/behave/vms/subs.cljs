(ns behave.vms.subs
  (:require [re-frame.core    :refer [reg-sub subscribe]]
            [behave.vms.store :refer [pull pull-many q entity-from-uuid entity-from-eid]]))

(reg-sub
 :vms/query
 (fn [_ [_ query & variables]]
   @(apply q query variables)))

(reg-sub
 :vms/pull
 (fn [_ [_ pattern id]]
   @(pull pattern id)))

(reg-sub
 :vms/pull-many
 (fn [_ [_ pattern ids]]
   @(pull-many pattern ids)))

(reg-sub
 :vms/ids-with-attr
 (fn [_ [_ attr]]
   @(q '[:find  ?e
         :in    $ ?attr
         :where [?e ?attr]]
       attr)))

(reg-sub
 :vms/pull-with-attr
 (fn [[_ attr _]]
   (subscribe [:vms/ids-with-attr attr]))

 (fn [eids [_ _ pattern]]
   @(pull-many (or pattern '[*])
               (reduce into [] eids))))

(reg-sub
 :vms/children-ids
 (fn [_ [_ child-attr eid]]
   @(q '[:find  ?children
         :in    $ ?child-attr ?e
         :where [?e ?child-attr ?children]]
       child-attr eid)))

(reg-sub
 :vms/pull-children
 (fn [[_ child-attr id _]]
   (subscribe [:vms/children-ids child-attr id]))

 (fn [eids [_ _ _ pattern]]
   @(pull-many (or pattern '[*]) (reduce into [] eids))))

(reg-sub
 :vms/entity-from-uuid
 (fn [_ [_ uuid]]
   (entity-from-uuid uuid)))

(reg-sub
 :vms/entity-from-eid
 (fn [_ [_ eid]]
   (entity-from-eid eid)))

(defn- drill-in [entity]
  (cond
    (map? entity)
    (cond
      (:submodule/name entity)        (drill-in (:submodule/groups entity))
      (:group/group-variables entity) (drill-in (:group/group-variables entity))
      (:variable/name entity)         [(:variable/name entity) (:bp/uuid entity)])

    (and (coll? entity) (map? (first entity)) (:variable/name (first entity)))
    (map #(drill-in %) entity)

    (coll? entity)
    (mapcat #(drill-in %) entity)

    :else nil))

(reg-sub
 :vms/variable-name->uuid
 (fn [_ [_ module-name io]]
   (let [module                   @(subscribe [:wizard/*module module-name])
         submodules               @(subscribe [:wizard/submodules (:db/id module)])
         submodule-io-output-only (filter #(= (:submodule/io %) io) submodules)]
     (into {} (drill-in submodule-io-output-only)))))

(defn process-group [group]
  (cond-> (mapv :bp/uuid (:group/group-variables group))
    (seq (:group/children group))
    (into (for [child-group (sort-by :group/order (:group/children group))]
            (process-group child-group)))))

(reg-sub
 :vms/group-variable-order
 (fn [_]
   (let [module-eids @(q '[:find [?e ...]
                           :where [?e :module/name ?name]])
         modules     (mapv entity-from-eid module-eids)]

     (->> (for [module (->> modules
                            (sort-by :module/order))]
            (let [submodules        (:module/submodules module)
                  input-submodules  (->> (filter #(= (:submodule/io %) :input) submodules)
                                         (sort-by :submodule/order))
                  output-submodules (->> (filter #(= (:submodule/io %) :output) submodules)
                                         (sort-by :submodule/order))]
              (for [submodule (concat input-submodules output-submodules)]
                (for [group (->> (:submodule/groups submodule)
                                 (sort-by :group/order))]
                  (process-group group)))))
          (flatten)
          (into [])))))
