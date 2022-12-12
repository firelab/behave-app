(ns behave-cms.modules.events
  (:require [re-frame.core   :refer [reg-event-fx]]))

(reg-event-fx
  :module/reorder
  (fn [_ [_ module direction all-modules]]
    (let [curr-order     (:module/order module)
          sorted-modules (sort-by :module/order all-modules)

          next-order     (condp = direction
                           :down (when (< curr-order (dec (count sorted-modules)))
                                   (inc curr-order))
                           :up   (when (> curr-order 0)
                                   (dec curr-order)))]
      (when next-order
          {:fx [[:dispatch [:transact [{:db/id (:db/id (nth sorted-modules next-order))
                                        :module/order curr-order}
                                       {:db/id (:db/id module)
                                        :module/order next-order}]]]]}))))

