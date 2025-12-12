(ns behave.worksheet.subs
  (:require [austinbirch.reactive-entity :as re]
            [behave.schema.core          :refer [rules]]
            [behave.store                :as s]
            [behave.translate            :refer [<t]]
            [behave.vms.store            :as vms :refer [vms-conn]]
            [behave.wizard.subs          :refer [all-conditionals-pass?]]
            [clojure.set                 :as set]
            [clojure.string              :as str]
            [datascript.core             :as d]
            [goog.string                 :as gstring]
            [map-utils.interface         :refer [index-by]]
            [number-utils.core           :refer [parse-float]]
            [re-frame.core               :as rf]
            [re-posh.core                :as rp]
            [string-utils.interface      :refer [->kebab ->str]]))

;; Helpers
(defn make-tree
  [xs]
  (into {} (map (fn [x] [(butlast x) [(last x)]]) xs)))

(defn input-tree-to-vec
  [[path leaf]]
  (let [input-vec (vec (concat (vec path) leaf))]
    (if (= (count input-vec) 4)
      (conj input-vec :none)
      input-vec)))

(defn re-entity-from-uuid [bp-uuid]
  (re/entity [:bp/uuid bp-uuid]))

(defn re-entity-from-eid [eid]
  (re/entity eid))

(rf/reg-sub
 :worksheet/all
 (fn [_ _]
   (d/q '[:find ?created ?uuid
          :in $
          :where
          [?e :worksheet/uuid ?uuid]
          [?e :worksheet/created ?created]]
        @@s/conn)))

;; Retrieve latest worksheet UUID
(rf/reg-sub
 :worksheet/latest
 (fn [_]
   (rf/subscribe [:worksheet/all]))
 (fn [all-worksheets [_]]
   (last (last (sort-by first all-worksheets)))))

;; Retrieve worksheet as reactive entity
(rf/reg-sub
 :worksheet
 (fn [_ [_ ws-uuid]]
   (when-let [eid (d/entid @@s/conn [:worksheet/uuid ws-uuid])]
     (let [worksheet (re/entity eid)]
       (when (re/exists? worksheet)
         worksheet)))))

;; Retrieve worksheet as entity
(rf/reg-sub
 :worksheet-entity
 (fn [_ [_ ws-uuid]]
   (d/entity @@s/conn [:worksheet/uuid ws-uuid])))

(rp/reg-sub
 :worksheet/name
 (fn [_ [_ ws-uuid]]
   {:type      :query
    :query     '[:find  ?name .
                 :in    $ ?ws-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/name ?name]]
    :variables [ws-uuid]}))

(rf/reg-sub
 :worksheet/modules
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet ws-uuid]))

 (fn [worksheet _]
   (->> worksheet
        :worksheet/modules
        (map #(deref (rf/subscribe [:wizard/*module (name %)])))
        (sort-by :module/order))))

;; Get state of a particular output
(rf/reg-sub
 :worksheet/output-enabled?
 (fn [[_ ws-uuid _variable-uuid]]
   (rf/subscribe [:worksheet ws-uuid]))

 (fn [worksheet [_ _ws-uuid variable-uuid]]
   (->> worksheet
        (:worksheet/outputs)
        (filter (fn matching-uuid [output]
                  (= (:output/group-variable-uuid output) variable-uuid)))
        (first)
        (:output/enabled?))))

;; Get the Input entity
(rf/reg-sub
 :worksheet/input
 (fn [_ [_ ws-uuid group-uuid repeat-id group-variable-uuid]]
   (let [eid (d/q '[:find  ?i .
                    :in    $ ?ws-uuid ?group-uuid ?repeat-id ?group-var-uuid
                    :where
                    [?w :worksheet/uuid ?ws-uuid]
                    [?w :worksheet/input-groups ?g]
                    [?g :input-group/group-uuid ?group-uuid]
                    [?g :input-group/repeat-id ?repeat-id]
                    [?g :input-group/inputs ?i]
                    [?i :input/group-variable-uuid ?group-var-uuid]]
                  @@s/conn ws-uuid group-uuid repeat-id group-variable-uuid)]
     (d/touch (d/entity @@s/conn eid)))))

;; Get the value of a particular input
(rp/reg-sub
 :worksheet/input-value
 (fn [_ [_ ws-uuid group-uuid repeat-id group-variable-uuid]]
   {:type      :query
    :query     '[:find  ?value .
                 :in    $ ?ws-uuid ?group-uuid ?repeat-id ?group-var-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/input-groups ?g]
                 [?g :input-group/group-uuid ?group-uuid]
                 [?g :input-group/repeat-id ?repeat-id]
                 [?g :input-group/inputs ?i]
                 [?i :input/group-variable-uuid ?group-var-uuid]
                 [?i :input/value ?value]]
    :variables [ws-uuid group-uuid repeat-id group-variable-uuid]}))

;; Get the units for a particular input

;; deprecated because this is using `:input/units`
(rp/reg-sub
 :worksheet/input-units-deprecated
 (fn [_ [_ ws-uuid group-uuid repeat-id group-variable-uuid]]
   {:type      :query
    :query     '[:find  ?unit-uuid .
                 :in    $ ?ws-uuid ?group-uuid ?repeat-id ?group-var-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/input-groups ?g]
                 [?g :input-group/group-uuid ?group-uuid]
                 [?g :input-group/repeat-id ?repeat-id]
                 [?g :input-group/inputs ?i]
                 [?i :input/group-variable-uuid ?group-var-uuid]
                 [?i :input/units ?unit-uuid]]
    :variables [ws-uuid group-uuid repeat-id group-variable-uuid]}))

(rp/reg-sub
 :worksheet/input-units-uuid
 (fn [_ [_ ws-uuid group-uuid repeat-id group-variable-uuid]]
   {:type      :query
    :query     '[:find  ?unit-uuid .
                 :in    $ ?ws-uuid ?group-uuid ?repeat-id ?group-var-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/input-groups ?g]
                 [?g :input-group/group-uuid ?group-uuid]
                 [?g :input-group/repeat-id ?repeat-id]
                 [?g :input-group/inputs ?i]
                 [?i :input/group-variable-uuid ?group-var-uuid]
                 [?i :input/units-uuid ?unit-uuid]]
    :variables [ws-uuid group-uuid repeat-id group-variable-uuid]}))

(rf/reg-sub
 :worksheet/input-units
 (fn [[_ ws-uuid group-uuid repeat-id group-variable-uuid]]
   [(rf/subscribe [:worksheet/input-units-deprecated ws-uuid group-uuid repeat-id group-variable-uuid])
    (rf/subscribe [:worksheet/input-units-uuid ws-uuid group-uuid repeat-id group-variable-uuid])])
 (fn [[units units-uuid] _]
   (or units units-uuid)))

