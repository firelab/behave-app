(ns behave.diagram.events
  (:require [behave.diagram.core :as core]
            [browser-utils.core  :refer [get-local-storage]]
            [re-frame.core       :as rf]))

(rf/reg-event-fx
 :diagram/set-visible-series
 (fn [{:keys [db]} [_ k visible]]
   (if (= visible (get-in db (conj core/db-path k)))
     {}
     {:db (assoc-in db (conj core/db-path k) visible)
      :fx [[:dispatch [:local-storage/update-in (conj core/ls-path k) visible]]]})))

(rf/reg-event-fx
 :diagram/hydrate-visible-series
 (fn [{:keys [db]} _]
   (let [stored (or (get-in (get-local-storage) core/ls-path) {})]
     (if (= stored (get-in db core/db-path))
       {}
       {:db (assoc-in db core/db-path stored)}))))

(rf/reg-event-fx
 :diagram/clear-visible-series
 (fn [{:keys [db]} [_ ws-uuid]]
   {:db (update-in db core/db-path core/clear-worksheet ws-uuid)
    :fx [[:dispatch [:local-storage/update-in
                     core/ls-path
                     (core/clear-worksheet
                      (or (get-in (get-local-storage) core/ls-path) {})
                      ws-uuid)]]]}))
