(ns behave-cms.subtools.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :subtool/edit-variable
 (fn [_ [_ variable]]
   {:fx [[:dispatch [:navigate "/variables"]]
         [:dispatch [:state/set-state :variables [:bp/nid (:bp/nid variable)]]]]}))
