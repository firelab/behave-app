(ns behave-cms.subgroups.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :subgroups/edit-variables
 (fn [_ [_ variable]]
   {:fx [[:dispatch [:navigate "/variables"]]
         [:dispatch [:state/set-state :variables [:bp/nid (:bp/nid variable)]]]
         [:dispatch-later {:ms       500
                           :dispatch [:scroll-into-view (:db/id variable)]}]]}))
