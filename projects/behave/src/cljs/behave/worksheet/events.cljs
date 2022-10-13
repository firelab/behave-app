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
 :worksheet/new
 (fn [_ [_ {:keys [uuid name modules]}]]
   {:transact [{:worksheet/uuid (or uuid (str (d/squuid)))
                :worksheet/name name
                :worksheet/modules modules
                :worksheet/created (.now js/Date)}]}))

(rp/reg-event-fx
 :worksheet/add-input-group
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid group-uuid repeat-id]]
   (when-let [ws (first (d/q '[:find  [?ws ...]
                               :in    $ ?uuid
                               :where [?ws :worksheet/uuid ?uuid]] ds ws-uuid))]
     (when (nil? (d/q '[:find [?g]
                        :in $ ?ws ?group-uuid ?repeat-id
                        :where [?ws :worksheet/input-groups ?g]
                               [?g :input-group/group-uuid ?group-uuid]
                               [?g :input-group/repeat-id ?repeat-id]]
                      ds ws group-uuid repeat-id))
           {:transact [{:worksheet/_input-groups ws
                        :db/id                   -1
                        :input-group/group-uuid  group-uuid
                        :input-group/repeat-id   repeat-id}]}))))

(rp/reg-event-fx
 :worksheet/upsert-input-variable
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid group-uuid repeat-id group-variable-uuid value units]]
   (when-let [ws (first (d/q '[:find  [?ws ...]
                               :in    $ ?uuid
                               :where [?ws :worksheet/uuid ?uuid]] ds ws-uuid))]
     (when-let [group-id (first (d/q '[:find  [?ig]
                                       :in    $ ?ws ?group-uuid ?repeat-id
                                       :where [?ws :worksheet/input-groups ?ig]
                                              [?ig :input-group/group-uuid ?group-uuid]
                                              [?ig :input-group/repeat-id  ?repeat-id]]
                                     ds ws group-uuid repeat-id))]
       (if-let [var-id (first (d/q '[:find  [?i]
                                     :in    $ ?ig ?uuid
                                     :where [?ig :input-group/inputs ?i]
                                            [?i :input/group-variable-uuid ?uuid]]
                                   ds group-id group-variable-uuid))]
         {:transact [{:db/id       var-id
                      :input/value value}]}
         {:transact [{:input-group/_inputs       group-id
                      :input/group-variable-uuid group-variable-uuid
                      :input/value               value}]})))))

(rp/reg-event-fx
 :worksheet/upsert-output
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid variable-uuid enabled?]]
   (when-let [ws (first (d/q '[:find  [?e ...]
                               :in    $ ?uuid
                               :where [?e :worksheet/uuid ?uuid]] ds ws-uuid))]
     (if-let [output-id (first (d/q '[:find  [?o]
                                      :in    $ ?ws ?var-uuid
                                      :where [?ws :worksheet/outputs ?o]
                                      [?o :output/variable-uuid ?var-uuid]] ds ws variable-uuid))]
       {:transact [{:db/id output-id :output/enabled? enabled?}]}
       {:transact [{:worksheet/_outputs   ws
                    :output/variable-uuid variable-uuid
                    :output/enabled?      enabled?}]}))))
