(ns behave.tool.events
  (:require [re-frame.core :as rf]
            [behave.tool.solver :refer [solve-tool]]))

(def db-tool [:state :tool])

(rf/reg-event-fx
 :tool/upsert-input-value
 (rf/path db-tool)
 (fn [{:keys [db]} [_ tool-uuid subtool-uuid variable-uuid value auto-compute?]]
   (cond-> {:db (assoc-in db
                          [:data
                           tool-uuid
                           subtool-uuid
                           :tool/inputs
                           variable-uuid
                           :input/value]
                          value)}
     auto-compute? (assoc :fx [[:dispatch [:tool/solve tool-uuid subtool-uuid]]]))))

(rf/reg-event-fx
 :tool/update-input-units
 (rf/path db-tool)
 (fn [{:keys [db]} [_ tool-uuid subtool-uuid variable-uuid unit-uuid auto-compute?]]
   (cond-> {:db (assoc-in db
                          [:data
                           tool-uuid
                           subtool-uuid
                           :tool/inputs
                           variable-uuid
                           :input/units-uuid]
                          unit-uuid)}
     auto-compute? (assoc :fx [[:dispatch [:tool/solve tool-uuid subtool-uuid]]]))))

(rf/reg-event-db
 :tool/upsert-outupt-value
 (rf/path db-tool)
 (fn [db [_ tool-uuid subtool-uuid variable-uuid value]]
   (assoc-in db
             [:data
              tool-uuid
              subtool-uuid
              :tool/outputs
              variable-uuid]
             value)))

(rf/reg-event-fx
 :tool/update-output-units
 (rf/path db-tool)
 (fn [{:keys [db]} [_ tool-uuid subtool-uuid variable-uuid unit-uuid auto-compute?]]
   (cond-> {:db (assoc-in db
                          [:data
                           tool-uuid
                           subtool-uuid
                           :tool/outputs
                           variable-uuid
                           :output/units-uuid-uuid]
                          unit-uuid)}
     auto-compute? (assoc :fx [[:dispatch [:tool/solve tool-uuid subtool-uuid]]]))))

(rf/reg-event-db
 :tool/close-tool-selector
 (fn [db _]
   (assoc-in db [:state :sidebar :*tools-or-settings] nil)))

(rf/reg-event-db
 :tool/close-tool
 (rf/path db-tool)
 (fn [db _]
   (-> db
       (dissoc :selected-tool)
       (dissoc :selected-subtool))))

(rf/reg-event-fx
 :tool/select-tool
 (rf/path db-tool)
 (fn [{:keys [db]} [_ tool-uuid]]
   {:db (-> db
            (dissoc :selected-subtool)
            (assoc :selected-tool tool-uuid))
    :fx [[:dispatch [:tool/close-tool-selector]]]}))

(rf/reg-event-db
 :tool/select-subtool
 (rf/path db-tool)
 (fn [db [_ subtool-uuid]]
   (assoc db :selected-subtool subtool-uuid)))

(rf/reg-event-db
 :tool/solve
 (rf/path db-tool)
 (fn [db [_ selected-tool selected-subtool]]
   (let [results (solve-tool selected-tool selected-subtool)]
     (assoc-in db [:data selected-tool selected-subtool :tool/outputs] results))))
