(ns behave.worksheet.events
 (:require [re-frame.core    :as rf]
           [re-posh.core     :as rp]
           [datascript.core  :as d]
           [behave.importer  :refer [import-worksheet]]
           [behave.solver    :refer [solve-worksheet]]))

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
 (fn [_ [_ {:keys [uuid name modules]}]]
   {:transact [{:worksheet/uuid (or uuid (str (d/squuid)))
                :worksheet/name name
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
 (fn [{:keys [ds]} [_ ws-uuid variable-uuid enabled?]]
   (when-let [id (first (d/q '[:find  [?e ...]
                               :in    $ ?uuid
                               :where [?e :worksheet/uuid ?uuid]] ds ws-uuid))]
     (if-let [output-id (first (d/q '[:find  [?o]
                                      :in    $ ?ws ?var-uuid
                                      :where [?ws :worksheet/outputs ?o]
                                             [?o :output/variable-uuid ?var-uuid]] ds id variable-uuid))]
     {:transact [{:db/id output-id :output/enabled? enabled?}]}
     {:transact [{:worksheet/_outputs   id
                  :output/variable-uuid variable-uuid
                  :output/enabled?      enabled?}]}))))

(comment
  (first @(rf/subscribe [:query '[:find [?name ...] :where [?e :worksheet/id ?name]]]))
  (def ws-uuid (first @(rf/subscribe [:query '[:find [?name ...] :where [?e :worksheet/uuid ?name]]])))

  (d/q ws-uuid )



  (rf/dispatch [:worksheet/add {:worksheet/uuid (str (d/squuid)) :worksheet/name "The New Worksheet"}])

  )

