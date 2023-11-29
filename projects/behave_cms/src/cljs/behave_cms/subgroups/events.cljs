(ns behave-cms.subgroups.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :subgroups/edit-variables
 (fn [_ [_ eid]]
   {:fx [[:dispatch [:navigate "/variables"]]
         [:dispatch [:state/set-state :variable eid]]]}))
