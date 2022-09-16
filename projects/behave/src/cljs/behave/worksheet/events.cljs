(ns behave.worksheet.events
  (:require [re-frame.core :as rf]
            [behave.import :refer [import-worksheet]]))

(rf/reg-fx :ws/import-worksheet import-worksheet)

(rf/reg-event-fx
  :ws/worksheet-selected
  (fn [{db :db} [_ files]]
    (let [file (first (array-seq files))]
      {:db                  (assoc-in db [:state :worksheet :file] (.-name file))
       :ws/import-worksheet (import-worksheet file)})))

(comment

  (rf/clear-sub :ws/worksheet-solve)
  (rf/clear-subscription-cache!)

  (rf/dispatch [:worksheet/solve])

  )
