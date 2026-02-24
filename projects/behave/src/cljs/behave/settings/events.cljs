(ns behave.settings.events
  (:require [re-frame.core                 :as rf]
            [re-posh.core                  :as rp]
            [vimsical.re-frame.cofx.inject :as inject]))

(rp/reg-event-fx
 :settings/clear-inputs-in-domain
 (rf/inject-cofx ::inject/sub (fn [[_ ws-uuid domain-uuid]] [:worksheet/inputs-in-domain ws-uuid domain-uuid]))
 (fn [{input-eids :worksheet/inputs-in-domain} _]
   (let [payload (mapv (fn [input-eid] [:db/retract input-eid :input/value]) input-eids)]
     {:transact payload})))

(rf/reg-event-fx
 :settings/cache-unit-preference
 (fn [_ [_ domain domain-uuid unit-uuid ws-uuid]]
   {:fx (cond-> [[:dispatch [:settings/set [:units domain domain-uuid :unit-uuid] unit-uuid]]
                 [:dispatch [:local-storage/update-in [:units domain-uuid :unit-uuid] unit-uuid]]]
          ws-uuid
          (conj [:dispatch [:settings/clear-inputs-in-domain ws-uuid domain-uuid]]))}))

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
              (for [[domain settings] units-settings
                    [_
                     domain-name
                     domain-uuid
                     domain-dimension-uuid
                     cached-unit-uuid
                     native-domain-unit-uuid
                     english-domain-unit-uuid
                     metric-domain-unit-uuid
                     decimals]        settings]
                [:dispatch [:settings/set [:units domain domain-uuid]
                            {:domain-name              domain-name
                             :domain-dimension-uuid    domain-dimension-uuid
                             :domain-cached-unit-uuid  cached-unit-uuid
                             :domain-native-unit-uuid  native-domain-unit-uuid
                             :domain-english-unit-uuid english-domain-unit-uuid
                             :domain-metric-unit-uuid  metric-domain-unit-uuid
                             :domain-decimals          decimals}]]))}))

(rf/reg-event-fx
 :settings/reset-custom-unit-preferences
 (fn [_]
   {:fx [[:dispatch [:local-storage/clear]]
         [:dispatch [:settings/set :units nil]]
         [:dispatch [:settings/load-units-from-local-storage]]]}))

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

(rf/reg-event-fx
 :settings/set-units-system
 (fn [_ [_ units-system]]
   {:fx [[:dispatch [:settings/set [:application-units-system] units-system]]
         [:dispatch [:local-storage/update-in [:application-units-system] units-system]]
         [:dispatch [:state/set [:tool :data] nil]]]}))

(rf/reg-event-fx
 :settings/set-tool-units-system
 (fn [_ [_ tool-uuid subtool-uuid auto-compute? units-system]]
   {:fx (cond-> [[:dispatch [:settings/set [:tool-units-system] units-system]]
                 [:dispatch [:local-storage/update-in [:tool-units-system] units-system]]
                 [:dispatch [:state/set [:tool :data] nil]]]
          auto-compute? (conj [:dispatch [:tool/solve tool-uuid subtool-uuid]]))}))