;; Find groups matching a group-uuid
(rp/reg-sub
 :worksheet/repeat-groups
 (fn [_ [_ ws-uuid group-uuid]]
   {:type      :query
    :query     '[:find  [?g ...]
                 :in    $ ?ws-uuid ?group-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/input-groups ?g]
                 [?g :input-group/group-uuid ?group-uuid]]
    :variables [ws-uuid group-uuid]}))

;; Find inputs for a given group-uuid and repeat-id
(rp/reg-sub
 :worksheet/input-ids
 (fn [_ [_ ws-uuid group-uuid repeat-id]]
   {:type      :query
    :query     '[:find [?i ...]
                 :in  $ ?ws-uuid ?group-uuid ?repeat-id
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/input-groups ?g]
                 [?g :input-group/group-uuid ?group-uuid]
                 [?g :input-group/repeat-id ?repeat-id]
                 [?g :input-group/inputs ?i]]
    :variables [ws-uuid group-uuid repeat-id]}))

(rp/reg-sub
 :worksheet/group-repeat-ids
 (fn [_ [_ ws-uuid group-uuid]]
   {:type      :query
    :query     '[:find  [?rid ...]
                 :in    $ ?ws-uuid ?group-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/input-groups ?ig]
                 [?ig :input-group/group-uuid ?group-uuid]
                 [?ig :input-group/repeat-id ?rid]]
    :variables [ws-uuid group-uuid]}))

;; Find inputs for a given group-uuid and repeat-id
(rp/reg-sub
 :worksheet/input-ids
 (fn [_ [_ ws-uuid group-uuid repeat-id]]
   {:type      :query
    :query     '[:find [?i ...]
                 :in  $ ?ws-uuid ?group-uuid ?repeat-id
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/input-groups ?g]
                 [?g :input-group/group-uuid ?group-uuid]
                 [?g :input-group/repeat-id ?repeat-id]
                 [?g :input-group/inputs ?i]]
    :variables [ws-uuid group-uuid repeat-id]}))

(rp/reg-sub
 :worksheet/group-repeat-ids
 (fn [_ [_ ws-uuid group-uuid]]
   {:type      :query
    :query     '[:find  [?rid ...]
                 :in    $ ?ws-uuid ?group-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/input-groups ?ig]
                 [?ig :input-group/group-uuid ?group-uuid]
                 [?ig :input-group/repeat-id ?rid]]
    :variables [ws-uuid group-uuid]}))

(rf/reg-sub
 :worksheet/all-input-entities
 (fn [_ [_ ws-uuid]]
   (let [input-eids @(rf/subscribe [:query
                                    '[:find  [?i ...]
                                      :in    $ ?ws-uuid
                                      :where
                                      [?w :worksheet/uuid ?ws-uuid]
                                      [?w :worksheet/input-groups ?g]
                                      [?g :input-group/group-uuid ?group-uuid]
                                      [?g :input-group/repeat-id ?repeat-id]
                                      [?g :input-group/inputs ?i]]
                                    [ws-uuid]])]
     (map #(d/entity @@s/conn %) input-eids))))

(rf/reg-sub
 :worksheet/all-inputs-vector
 (fn [_ [_ ws-uuid]]
   (let [inputs @(rf/subscribe [:query
                                '[:find  ?group-uuid ?repeat-id ?group-var-uuid ?value
                                  :in    $ ?ws-uuid
                                  :where
                                  [?w :worksheet/uuid ?ws-uuid]
                                  [?w :worksheet/input-groups ?g]
                                  [?g :input-group/group-uuid ?group-uuid]
                                  [?g :input-group/repeat-id ?repeat-id]
                                  [?g :input-group/inputs ?i]
                                  [?i :input/group-variable-uuid ?group-var-uuid]
                                  [?i :input/value ?value]]
                                [ws-uuid]])]
     (into [] inputs))))

(rf/reg-sub
 :worksheet/all-variable-level-units
 (fn [_]
   (rf/subscribe [:settings/application-units-system]))
 (fn [units-system [_ ws-uuid]]
   (let [units-system-attr (case units-system
                             :native  :variable/native-unit-uuid
                             :english :variable/english-unit-uuid
                             :metric  :variable/metric-unit-uuid)]
     (into []
           (d/q '[:find  ?group-uuid ?repeat-id ?gv-uuid ?unit-uuid
                  :in    $ $ws % ?ws-uuid ?units-system
                  :where
                  [$ws ?w :worksheet/uuid ?ws-uuid]
                  [$ws ?w :worksheet/input-groups ?g]
                  [$ws ?g :input-group/group-uuid ?group-uuid]
                  [$ws ?g :input-group/repeat-id ?repeat-id]
                  [$ws ?g :input-group/inputs ?i]
                  [$ws ?i :input/group-variable-uuid ?gv-uuid]
                  (lookup ?gv-uuid ?gv)
                  (group-variable _ ?gv ?v)
                  [?v :variable/kind :continuous]
                  [?v ?units-system-attr ?unit-uuid]]
                @@vms-conn @@s/conn rules ws-uuid units-system-attr)))))

(rf/reg-sub
 :worksheet/all-domain-level-units
 (fn [_]
   (rf/subscribe [:settings/application-units-system]))
 (fn [units-system [_ ws-uuid]]
   (let [units-system-attr (case units-system
                             :english :domain/english-unit-uuid
                             :metric  :domain/metric-unit-uuid
                             :domain/native-unit-uuid)]
     (into []
           (d/q '[:find  ?group-uuid ?repeat-id ?gv-uuid ?unit-uuid
                  :in    $ $ws % ?ws-uuid ?units-system-attr
                  :where
                  [$ws ?w :worksheet/uuid ?ws-uuid]
                  [$ws ?w :worksheet/input-groups ?g]
                  [$ws ?g :input-group/group-uuid ?group-uuid]
                  [$ws ?g :input-group/repeat-id ?repeat-id]
                  [$ws ?g :input-group/inputs ?i]
                  [$ws ?i :input/group-variable-uuid ?gv-uuid]
                  (lookup ?gv-uuid ?gv)
                  (group-variable _ ?gv ?v)
                  [?v :variable/kind :continuous]
                  [?v :variable/domain-uuid ?domain-uuid]
                  [?d :bp/uuid ?domain-uuid]
                  [?d ?units-system-attr ?unit-uuid]]
                @@vms-conn @@s/conn rules ws-uuid units-system-attr)))))

