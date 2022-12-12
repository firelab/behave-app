(ns behave-cms.subgroups.events
  (:require [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx
  :subgroup/reorder
  (fn [_ [_ subgroup direction all-subgroups]]
    (let [curr-order       (:group_order subgroup)
          sorted-subgroups (->> all-subgroups (vals) (sort-by :group_order))]

      (condp = direction
        :down ;; Swap the current with the next
        (when (< curr-order (dec (count sorted-subgroups)))
          {:fx [[:dispatch [:api/update-entity :groups {:uuid (:uuid (nth sorted-subgroups (inc curr-order))) :group_order curr-order}]]
                [:dispatch [:api/update-entity :groups {:uuid (:uuid subgroup) :group_order (inc curr-order)}]]]})

        :up ;; Swap the current with the previous
        (when (> curr-order 0)
          {:fx [[:dispatch [:api/update-entity :groups {:uuid (:uuid (nth sorted-subgroups (dec curr-order))) :group_order curr-order}]]
                [:dispatch [:api/update-entity :groups {:uuid (:uuid subgroup) :group_order (dec curr-order)}]]]})))))

(reg-event-fx
  :group-variable/reorder
  (fn [_ [_ group-var direction all-group-vars]]
    (let [curr-order        (:variable_order group-var)
          sorted-group-vars (->> all-group-vars (vals) (sort-by :variable_order))]

      (condp = direction
        :down ;; Swap the current with the next
        (when (< curr-order (dec (count sorted-group-vars)))
          {:fx [[:dispatch [:api/update-entity :group-variables {:uuid (:uuid (nth sorted-group-vars (inc curr-order))) :group_order curr-order}]]
                [:dispatch [:api/update-entity :group-variables {:uuid (:uuid group-var) :group_order (inc curr-order)}]]]})

        :up ;; Swap the current with the previous
        (when (> curr-order 0)
          {:fx [[:dispatch [:api/update-entity :group-variables {:uuid (:uuid (nth sorted-group-vars (dec curr-order))) :group_order curr-order}]]
                [:dispatch [:api/update-entity :group-variables {:uuid (:uuid group-var) :group_order (dec curr-order)}]]]})))))
