(ns behave.components.table-settings.events
  (:require [datascript.core               :as d]
            [re-frame.core                 :as rf]
            [re-posh.core                  :as rp]
            [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-db
 :table-settings/toggle
 (fn [db _]
   (update-in db [:state :wizard :show-table-settings?] not)))

(rf/reg-event-db
 :table-settings/close
 (fn [db _]
   (assoc-in db [:state :wizard :show-table-settings?] false)))

(def ^:private matrix-table-attrs
  [:table-settings/row-group-variable-uuid
   :table-settings/col-group-variable-uuid
   :table-settings/submatrix-group-variable-uuid])

(rp/reg-event-fx
 :table-settings/update-attr
 [(rp/inject-cofx :ds)
  (rf/inject-cofx ::inject/sub (fn [[_ ws-uuid attr]] [:table-settings/gv-uuid-for-attr ws-uuid attr]))
  (rf/inject-cofx ::inject/sub (fn [[_ ws-uuid _ value]] [:table-settings/attr-holding-gv-uuid ws-uuid value matrix-table-attrs]))]
 (fn [{ds               :ds
       original-gv-uuid :table-settings/gv-uuid-for-attr
       attr-to-swap     :table-settings/attr-holding-gv-uuid} [_ ws-uuid attr value]]
   (when-let [t (first (d/q '[:find [?t]
                              :in $ ?uuid
                              :where
                              [?w :worksheet/uuid ?uuid]
                              [?w :worksheet/table-settings ?t]]
                            ds
                            ws-uuid))]
     (if (and attr-to-swap (not= attr-to-swap attr))
       {:transact [(assoc {:db/id t} attr value)
                   (assoc {:db/id t} attr-to-swap original-gv-uuid)]}
       {:transact [(assoc {:db/id t} attr value)]}))))