(rf/reg-sub
 :worksheet/all-cached-units
 (fn [_]
   (rf/subscribe [:settings/local-storage-units]))

 (fn [units-settings [_ ws-uuid]]
   (into []
         (comp (filter (fn [[_ _ _ domain-uuid]] (contains? units-settings domain-uuid)))
               (map (fn [[group-uuid repeat-uuid gv-uuid domain-uuid]]
                      [group-uuid repeat-uuid gv-uuid (get-in units-settings [domain-uuid :unit-uuid])])))
         (d/q '[:find  ?group-uuid ?repeat-id ?gv-uuid ?domain-uuid
                :in    $ $ws % ?ws-uuid
                :where
                [$ws ?w :worksheet/uuid ?ws-uuid]
                [$ws ?w :worksheet/input-groups ?g]
                [$ws ?g :input-group/group-uuid ?group-uuid]
                [$ws ?g :input-group/repeat-id ?repeat-id]
                [$ws ?g :input-group/inputs ?i]
                [$ws ?i :input/group-variable-uuid ?gv-uuid]
                (lookup ?gv-uuid ?gv)
                (group-variable _ ?gv ?v)
                [?v :variable/kind :continuous]
                [?v :variable/domain-uuid ?domain-uuid]
                [?d :bp/uuid ?domain-uuid]]
              @@vms-conn @@s/conn rules ws-uuid))))

;; deprecated because this is using `:input/units`
(rf/reg-sub
 :worksheet/all-custom-units-deprecated
 (fn [_ [_ ws-uuid]]
   (d/q '[:find  ?group-uuid ?repeat-id ?gv-uuid ?unit-uuid
          :in    $ $ws % ?ws-uuid
          :where
          [$ws ?w :worksheet/uuid ?ws-uuid]
          [$ws ?w :worksheet/input-groups ?g]
          [$ws ?g :input-group/group-uuid ?group-uuid]
          [$ws ?g :input-group/repeat-id ?repeat-id]
          [$ws ?g :input-group/inputs ?i]
          [$ws ?i :input/group-variable-uuid ?gv-uuid]
          (lookup ?gv-uuid ?gv)
          (group-variable _ ?gv ?v)
          [$ws ?i :input/units ?unit-uuid]]
        @@vms-conn @@s/conn rules ws-uuid)))

(rf/reg-sub
 :worksheet/all-custom-units-using-input-units-uuids
 (fn [_ [_ ws-uuid]]
   (d/q '[:find  ?group-uuid ?repeat-id ?gv-uuid ?unit-uuid
          :in    $ $ws % ?ws-uuid
          :where
          [$ws ?w :worksheet/uuid ?ws-uuid]
          [$ws ?w :worksheet/input-groups ?g]
          [$ws ?g :input-group/group-uuid ?group-uuid]
          [$ws ?g :input-group/repeat-id ?repeat-id]
          [$ws ?g :input-group/inputs ?i]
          [$ws ?i :input/group-variable-uuid ?gv-uuid]
          (lookup ?gv-uuid ?gv)
          (group-variable _ ?gv ?v)
          [$ws ?i :input/units-uuid ?unit-uuid]]
        @@vms-conn @@s/conn rules ws-uuid)))

(rf/reg-sub
 :worksheet/all-custom-units
 (fn [[_ ws-uuid group-uuid repeat-id group-variable-uuid]]
   [(rf/subscribe [:worksheet/all-custom-units-deprecated ws-uuid group-uuid repeat-id group-variable-uuid])
    (rf/subscribe [:worksheet/all-custom-units-using-input-units-uuids ws-uuid group-uuid repeat-id group-variable-uuid])])
 (fn [[all-custom-units-deprecated all-custom-units-using-input-uuids]]
   (into []
         (or (seq all-custom-units-deprecated) (seq all-custom-units-using-input-uuids)))))

(rf/reg-sub
 :worksheet/all-inputs+units-vector
 (fn [[_ ws-uuid]]
   [(rf/subscribe [:worksheet/all-inputs-vector ws-uuid])
    (rf/subscribe [:worksheet/all-variable-level-units ws-uuid])
    (rf/subscribe [:worksheet/all-custom-units ws-uuid])
    (rf/subscribe [:worksheet/all-cached-units ws-uuid])
    (rf/subscribe [:worksheet/all-domain-level-units ws-uuid])])
 (fn [sub-results]
   (let [[inputs
          variable-native-units
          worksheet-saved-units
          settings-cached-units
          all-domain-level-units] (map make-tree sub-results)]
     (->> (merge variable-native-units
                 all-domain-level-units
                 settings-cached-units
                 worksheet-saved-units)
          (merge-with (comp vec concat) inputs)
          (mapv input-tree-to-vec)))))

(rf/reg-sub
 :worksheet/all-inputs
 (fn [_ [_ ws-uuid]]
   (let [inputs @(rf/subscribe [:query
                                '[:find  ?group-uuid ?repeat-id ?group-var-uuid ?value
                                  :in    $ ?ws-uuid
                                  :where [?w :worksheet/uuid ?ws-uuid]
                                  [?w :worksheet/input-groups ?g]
                                  [?g :input-group/group-uuid ?group-uuid]
                                  [?g :input-group/repeat-id ?repeat-id]
                                  [?g :input-group/inputs ?i]
                                  [?i :input/group-variable-uuid ?group-var-uuid]
                                  [?i :input/value ?value]]
                                [ws-uuid]])]
     (reduce (fn [acc [group-uuid repeat-id group-var-uuid value]]
               (assoc-in acc [group-uuid repeat-id group-var-uuid] value))
             {}
             inputs))))

(rf/reg-sub
 :worksheet/all-input-values
 (fn [_ [_ ws-uuid]]
   @(rf/subscribe [:query
                   '[:find [?value ...]
                     :in $ ?ws-uuid
                     :where
                     [?w :worksheet/uuid ?ws-uuid]
                     [?w :worksheet/input-groups ?g]
                     [?g :input-group/inputs ?i]
                     [?i :input/value ?value]]
                   [ws-uuid]])))

(rf/reg-sub
 :worksheet/input-id+value
 (fn [_ [_ ws-uuid]]
   @(rf/subscribe [:query
                   '[:find ?group-var-uuid ?value
                     :in $ ?ws-uuid
                     :where
                     [?w :worksheet/uuid ?ws-uuid]
                     [?w :worksheet/input-groups ?g]
                     [?g :input-group/inputs ?i]
                     [?i :input/group-variable-uuid ?group-var-uuid]
                     [?i :input/value ?value]]
                   [ws-uuid]])))

