(ns behave.tool.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 :tool/upsert-input-value
 (fn [db [_ tool-uuid subtool-uuid variable-uuid value]]
   (update-in db
              [:tool
               :data
               tool-uuid
               subtool-uuid
               :inputs
               variable-uuid]
              value)))

(rf/reg-event-db
 :tool/close-tool-selector
 (rf/path [:state :sidebar :*tools-or-settings])
 (fn [_] nil))

(rf/reg-event-fx
 :tool/close-tool
 (fn [_ _]
   {:fx [[:dispatch [:state/set [:tool :selected-tool] nil]]
         [:dispatch [:state/set [:tool :selected-subtool] nil]]]}))

(rf/reg-event-fx
 :tool/select-tool
 (fn [_ [_ tool-uuid]]
   {:fx [[:dispatch [:state/set [:tool :selected-subtool] nil]]
         [:dispatch [:state/set [:tool :selected-tool] tool-uuid]]
         [:dispatch [:tool/close-tool-selector]]
         [:dispatch [:help/select-tab {:tab :tools}]]]}))

(rf/reg-event-db
 :tool/select-subtool
 (fn [db [_ subtool-uuid]]
   (update-in db [:state :tool :selected-subtool] subtool-uuid)))

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
