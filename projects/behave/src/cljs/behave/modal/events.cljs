(ns behave.modal.events
  (:require [re-frame.core :as rf]))

(defn open
  "Pushes a modal onto `stack`."
  [stack id opts]
  (conj (vec stack) (assoc opts :modal/id id)))

(defn close
  "Pops the top-most modal off `stack`."
  [stack]
  (vec (butlast stack)))

(rf/reg-event-db
 :modal/open
 (fn [db [_ id opts]]
   (update-in db [:state :modals] open id opts)))

(rf/reg-event-db
 :modal/close
 (fn [db _]
   (update-in db [:state :modals] close)))

(rf/reg-event-db
 :modal/close-all
 (fn [db _]
   (assoc-in db [:state :modals] [])))
