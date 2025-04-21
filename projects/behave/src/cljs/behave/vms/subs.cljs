(ns behave.vms.subs
  (:require [behave.schema.core :refer [rules]]
            [behave.vms.store   :refer [entity-from-eid
                                        entity-from-nid
                                        entity-from-uuid
                                        pull
                                        pull-many
                                        q
                                        vms-conn]]
            [datascript.core    :as d]
            [re-frame.core      :refer [reg-sub subscribe]]))

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
 (fn [_ [_ bp-uuid]]
   (entity-from-uuid bp-uuid)))

(reg-sub
 :vms/entity-from-nid
 (fn [_ [_ bp-nid]]
   (entity-from-nid bp-nid)))

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

(defn- process-group [group]
  (cond-> (mapv :bp/uuid (sort-by :group-variable/order (:group/group-variables group)))
    (seq (:group/children group))
    (into (for [child-group (sort-by :group/order (:group/children group))]
            (process-group child-group)))))

(reg-sub
 :vms/group-variable-order
 (fn [_]
   (let [app-id              @(q '[:find ?app-id .
                                   :in $ ?app-name
                                   :where
                                   [?app-id :application/name ?app-name]]
                                 "BehavePlus")
         module-eids         @(q '[:find [?m ...]
                                   :in $ ?app-id
                                   :where
                                   [?app-id :application/modules ?m]
                                   [?m :module/name ?name]]
                                 app-id)
         modules             (mapv entity-from-eid module-eids)
         normal-order        (->> (for [module (->> modules
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
                                  (into []))
         prioritized-results (sort-by
                              :prioritized-results/order
                              (mapv #(:bp/uuid (:prioritized-results/group-variable %))
                                    (:application/prioritized-results
                                     (d/entity @@vms-conn app-id))))]
     (distinct (concat prioritized-results normal-order)))))

(reg-sub
 :vms/units-uuid->short-code
 (fn [_ [_ units-uuid]]
   (:unit/short-code (entity-from-uuid units-uuid))))

(reg-sub
 :vms/native-units
 (fn [_ [_ gv-uuid]]
   (d/q '[:find  ?unit-uuid .
          :in    $ % ?gv-uuid
          :where
          (lookup ?gv-uuid ?gv)
          (group-variable _ ?gv ?v)
          [?v :variable/kind :continuous]
          [?v :variable/native-unit-uuid ?unit-uuid]]
        @@vms-conn rules gv-uuid)))


(reg-sub
 :entity-uuid->name
 (fn [_ [_ uuid]]
   (let [entity (entity-from-uuid uuid)]
     (->> entity
          (keys)
          (filter #(= (name %) "name"))
          first
          (get entity)))))

(reg-sub
 :vms/translations
 (fn [_ [_ language-short-code]]
   (->> (d/q '[:find  ?key ?translation
               :in    $ ?short-code
               :where
               [?l :language/shortcode ?short-code]
               [?l :language/translation ?t]
               [?t :translation/key ?key]
               [?t :translation/translation ?translation]]
             @@vms-conn language-short-code)
        (into {}))))

(reg-sub
 :vms/is-group-variable-discrete-multiple?
 (fn [_ [_ gv-uuid]]
   (d/q '[:find ?discrete-multiple .
          :in $ ?gv-uuid
          :where
          [?gv :bp/uuid ?gv-uuid]
          [?gv :group-variable/discrete-multiple? ?discrete-multiple]]
        @@vms-conn gv-uuid)))

(reg-sub
 :vms/gv-uuid->list-eid
 (fn [_ [_ gv-uuid]]
   (d/q '[:find ?l .
          :in $ ?gv-uuid
          :where
          [?gv :bp/uuid ?gv-uuid]
          [?v :variable/group-variables ?gv]
          [?v :variable/list ?l]]
        @@vms-conn gv-uuid)))
