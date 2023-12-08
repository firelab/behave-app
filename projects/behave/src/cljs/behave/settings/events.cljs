(ns behave.settings.events
  (:require [re-frame.core :as rf]
            [vimsical.re-frame.cofx.inject :as inject]))

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

(rf/reg-event-fx
 :load-units-from-local-storage

 (rf/inject-cofx ::inject/sub (fn [_] [:settings/all-units+decimals]))

 (fn [{units-settings :settings/all-units+decimals} _]
   {:fx (into []
              (for [[category settings]                                   units-settings
                    [_ v-name v-uuid v-dimension-uuid unit-uuid decimals] settings]
                [:dispatch [:settings/set [:units category v-uuid]
                            {:v-name           v-name
                             :v-dimension-uuid v-dimension-uuid
                             :unit-uuid        unit-uuid
                             :decimals         decimals}]]))}))

(rf/reg-event-fx
 :settings/reset-custom-unit-preferences
 (fn [_]
   (when (js/confirm (str "Are you sure you want to reset your unit prefereneces?"))
     {:fx [[:dispatch [:local-storage/clear]]
           [:dispatch [:settings/set :units nil]]
           [:dispatch [:load-units-from-local-storage]]]})))

(rf/reg-event-db
 :settings/close-settings-selector
 (fn [db _]
   (assoc-in db [:state :sidebar :*tools-or-settings] nil)))

(rf/reg-event-db
 :setting/set-current-tab
 (fn [db [_ tab]]
   (assoc-in db [:state :settings :units :current-tab] tab)))
