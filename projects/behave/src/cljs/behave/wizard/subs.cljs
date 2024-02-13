(ns behave.wizard.subs
  (:require [behave.schema.core     :refer [rules]]
            [behave.vms.store       :refer [vms-conn]]
            [clojure.set            :refer [rename-keys intersection]]
            [datascript.core        :as d]
            [re-frame.core          :refer [reg-sub subscribe] :as rf]
            [string-utils.interface :refer [->kebab]]
            [clojure.string         :as str]))

;;; Helpers

(defn- matching-submodule? [io slug submodule]
  (and (= io (:submodule/io submodule))
       (= slug (:slug submodule))))

;;; Subscriptions

(reg-sub
 :wizard/*module
 (fn [_]
   (subscribe [:vms/pull-with-attr :module/name]))
 (fn [modules [_ selected-module]]
   (first (filter (fn [{m-name :module/name}]
                    (= selected-module (str/lower-case m-name))) modules))))

(reg-sub
 :wizard/submodules
 (fn [[_ module-id]]
   (subscribe [:vms/pull-children
               :module/submodules
               module-id]))
 (fn [submodules _]
   (map (fn [submodule]
          (-> submodule
              (assoc :slug (-> submodule (:submodule/name) (->kebab)))
              (assoc :submodule/groups @(subscribe [:wizard/groups (:db/id submodule)]))))
        submodules)))

(reg-sub
 :wizard/submodules-io-input-only
 (fn [[_ module-id]]
   (subscribe [:wizard/submodules module-id]))

 (fn [submodules _]
   (->> submodules
        (filter (fn [submodule] (= (:submodule/io submodule) :input)))
        (filter #(not (:submodule/research? %))) ;; TODO: Remove when "Research Mode" is enabled
        (sort-by :submodule/order))))

(reg-sub
 :wizard/submodules-io-output-only
 (fn [[_ module-id]]
   (subscribe [:wizard/submodules module-id]))

 (fn [submodules _]
   (->> submodules
        (filter (fn [submodule] (= (:submodule/io submodule) :output)))
        (filter #(not (:submodule/research? %))) ;; TODO: Remove when "Research Mode" is enabled
        (sort-by :submodule/order))))

(reg-sub
 :wizard/submodules-conditionally-filtered
 (fn [[_ _ws-uuid module-id _io]]
   [(subscribe [:wizard/submodules-io-input-only module-id])
    (subscribe [:wizard/submodules-io-output-only module-id])])

 (fn [[input-submodules output-submodules] [_ ws-uuid _module-id io]]
   (let [submodules (if (= io :output) output-submodules input-submodules)]
     (filter (fn [{id :db/id
                   op :submodule/conditionals-operator}]
               @(subscribe [:wizard/show-submodule? ws-uuid id op]))
             submodules))))

(reg-sub
 :wizard/*submodule

 (fn [[_ module-id _ _]]
   (subscribe [:wizard/submodules module-id]))

 (fn [submodules [_ _ slug io]]
   (let [[inputs outputs] (partition-by :submodules/io submodules)]
     (or (first (filter (partial matching-submodule? io slug) submodules))
         (first (if (= :input io) inputs outputs))))))

(defn edit-groups [group]
  (when group
    (cond-> group
      (seq (:group/group-variables group))
      (assoc :group/group-variables
             (mapv #(let [variable-data (rename-keys (first (:variable/_group-variables %))
                                                     {:bp/uuid :variable/uuid})]
                      (-> %
                          (dissoc :variable/_group-variables)
                          (merge variable-data)
                          (dissoc :variable/group-variables)
                          (update :variable/kind keyword)))
                   (filter #(not (:group-variable/research? %)) ;; TODO: Remove when "Research Mode" is enabled
                           (:group/group-variables group))))

      (seq (:group/children group))
      (assoc :group/children
             (map edit-groups (:group/children group))))))

(reg-sub
 :wizard/groups
 (fn [[_ submodule-id]]
   (subscribe [:vms/pull-children
               :submodule/groups
               submodule-id
               '[* {:group/group-variables [* {:variable/_group-variables [* {:variable/list [* {:list/options [*]}]}]}]}
                 {:group/children 6}]])) ;; recursively apply pattern up to 6 levels deep

 (fn [groups]
   (->> (mapv edit-groups groups)
        (sort-by #(:group/order %)))))

;; Subgroups

(reg-sub
 :wizard/subgroups
 (fn [[_ group-id]]
   (subscribe [:vms/pull
               '[{:group/children
                  [* {:group/group-variables
                      [* {:variable/_group-variables
                          [* {:variable/list
                              [* {:list/options [*]}]}]}]
                      :group/children [*]}]}]
               group-id]))

 (fn [group]
   (mapv (fn [subgroup]
           (assoc subgroup
                  :group/group-variables
                  (mapv #(let [variable-data (rename-keys (first (:variable/_group-variables %))
                                                          {:bp/uuid :variable/uuid})]
                           (-> %
                               (dissoc :variable/_group-variables)
                               (merge variable-data)
                               (dissoc :variable/group-variables)
                               (update :variable/kind keyword))) (:group/group-variables subgroup))))
         (:group/children group))))


;; Lists

(reg-sub
 :wizard/variable-list
 (fn [[_ group-id]]
   (subscribe [:vms/pull
               '[{:group/children [* {:group/group-variables [* {:variable/_group-variables [*]}] :group/children [*]}]}]
               group-id])))

;; Group Variables

(reg-sub
 :wizard/multi-value-input-count

 (fn [[_ ws-uuid]]
   (subscribe [:worksheet/all-input-values ws-uuid]))

 (fn [all-input-values _query]
   (count
    (filter (fn multiple-values? [value]
              (> (count (str/split value #",|\s"))
                 1))
            all-input-values))))

(reg-sub
 :wizard/gv-uuid->variable-name
 (fn [_ [_ gv-uuid]]
   @(subscribe [:vms/query '[:find ?name .
                             :in    $ ?gv-uuid
                             :where
                             [?gv :bp/uuid ?gv-uuid]
                             [?v :variable/group-variables ?gv]
                             [?v :variable/name ?name]]
                gv-uuid])))

(reg-sub
 :wizard/gv-uuid->variable-units
 (fn [_ [_ gv-uuid]]
   @(subscribe [:vms/query '[:find ?unit-short-code .
                             :in    $ ?gv-uuid
                             :where
                             [?gv :bp/uuid ?gv-uuid]
                             [?v :variable/group-variables ?gv]
                             [?v :variable/native-unit-uuid ?unit-uuid]
                             [?u :bp/uuid ?unit-uuid]
                             [?u :unit/short-code ?unit-short-code]]
                gv-uuid])))

;; Returns a map of group-variable-uuids -> variable native units
;; if and only if the variable is allowed to convert to map-units
(reg-sub
 :wizard/map-unit-convertible-variables
 (fn [_]
   (subscribe [:vms/query '[:find [?gv-uuid ...]
                            :where
                            [?v :variable/group-variables ?gv]
                            [?gv :bp/uuid ?gv-uuid]
                            [?v :variable/map-units-convertible? true]]]))
 (fn [results _]
   (set results)))

(reg-sub
 :wizard/group-variable
 (fn [[_ gv-uuid]]
   (subscribe [:vms/pull '[* {:variable/_group-variables [:variable/name :variable/native-unit-uuid]}] [:bp/uuid gv-uuid]]))

 (fn [group-variable _query]
   (let [variable-data (rename-keys (first (:variable/_group-variables group-variable))
                                    {:db/id :variable/id})]
     (-> group-variable
         (dissoc :variable/_group-variables)
         (merge variable-data)
         (dissoc :variable/group-variables)
         (update :variable/kind keyword)))))

;; TODO Might want to set this in a config file to the application
(def ^:const multi-value-input-limit 3)

(reg-sub
 :wizard/multi-value-input-limit
 (fn [_db _query]
   multi-value-input-limit))

(reg-sub
 :wizard/warn-limit?
 (fn [[_id ws-uuid]]
   (subscribe [:wizard/multi-value-input-count ws-uuid]))

 (fn [multi-value-input-count _query]
   (> multi-value-input-count multi-value-input-limit)))

(reg-sub
 :wizard/submodule-name+io
 (fn [_ [_ submodule-uuid]]
   @(subscribe [:vms/query '[:find [?s-name ?io]
                             :in    $ ?uuid
                             :where
                             [?s :bp/uuid ?uuid]
                             [?s :submodule/name ?s-name]
                             [?s :submodule/io ?io]]
                submodule-uuid])))

;; returns a collection of [note-id note-name note-content submodule-name submodule-io]
;; Optionally filter notes using submodule-uuid
(reg-sub
 :wizard/notes

 (fn [[_id ws-uuid]]
   (subscribe [:worksheet ws-uuid]))

 (fn [worksheet [_ _ws-uuid submodule-uuid]]
   (let [notes (:worksheet/notes worksheet)]
     (cond->> notes
       submodule-uuid (filter (fn [{s-uuid :note/submodule}]
                                (= s-uuid submodule-uuid)))
       :always        (map (fn resolve-uuid [{id      :db/id
                                              name    :note/name
                                              content :note/content
                                              s-uuid  :note/submodule}]
                             (into   [id name content]
                                     @(subscribe [:wizard/submodule-name+io s-uuid]))))))))

(reg-sub
 :wizard/edit-note?
 (fn [{:keys [state]} [_ note-id]]
   (true? (get-in state [:worksheet :notes note-id :edit?]))))


(reg-sub
 :wizard/show-notes?
 (fn [{:keys [state]} _]
   (true? (get-in state [:worksheet :show-notes?]))))

(reg-sub
 :wizard/show-add-note-form?
 (fn [{:keys [state]} _]
   (true? (get-in state [:worksheet :show-add-note-form?]))))


(reg-sub
 :wizard/results-tab-selected
 (fn [_ _]
   (subscribe [:state [:worksheet :results :tab-selected]]))
 (fn [tab-selected _]
   tab-selected))

(reg-sub
 :wizard/worksheet-date

 (fn [[_ ws-uuid]]
   (subscribe [:worksheet ws-uuid]))

 (fn [worksheet _]
   (let [created-date (:worksheet/created worksheet)
         d            (js/Date.)]
     (.setTime d created-date)
     (.toLocaleDateString d))))

(reg-sub
 :wizard/first-module+submodule
 (fn [[_ ws-uuid _]]
   (subscribe [:worksheet ws-uuid]))

 (fn [worksheet [_ ws-uuid io]]
   (when-let [module-kw (first (:worksheet/modules worksheet))]
     (let [module     @(subscribe [:wizard/*module (name module-kw)])
           module-id  (:db/id module)
           submodules (->> @(subscribe [:wizard/submodules-conditionally-filtered ws-uuid module-id io])
                           (sort-by :submodule/order))]

       [(name module-kw) (:slug (first submodules))]))))

;;; show-group?
(defn- csv? [s] (< 1 (count (str/split s #","))))

(defn- intersect? [s1 s2]
  (pos? (count (intersection s1 s2))))

(defn- resolve-conditionals [worksheet conditionals]
  (let [ws-uuid (:worksheet/uuid worksheet)]
    (map (fn pass?
           [{group-variable-uuid :conditional/group-variable-uuid
             ttype               :conditional/type
             op                  :conditional/operator
             values              :conditional/values}]
           (let [{:keys [group-uuid io]} (-> (subscribe [:wizard/conditional-io+group-uuid
                                                         group-variable-uuid])
                                             deref
                                             first)
                 conditional-values-set  (set values)
                 worksheet-value         (cond
                                           (= ttype :module)
                                           (map name (:worksheet/modules worksheet))

                                           (= io :output)
                                           @(subscribe [:worksheet/output-enabled?
                                                        ws-uuid
                                                        group-variable-uuid])

                                           (= io :input)
                                           @(subscribe [:worksheet/input-value
                                                        ws-uuid
                                                        group-uuid
                                                        0
                                                        group-variable-uuid]))
                 worksheet-value-set (cond
                                       (= ttype :module)      (set worksheet-value)
                                       (csv? worksheet-value) (set (map str/trim (str/split worksheet-value ",")))
                                       :else                  #{worksheet-value})]
             (case op
               :equal     (if (= ttype :module)
                            (= conditional-values-set worksheet-value-set)
                            (= (first conditional-values-set)
                               (if worksheet-value (str worksheet-value) "false")))
               :not-equal (not= (first conditional-values-set) (str worksheet-value))
               :in        (intersect? conditional-values-set worksheet-value-set))))
         conditionals)))

(defn- all-conditionals-pass? [worksheet conditionals-operator conditionals]
  (let [resolved-conditionals (resolve-conditionals worksheet conditionals)]
    (if (= conditionals-operator :or)
      (some true? resolved-conditionals)
      (every? true? resolved-conditionals))))

(reg-sub
 :wizard/conditional-io+group-uuid
 (fn [_ [_ gv-uuid]]
   (d/q '[:find  ?io ?g-uuid
          :keys   io  group-uuid
          :in    $ % ?gv-uuid
          :where
          (conditional-variable ?io ?g-uuid ?gv-uuid)]
        @@vms-conn rules gv-uuid)))


(reg-sub
 :wizard/_select-actions
 (fn [_ [_ gv-uuid]]
   (d/q '[:find (pull ?a [* {:action/conditionals [*]}]) .
          :in $ ?gv-uuid
          :where
          [?gv :bp/uuid ?gv-uuid]
          [?gv :group-variable/actions ?a]
          [?a :action/type :select]]
        @@vms-conn gv-uuid)))

(reg-sub
 :wizard/default-option
 (fn [[_ ws-uuid gv-uuid]]
   [(subscribe [:worksheet ws-uuid])
    (subscribe [:wizard/_select-actions gv-uuid])])
 (fn [[worksheet action]]
   (let [conditionals  (:action/conditionals action)
         cond-operator (:action/conditionals-operator action)
         target-value  (:action/target-value action)

         conditionals-passed?
         (or (nil? conditionals)
             (all-conditionals-pass? worksheet cond-operator conditionals))]
     (when (and target-value conditionals-passed?)
       (:action/target-value action)))))

(reg-sub
 :wizard/_disabled-actions
 (fn [_ [_ gv-uuid]]
   (d/q '[:find [(pull ?a [* {:action/conditionals [*]}]) ...]
          :in $ ?gv-uuid
          :where
          [?gv :bp/uuid ?gv-uuid]
          [?gv :group-variable/actions ?a]
          [?a :action/type :disable]]
        @@vms-conn gv-uuid)))

(reg-sub
 :wizard/disabled-options
 (fn [[_ ws-uuid gv-uuid]]
   [(subscribe [:worksheet ws-uuid])
    (subscribe [:wizard/_disabled-actions gv-uuid])])
 (fn [[worksheet actions]]
   (->> actions
        (map #(let [conditionals  (:action/conditionals %)
                    cond-operator (:action/conditionals-operator %)
                    target-value  (:action/target-value %)

                    conditionals-passed?
                    (or (nil? conditionals)
                        (all-conditionals-pass? worksheet cond-operator conditionals))]
                (when (and target-value conditionals-passed?)
                  (:action/target-value %))))
        (remove nil?)
        (set))))

(reg-sub
 :wizard/show-group?
 (fn [[_ ws-uuid group-id & _rest]]
   [(subscribe [:worksheet ws-uuid])
    (subscribe [:vms/pull-children :group/conditionals group-id])
    (subscribe [:vms/entity-from-eid group-id])])

 (fn [[worksheet conditionals group-entity] [_ _ws-uuid _group-id conditionals-operator]]
   (and (if (seq conditionals)
          (all-conditionals-pass? worksheet conditionals-operator conditionals)
          true)
        (not (:group/research? group-entity)))))

(reg-sub
 :wizard/show-group?
 (fn [[_ ws-uuid group-id & _rest]]
   [(subscribe [:worksheet ws-uuid])
    (subscribe [:vms/pull-children :group/conditionals group-id])
    (subscribe [:vms/entity-from-eid group-id])])

 (fn [[worksheet conditionals group-entity] [_ _ws-uuid _group-id conditionals-operator]]
   (and (if (seq conditionals)
          (all-conditionals-pass? worksheet conditionals-operator conditionals)
          true)
        (not (:group/research? group-entity)))))

(reg-sub
 :wizard/show-submodule?
 (fn [[_ ws-uuid submodule-id & _rest]]
   [(subscribe [:worksheet ws-uuid])
    (rf/subscribe [:vms/pull-children :submodule/conditionals submodule-id])])

 (fn [[worksheet conditionals] [_ _ws-uuid _submodule-id conditionals-operator]]
   (if (seq conditionals)
     (all-conditionals-pass? worksheet conditionals-operator conditionals)
     true)))

(reg-sub
 :wizard/diagram-input-gv-uuids
 (fn [_ [_ gv-uuid]]
   (d/q '[:find  [?gv-uuid ...]
          :in    $ ?gv
          :where
          [?d :diagram/group-variable ?gv]
          [?d :diagram/input-group-variables ?g]
          [?g :bp/uuid ?gv-uuid]]
        @@vms-conn [:bp/uuid gv-uuid])))

(reg-sub
 :wizard/diagram-output-gv-uuids
 (fn [_ [_ gv-uuid]]
   (d/q '[:find  [?gv-uuid ...]
          :in    $ ?gv
          :where
          [?d :diagram/group-variable ?gv]
          [?d :diagram/output-group-variables ?g]
          [?g :bp/uuid ?gv-uuid]]
        @@vms-conn [:bp/uuid gv-uuid])))

(reg-sub
 :wizard/show-range-selector?
 (fn [{:keys [state]} [_ gv-uuid repeat-id]]
   (true? (get-in state [:show-range-selector? gv-uuid repeat-id]))))


(defn index-by
  "Indexes collection by key or fn."
  [k-or-fn coll]
  (persistent! (reduce
                (fn [acc cur] (assoc! acc (k-or-fn cur) cur))
                (transient {})
                coll)))

(reg-sub
 :wizard/units-used-short-code

 (fn [[_ _ _ dimension-uuid]]
   (rf/subscribe [:vms/entity-from-uuid dimension-uuid]))

 (fn [{units :dimension/units} [_ v-uuid ws-unit-uuid _dimension-uuid native-unit-uuid english-unit-uuid metric-unit-uuid]]
   (let [units-by-uuid     (index-by :bp/uuid units)
         *cached-unit-uuid (rf/subscribe [:settings/cached-unit v-uuid])
         *cached-unit      (rf/subscribe [:vms/entity-from-uuid @*cached-unit-uuid])
         native-unit       (get units-by-uuid native-unit-uuid)
         english-unit      (get units-by-uuid english-unit-uuid)
         metric-unit       (get units-by-uuid metric-unit-uuid)
         default-unit      (or @*cached-unit native-unit english-unit metric-unit)]
     (:unit/short-code (or (get units-by-uuid ws-unit-uuid) default-unit)))))

(reg-sub
 :wizard/x-axis-limit-min+max-defaults

 (fn [[_ ws-uuid]]
   (subscribe [:worksheet/multi-value-input-uuid+value ws-uuid]))

 (fn [multi-value-inputs [_ _ gv-uuid]]
   (let [[_ values]    (first (filter #(= (first %) gv-uuid) multi-value-inputs))
         parsed-values (map js/parseFloat (str/split values ","))]
     [(apply min parsed-values) (apply max parsed-values)])
   )
 )

(comment
  (rf/subscribe [:wizard/x-axis-limit-min+max-defaults
                 "65aaf0bb-1568-4db9-b21e-87ae840bcb9b"
                 "64a75ef0-5caf-4584-b0cf-369d7058f974"])

  )
