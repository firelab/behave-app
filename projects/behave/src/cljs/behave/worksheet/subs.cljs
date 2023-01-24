(ns behave.worksheet.subs
  (:require [clojure.string   :as str]
            [re-posh.core     :as rp]
            [re-frame.core    :as rf]))

;; Retrieve all worksheet UUID's
(rp/reg-sub
 :worksheet/all
 (fn [_ _]
   {:type  :query
    :query '[:find  ?created ?uuid
             :where [?ws :worksheet/uuid ?uuid]
                    [?ws :worksheet/created ?created]]}))

;; Retrieve latest worksheet UUID
(rf/reg-sub
 :worksheet/latest
 (fn [_]
   (rf/subscribe [:worksheet/all]))
 (fn [all-worksheets [_]]
   (last (last (sort-by first all-worksheets)))))

;; Retrieve latest worksheet UUID
(rp/reg-sub
 :worksheet/modules
 (fn [_ [_ ws-uuid]]
   {:type  :query
    :query '[:find  [?modules ...]
             :in    $ ?ws-uuid
             :where [?w :worksheet/uuid ?ws-uuid]
                    [?w :worksheet/modules ?modules]]
    :variables [ws-uuid]}))

(rp/reg-sub
  :worksheet/get-attr
  (fn [_ [_ ws-uuid attr]]
    {:type      :query
     :query     '[:find [?value ...]
                  :in    $ ?ws-uuid ?attr
                  :where [?w :worksheet/uuid ?ws-uuid]
                         [?w ?attr ?value]]
     :variables [ws-uuid attr]}))

;; Get state of a particular output
(rp/reg-sub
 :worksheet/output-enabled?
 (fn [_ [_ ws-uuid variable-uuid]]
   {:type  :query
    :query '[:find  [?enabled]
             :in    $ ?ws-uuid ?var-uuid
             :where [?w :worksheet/uuid ?ws-uuid]
                    [?w :worksheet/outputs ?o]
                    [?o :output/group-variable-uuid ?var-uuid]
                    [?o :output/enabled? ?enabled]]
    :variables [ws-uuid variable-uuid]}))

;; Get the value of a particular input
(rp/reg-sub
 :worksheet/input
 (fn [_ [_ ws-uuid group-uuid repeat-id group-variable-uuid]]
   {:type  :query
    :query '[:find  [?value]
             :in    $ ?ws-uuid ?group-uuid ?repeat-id ?group-var-uuid
             :where [?w :worksheet/uuid ?ws-uuid]
                    [?w :worksheet/input-groups ?g]
                    [?g :input-group/group-uuid ?group-uuid]
                    [?g :input-group/repeat-id ?repeat-id]
                    [?g :input-group/inputs ?i]
                    [?i :input/group-variable-uuid ?group-var-uuid]
                    [?i :input/value ?value]]
    :variables [ws-uuid group-uuid repeat-id group-variable-uuid]}))

;; Find groups matching a group-uuid
(rp/reg-sub
 :worksheet/repeat-groups
 (fn [_ [_ ws-uuid group-uuid]]
   {:type  :query
    :query '[:find  [?g ...]
             :in    $ ?ws-uuid ?group-uuid
             :where [?w :worksheet/uuid ?ws-uuid]
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
 :worksheet/multi-value-input-uuids
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet/input-id+value ws-uuid]))

 (fn [inputs _query]
   (->> inputs
        (filter (fn multiple-values? [[_uuid value]]
                  (> (count (str/split value #",|\s"))
                     1)))
        (map first))))

(rp/reg-sub
 :worksheet/all-output-uuids
 (fn [_ [_ ws-uuid]]
   {:type      :query
    :query     '[:find  [?uuid ...]
                 :in    $ ?ws-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/outputs ?o]
                 [?o :output/group-variable-uuid ?uuid]
                 [?o :output/enabled? true]]
    :variables [ws-uuid]}))

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
 (fn [_ [_ ws-uuid]]
   {:type  :query
    :query '[:find ?group-var-uuid ?min ?max
             :in   $ ?ws-uuid
             :where
             [?w :worksheet/uuid ?ws-uuid]
             [?w :worksheet/graph-settings ?g]
             [?g :graph-settings/y-axis-limits ?y]
             [?y :y-axis-limit/group-variable-uuid ?group-var-uuid]
             [?y :y-axis-limit/min ?min]
             [?y :y-axis-limit/max ?max]]
    :variables [ws-uuid]}))

(rf/reg-sub
 :worksheet/results-tab-selected
 (fn [_ _]
   :notes)) ;TODO update when more results tabs are are added.

(rp/reg-sub
 :worksheet/notes
 (fn [_ [_ ws-uuid]]
   {:type      :query
    :query     '[:find ?n ?name ?content ?s-uuid
                 :in   $ ?ws-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/notes ?n]
                 [?n :note/submodule ?s-uuid]
                 [?n :note/name ?name]
                 [?n :note/content ?content]]
    :variables [ws-uuid]}))

(comment
  (let [ws-uuid @(rf/subscribe [:worksheet/latest])]
    (rf/subscribe [:worksheet/graph-settings-y-axis-limits ws-uuid])))

(comment
  (let [ws-uuid @(rf/subscribe [:worksheet/latest])]
    @(rf/subscribe [:worksheet/table-settings-enabled? ws-uuid]))

  (let [ws-uuid @(rf/subscribe [:worksheet/latest])]
    (rf/subscribe [:worksheet/all-output-names ws-uuid]))

  (let [ws-uuid @(rf/subscribe [:worksheet/latest])]
    (rf/subscribe [:worksheet/get-attr ws-uuid :graph-settings/enabled?]))
  )
