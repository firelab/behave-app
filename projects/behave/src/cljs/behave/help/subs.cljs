(ns behave.help.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :help/current-tab
  (fn [db _]
    (get-in db [:state :help-tab] :help)))
