(ns behave.worksheet.events
  (:require [re-frame.core    :as rf]
            [re-posh.core     :as rp]
            [datascript.core  :as d]
            [behave.components.toolbar :refer [get-step-number step-kw->number get-step-kw]]
            [behave.importer  :refer [import-worksheet]]
            [behave.solver    :refer [solve-worksheet]]
            [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-fx :ws/import-worksheet import-worksheet)

(rf/reg-event-fx
  :ws/worksheet-selected
  (fn [{db :db} [_ files]]
    (let [file (first (array-seq files))]
      {:db                  (assoc-in db [:state :worksheet :file] (.-name file))
       :ws/import-worksheet (import-worksheet file)})))

(rf/reg-event-fx
  :worksheet/solve
  (fn [_ [_ ws-uuid]]
    (solve-worksheet ws-uuid)))

(rp/reg-event-fx
 :worksheet/new
 (fn [_ [_ {:keys [uuid name modules]}]]
   {:transact [{:worksheet/uuid (or uuid (str (d/squuid)))
                :worksheet/name name
                :worksheet/modules modules
                :worksheet/created (.now js/Date)}]}))

(rp/reg-event-fx
  :worksheet/update-attr
  (fn [_ [_ ws-uuid attr value]]
    {:transact [(assoc {:db/id [:worksheet/uuid ws-uuid]} attr value)]}))

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
         {:transact [(cond-> {:db/id       var-id
                              :input/value value}
                       units (assoc :input/units units))]}
         {:transact [(cond-> {:input-group/_inputs       group-id
                              :input/group-variable-uuid group-variable-uuid
                              :input/value               value}
                       units (assoc :input/units units))]})))))

(rp/reg-event-fx
 :worksheet/delete-repeat-input-group
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid group-uuid repeat-id]]
   (let [input-ids (d/q '[:find [?g ...]
                          :in  $ ?ws-uuid ?group-uuid ?repeat-id
                          :where
                          [?w :worksheet/uuid ?ws-uuid]
                          [?w :worksheet/input-groups ?g]
                          [?g :input-group/group-uuid ?group-uuid]
                          [?g :input-group/repeat-id ?repeat-id]]
                        ds ws-uuid group-uuid repeat-id)]
     (when (seq input-ids)
       (let [payload (mapv (fn [id] [:db.fn/retractEntity id]) input-ids)]
         {:transact payload})))))

(rp/reg-event-fx
 :worksheet/upsert-output
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet ws-uuid]))]
 (fn [{:keys [worksheet]} [_ ws-uuid group-variable-uuid enabled?]]
   (if-let [output-id (some->> worksheet
                               (:worksheet/outputs)
                               (filter #(= (:output/group-variable-uuid %) group-variable-uuid))
                               (first)
                               (:db/id))]
     (cond-> {:transact [{:db/id output-id :output/enabled? enabled?}]}
       (true? enabled?)
       (update :fx #(conj (vec %) [:dispatch [:worksheet/add-table-filter ws-uuid group-variable-uuid]]))

       (false? enabled?)
       (update :fx #(conj (vec %) [:dispatch [:worksheet/remove-table-filter ws-uuid group-variable-uuid]])))
     ;;else
     {:transact [{:worksheet/_outputs         [:worksheet/uuid ws-uuid]
                  :output/group-variable-uuid group-variable-uuid
                  :output/enabled?            enabled?}]
      :fx       [[:dispatch [:worksheet/add-table-filter ws-uuid group-variable-uuid]]]})))

(rp/reg-event-fx
 :worksheet/add-result-table
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid]]
   (when-let [ws (first (d/q '[:find  [?e ...]
                               :in    $ ?uuid
                               :where [?e :worksheet/uuid ?uuid]] ds ws-uuid))]
     {:transact [{:worksheet/_result-table ws}]})))

(rp/reg-event-fx
 :worksheet/add-result-table-header
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid group-variable-uuid repeat-id units]]
   (when-let [table (first (d/q '[:find  [?table]
                                  :in    $ ?uuid
                                  :where [?w :worksheet/uuid ?uuid]
                                  [?w :worksheet/result-table ?table]] ds ws-uuid))]
     ;; header with gv-uuid and repeat-id does not already exist
     (when-not (d/q '[:find ?h .
                      :in $ ?t ?group-variable-uuid ?repeat-id
                      :where [?t :result-table/headers ?h]
                             [?h :result-header/group-variable-uuid ?group-variable-uuid]
                             [?h :result-header/repeat-id ?repeat-id]]
                    ds table group-variable-uuid repeat-id)
       (let [headers (count (d/q '[:find [?h ...]
                                   :in $ ?t
                                   :where [?t :result-table/headers ?h]]
                                 ds table))]
         {:transact [{:db/id                table
                      :result-table/headers [{:result-header/group-variable-uuid group-variable-uuid
                                              :result-header/repeat-id           repeat-id
                                              :result-header/order               headers
                                              :result-header/units               units}]}]})))))

