(ns behave-cms.groups.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]))

;; Variables Search

(reg-event-db
  :groups/on-search-results
  (fn [db [_ {body :body}]]
    (assoc-in db [:state :search :variables] body)))

(reg-event-fx
  :groups/search-variables
  (fn [_ [_ query]]
    {:api/request {:method     :get
                   :uri        "/api/variables/search"
                   :data       {:query query}
                   :on-success :groups/on-search-results}}))

(reg-event-fx
  :group/reorder
  (fn [_ [_ group direction all-groups]]
    (let [curr-order     (:group_order group)
          sorted-groups (->> all-groups (vals) (sort-by :group_order))]

      (condp = direction
        :down ;; Swap the current with the next
        (when (< curr-order (dec (count sorted-groups)))
          {:fx [[:dispatch [:api/update-entity :groups {:uuid (:uuid (nth sorted-groups (inc curr-order))) :group_order curr-order}]]
                [:dispatch [:api/update-entity :groups {:uuid (:uuid group) :group_order (inc curr-order)}]]]})

        :up ;; Swap the current with the previous
        (when (> curr-order 0)
          {:fx [[:dispatch [:api/update-entity :groups {:uuid (:uuid (nth sorted-groups (dec curr-order))) :group_order curr-order}]]
                [:dispatch [:api/update-entity :groups {:uuid (:uuid group) :group_order (dec curr-order)}]]]})))))
