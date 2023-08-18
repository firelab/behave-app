(ns behave.tool.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :tool/upsert-input-value
 (fn [_ [_ tool-uuid subtool-uuid variable-uuid value]]
   {:fx [[:dispatch [:state/set
                     [:tool :data tool-uuid subtool-uuid :inputs variable-uuid]
                     value]]]}))
(rf/reg-event-fx
 :tool/close-tool-selector
 (fn [_ _]
   {:fx [[:dispatch [:state/set [:sidebar :*tools-or-settings] nil]]]}))

(rf/reg-event-fx
 :tool/select-tool
 (fn [_ [_ tool-uuid]]
   {:fx [[:dispatch [:state/set [:tool :selected-subtool] nil]]
         [:dispatch [:state/set [:tool :selected-tool] tool-uuid]]
         [:dispatch [:tool/close-tool-selector]]]}))

(rf/reg-event-fx
 :tool/select-subtool
 (fn [_ [_ subtool-uuid]]
   {:fx [[:dispatch [:state/set [:tool :selected-subtool] subtool-uuid]]]}))