(rf/reg-sub
 :worksheet/multi-value-input-uuid+value
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet/input-id+value ws-uuid]))

 (fn [inputs _query]
   (->> inputs
        (filter (fn multiple-values? [[gv-uuid value]]
                  (and
                   (not @(rf/subscribe [:wizard/text-group-variable? gv-uuid]))
                   (> (count (str/split value #",|\s"))
                      1)))))))

(rf/reg-sub
 :worksheet/multi-value-input-uuids
 (fn [[_ ws-uuid]]
   [(rf/subscribe [:worksheet/multi-value-input-uuid+value ws-uuid])
    (rf/subscribe [:vms/group-variable-order])])

 (fn [[inputs gv-order] _query]
   (->> inputs
        (map first)
        (sort-by #(.indexOf gv-order %)))))

(rf/reg-sub
 :worksheet/output-uuids-conditionally-filtered
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet ws-uuid]))
 (fn [worksheet [_ ws-uuid]]
   (->> (d/q '[:find  ?gv ?hide-result
               :in    $ $ws % ?ws-uuid
               :where
               [$ws ?w :worksheet/uuid ?ws-uuid]
               [$ws ?w :worksheet/outputs ?o]
               [$ws ?o :output/group-variable-uuid ?uuid]
               [$ws ?o :output/enabled? true]
               (lookup ?uuid ?gv)
               [(get-else $ ?gv :group-variable/hide-result? false) ?hide-result]]
             @@vms-conn
             @@s/conn
             rules
             ws-uuid)
        (remove (fn [[_ hide-result?]] (true? hide-result?)))
        (map first)
        (map (fn [gv] @(rf/subscribe [:vms/entity-from-eid gv])))
        (remove #(if (seq (:group-variable/hide-result-conditionals %))
                   (all-conditionals-pass? worksheet
                                           (:group-variable/hide-result-conditional-operator %)
                                           (:group-variable/hide-result-conditionals %))
                   false))
        (map :bp/uuid))))

(rf/reg-sub
 :worksheet/output-uuids-filtered
 (fn [_ [_ ws-uuid]]
   (->> (d/q '[:find  ?uuid ?hide-result
               :in    $ $ws % ?ws-uuid
               :where
               [$ws ?w :worksheet/uuid ?ws-uuid]
               [$ws ?w :worksheet/outputs ?o]
               [$ws ?o :output/group-variable-uuid ?uuid]
               [$ws ?o :output/enabled? true]
               (lookup ?uuid ?gv)
               [(get-else $ ?gv :group-variable/hide-result? false) ?hide-result]]
             @@vms-conn
             @@s/conn
             rules
             ws-uuid)
        (remove (fn [[_ hide-result?]] (true? hide-result?)))
        (map first))))

(rf/reg-sub
 :worksheet/graphed-output-uuids
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet ws-uuid]))
 (fn [worksheet [_ ws-uuid]]
   (->> (d/q '[:find  ?uuid ?hide-result ?graph-result
               :in    $ $ws % ?ws-uuid
               :where
               [$ws ?w :worksheet/uuid ?ws-uuid]
               [$ws ?w :worksheet/outputs ?o]
               [$ws ?o :output/group-variable-uuid ?uuid]
               [$ws ?o :output/enabled? true]
               (lookup ?uuid ?gv)
               [(get-else $ ?gv :group-variable/hide-result? false) ?hide-result]
               [(get-else $ ?gv :group-variable/hide-graph? false) ?graph-result]]
             @@vms-conn
             @@s/conn
             rules
             ws-uuid)
        (remove (fn [[_ hide-result? hide-graph?]] (or hide-result? hide-graph?)))
        (map first)
        (map (fn [gv-uuid] @(rf/subscribe [:vms/entity-from-uuid gv-uuid])))
        (remove #(if (seq (:group-variable/hide-result-conditionals %))
                   (all-conditionals-pass? worksheet
                                           (:group-variable/hide-result-conditional-operator %)
                                           (:group-variable/hide-result-conditionals %))
                   false))
        (map :bp/uuid))))

(rf/reg-sub
 :worksheet/all-output-uuids
 (fn [_ [_ ws-uuid]]
   (->> (d/q '[:find  [?uuid ...]
               :in  $ ?ws-uuid
               :where
               [?w :worksheet/uuid ?ws-uuid]
               [?w :worksheet/outputs ?o]
               [?o :output/group-variable-uuid ?uuid]
               [?o :output/enabled? true]]
             @@s/conn
             ws-uuid))))

(rp/reg-sub
 :worksheet/get-table-settings-attr
 (fn [_ [_ ws-uuid attr]]
   {:type      :query
    :query     '[:find  [?value ...]
                 :in    $ ?ws-uuid ?attr
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/table-settings ?t]
                 [?t ?attr ?value]]
    :variables [ws-uuid attr]}))

(rp/reg-sub
 :worksheet/get-graph-settings-attr
 (fn [_ [_ ws-uuid attr]]
   {:type      :query
    :query     '[:find  [?value ...]
                 :in    $ ?ws-uuid ?attr
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/graph-settings ?g]
                 [?g ?attr ?value]]
    :variables [ws-uuid attr]}))

(rp/reg-sub
 :worksheet/graph-settings-y-axis-limits
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet/graphed-output-uuids ws-uuid]))
 (fn [graph-output-uuids [_ ws-uuid]]
   {:type      :query
    :query     '[:find ?group-var-uuid ?min ?max
                 :in   $ ?ws-uuid [?group-var-uuid ...]
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/graph-settings ?g]
                 [?g :graph-settings/y-axis-limits ?y]
                 [?y :y-axis-limit/group-variable-uuid ?group-var-uuid]
                 [?y :y-axis-limit/min ?min]
                 [?y :y-axis-limit/max ?max]
                 [?w :worksheet/outputs ?o]
                 [?o :output/group-variable-uuid ?group-var-uuid]]
    :variables [ws-uuid graph-output-uuids]}))

(rf/reg-sub
 :worksheet/graph-settings-y-axis-limits-filtered
 (fn [[_ ws-uuid]] (rf/subscribe [:worksheet/graph-settings-y-axis-limits ws-uuid]))
 (fn [table-settings-filters _]
   (remove
    (fn [[group-var-uuid]]
      (let [kind (d/q '[:find ?kind .
                        :in  $ ?group-var-uuid
                        :where
                        [?gv :bp/uuid ?group-var-uuid]
                        [?v :variable/group-variables ?gv]
                        [?v :variable/kind ?kind]]
                      @@vms-conn
                      group-var-uuid)]
        (or (= kind :discrete)
            (= kind :text))))
    table-settings-filters)))

(rp/reg-sub
 :worksheet/graph-settings-x-axis-limits
 (fn [_ [_ ws-uuid]]
   {:type      :query
    :query     '[:find ?group-var-uuid ?min ?max
                 :in   $ ?ws-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/graph-settings ?g]
                 [?g :graph-settings/x-axis-limits ?y]
                 [?y :x-axis-limit/group-variable-uuid ?group-var-uuid]
                 [?y :x-axis-limit/min ?min]
                 [?y :x-axis-limit/max ?max]]
    :variables [ws-uuid]}))

