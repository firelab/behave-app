(ns behave.tool.events
  (:require [re-frame.core :as rf]))

(def db-tool [:state :tool])

(rf/reg-event-db
 :tool/upsert-input-value
 (rf/path db-tool)
 (fn [db [_ tool-uuid subtool-uuid variable-uuid value]]
   (assoc-in db
             [:data
              tool-uuid
              subtool-uuid
              :inputs
              variable-uuid]
             value)))

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
    :fx [[:dispatch [:tool/close-tool-selector]]
         [:dispatch [:help/select-tab {:tab :tools}]]]}))

(rf/reg-event-db
 :tool/select-subtool
 (rf/path db-tool)
 (fn [db [_ subtool-uuid]]
   (assoc db :selected-subtool subtool-uuid)))

;;TODO update compute to actually run the selected subtool's compute fn
(rf/reg-event-db
 :tool/compute
 (rf/path db-tool)
 (fn [db [_ selected-tool selected-subtool]]
   (let [output-variables @(rf/subscribe [:subtool/output-variables selected-subtool])]
     (reduce (fn [db variable]
               (assoc-in db [:data
                             selected-tool
                             selected-subtool
                             :outputs
                             (:bp/uuid variable)]
                         (str "computed subtool:" selected-subtool)))
             db
             output-variables))))
