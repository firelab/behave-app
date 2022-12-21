(ns behave.worksheet.subs
  (:require [re-frame.core :as rf]
            [re-posh.core  :as rp]))

; Retrieve all worksheet UUID's
(rp/reg-sub
 :worksheet/all
 (fn [_ _]
   {:type  :query
    :query '[:find  ?created ?uuid
             :where [?ws :worksheet/uuid ?uuid]
                    [?ws :worksheet/created ?created]]}))

; Retrieve latest worksheet UUID
(rf/reg-sub
 :worksheet/latest
 (fn [_]
   (rf/subscribe [:worksheet/all]))
 (fn [all-worksheets [_]]
   (last (last (sort-by first all-worksheets)))))

; Retrieve latest worksheet UUID
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

; Get state of a particular output
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

; Get the value of a particular input
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

; Find groups matching a group-uuid
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

(rp/reg-sub
 :worksheet/all-outputs
 (fn [_ [_ ws-uuid]]
   {:type  :query
    :query '[:find  [?uuid ...]
             :in    $ ?ws-uuid
             :where [?w :worksheet/uuid ?ws-uuid]
                    [?w :worksheet/outputs ?o]
                    [?o :output/group-variable-uuid ?uuid]
                    [?o :output/enabled? true]]
    :variables [ws-uuid]}))