(rp/reg-sub
 :worksheet/table-settings-filters
 (fn [_ [_ ws-uuid]]
   {:type      :query
    :query     '[:find ?group-var-uuid ?min ?max ?enabled
                 :in   $ ?ws-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/table-settings ?ts]
                 [?ts :table-settings/filters ?tf]
                 [?tf :table-filter/group-variable-uuid ?group-var-uuid]
                 [?tf :table-filter/min ?min]
                 [?tf :table-filter/max ?max]
                 [?tf :table-filter/enabled? ?enabled]
                 [?w :worksheet/outputs ?o]
                 [?o :output/group-variable-uuid ?group-var-uuid]
                 [?o :output/enabled? true]]
    :variables [ws-uuid]}))

(rf/reg-sub
 :worksheet/table-settings-filters-filtered
 (fn [[_ ws-uuid]] (rf/subscribe [:worksheet/table-settings-filters ws-uuid]))
 (fn [table-settings-filters _]
   (remove
    (fn [[group-var-uuid]]
      (let [kind (d/q '[:find ?kind .
                        :in  $ ?group-var-uuid
                        :where
                        [?gv :bp/uuid ?group-var-uuid]
                        [?v :variable/group-variables ?gv]
                        [?v :variable/kind ?kind]]
                      @@vms-conn
                      group-var-uuid)]
        (or (= kind :discrete)
            (= kind :text))))
    table-settings-filters)))

;; Results Table formatters

(defn ^:private create-formatter [variable multi-discrete? is-output?]
  (let [v-kind (:variable/kind variable)]
    (cond
      (= v-kind :continuous)
      (let [domain-uuid        (:variable/domain-uuid variable)
            domain             @(rf/subscribe [:vms/entity-from-uuid domain-uuid])
            *cached-decimals   (rf/subscribe [:settings/cached-decimal domain-uuid])
            significant-digits (or @*cached-decimals (:domain/decimals domain))]
        (fn continuous-fmt [value]
          (cond->> value
            :always                             (parse-float)
            (and significant-digits is-output?) (gstring/format (str "%." significant-digits "f")))))

      (or (= v-kind :discrete) multi-discrete?)
      (let [{llist :variable/list}  (d/pull @@vms-conn '[{:variable/list [* {:list/options [*]}]}] (:db/id variable))
            {options :list/options} llist
            options                 (index-by :list-option/value options)]
        (fn discrete-fmt [value & [{:keys [export?]}]]
          (if-let [option (get options value)]
            (if export?
              (or @(<t (:list-option/export-translation-key option))
                  @(<t (:list-option/result-translation-key option))
                  @(<t (:list-option/translation-key option)))
              (or @(<t (:list-option/result-translation-key option))
                  @(<t (:list-option/translation-key option))))
            value)))

      (= v-kind :text)
      identity)))

(rf/reg-sub
 :worksheet/result-table-formatters
 (fn [_ [_ gv-uuids]]
   (let [results (d/q '[:find ?gv ?gv-uuid (pull ?v [*]) ?multi-discrete
                        :in $ % [?gv-uuid ...]
                        :where
                        (lookup ?gv-uuid ?gv)
                        [(get-else $ ?gv :group-variable/discrete-multiple? false) ?multi-discrete]
                        (group-variable _ ?gv ?v)]
                      @@vms-conn rules gv-uuids)]
     (into {} (map
               (fn [[gv-eid gv-uuid variable multi-discrete?]]
                 (let [is-output? @(rf/subscribe [:vms/group-variable-is-output? gv-eid])]
                   [gv-uuid (create-formatter variable multi-discrete? is-output?)]))
               results)))))

(rp/reg-sub
 :worksheet/map-units-settings-eid
 (fn [_ [_ ws-uuid]]
   {:type      :query
    :query     '[:find  ?m .
                 :in    $ ?ws-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/table-settings ?t]
                 [?t :table-settings/map-units-settings ?m]]
    :variables [ws-uuid]}))

(rf/reg-sub
 :worksheet/map-units-settings-entity
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet/map-units-settings-eid ws-uuid]))
 (fn [map-units-settings-eid _]
   (re-entity-from-eid map-units-settings-eid)))

(rp/reg-sub
 :worksheet/map-units-enabled?
 (fn [_ [_ ws-uuid]]
   {:type      :query
    :query     '[:find  ?enabled .
                 :in    $ ?ws-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/table-settings ?t]
                 [?t :table-settings/map-units-settings ?m]
                 [?m :map-units-settings/enabled? ?enabled]]
    :variables [ws-uuid]}))

(rp/reg-sub
 :worksheet/result-table-cell-data
 (fn [_ [_ ws-uuid]]
   {:type  :query
    :query '[:find ?row ?col-uuid ?repeat-id ?value
             :in $ ?ws-uuid
             :where
             [?w :worksheet/uuid ?ws-uuid]
             [?w :worksheet/result-table ?rt]
             [?rt :result-table/rows ?r]

             ;;get row
             [?r :result-row/id ?row]

             ;;get-header
             [?r :result-row/cells ?c]
             [?c :result-cell/header ?h]
             [?h :result-header/group-variable-uuid ?col-uuid]
             [?h :result-header/repeat-id ?repeat-id]

             ;;get value
             [?c :result-cell/value ?value]]
    :variables
    [ws-uuid]}))

(defn- is-directional? [gv-uuid direction]
  (= (d/q '[:find  ?direction .
            :in $ ?gv-uuid
            :where
            [?gv :bp/uuid ?gv-uuid]
            [?gv :group-variable/direction ?direction]]
          @@vms-conn
          gv-uuid)
     direction))

(rf/reg-sub
 :worksheet/result-table-cell-data-direction
 (fn [[_ ws-uuid]]
   [(rf/subscribe [:worksheet/result-table-cell-data ws-uuid])
    (rf/subscribe [:worksheet/multi-value-input-uuids ws-uuid])])
 (fn [[data multi-input-uuids] [_ _ direction]]
   (filterv
    (fn [[_ col-uuid]]
      (or (contains? (set multi-input-uuids) col-uuid)
          (is-directional? col-uuid direction)))
    data)))

(comment
  (rf/subscribe [:worksheet/multi-value-input-uuids "6685957e-39fc-454d-bd3d-6f7dafa5775a"])
  (rf/subscribe [:worksheet/result-table-cell-data-direction "6685957e-39fc-454d-bd3d-6f7dafa5775a" :heading]))

