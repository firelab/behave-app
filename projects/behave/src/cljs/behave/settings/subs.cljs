(ns behave.settings.subs
  (:require [re-frame.core    :as rf]
            [datascript.core  :as d]
            [behave.vms.store :refer [vms-conn]]))

(rf/reg-sub
 :settings/show-settings-selector?
 (fn [db _]
   (let [state (get-in db [:state :sidebar :*tools-or-settings])]
     (= state :settings))))

(rf/reg-sub
 :settings/local-storage-units
 (fn [_]
   (rf/subscribe [:local-storage/get]))

 (fn [local-storage]
   (:units local-storage)))

(rf/reg-sub
 :settings/cached-unit
 (fn []
   (rf/subscribe [:settings/local-storage-units]))

 (fn [local-storage [_ v-uuid]]
   (:unit-uuid (get local-storage v-uuid))))

(rf/reg-sub
 :settings/cached-decimal
 (fn []
   (rf/subscribe [:settings/local-storage-units]))

 (fn [local-storage [_ v-uuid]]
   (:decimals (get local-storage v-uuid))))

(rf/reg-sub
 :settings/all-units+decimals
 (fn []
   (rf/subscribe [:settings/local-storage-units]))

 (fn [cached-units _]
   (let [vms-units (->> (d/q '[:find ?category ?v-name ?v-uuid ?v-dimension-uuid ?unit-uuid ?decimals
                               :where
                               [?v :bp/uuid ?v-uuid]
                               [?v :variable/kind :continuous]
                               [?v :variable/category-uuid ?c-uuid]
                               [?c :bp/uuid ?c-uuid]
                               [?c :category/name ?category]
                               [?v :variable/name ?v-name]
                               [?v :variable/dimension-uuid ?v-dimension-uuid]
                               [?v :variable/native-unit-uuid ?unit-uuid]
                               [?v :variable/native-decimals ?decimals]]
                             @@vms-conn)
                        (sort-by (juxt first second)))]
     (->> (map (fn [[v-category v-name v-uuid v-dimension-uuid default-unit-uuid default-decimals]]
                 (let [{:keys [unit-uuid decimals]} (get cached-units v-uuid)]
                   (-> [v-category v-name v-uuid v-dimension-uuid]
                       (conj (or unit-uuid default-unit-uuid))
                       (conj (or decimals default-decimals)))))
               vms-units)
          (group-by first)))))
