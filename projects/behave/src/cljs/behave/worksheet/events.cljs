(ns behave.worksheet.events
 (:require [re-frame.core    :as rf]
            [re-posh.core    :as rp]
            [datascript.core :as d]
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


(rp/reg-event-fx
 :worksheet/add
 (fn [{:keys [ds]} [_ data]]
   (println data)
   {:transact [data]}))

(rp/reg-event-fx
 :worksheet/new
 (fn [{:keys [ds]} [_ {:keys [name modules]}]]
   {:transact [{:worksheet/name name
                :worksheet/modules modules}]}))

(rp/reg-event-fx
 :worksheet/update-input
 (fn [{:keys [ds]} [_ uuid input-path value]]
   (when-let [id (first (d/q '[:find [?e ...] [?e :worksheet/uuid uuid]]))]
     (println "-- WORKSHEET ID:" id)
     (let [tx []])
   {:transact [[:db/add id attr value]]}))
 
 )

(rp/reg-event-fx
 :worksheet/update-output
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ uuid attr value]]
   (d/q ds)
   {:transact [[:db/add id attr value]]}))

(comment
  (first @(rf/subscribe [:query '[:find [?name ...] :where [?e :worksheet/id ?name]]]))
  (def ws-uuid (first @(rf/subscribe [:query '[:find [?name ...] :where [?e :worksheet/uuid ?name]]])))

  (d/q ws-uuid )



  (rf/dispatch [:worksheet/add {:worksheet/uuid (str (d/squuid)) :worksheet/name "The New Worksheet"}])

  )