(rf/reg-sub
 :worksheet/output-uuid->result-min-values
 (fn [[_ ws-uuid]]
   [(rf/subscribe [:worksheet/result-table-cell-data ws-uuid])
    (rf/subscribe [:worksheet/output-uuids-filtered ws-uuid])])
 (fn [[result-table-cell-data all-output-uuids] _]
   (reduce
    (fn [acc [_row-id gv-uuid _repeat-id value]]
      (if (contains? (set all-output-uuids) gv-uuid )
        (update acc gv-uuid (fn [min-v]
                              (let [min-float   (js/parseFloat min-v)
                                    value-float (js/parseFloat value)]
                                (min (or min-float ##Inf) value-float))))
        acc))
    {}
    result-table-cell-data)))

(rf/reg-sub
 :worksheet/output-min+max-values
 (fn [[_ ws-uuid]]
   [(rf/subscribe [:worksheet/result-table-cell-data ws-uuid])
    (rf/subscribe [:worksheet/output-uuids-filtered ws-uuid])])
 (fn [[result-table-cell-data all-output-uuids] _]
   (reduce
    (fn [acc [_row-id gv-uuid _repeat-id value]]
      (if (contains? (set all-output-uuids) gv-uuid )
        (update acc gv-uuid (fn [[min-v max-v]]
                              (let [min-float   (js/parseFloat min-v)
                                    max-float   (js/parseFloat max-v)
                                    value-float (js/parseFloat value)]
                                [(min (or min-float ##Inf) value-float)
                                 (max (or max-float ##-Inf) value-float)])))
        acc))
    {}
    result-table-cell-data)))

(rf/reg-sub
 :worksheet/output-uuid->result-max-values
 (fn [[_ ws-uuid]]
   [(rf/subscribe [:worksheet/result-table-cell-data ws-uuid])
    (rf/subscribe [:worksheet/output-uuids-filtered ws-uuid])])
 (fn [[result-table-cell-data all-output-uuids] _]
   (reduce
    (fn [acc [_row-id gv-uuid _repeat-id value]]
      (if (contains? (set all-output-uuids) gv-uuid )
        (update acc gv-uuid (fn [max-v]
                              (let [max-float   (js/parseFloat max-v)
                                    value-float (js/parseFloat value)]
                                (max (or max-float ##-Inf) value-float))))
        acc))
    {}
    result-table-cell-data)))

;; returns headers of table in sorted order
(rf/reg-sub
 :worksheet/result-table-headers-sorted
 (fn [_]
   (rf/subscribe [:vms/group-variable-order]))
 (fn [gv-order [_ ws-uuid]]
   (let [headers @(rf/subscribe [:query
                                 '[:find ?gv-uuid ?repeat-id ?units
                                   :in $ ?ws-uuid
                                   :where
                                   [?w :worksheet/uuid ?ws-uuid]
                                   [?w :worksheet/result-table ?r]
                                   [?r :result-table/headers ?h]
                                   [?h :result-header/repeat-id ?repeat-id]
                                   [?h :result-header/group-variable-uuid ?gv-uuid]
                                   [?h :result-header/units ?units]]
                                 [ws-uuid]])]
     (->> headers
          (sort-by (juxt #(.indexOf gv-order (first %))
                         #(second %)))))))

(rf/reg-sub
 :worksheet/result-table-headers-sorted-direction
 (fn [[_ ws-uuid]]
   [(rf/subscribe [:vms/group-variable-order])
    (rf/subscribe [:worksheet/multi-value-input-uuids ws-uuid])])
 (fn [[gv-order multi-input-uuids] [_ ws-uuid direction]]
   (let [headers @(rf/subscribe [:query
                                 '[:find ?gv-uuid ?repeat-id ?units
                                   :in $ ?ws-uuid
                                   :where
                                   [?w :worksheet/uuid ?ws-uuid]
                                   [?w :worksheet/result-table ?r]
                                   [?r :result-table/headers ?h]
                                   [?h :result-header/repeat-id ?repeat-id]
                                   [?h :result-header/group-variable-uuid ?gv-uuid]
                                   [?h :result-header/units ?units]]
                                 [ws-uuid]])]
     (->> headers
          (filter (fn [[gv-uuid]]
                    (or (contains? (set multi-input-uuids) gv-uuid)
                        (is-directional? gv-uuid direction))))
          (sort-by (juxt #(.indexOf gv-order (first %))
                         #(second %)))))))

(rf/reg-sub
 :worksheet/pivot-table-headers
 (fn [[_ ws-uuid gvs]]
   (rf/subscribe [:query
                  '[:find ?gv-uuid ?repeat-id ?units
                    :in $ ?ws-uuid [?gv-uuid ...]
                    :where
                    [?w :worksheet/uuid ?ws-uuid]
                    [?w :worksheet/result-table ?r]
                    [?r :result-table/headers ?h]
                    [?h :result-header/repeat-id ?repeat-id]
                    [?h :result-header/group-variable-uuid ?gv-uuid]
                    [?h :result-header/units ?units]]
                  [ws-uuid gvs]]))
 (fn [headers [_ _ gvs]]
   (->> headers
        (sort-by (juxt #(.indexOf gvs (first %))
                       #(second %))))))

(rf/reg-sub
 :worksheet/csv-export-headers
 (fn [_]
   (rf/subscribe [:vms/group-variable-order]))
 (fn [gv-order [_ ws-uuid]]
   (->> (d/q '[:find  ?gv-uuid ?repeat-id ?units ?hide-csv
               :in    $ $ws % ?ws-uuid
               :where
               [$ws ?w :worksheet/uuid ?ws-uuid]
               [$ws ?w :worksheet/result-table ?r]
               [$ws ?r :result-table/headers ?h]
               [$ws ?h :result-header/repeat-id ?repeat-id]
               [$ws ?h :result-header/group-variable-uuid ?gv-uuid]
               [$ws ?h :result-header/units ?units]
               (lookup ?gv-uuid ?gv)
               [(get-else $ ?gv :group-variable/hide-csv? false) ?hide-csv]]
             @@vms-conn @@s/conn rules ws-uuid)
        (remove (fn [[_ _ _ hide-csv?]] hide-csv?))
        (sort-by (juxt #(.indexOf gv-order (first %))
                       #(second %))))))
;; returns a map of group-variable uuid to units
(rf/reg-sub
 :worksheet/result-table-units
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet/result-table-headers-sorted ws-uuid]))

 (fn [headers _]
   (into {} (map (juxt first last) headers))))

(rf/reg-sub
 :worksheet/graph-settings
 (fn [[_ ws-uuid]]
   (rf/subscribe [:query '[:find ?gs .
                           :in $ ?ws-uuid
                           :where
                           [?w :worksheet/uuid ?ws-uuid]
                           [?w :worksheet/graph-settings ?gs]]
                  [ws-uuid]]))
 (fn [id _]
   (d/entity @@s/conn id)))

(defn- missing-input? [value]
  (or (nil? value) (empty? value)))

(defn- process-group-for-missing-inputs [worksheet all-inputs missing-inputs? group]
  (when-let [group-variables (:group/group-variables group)]
    (if (:group/repeat? group)
      (let [repeat-ids (d/q '[:find  [?rid ...]
                              :in    $ ?ws-uuid ?group-uuid
                              :where
                              [?w :worksheet/uuid ?ws-uuid]
                              [?w :worksheet/input-groups ?ig]
                              [?ig :input-group/group-uuid ?group-uuid]
                              [?ig :input-group/repeat-id ?rid]]
                            @@s/conn (:worksheet/uuid worksheet) (:bp/uuid group))]
        (doseq [group-variable group-variables
                repeat-id      repeat-ids
                :when          (not (:group-variable/conditionally-set? group-variable))]
          (let [worksheet-value (get-in all-inputs [(:bp/uuid group) repeat-id (:bp/uuid group-variable)])]
            (when (missing-input? worksheet-value)
              (reset! missing-inputs? true)))))
      (doseq [group-variable group-variables
              :when          (not (:group-variable/conditionally-set? group-variable))]
        (let [worksheet-value (get-in all-inputs [(:bp/uuid group) 0 (:bp/uuid group-variable)])]
          (when (missing-input? worksheet-value)
            (reset! missing-inputs? true))))))
  (when-let [children (:group/children group)]
    (doseq [group (filter #(and (all-conditionals-pass? worksheet (:group/conditionals-operator %) (:group/conditionals %))
                                (not (:group/research? %))
                                (not (:group/hidden? %)))
                          children)]
      (process-group-for-missing-inputs worksheet all-inputs missing-inputs? group))))

(rf/reg-sub
 :worksheet/missing-inputs?
 (fn [[_ ws-uuid]]
   [(rf/subscribe [:worksheet/modules ws-uuid])
    (rf/subscribe [:worksheet ws-uuid])])
 (fn [[modules worksheet] [_ ws-uuid]]
   (let [all-inputs      @(rf/subscribe [:worksheet/all-inputs ws-uuid])
         missing-inputs? (atom false)]
     (doseq [module    modules
             submodule (filter #(and (= (:submodule/io %) :input)
                                     (all-conditionals-pass? worksheet (:submodule/conditionals-operator %) (:submodule/conditionals %)))
                               (:module/submodules module))
             group     (filter #(and (all-conditionals-pass? worksheet (:group/conditionals-operator %) (:group/conditionals %))
                                     (not (:group/research? %))
                                     (not (:group/hidden? %)))
                               (:submodule/groups submodule))]
       (process-group-for-missing-inputs worksheet all-inputs missing-inputs? group))
     @missing-inputs?)))

(rf/reg-sub
 :worksheet/some-outputs-entered?
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet/output-uuids-filtered ws-uuid]))

 (fn [all-output-uuids [_ _ws-uuid module-id submodule-slug]]
   (if (seq all-output-uuids)
     (let [submodule       @(rf/subscribe [:wizard/*submodule module-id submodule-slug :output])
           groups          (:submodule/groups submodule)
           group-variables (set (flatten (map #(map :bp/uuid (:group/group-variables %)) groups)))]
       (boolean (seq (set/intersection (set all-output-uuids) group-variables))))
     false)))

(rf/reg-sub
 :worksheet/first-output-submodule-slug
 (fn [_]
   (rf/subscribe [:vms/pull-with-attr :module/name]))
 (fn [modules [_ module]]
   (when module
     (let [module     (first (filter (fn [{m-name :module/name}]
                                       (= (->str module) (str/lower-case m-name))) modules))
           submodules @(rf/subscribe [:vms/pull-children :module/submodules (:db/id module)])]
       (as-> submodules $
         (filter #(= :output (:submodule/io %)) $)
         (sort-by :submodule/order $)
         (first $)
         (:submodule/name $)
         (->kebab $))))))

(rp/reg-sub
 :worksheet/input-gv-uuid+value+units
 (fn [_ [_ ws-uuid row-id]]
   {:type  :query
    :query '[:find  ?gv-uuid ?value ?units
             :in $ ?ws-uuid ?row-id
             :where
             [?ws :worksheet/uuid ?ws-uuid]
             [?ws :worksheet/input-groups ?ig]
             [?ws :worksheet/result-table ?t]
             [?t  :result-table/rows ?rr]
             [?rr :result-row/id ?row-id]
             [?rr :result-row/cells ?c]

             ;; Filter only input variables
             [?ig :input-group/inputs ?i]
             [?i  :input/group-variable-uuid ?gv-uuid]

             ;; Get  gv-uuid, value and units
             [?rh :result-header/group-variable-uuid ?gv-uuid]
             [?rh :result-header/units ?units]
             [?c  :result-cell/header ?rh]
             [?c  :result-cell/value ?value]]
    :variables [ws-uuid row-id]}))

(rp/reg-sub
 :worksheet/output-gv-uuid+value+units
 (fn [_ [_ ws-uuid row-id]]
   {:type  :query
    :query '[:find  ?gv-uuid ?value ?units
             :in $ ?ws-uuid ?row-id
             :where
             [?ws :worksheet/uuid ?ws-uuid]
             [?ws :worksheet/outputs ?o]
             [?ws :worksheet/result-table ?t]
             [?t  :result-table/rows ?rr]
             [?rr :result-row/id ?row-id]
             [?rr :result-row/cells ?c]

             ;; Filter only output variables
             [?o  :output/group-variable-uuid  ?gv-uuid]

             ;; Get  gv-uuid, value and units
             [?rh :result-header/group-variable-uuid ?gv-uuid]
             [?rh :result-header/units ?units]
             [?c  :result-cell/header ?rh]
             [?c  :result-cell/value ?value]]
    :variables [ws-uuid row-id]}))

(rf/reg-sub
 :worksheet/resolve-enum-value

 (fn [_ [_ variable-eid value]]
   (let [variable                (d/pull @@vms-conn '[{:variable/list [* {:list/options [*]}]}] variable-eid)
         {v-list :variable/list} variable
         {options :list/options} v-list
         options                 (index-by :list-option/value options)]
     (if-let [option (get options value)]
       @(<t (:list-option/translation-key option))
       value))))

(rf/reg-sub
 :worksheet/resolve-enum-order

 (fn [_ [_ list-eid value]]
   (let [list-entity             (d/touch (d/entity @@vms-conn list-eid))
         {options :list/options} list-entity
         options                 (index-by :list-option/value options)
         option                  (get options value)]
     (:list-option/order option))))

(rf/reg-sub
 :worksheet/result-table-gv-uuid->units
 (fn [_ [_ ws-uuid]]
   (let [gv-uuids+units (d/q '[:find  ?gv-uuid ?units
                               :in $ ?ws-uuid
                               :where
                               [?ws :worksheet/uuid ?ws-uuid]
                               [?ws :worksheet/result-table ?t]
                               [?t  :result-table/headers ?h]
                               [?h  :result-header/group-variable-uuid ?gv-uuid]
                               [?h  :result-header/units ?units]]
                             @@s/conn ws-uuid)]
     (into {} gv-uuids+units))))

(rf/reg-sub
 :worksheet/inputs-in-domain
 (fn [_ [_ ws-uuid domain-uuid]]
   (d/q '[:find  [?i ...]
          :in    $ $ws % ?ws-uuid ?domain-uuid
          :where
          [$ws ?w :worksheet/uuid ?ws-uuid]
          [$ws ?w :worksheet/input-groups ?g]
          [$ws ?g :input-group/group-uuid ?group-uuid]
          [$ws ?g :input-group/repeat-id ?repeat-id]
          [$ws ?g :input-group/inputs ?i]
          [$ws ?i :input/value ?value]
          (lookup ?gv-uuid ?gv)
          (group-variable _ ?gv ?v)
          [?v :variable/domain-uuid ?domain-uuid]]
        @@vms-conn @@s/conn rules ws-uuid domain-uuid)))

(rf/reg-sub
 :worksheet/output-directions
 (fn [_ [_ ws-uuid]]
   (d/q '[:find  [?direction ...]
          :in    $ $ws % ?ws-uuid
          :where
          [$ws ?w :worksheet/uuid ?ws-uuid]
          [$ws ?w :worksheet/outputs ?o]
          [$ws ?o :output/group-variable-uuid ?gv-uuid]
          [$ws ?o :output/enabled? true]
          (lookup ?gv-uuid ?gv)
          [?gv :group-variable/direction ?direction]]
        @@vms-conn @@s/conn rules ws-uuid)))

(rf/reg-sub
 :worksheet/pivot-table-field-uuids
 (fn [_ [_ pivot-table-id]]
   (d/q '[:find  ?gv-uuid ?order
          :keys  gv-uuid order
          :in    $ ?p
          :where
          [?p :pivot-table/columns ?c]
          [?c :pivot-column/type :field]
          [?c :pivot-column/group-variable-uuid ?gv-uuid]
          [?c :pivot-column/order ?order]]
        @@vms-conn pivot-table-id)))

(rf/reg-sub
 :worksheet/pivot-table-fields
 (fn [[_ pivot-table-id]]
   (rf/subscribe [:worksheet/pivot-table-field-uuids pivot-table-id]))
 (fn [fields]
   (map :gv-uuid (sort-by :order fields))))

(rf/reg-sub
 :worksheet/pivot-table-values
 (fn [_ [_ pivot-table-id]]
   (d/q '[:find  ?gv-uuid ?function
          :in    $ ?p
          :where
          [?p :pivot-table/columns ?c]
          [?c :pivot-column/type :value]
          [?c :pivot-column/group-variable-uuid ?gv-uuid]
          [?c :pivot-column/function ?function]]
        @@vms-conn pivot-table-id)))

(rf/reg-sub
 :worksheet/pivot-tables
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet/modules ws-uuid]))
 (fn [modules]
   (mapcat :module/pivot-tables modules)))

(rp/reg-sub
 :worksheet/version
 (fn [_ [_ ws-uuid]]
   {:type      :query
    :query     '[:find  ?version .
                 :in    $ ?ws-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/version ?version]]
    :variables [ws-uuid]}))

(rf/reg-sub
 :worksheet/search-tables
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet/modules ws-uuid]))
 (fn [modules]
   (mapcat :module/search-tables modules)))


(rf/reg-sub
 :worksheet/output-eid
 (fn [_ [_ ws-uuid gv-uuid]]
   (d/q '[:find  ?o .
          :in  $ ?ws-uuid ?gv-uuid
          :where
          [?w :worksheet/uuid ?ws-uuid]
          [?w :worksheet/outputs ?o]
          [?o :output/group-variable-uuid ?gv-uuid]]
        @@s/conn
        ws-uuid
        gv-uuid)))

(rf/reg-sub
 :worksheet/output-unit-uuid
 (fn [_ [_ ws-uuid gv-uuid]]
   (d/q '[:find  ?unit-uuid .
          :in  $ ?ws-uuid ?gv-uuid
          :where
          [?w :worksheet/uuid ?ws-uuid]
          [?w :worksheet/outputs ?o]
          [?o :output/group-variable-uuid ?gv-uuid]
          [?o :output/units-uuid ?unit-uuid]]
        @@s/conn
        ws-uuid
        gv-uuid)))

(rf/reg-sub
 :worksheet/repeat-groups?
 (fn [_ [_ ws-uuid]]
   (some pos? (d/q '[:find [?rid ...]
                     :in  $ ?ws-uuid
                     :where
                     [?w :worksheet/uuid ?ws-uuid]
                     [?w :worksheet/input-groups ?g]
                     [?g :input-group/group-uuid ?g-uuid]
                     [?g :input-group/repeat-id ?rid]]
                   @@s/conn ws-uuid))))

(rf/reg-sub
 :worksheet/should-keep-input?
 (fn [[_ _ws-uuid gv-uuid]]
   (rf/subscribe [:vms/group-variable-heirarchy gv-uuid]))
 (fn [hierarchy-eids [_ ws-uuid _gv-uuid]]
   (let [[submodule & groups] (map #(d/pull @@vms-conn
                                            '[:db/id
                                              :group/name
                                              :group/translation-key
                                              :group/conditionals-operator
                                              :submodule/name
                                              :submodule/conditionals-operator]
                                            (:db/id %))
                                   hierarchy-eids)]
     (and
      (true? @(rf/subscribe [:wizard/show-submodule?
                             ws-uuid
                             (:db/id submodule)
                             (:submodule/conditionals-operator submodule)]))

      (every? true?
              (map #(deref (rf/subscribe [:wizard/show-group?
                                          ws-uuid
                                          (:db/id %)
                                          (:group/conditionals-operator %)]))
                   groups))))))

(rf/reg-sub
 :worksheet/input-eids-to-delete
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet/all-input-entities ws-uuid]))
 (fn [inputs [_ ws-uuid]]
   (prn inputs)
   (map :db/id
        (remove (fn [{group-variable-uuid :input/group-variable-uuid}]
                  @(rf/subscribe [:worksheet/should-keep-input? ws-uuid group-variable-uuid]))
                inputs))))

(comment
  (rf/subscribe [:worksheet/input-eids-to-delete "693c7777-a8a5-4948-9169-c99f0d9654c3"])

  )
