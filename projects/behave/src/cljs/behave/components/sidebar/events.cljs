(ns behave.components.sidebar.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 :sidebar/toggle-hidden
 (fn [db _]
   (update-in db [:state :sidebar :hidden?] not)))

(rf/reg-event-db
 :sidebar/set-hidden
 (fn [db [_ v]]
   (assoc-in db [:state :sidebar :hidden?] v)))

(rf/reg-event-db
 :sidebar/set-modules
 (fn [db [_ modules]]
   (assoc-in db [:state :sidebar :*modules] modules)))

(rf/reg-event-db
 :sidebar/select-tools
 (fn [db _]
   (assoc-in db [:state :sidebar :*tools-or-settings] :tools)))

(rf/reg-event-db
 :sidebar/clear-tools-or-settings
 (fn [db _]
   (assoc-in db [:state :sidebar :*tools-or-settings] nil)))
