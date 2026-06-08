(ns behave.components.sidebar.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :sidebar/hidden?
 (fn [db _]
   (get-in db [:state :sidebar :hidden?])))

(rf/reg-sub
 :sidebar/modules
 (fn [db _]
   (get-in db [:state :sidebar :*modules])))

(rf/reg-sub
 :sidebar/worksheet-modules
 (fn [[_ ws-uuid]]
   (rf/subscribe [:worksheet ws-uuid]))

 (fn [worksheet _]
   (->> worksheet
        :worksheet/modules
        (map #(deref (rf/subscribe [:wizard/*module (name %)])))
        (sort-by :module/results-order))))
