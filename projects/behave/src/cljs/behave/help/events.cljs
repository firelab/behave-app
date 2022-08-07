(ns behave.help.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
  :help/select-tab
  (fn [db [_ new-tab]]
    (assoc-in db [:state :help-tab] (:tab new-tab))))
