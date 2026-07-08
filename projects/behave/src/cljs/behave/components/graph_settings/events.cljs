(ns behave.components.graph-settings.events
  (:require [datascript.core               :as d]
            [re-frame.core                 :as rf]
            [re-posh.core                  :as rp]
            [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-db
 :graph-settings/toggle
 (fn [db _]
   (update-in db [:state :wizard :show-graph-settings?] not)))

(def ^:private graph-axis-attrs
  [:graph-settings/x-axis-group-variable-uuid
   :graph-settings/z-axis-group-variable-uuid
   :graph-settings/z2-axis-group-variable-uuid])

(rp/reg-event-fx
 :graph-settings/update-attr
 [(rp/inject-cofx :ds)
  (rf/inject-cofx ::inject/sub (fn [[_ ws-uuid attr]] [:graph-settings/gv-uuid-for-attr ws-uuid attr]))
  (rf/inject-cofx ::inject/sub (fn [[_ ws-uuid _ value]] [:graph-settings/attr-holding-gv-uuid ws-uuid value graph-axis-attrs]))]
 (fn [{ds               :ds
       original-gv-uuid :graph-settings/gv-uuid-for-attr
       attr-to-swap     :graph-settings/attr-holding-gv-uuid} [_ ws-uuid attr value]]
   (when-let [g (first (d/q '[:find [?g]
                              :in $ ?uuid
                              :where
                              [?w :worksheet/uuid ?uuid]
                              [?w :worksheet/graph-settings ?g]]
                            ds
                            ws-uuid))]
     (if (and attr-to-swap (not= attr-to-swap attr))
       {:transact [(assoc {:db/id g} attr value)
                   (assoc {:db/id g} attr-to-swap original-gv-uuid)]}
       {:transact [(assoc {:db/id g} attr value)]}))))
