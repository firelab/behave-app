(ns behave.worksheet.subs
  (:require [behave.store    :as s]
            [clojure.string  :as str]
            [datascript.core :as d]
            [re-posh.core    :as rp]
            [re-frame.core   :as rf]
            [behave.store    :as s]
            [datascript.core :as d]))

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

;; Retrieve worksheet
(rf/reg-sub
 :worksheet
 (fn [_ [_ ws-uuid]]
   (d/entity @@s/conn [:worksheet/uuid ws-uuid])))

;; Retrieve latest worksheet UUID
(rp/reg-sub
 :worksheet/modules
 (fn [_ [_ ws-uuid]]
   {:type      :query
    :query     '[:find  [?modules ...]
                 :in    $ ?ws-uuid
                 :where [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/modules ?modules]]
    :variables [ws-uuid]}))

;; Get state of a particular output
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
   {:type      :query
    :query     '[:find  [?enabled]
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
   {:type      :query
    :query     '[:find  [?value]
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
   {:type      :query
    :query     '[:find  [?g ...]
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

(rp/reg-sub
 :worksheet/result-table-cell-data
 (fn [_ [_ ws-uuid]]
   {:type  :query
    :query '[:find ?row ?col-uuid ?value
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

             ;;get value
             [?c :result-cell/value ?value]]
    :variables [ws-uuid]}))

;; returns headers of table in sorted order
(rf/reg-sub
 :worksheet/result-table-headers-sorted
 (fn [_ [_ ws-uuid]]
   (let [headers @(rf/subscribe [:query
                                 '[:find ?order ?uuid ?units
                                   :in $ ?ws-uuid
                                   :where
                                   [?w :worksheet/uuid ?ws-uuid]
                                   [?w :worksheet/result-table ?r]
                                   [?r :result-table/headers ?h]
                                   [?h :result-header/order ?order]
                                   [?h :result-header/group-variable-uuid ?uuid]
                                   [?h :result-header/units ?units]]
                                 [ws-uuid]])]
     (->> headers
          (sort-by first)))))

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

(comment
  (let [ws-uuid @(rf/subscribe [:worksheet/latest])]
    (rf/subscribe [:worksheet/graph-settings-y-axis-limits ws-uuid]))

  (let [ws-uuid @(rf/subscribe [:worksheet/latest])]
    (:y-axis-limit/min (first (:graph-settings/y-axis-limits @(rf/subscribe [:worksheet/graph-settings ws-uuid])))))
  )

(comment
  (let [ws-uuid @(rf/subscribe [:worksheet/latest])]
    @(rf/subscribe [:worksheet/table-settings-enabled? ws-uuid]))

  (let [ws-uuid @(rf/subscribe [:worksheet/latest])]
    (rf/subscribe [:worksheet/all-output-names ws-uuid]))

  (let [ws-uuid @(rf/subscribe [:worksheet/latest])]
    (rf/subscribe [:worksheet/get-attr ws-uuid :graph-settings/enabled?])))

(rf/reg-sub
 :worksheet/all-inputs-entered?
 (fn [_ [_ ws-uuid module-id submodule]]
   (let [submodule                             @(rf/subscribe [:wizard/*submodule module-id submodule :input])
         groups                                @(rf/subscribe [:wizard/groups (:db/id submodule)])
         groups-repeat                         (filter #(true? (:group/repeat? %)) groups)
         groups-not-repeat                     (remove #(true? (:group/repeat? %)) groups)
         all-inputs                            @(rf/subscribe [:worksheet/all-inputs ws-uuid])
         groups-not-repeat-all-values-entered? (->> (for [group    groups-not-repeat
                                                          variable (:group/group-variables group)
                                                          :let     [group-uuid (:bp/uuid group)
                                                                    var-uuid   (:bp/uuid variable)]]
                                                      (get-in all-inputs [group-uuid 0 var-uuid]))
                                                    (every? seq))
         groups-repeat-all-values-entered?     (every? (fn [group]
                                                         (let [group-uuid   (:bp/uuid group)
                                                               vars-needed  (* (count (:group/group-variables group))
                                                                               (count @(rf/subscribe [:worksheet/group-repeat-ids ws-uuid group-uuid])))
                                                               vars-entered (reduce (fn [acc [_repeat-id variables]]
                                                                                      (+ acc (count (filter (fn has-value? [[_variable-id val]]
                                                                                                              (seq val))
                                                                                                            variables))))
                                                                                    0
                                                                                    (get all-inputs group-uuid))]
                                                           (= vars-needed vars-entered)))
                                                       groups-repeat)]

     (and groups-not-repeat-all-values-entered? groups-repeat-all-values-entered?))))

(comment
  (let [ws-id     @(rf/subscribe [:worksheet/latest])
        module    @(rf/subscribe [:wizard/*module "contain"])
        module-id (:db/id module)]
    (rf/subscribe [:worksheet/all-inputs-entered? ws-id  module-id "suppression"])))

(rp/reg-sub
 :worksheet/furthest-visited-step
 (fn [_ [_ ws-uuid]]
   {:type      :query
    :query     '[:find  ?furthest-visited-step .
                 :in    $ ?ws-uuid
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/furthest-visited-step ?furthest-visited-step]]
    :variables [ws-uuid]}))
