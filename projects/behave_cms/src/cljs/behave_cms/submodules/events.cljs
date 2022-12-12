(ns behave-cms.submodules.events
  (:require [re-frame.core   :refer [reg-event-fx]]))

(reg-event-fx
  :submodule/reorder
  (fn [_ [_ submodule direction all-submodules]]
    (let [curr-order     (:submodule_order submodule)
          sorted-submodules (->> all-submodules (vals) (sort-by :submodule_order))]

      (condp = direction
        :down ;; Swap the current with the next
        (when (< curr-order (dec (count sorted-submodules)))
          {:fx [[:dispatch [:api/update-entity :submodules {:uuid (:uuid (nth sorted-submodules (inc curr-order))) :submodule_order curr-order}]]
                [:dispatch [:api/update-entity :submodules {:uuid (:uuid submodule) :submodule_order (inc curr-order)}]]]})

        :up ;; Swap the current with the previous
        (when (> curr-order 0)
          {:fx [[:dispatch [:api/update-entity :submodules {:uuid (:uuid (nth sorted-submodules (dec curr-order))) :submodule_order curr-order}]]
                [:dispatch [:api/update-entity :submodules {:uuid (:uuid submodule) :submodule_order (dec curr-order)}]]]})))))
