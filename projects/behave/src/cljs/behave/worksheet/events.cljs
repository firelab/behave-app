(ns behave.worksheet.events
  (:require [re-frame.core   :as rf]
            [behave.importer :refer [import-worksheet]]
            [behave.solver   :refer [solve-worksheet]]))

(rf/reg-fx :ws/import-worksheet import-worksheet)

(rf/reg-event-fx
  :ws/worksheet-selected
  (fn [{db :db} [_ files]]
    (let [file (first (array-seq files))]
      {:db                  (assoc-in db [:state :worksheet :file] (.-name file))
       :ws/import-worksheet (import-worksheet file)})))

(rf/reg-event-db
  :worksheet/solve
  (fn [{:keys [state] :as db} _]
    (assoc-in db
              [:state :worksheet :results]
              (solve-worksheet (:worksheet state)))))
