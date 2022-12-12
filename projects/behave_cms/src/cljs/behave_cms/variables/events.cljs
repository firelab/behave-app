(ns behave-cms.variables.events
  (:require [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx
  :group-variable/reorder
  (fn [_ [_ variable direction all-variables]]
    (let [curr-order       (:variable_order variable)
          sorted-variables (->> all-variables (vals) (sort-by :variable_order))]

      (condp = direction
        :down ;; Swap the current with the next
        (when (< curr-order (dec (count sorted-variables)))
          {:fx [[:dispatch [:api/update-entity :group_variables {:uuid (:uuid (nth sorted-variables (inc curr-order))) :group_order curr-order}]]
                [:dispatch [:api/update-entity :group_variables {:uuid (:uuid variable) :group_order (inc curr-order)}]]]})

        :up ;; Swap the current with the previous
        (when (> curr-order 0)
          {:fx [[:dispatch [:api/update-entity :group_variables {:uuid (:uuid (nth sorted-variables (dec curr-order))) :group_order curr-order}]]
                [:dispatch [:api/update-entity :group_variables {:uuid (:uuid variable) :group_order (dec curr-order)}]]]})))))
