(ns behave.components.graph-settings.subs
  (:require [re-frame.core :refer [reg-sub]]
            [re-posh.core  :as rp]))

(reg-sub
 :graph-settings/show?
 (fn [db _]
   (get-in db [:state :wizard :show-graph-settings?] false)))

(rp/reg-sub
 :graph-settings/get-attr
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
 :graph-settings/axis-group-variable-uuid
 (fn [_ [_ ws-uuid attr]]
   {:type      :query
    :query     '[:find  ?gv-uuid .
                 :in    $ ?ws-uuid ?attr
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/graph-settings ?gs]
                 [?gs ?attr ?gv-uuid]]
    :variables [ws-uuid attr]}))

(rp/reg-sub
 :graph-settings/axis-attr-in
 (fn [_ [_ ws-uuid gv-uuid attrs]]
   {:type      :query
    :query     '[:find  ?attr .
                 :in    $ ?ws-uuid ?gv-uuid [?attr ...]
                 :where
                 [?w :worksheet/uuid ?ws-uuid]
                 [?w :worksheet/graph-settings ?gs]
                 [?gs ?attr ?gv-uuid]]
    :variables [ws-uuid gv-uuid attrs]}))
