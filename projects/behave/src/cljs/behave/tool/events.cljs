(ns behave.tool.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :tool/upsert-input-value
 (fn [_ [_ tool-uuid subtool-uuid variable-uuid value]]
   {:fx [[:dispatch [:state/set [:tool
                                 :data
                                 tool-uuid
                                 subtool-uuid
                                 :inputs
                                 variable-uuid]
                     value]]]}))
(rf/reg-event-fx
 :tool/close-tool-selector
 (fn [_ _]
   {:fx [[:dispatch [:state/set [:sidebar :*tools-or-settings] nil]]]}))

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

(rf/reg-event-fx
 :tool/select-subtool
 (fn [_ [_ subtool-uuid]]
   {:fx [[:dispatch [:state/set [:tool :selected-subtool] subtool-uuid]]]}))

;TODO update compute to actually run the selected subtool's compute fn
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
