(ns behave.settings.events
  (:require [re-frame.core :as rf]
            [vimsical.re-frame.cofx.inject :as inject]
            [behave.translate :refer [<t bp]]))

(rf/reg-event-fx
 :settings/cache-unit-preference
 (fn [_ [_ domain v-uuid unit-uuid]]
   {:fx [[:dispatch [:settings/set [:units domain v-uuid :unit-uuid] unit-uuid]]
         [:dispatch [:local-storage/update-in [:units v-uuid :unit-uuid] unit-uuid]]]}))

(rf/reg-event-fx
 :settings/cache-decimal-preference
 (fn [_ [_ domain v-uuid decimal]]
   {:fx [[:dispatch [:settings/set [:units domain v-uuid :decimals] decimal]]
         [:dispatch [:local-storage/update-in [:units v-uuid :decimals] decimal]]]}))

(rf/reg-event-fx
 :settings/load-units-from-local-storage

 (rf/inject-cofx ::inject/sub (fn [_] [:settings/all-units+decimals]))

 (fn [{units-settings :settings/all-units+decimals} _]
   {:fx (into []
              (for [[domain settings]                                     units-settings
                    [_ v-name v-uuid v-dimension-uuid unit-uuid decimals] settings]
                [:dispatch [:settings/set [:units domain v-uuid]
                            {:v-name           v-name
                             :v-dimension-uuid v-dimension-uuid
                             :unit-uuid        unit-uuid
                             :decimals         decimals}]]))}))

(rf/reg-event-fx
 :settings/reset-custom-unit-preferences
 (fn [_]
   (when (js/confirm @(<t (bp "are_you_sure_you_want_to_reset_your_unit_preferences?")))
     {:fx [[:dispatch [:local-storage/clear]]
           [:dispatch [:settings/set :units nil]]
           [:dispatch [:settings/load-units-from-local-storage]]]})))

(rf/reg-event-db
 :settings/close-settings-selector
 (fn [db _]
   (assoc-in db [:state :sidebar :*tools-or-settings] nil)))

(rf/reg-event-db
 :settings/set-current-tab
 (fn [db [_ tab]]
   (assoc-in db [:state :settings :units :current-tab] tab)))

(rf/reg-event-db
 :settings/set
 (rf/path [:settings])
 (fn [settings [_ k v]]
   (cond
     (keyword? k)
     (assoc settings k v)

     (vector? k)
     (assoc-in settings k v))))
