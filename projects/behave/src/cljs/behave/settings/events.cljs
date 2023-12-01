(ns behave.settings.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :setting/cache-unit-preference
 (fn [_ [_ category v-uuid unit-uuid]]
   {:fx [[:dispatch [:settings/set [:units category v-uuid :unit-uuid] unit-uuid]]
         [:dispatch [:local-storage/update-in [:units v-uuid :unit-uuid] unit-uuid]]]}))

(rf/reg-event-fx
 :setting/cache-decimal-preference
 (fn [_ [_ category v-uuid decimal]]
   {:fx [[:dispatch [:settings/set [:units category v-uuid :decimals] decimal]]
         [:dispatch [:local-storage/update-in [:units v-uuid :decimals] decimal]]]}))
