(ns behave.tool.events
  (:require [re-frame.core :as rf]))

(def db-here [:state :tool])

(rf/reg-event-db
 :tool/upsert-input-value
 (rf/path db-here)
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
 (rf/path db-here)
 (fn [db _]
   (-> db
       (dissoc :selected-tool)
       (dissoc :selected-subtool))))

(rf/reg-event-fx
 :tool/select-tool
 (fn [_ [_ tool-uuid]]
   {:fx [[:dispatch [:state/set [:tool :selected-subtool] nil]]
         [:dispatch [:state/set [:tool :selected-tool] tool-uuid]]
         [:dispatch [:tool/close-tool-selector]]
         [:dispatch [:help/select-tab {:tab :tools}]]]}))

(rf/reg-event-db
 :tool/select-subtool
 (rf/path db-here)
 (fn [db [_ subtool-uuid]]
   (assoc db :selected-subtool subtool-uuid)))

;;TODO update compute to actually run the selected subtool's compute fn
(rf/reg-event-fx
 :tool/compute
 (fn [_ [_ selected-tool selected-subtool]]
   (let [output-variables @(rf/subscribe [:subtool/output-variables selected-subtool])
         effects          (mapv (fn [variable]
                                  [:dispatch [:state/set [:tool
                                                          :data
                                                          selected-tool
                                                          selected-subtool
                                                          :outputs
                                                          (:bp/uuid variable)]
                                              (str "computed subtool:" selected-subtool)]])
                                output-variables)]
     {:fx effects})))