(rp/reg-event-fx
 :worksheet/add-result-table-row
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid row-id]]
   (when-let [table (first (d/q '[:find  [?table]
                                  :in    $ ?uuid
                                  :where [?w :worksheet/uuid ?uuid]
                                  [?w :worksheet/result-table ?table]]
                                ds ws-uuid))]
     {:transact [{:db/id             table
                  :result-table/rows [{:result-row/id row-id}]}]})))

(rp/reg-event-fx
 :worksheet/add-result-table-cell
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid row-id group-variable-uuid repeat-id value]]
   (when-let [table (d/q '[:find  ?table .
                           :in    $ ?uuid
                           :where
                           [?w :worksheet/uuid ?uuid]
                           [?w :worksheet/result-table ?table]]
                         ds ws-uuid)]
     (when-let [row (d/q '[:find  ?r .
                           :in    $ ?table ?row-id
                           :where
                           [?table :result-table/rows ?r]
                           [?r :result-row/id ?row-id]]
                         ds table row-id)]
       (when-let [header (d/q '[:find  ?h .
                                :in    $ ?table ?group-var-uuid ?repeat-id
                                :where
                                [?t :result-table/headers ?h]
                                [?h :result-header/group-variable-uuid ?group-var-uuid]
                                [?h :result-header/repeat-id ?repeat-id]]
                              ds table group-variable-uuid repeat-id)]
         {:transact [{:result-row/_cells  row
                      :result-cell/header header
                      :result-cell/value  value}]})))))

(rp/reg-event-fx
 :worksheet/toggle-table-settings
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet ws-uuid]))]
 (fn [{:keys [worksheet]} _]
   (let [table-setting-id (get-in worksheet [:worksheet/table-settings :db/id])
         enabled?         (get-in worksheet [:worksheet/table-settings :table-settings/enabled?])]
     {:transact [{:db/id                   table-setting-id
                  :table-settings/enabled? (not enabled?)}]})))

(rp/reg-event-fx
 :worksheet/toggle-graph-settings
 [(rp/inject-cofx :ds)
  (rf/inject-cofx ::inject/sub
                  (fn [[_ ws-uuid]]
                    [:worksheet/all-output-uuids ws-uuid]))
  (rf/inject-cofx ::inject/sub
                  (fn [[_ ws-uuid]]
                    [:worksheet/multi-value-input-uuids ws-uuid]))]
 (fn [{ds                      :ds
       output-uuids            :worksheet/all-output-uuids
       multi-value-input-uuids :worksheet/multi-value-input-uuids} [_ ws-uuid]]
   (when-let [ws (d/q '[:find  [?ws]
                        :in    $ ?uuid
                        :where [?ws :worksheet/uuid ?uuid]] ds ws-uuid)]
     (if-let [[id enabled?] (d/q '[:find [?t ?enabled]
                                   :in    $ ?uuid
                                   :where
                                   [?w :worksheet/uuid ?uuid]
                                   [?w :worksheet/graph-settings ?t]
                                   [?t :graph-settings/enabled? ?enabled]]
                                  ds
                                  ws-uuid)]
       {:transact [{:db/id                   id
                    :graph-settings/enabled? (not enabled?)}]}
       {:transact [(cond-> {:worksheet/_graph-settings ws
                            :db/id                     -1
                            :graph-settings/enabled?   true}

                     ;; sets default x-axis selection if available
                     (first multi-value-input-uuids)
                     (assoc :graph-settings/x-axis-group-variable-uuid (first multi-value-input-uuids))

                     ;; sets default z-axis selection if available
                     (second multi-value-input-uuids)
                     (assoc :graph-settings/z-axis-group-variable-uuid (second multi-value-input-uuids))

                     ;; sets default z2-axis selection if available
                     (nth multi-value-input-uuids 2 nil)
                     (assoc :graph-settings/z2-axis-group-variable-uuid (nth multi-value-input-uuids 2))

                     (seq output-uuids)
                     (assoc :graph-settings/y-axis-limits
                            (mapv (fn [output-uuid]
                                    {:y-axis-limit/group-variable-uuid output-uuid
                                     :y-axis-limit/min                 0     ;TODO compute from results
                                     :y-axis-limit/max                 100}) ;TODO compute from results
                                  output-uuids)))]}))))

