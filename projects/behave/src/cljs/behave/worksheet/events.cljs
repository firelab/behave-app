(ns behave.worksheet.events
  (:require [re-frame.core :as rf]
            [behave.solver :refer [solve-worksheet]]))

(rf/reg-event-db
  :worksheet/solve
  (fn [{:keys [state] :as db} _]
    (assoc-in db
              [:state :worksheet]
              (solve-worksheet (:worksheet state)))))

(comment

  (rf/clear-sub :worksheet/solve)
  (rf/clear-subscription-cache!)

  (rf/dispatch [:worksheet/solve])

  )
