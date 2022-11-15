(ns behave.help.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :help/highlight-section
 (fn [_ [_ help-key]]
   {:fx [[:dispatch [:state/set :help-current-highlighted-key help-key]]
         [:help/scroll-into-view help-key]]}))

(rf/reg-fx
 :help/scroll-into-view
 (fn [help-key]
   (-> (.getElementById js/document help-key)
       (.scrollIntoView true))))

(rf/reg-event-db
 :help/select-tab
 (fn [db [_ new-tab]]
   (assoc-in db [:state :help-tab] (:tab new-tab))))