(rp/reg-event-fx
 :worksheet/update-y-axis-limit-attr
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid group-var-uuid attr value]]
   (when-let [y (first (d/q '[:find [?y]
                              :in    $ ?ws-uuid ?group-var-uuid
                              :where
                              [?w :worksheet/uuid ?ws-uuid]
                              [?w :worksheet/graph-settings ?g]
                              [?g :graph-settings/y-axis-limits ?y]
                              [?y :y-axis-limit/group-variable-uuid ?group-var-uuid]]
                            ds
                            ws-uuid
                            group-var-uuid))]
     {:transact [(assoc {:db/id y} attr value)]})))

(rp/reg-event-fx
 :worksheet/update-table-filter-attr
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid group-var-uuid attr value]]
   (when-let [table-filter-id (d/q '[:find ?f .
                                     :in    $ ?ws-uuid ?group-var-uuid
                                     :where
                                     [?w :worksheet/uuid ?ws-uuid]
                                     [?w :worksheet/table-settings ?t]
                                     [?t :table-settings/filters ?f]
                                     [?f :table-filter/group-variable-uuid ?group-var-uuid]]
                                   ds
                                   ws-uuid
                                   group-var-uuid)]
     (let [v (cond
               (and (js/isNaN value) (= attr :table-filter/min)) -999999999
               (and (js/isNaN value) (= attr :table-filter/max)) 999999999
               :else                                             value)]
       {:transact [(assoc {:db/id table-filter-id} attr v)]}))))

(rp/reg-event-fx
 :worksheet/add-table-filter
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet ws-uuid]))]
 (fn [{:keys [worksheet]} [_ ws-uuid gv-uuid]]
   (let [filter {:table-filter/group-variable-uuid gv-uuid
                 :table-filter/min                 -999999999
                 :table-filter/max                 999999999}]
     (if-let [id (get-in worksheet [:worksheet/table-settings :db/id])]
       {:transact [{:db/id                  id
                    :table-settings/filters [filter]}]}
       {:transact [{:worksheet/_table-settings [:worksheet/uuid ws-uuid]
                    :table-settings/filters    [filter]}]}))))

(rp/reg-event-fx
 :worksheet/remove-table-filter
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid gv-uuid]]
   (when-let [eid (d/q '[:find  ?f .
                         :in    $ ?uuid ?gv-uuid
                         :where
                         [?w :worksheet/uuid ?uuid]
                         [?w :worksheet/table-settings ?t]
                         [?t :table-settings/filters ?f]
                         [?f :table-filter/group-variable-uuid ?gv-uuid]]
                       ds ws-uuid gv-uuid)]
     {:transact [[:db.fn/retractEntity eid]]})))

(rp/reg-event-fx
 :worksheet/update-graph-settings-attr
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid attr value]]
   (when-let [g (first (d/q '[:find [?g]
                              :in    $ ?uuid
                              :where
                              [?w :worksheet/uuid ?uuid]
                              [?w :worksheet/graph-settings ?g]]
                             ds
                             ws-uuid))]
     {:transact [(assoc {:db/id g} attr value)]})))

;;Notes

(rp/reg-event-fx
 :worksheet/create-note
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_id
                    ws-uuid
                    submodule-uuid
                    submodule-name
                    submodule-io
                    {:keys [title body] :as _payload}]]
   (when-let [ws-id (d/entid ds [:worksheet/uuid ws-uuid])]
     {:transact [{:db/id            -1
                  :worksheet/_notes ws-id
                  :note/name        (if (empty? title)
                                      (str submodule-name " " submodule-io)
                                      title)
                  :note/content     body
                  :note/submodule   submodule-uuid}]})))

(rp/reg-event-fx
 :worksheet/update-note
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_  note-id {:keys [title body] :as _payload}]]
   (when-let [note (d/entity ds note-id)]
     {:transact [{:db/id        (:db/id note)
                  :note/name    title
                  :note/content body}]})))

(rp/reg-event-fx
 :worksheet/delete-note
 [(rp/inject-cofx :ds)]
 (fn [_ [_ note-id]]
   {:transact [[:db.fn/retractEntity note-id]]}))

(rp/reg-event-fx
 :worksheet/update-furthest-visited-step
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid route-handler io]]
   (when-let [worksheet (d/entity ds [:worksheet/uuid ws-uuid])]
     (let [worksheet-visited-step (get step-kw->number (:worksheet/furthest-visited-step worksheet))
           current-step           (get-step-number route-handler io)]
       (when (or (nil? worksheet-visited-step)
                 (< worksheet-visited-step current-step))
         {:transact [{:db/id                           [:worksheet/uuid ws-uuid]
                      :worksheet/furthest-visited-step (get-step-kw route-handler io)}]})))))

