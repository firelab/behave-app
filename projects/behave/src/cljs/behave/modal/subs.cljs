(ns behave.modal.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :modal/stack
 (fn [db _]
   (get-in db [:state :modals] [])))

(rf/reg-sub
 :modal/current
 :<- [:modal/stack]
 (fn [stack _]
   (last stack)))
