(ns behave.vms.subs
  (:require [behave.schema.core :refer [rules]]
            [behave.vms.store   :refer [entity-from-eid
                                        entity-from-nid
                                        entity-from-uuid
                                        pull
                                        pull-many
                                        q
                                        vms-conn]]
            [behave.translate            :refer [<t]]
            [map-utils.interface         :refer [index-by]]
            [datascript.core    :as d]
            [re-frame.core      :refer [reg-sub subscribe]]
            [re-frame.core :as rf]))

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


(reg-sub
 :vms/group-variable-eid->variable-name
 (fn [_ [_ group-variable-eid]]
   (d/q '[:find ?v-name .
          :in $ ?gv
          :where
          [?v :variable/group-variables ?gv]
          [?v :variable/name ?v-name]]
         @@vms-conn group-variable-eid)))

(reg-sub
 :vms/group-variable-is-output?
 (fn [_ [_ group-variable-id]]
   (d/q '[:find ?is-output .
          :in $ % ?gv
          :where
          [?g :group/group-variables ?gv]
          (submodule-root ?sm ?g)
          [?sm :submodule/io ?io]
          [(= ?io :output) ?is-output]]
         @@vms-conn rules group-variable-id)))


(reg-sub
 :vms/directional-group-variable-uuids
 (fn [_]
   (d/q '[:find  [?gv-uuid ...]
          :in $
          :where
          [?gv :bp/uuid ?gv-uuid]
          [?gv :group-variable/direction ?direction]]
         @@vms-conn)))

(reg-sub
 :vms/group-variable-is-directional?
 (fn [_ [_ gv-uuid direction]]
   (= (d/q '[:find  ?direction .
             :in $ ?gv-uuid
             :where
             [?gv :bp/uuid ?gv-uuid]
             [?gv :group-variable/direction ?direction]]
            @@vms-conn
            gv-uuid)
      direction)))

(reg-sub
 :vms/resolve-enum-translation
 (fn [_ [_ gv-uuid value]]
   (let [result                  (d/pull @@vms-conn '[{:variable/_group-variables
                                                       [{:variable/list [* {:list/options [*]}]}]}]
                                         [:bp/uuid gv-uuid])
         variable                (first (:variable/_group-variables result))
         {v-list :variable/list} variable
         {options :list/options} v-list
         options                 (index-by :list-option/value options)]
     (if-let [option (get options value)]
       (or @(<t (:list-option/result-translation-key option))
           @(<t (:list-option/translation-key option)))
       value))))


(defn- get-group-variable-hierarchy
  "Returns a sequence of datomic entities from submodule down to group.

  Uses datomic rules from behave.schema.rules for cleaner queries:
  - lookup: Find entity by UUID
  - group-variable: Link group-variable to its parent group
  - submodule-root: Recursively find the submodule for any (sub)group
  - subgroup: Recursively find all ancestor groups

  Returns: [{:db/id submodule} {:db/id parent-1} ... {:db/id immediate-group}]

  Arguments:
    db - Datomic database value
    gv-uuid - UUID of the group-variable

  Returns:
    Sequence of entities (just :db/id) from submodule to immediate group,
    or nil if group-variable not found"
  [db gv-uuid]
  ;; Use rules to find the immediate group and submodule
  (when-let [[submodule-eid immediate-group-eid]
             (d/q '[:find [?submodule ?group]
                    :in $ % ?gv-uuid
                    :where
                    (lookup ?gv-uuid ?gv)
                    (group-variable ?group ?gv ?v)
                    (submodule-root ?submodule ?group)]
                  db
                  rules
                  gv-uuid)]

    ;; Use the subgroup rule to find all ancestor groups
    ;; The subgroup rule: (subgroup ?parent ?child) means ?child is a subgroup of ?parent
    (let [ancestor-eids (d/q '[:find [?ancestor ...]
                               :in $ % ?child
                               :where
                               (subgroup ?ancestor ?child)]
                             db
                             rules
                             immediate-group-eid)

          ;; Pull all groups with their parent references to sort them
          all-groups          (cons immediate-group-eid ancestor-eids)
          groups-with-parents (map #(d/pull db '[:db/id {:group/_children [:db/id]}] %)
                                   all-groups)

          ;; Build a map of child -> parent for quick lookup
          parent-map (into {} (map (fn [g]
                                     [(:db/id g)
                                      (when-let [parent (:group/_children g)]
                                        (:db/id parent))])
                                   groups-with-parents))

          ;; Sort groups from root to leaf by following parent chain
          sort-groups (fn [group-eid]
                        (loop [current group-eid
                               path    []]
                          (if-let [parent (get parent-map current)]
                            (recur parent (conj path current))
                            (reverse (conj path current)))))

          sorted-group-eids (sort-groups immediate-group-eid)
          submodule         {:db/id submodule-eid}]

      ;; Return: [submodule parent-groups... immediate-group]
      (cons submodule (map #(hash-map :db/id %) sorted-group-eids)))))

(reg-sub
 :vms/group-variable-heirarchy
 (fn [_ [_ gv-uuid]]
   (get-group-variable-hierarchy @@vms-conn gv-uuid)))