(comment


  (require '[datascript.core :as d])
  (require '[behave.store :as s])
  (require '[behave.vms.store :as vms])

  (defn clear! [k]
    (rf/clear-sub k)
    (rf/clear-subscription-cache!))

  (clear! :worksheet/upsert-result)

  (def ws-uuid @(rf/subscribe [:worksheet/latest]))

  (def ws-id (first (d/q '[:find [?w]
                           :in    $ ?ws-uuid
                           :where [?w :worksheet/uuid ?ws-uuid]]
                          @@s/conn ws-uuid)))

  ws-uuid

  (def gv-1 "6348a027-79b0-4123-8d62-3c0cf99dbe18")
  (def gv-2 (str (d/squuid)))

  (rf/dispatch [:worksheet/add-result-table ws-uuid])
  (rf/dispatch [:worksheet/add-result-table-header ws-uuid (str (d/squuid)) "m"])
  (rf/dispatch [:worksheet/add-result-table-header ws-uuid gv-2 "kg"])

  (rf/dispatch [:worksheet/add-result-table-row ws-uuid 0])

  (rf/dispatch [:worksheet/add-result-table-cell ws-uuid 0 gv-1 "100"])
  (rf/dispatch [:worksheet/add-result-table-cell ws-uuid 0 gv-2 "20"])

  (d/q '[:find ?row-id ?c ?v ?h ?units ?gv
         :in $ ?ws-uuid
         :where [?w :worksheet/uuid ?ws-uuid]
         [?w :worksheet/result-table ?t]
         [?t :result-table/rows ?r]
         [?r :result-row/id ?row-id]
         [?r :result-row/cells ?c]
         [?c :result-cell/value ?v]
         [?c :result-cell/header ?h]
         [?h :result-header/group-variable-uuid ?gv]
         [?h :result-header/units ?units]]
        @@s/conn ws-uuid)

  (d/transact @s/conn [{:worksheet/_result-table ws-id
                        :db/id                   -1
                        :result-table/headers    [{:result-header/group-variable-uuid (str (d/squuid))}]}])

  (d/pull @@s/conn '[{:worksheet/result-table [{:result-table/headers [*]}]}] ws-id)

  (d/pull @@s/conn '[{:worksheet/result-table [{:result-table/headers [*]}]}] ws-id)

  (def group-uuid "0fccfc94-f1f2-4d33-8fd7-b2257c414cfa")

  (def group-var-uuid "d5f5cc0a-5ff6-4fcd-b88d-51cd6e6b76f1")


  (d/q '[:find [?g ...]
         :in    $ ?ws-uuid ?group-uuid
         :where [?w :worksheet/uuid ?ws-uuid]
         [?w :worksheet/input-groups ?g]
         [?g :input-group/group-uuid ?group-uuid]]
        @@s/conn ws-uuid group-uuid)

  (d/q '[:find ?i ?v
         :in    $ ?ws-uuid ?group-uuid ?repeat-id ?group-var-uuid
         :where [?w :worksheet/uuid ?ws-uuid]
         [?w :worksheet/input-groups ?g]
         [?g :input-group/group-uuid ?group-uuid]
         [?g :input-group/repeat-id ?repeat-id]
         [?g :input-group/inputs ?i]
         [?i :input/group-variable-uuid ?group-var-uuid]
         [?i :input/value ?v]]
        @@s/conn ws-uuid group-uuid 2 group-var-uuid)

  (rf/subscribe [:worksheet/input ws-uuid group-uuid 0 group-var-uuid])

  (d/pull @@s/conn '[* {:input-group/inputs [*]}] 139)

  (d/transact @s/conn [{:db/id 135 :input-group/repeat-id 0}])

  (d/transact @s/conn [{:db/id                  -1
                        :input-group/repeat-id  2
                        :input-group/group-uuid group-uuid}
                       {:worksheet/_input-groups -1}])

  (rf/subscribe [:worksheet/repeat-groups ws-uuid group-uuid])

  (d/pull-many @@s/conn '[*]
               (d/q '[:find [?g ...]
                      :in    $ ?uuid
                      :where [?w :worksheet/uuid ?uuid]
                      [?w :worksheet/input-groups ?g]]
                     @@s/conn ws-uuid))

  (let [ws-uuid @(rf/subscribe [:worksheet/latest])]
    (rf/subscirbe [:worksheet/table-settings-enabled? ws-uuid]))

  )
