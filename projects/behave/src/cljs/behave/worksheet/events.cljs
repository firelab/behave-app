(ns behave.worksheet.events
  (:require [re-frame.core    :as rf]
            [re-posh.core     :as rp]
            [datascript.core  :as d]
            [behave.components.toolbar :refer [step-priority]]
            [behave.importer           :refer [import-worksheet]]
            [behave.logger             :refer [log]]
            [behave.solver.core        :refer [solve-worksheet]]
            [vimsical.re-frame.cofx.inject :as inject]
            [number-utils.core :refer [to-precision]]
            [behave.wizard.subs :refer [all-conditionals-pass?]]
            [clojure.string :as str]))

;;; Helpers

(defn ^:private q-worksheet [conn ws-uuid]
  (d/q '[:find  ?ws .
         :in    $ ?ws-uuid
         :where
         [?ws :worksheet/uuid ?ws-uuid]]
       conn ws-uuid))

(defn ^:private q-input-group [conn ws-uuid group-uuid repeat-id]
  (d/q '[:find  ?ig .
         :in    $ ?ws-uuid ?group-uuid ?repeat-id
         :where
         [?ws :worksheet/uuid ?ws-uuid]
         [?ws :worksheet/input-groups ?ig]
         [?ig :input-group/group-uuid ?group-uuid]
         [?ig :input-group/repeat-id  ?repeat-id]]
       conn ws-uuid group-uuid repeat-id))

(defn ^:private q-input-variable [conn group-id group-variable-uuid]
  (d/q '[:find  ?i .
         :in    $ ?ig ?uuid
         :where
         [?ig :input-group/inputs ?i]
         [?i :input/group-variable-uuid ?uuid]]
       conn group-id group-variable-uuid))

(defn ^:private q-input-value [conn ws-uuid group-uuid repeat-id]
  (d/q '[:find  ?value .
         :in    $ ?ws-uuid ?group-uuid ?repeat-id
         :where
         [?ws :worksheet/uuid ?ws-uuid]
         [?ws :worksheet/input-groups ?ig]
         [?ig :input-group/group-uuid ?group-uuid]
         [?ig :input-group/repeat-id  ?repeat-id]
         [?ig :input-group/inputs ?i]
         [?i :input/value ?value]]
       conn ws-uuid group-uuid repeat-id))

(defn ^:private q-input-unit [conn group-id group-variable-uuid]
  (d/q '[:find  ?units .
         :in    $ ?ig ?uuid
         :where
         [?ig :input-group/inputs ?i]
         [?i :input/group-variable-uuid ?uuid]
         [?i :input/units ?units]]
       conn group-id group-variable-uuid))

(defn ^:private add-input-group-tx [ws-uuid group-uuid repeat-id]
  {:db/id                   -1
   :worksheet/_input-groups [:worksheet/uuid ws-uuid]
   :input-group/group-uuid  group-uuid
   :input-group/repeat-id   repeat-id})

;;; Events

(rf/reg-fx :ws/import-worksheet import-worksheet)

(rf/reg-event-fx
 :ws/worksheet-selected
 (fn [{db :db} [_ files]]
   (let [file (first (array-seq files))]
     {:db                  (assoc-in db [:state :worksheet :file] {:name (.-name file)
                                                                   :obj  file})
      :ws/import-worksheet (import-worksheet file)})))

(rf/reg-event-fx
 :worksheet/solve
 (fn [_ [_ ws-uuid]]
   (solve-worksheet ws-uuid)))

(rp/reg-event-fx
 :worksheet/new
 (fn [_ [_ {:keys [uuid name modules version]}]]
   (let [tx (cond-> {:worksheet/uuid    (or uuid (str (d/squuid)))
                     :worksheet/modules modules
                     :worksheet/created (.now js/Date)}
              version
              (assoc :worksheet/version version))]
     {:transact [(merge tx (when name {:worksheet/name name}))]})))

(rp/reg-event-fx
 :worksheet/update-attr
 (fn [_ [_ ws-uuid attr value]]
   {:transact [(assoc {:db/id [:worksheet/uuid ws-uuid]} attr value)]}))

(rp/reg-event-fx
 :worksheet/add-input-group
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid group-uuid repeat-id]]
   (when (nil? (q-input-group ds ws-uuid group-uuid repeat-id))
     {:transact [(add-input-group-tx ws-uuid group-uuid repeat-id)]})))

(rp/reg-event-fx
 :worksheet/upsert-input-variable
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid group-uuid repeat-id group-variable-uuid value]]
   (let [group-id (or (q-input-group ds ws-uuid group-uuid repeat-id) -1)
         var-id   (q-input-variable ds group-id group-variable-uuid)
         payload  (cond-> []
                    var-id
                    (conj {:db/id       var-id
                           :input/value value})

                    (neg? group-id)
                    (conj (add-input-group-tx ws-uuid group-uuid repeat-id))

                    (nil? var-id)
                    (conj {:db/id                     -2
                           :input-group/_inputs       group-id
                           :input/group-variable-uuid group-variable-uuid
                           :input/value               value}))]
     {:transact payload})))

(rp/reg-event-fx
 :worksheet/upsert-multi-select-input
 [(rp/inject-cofx :ds)]
 (fn [
      {:keys [ds]} [_ ws-uuid group-uuid repeat-id group-variable-uuid value]]
   (let [group-id    (or (q-input-group ds ws-uuid group-uuid repeat-id) -1)
         input-id    (q-input-variable ds group-id group-variable-uuid)
         input-value (q-input-value ds ws-uuid group-uuid repeat-id)
         payload     (cond-> []
                       input-id
                       (conj {:db/id       input-id
                              :input/value (as-> (str/split input-value ",") $
                                             (remove empty? $)
                                             (set $)
                                             (conj $ value)
                                             (str/join "," $))})

                       (neg? group-id)
                       (conj (add-input-group-tx ws-uuid group-uuid repeat-id))

                       (nil? input-id)
                       (conj {:db/id                     -2
                              :input-group/_inputs       group-id
                              :input/group-variable-uuid group-variable-uuid
                              :input/value               (str value)}))]
     {:transact payload})))

(rp/reg-event-fx
 :worksheet/remove-multi-select-input
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid group-uuid repeat-id group-variable-uuid value]]
   (let [group-id    (or (q-input-group ds ws-uuid group-uuid repeat-id) -1)
         input-id    (q-input-variable ds group-id group-variable-uuid)
         input-value (q-input-value ds ws-uuid group-uuid repeat-id)
         payload     (cond-> []
                       input-id
                       (conj {:db/id       input-id
                              :input/value (as-> (str/split input-value ",") $
                                             (remove empty? $)
                                             (set $)
                                             (disj $ value)
                                             (str/join "," $))}))]
     {:transact payload})))

(rp/reg-event-fx
 :worksheet/update-input-units
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid group-uuid repeat-id group-variable-uuid units]]
   (let [group-id (or (q-input-group ds ws-uuid group-uuid repeat-id) -1)
         var-id   (q-input-variable ds group-id group-variable-uuid)
         payload  (cond-> []
                    var-id
                    (conj {:db/id       var-id
                           :input/units units})

                    (neg? group-id)
                    (conj (add-input-group-tx ws-uuid group-uuid repeat-id))

                    (nil? var-id)
                    (conj {:db/id                     -2
                           :input-group/_inputs       group-id
                           :input/group-variable-uuid group-variable-uuid
                           :input/units               units}))]
     {:transact payload})))

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
       (update :fx #(into (vec %) [[:dispatch [:worksheet/add-table-filter ws-uuid group-variable-uuid]]
                                   [:dispatch [:worksheet/add-y-axis-limit ws-uuid group-variable-uuid]]]))

       (false? enabled?)
       (update :fx #(into (vec %) [[:dispatch [:worksheet/remove-table-filter ws-uuid group-variable-uuid]]
                                   [:dispatch [:worksheet/remove-y-axis-limit ws-uuid group-variable-uuid]]])))
     ;;else
     {:transact [{:worksheet/_outputs         [:worksheet/uuid ws-uuid]
                  :output/group-variable-uuid group-variable-uuid
                  :output/enabled?            enabled?}]
      :fx       [[:dispatch [:worksheet/add-table-filter ws-uuid group-variable-uuid]]
                 [:dispatch [:worksheet/add-y-axis-limit ws-uuid group-variable-uuid]]]})))

(rf/reg-event-fx
 :worksheet/delete-existing-result-table
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid]]
   (when-let [existing-eid (d/q '[:find  ?t .
                                  :in    $ ?ws-uuid
                                  :where
                                  [?ws :worksheet/uuid     ?ws-uuid]
                                  [?ws :worksheet/result-table ?t]]
                                ds ws-uuid)]
     {:transact [[:db.fn/retractEntity existing-eid]]})))

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
 :worksheet/upsert-table-setting-map-units
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet ws-uuid]))]
 (fn [{:keys [worksheet]} [_ _ws-uuid attr value]]
   (if-let [map-units-settings-eid (get-in worksheet [:worksheet/table-settings
                                                      :table-settings/map-units-settings
                                                      :db/id])]
     {:transact [(assoc {:db/id map-units-settings-eid} attr value)]}
     (when-let [table-settings-eid (get-in worksheet [:worksheet/table-settings
                                                      :db/id])]
       {:transact [{:db/id                              -1
                    :table-settings/_map-units-settings table-settings-eid
                    attr                                value}]}))))

(rp/reg-event-fx
 :worksheet/toggle-table-settings
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet ws-uuid]))]
 (fn [{:keys [worksheet]} _]
   (let [table-setting-id (get-in worksheet [:worksheet/table-settings :db/id])
         enabled?         (get-in worksheet [:worksheet/table-settings :table-settings/enabled?])]
     {:transact [{:db/id                   table-setting-id
                  :table-settings/enabled? (not enabled?)}]})))

(rp/reg-event-fx
 :worksheet/toggle-map-units-settings
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet ws-uuid]))]
 (fn [{:keys [worksheet]} [_ ws-uuid]]
   (let [map-units-settings (get-in worksheet [:worksheet/table-settings
                                               :table-settings/map-units-settings])]
     (if-let [map-units-setting-eid (:db/id map-units-settings)]
       (let [enabled? (:map-units-settings/enabled? map-units-settings)]
         {:transact [{:db/id                       map-units-setting-eid
                      :map-units-settings/enabled? (not enabled?)}]})
       {:fx [[:dispatch [:worksheet/upsert-table-setting-map-units
                         ws-uuid
                         :map-units-settings/enabled?
                         true]]]}))))

(rp/reg-event-fx
 :worksheet/add-y-axis-limit
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet ws-uuid]))]
 (fn [{:keys [worksheet]} [_ ws-uuid gv-uuid]]
   (let [limit {:y-axis-limit/group-variable-uuid gv-uuid}]
     (if-let [id (get-in worksheet [:worksheet/graph-settings :db/id])]
       {:transact [{:db/id                        id
                    :graph-settings/y-axis-limits [limit]}]}
       {:transact [{:worksheet/_graph-settings    [:worksheet/uuid ws-uuid]
                    :graph-settings/y-axis-limits [limit]}]}))))

(rp/reg-event-fx
 :worksheet/remove-y-axis-limit
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid gv-uuid]]
   (when-let [eid (d/q '[:find  ?y .
                         :in    $ ?uuid ?gv-uuid
                         :where
                         [?w :worksheet/uuid ?uuid]
                         [?w :worksheet/graph-settings ?g]
                         [?g :graph-settings/y-axis-limits ?y]
                         [?y :y-axis-limit/group-variable-uuid ?gv-uuid]]
                       ds ws-uuid gv-uuid)]
     {:transact [[:db.fn/retractEntity eid]]})))

(rp/reg-event-fx
 :worksheet/upsert-x-axis-limit
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet ws-uuid]))
  (rf/inject-cofx ::inject/sub (fn [[_ ws-uuid gv-uuid]] [:wizard/x-axis-limit-min+max-defaults ws-uuid gv-uuid]))]
 (fn [{worksheet :worksheet
       defaults  :wizard/x-axis-limit-min+max-defaults} [_ ws-uuid gv-uuid]]
   (let [id                        (get-in worksheet [:worksheet/graph-settings :db/id])
         [default-min default-max] defaults]
     {:transact [{:db/id                        (or id [:worksheet/uuid ws-uuid])
                  :graph-settings/x-axis-limits {:x-axis-limit/group-variable-uuid gv-uuid
                                                 :x-axis-limit/min                 default-min
                                                 :x-axis-limit/max                 default-max}}]})))

(rp/reg-event-fx
 :worksheet/set-default-graph-settings
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet/multi-value-input-uuids ws-uuid]))
  (rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet ws-uuid]))]
 (fn [{worksheet               :worksheet
       multi-value-input-uuids :worksheet/multi-value-input-uuids} [_ ws-uuid]]
   (when (pos? (count multi-value-input-uuids))
     (let [graph-setting-id (get-in worksheet [:worksheet/graph-settings :db/id])
           gv-uuids         (sort-by #(deref (rf/subscribe [:wizard/discrete-group-variable? %])) multi-value-input-uuids)]
       (cond-> {:transact [; First clear any existing graph settings.
                           [:db/retract graph-setting-id :graph-settings/x-axis-group-variable-uuid]
                           [:db/retract graph-setting-id :graph-settings/y-axis-group-variable-uuid]
                           [:db/retract graph-setting-id :graph-settings/z-axis-group-variable-uuid]
                           ;; Then default any multi valued variables starting from the lowest to highest dimensions x->z.
                           (cond-> {:db/id                   graph-setting-id}

                             ;; sets default x-axis selection if available
                             (first multi-value-input-uuids)
                             (assoc :graph-settings/x-axis-group-variable-uuid (first gv-uuids))

                             ;; sets default z-axis selection if available
                             (second multi-value-input-uuids)
                             (assoc :graph-settings/z-axis-group-variable-uuid (second gv-uuids))

                             ;; sets default z2-axis selection if available
                             (nth multi-value-input-uuids 2 nil)
                             (assoc :graph-settings/z2-axis-group-variable-uuid (nth gv-uuids 2)))]}
         (first gv-uuids)
         (assoc :fx [[:dispatch [:worksheet/upsert-x-axis-limit ws-uuid (first gv-uuids)]]]))))))

(rp/reg-event-fx
 :worksheet/toggle-graph-settings
 (rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet ws-uuid]))
 (fn [{worksheet :worksheet} [_ ws-uuid]]
   (let [graph-setting-id (get-in worksheet [:worksheet/graph-settings :db/id])
         enabled?         (get-in worksheet [:worksheet/graph-settings :graph-settings/enabled?])]
     {:transact [{:db/id                   graph-setting-id
                  :graph-settings/enabled? (if (nil? enabled?)
                                             false
                                             (not enabled?))}]
      :fx       [[:dispatch [:worksheet/set-default-graph-settings ws-uuid]]]})))

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
 :worksheet/update-all-y-axis-limits-from-results
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet/output-uuid->result-min-values ws-uuid]))
  (rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet/output-uuid->result-max-values ws-uuid]))]
 (fn [{output-uuid->result-min-values :worksheet/output-uuid->result-min-values
       output-uuid->result-max-values :worksheet/output-uuid->result-max-values}
      [_ ws-uuid]]
   (let [gv-uuids (keys output-uuid->result-min-values)]
     {:fx (reduce (fn [acc gv-uuid]
                    (let [max-val (get output-uuid->result-max-values gv-uuid)]
                      (-> acc
                          (conj [:dispatch [:worksheet/update-y-axis-limit-attr
                                            ws-uuid
                                            gv-uuid
                                            :y-axis-limit/min
                                            0]])
                          (conj [:dispatch [:worksheet/update-y-axis-limit-attr
                                            ws-uuid
                                            gv-uuid
                                            :y-axis-limit/max
                                            (if (< max-val 1)
                                              (to-precision max-val 1)
                                              (.ceil js/Math max-val))]]))))
                  []
                  gv-uuids)})))

(rp/reg-event-fx
 :worksheet/update-x-axis-limit-attr
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid attr value]]
   (when-let [eid (d/q '[:find ?y .
                         :in    $ ?ws-uuid
                         :where
                         [?w :worksheet/uuid ?ws-uuid]
                         [?w :worksheet/graph-settings ?g]
                         [?g :graph-settings/x-axis-limits ?y]]
                       ds
                       ws-uuid)]
     {:transact [(assoc {:db/id eid} attr value)]})))

(rp/reg-event-fx
 :worksheet/update-table-filter-attr
 [(rp/inject-cofx :ds)]
 (fn [{ds :ds} [_ ws-uuid group-var-uuid attr value]]
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
     {:transact [(assoc {:db/id table-filter-id} attr value)]})))

(rp/reg-event-fx
 :worksheet/add-table-filter
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet-entity ws-uuid]))]
 (fn [{:keys [worksheet-entity]} [_ ws-uuid gv-uuid]]
   (let [existing-filters (->> (:worksheet/table-settings worksheet-entity)
                               :table-settings/filters
                               (map :table-filter/group-variable-uuid)
                               set)
         filter-entry     {:table-filter/group-variable-uuid gv-uuid
                           :table-filter/enabled?            false}]
     (when (not (contains? existing-filters gv-uuid))
       (if-let [id (get-in worksheet-entity [:worksheet/table-settings :db/id])]
         {:transact [{:db/id                  id
                      :table-settings/filters [filter-entry]}]}
         {:transact [{:worksheet/_table-settings [:worksheet/uuid ws-uuid]
                      :table-settings/filters    [filter-entry]}]})))))

(rp/reg-event-fx
 :worksheet/update-all-table-filters-from-results
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet/output-min+max-values ws-uuid]))]
 (fn [{output-min-max-values :worksheet/output-min+max-values} [_ ws-uuid]]
   {:fx (reduce (fn [acc [gv-uuid [min-val max-val]]]
                  (-> acc
                      (conj [:dispatch [:worksheet/update-table-filter-attr
                                        ws-uuid
                                        gv-uuid
                                        :table-filter/min
                                        (if (< min-val 1)
                                          (to-precision min-val 1)
                                          (.floor js/Math min-val))]])
                      (conj [:dispatch [:worksheet/update-table-filter-attr
                                        ws-uuid
                                        gv-uuid
                                        :table-filter/max
                                        (if (< max-val 1)
                                          (to-precision max-val 1)
                                          (.ceil js/Math max-val))]])))
                []
                output-min-max-values)}))

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
 :worksheet/toggle-enable-filter
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
     (let [enabled? (:table-filter/enabled? (d/entity ds eid))]
       {:transact [{:db/id                 eid
                    :table-filter/enabled? (not enabled?)}]}))))

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
     {:transact [(cond-> {:db/id            -1
                          :worksheet/_notes ws-id
                          :note/name        (if (empty? title)
                                              (str submodule-name " " submodule-io)
                                              title)
                          :note/content     body}
                   submodule-uuid (assoc :note/submodule submodule-uuid))]})))

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
     (let [furthest-handler       (:worksheet/furthest-visited-route-handler worksheet)
           furthest-io            (:worksheet/furthest-visited-io worksheet)
           worksheet-visited-step (.indexOf step-priority [furthest-handler furthest-io])
           current-step           (.indexOf step-priority [route-handler io])]
       (when (or (and (nil? (:worksheet/furthest-visited-route-handler worksheet))
                      (nil? (:worksheet/furthest-visited-io worksheet)))
                 (< worksheet-visited-step current-step))
         {:transact (if io
                      [{:db/id                                    [:worksheet/uuid ws-uuid]
                        :worksheet/furthest-visited-route-handler route-handler
                        :worksheet/furthest-visited-io            io}]
                      [{:db/id                                    [:worksheet/uuid ws-uuid]
                        :worksheet/furthest-visited-route-handler route-handler}
                       [:db/retract [:worksheet/uuid ws-uuid] :worksheet/furthest-visited-io]])})))))

(rp/reg-event-fx
 :worksheet/set-furthest-vistited-step
 (fn [_ [_ ws-uuid route-handler io]]
   {:transact (if io
                [{:db/id                                    [:worksheet/uuid ws-uuid]
                  :worksheet/furthest-visited-route-handler route-handler
                  :worksheet/furthest-visited-io            io}]
                [{:db/id                                    [:worksheet/uuid ws-uuid]
                  :worksheet/furthest-visited-route-handler route-handler}
                 [:db/retract [:worksheet/uuid ws-uuid] :worksheet/furthest-visited-io]])}))

(rf/reg-event-fx
 :worksheet/delete-existing-diagrams
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid]]
   (let [existing-eids (d/q '[:find  [?d ...]
                              :in    $ ?ws-uuid
                              :where
                              [?ws :worksheet/uuid     ?ws-uuid]
                              [?ws :worksheet/diagrams ?d]]
                            ds ws-uuid)
         payload       (mapv (fn [id] [:db.fn/retractEntity id]) existing-eids)]
     {:transact payload})))

(rp/reg-event-fx
 :worksheet/add-contain-diagram
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_
                    ws-uuid
                    title
                    group-variable-uuid
                    row-id
                    fire-perimeter-points-X
                    fire-perimeter-points-Y
                    length-to-width-ratio
                    fire-back-at-report
                    fire-head-at-report
                    fire-back-at-attack
                    fire-head-at-attack]]
   {:transact [{:worksheet/_diagrams                   [:worksheet/uuid ws-uuid]
                :worksheet.diagram/title               title
                :worksheet.diagram/group-variable-uuid group-variable-uuid
                :worksheet.diagram/row-id              row-id
                :worksheet.diagram/ellipses            [(let [l (- fire-head-at-report fire-back-at-report)
                                                              w (/ l length-to-width-ratio)]
                                                          {:ellipse/legend-id       "FirePerimiterAtReport"
                                                           :ellipse/semi-major-axis (/ l 2)
                                                           :ellipse/semi-minor-axis (/ w 2)
                                                           :ellipse/rotation        90
                                                           :ellipse/color           "blue"})
                                                        (let [l (- fire-head-at-attack fire-back-at-attack)
                                                              w (/ l length-to-width-ratio)]
                                                          {:ellipse/legend-id       "FirePerimiterAtAttack"
                                                           :ellipse/semi-major-axis (/ l 2)
                                                           :ellipse/semi-minor-axis (/ w 2)
                                                           :ellipse/rotation        90
                                                           :ellipse/color           "red"})]
                :worksheet.diagram/scatter-plots       [{:scatter-plot/legend-id     "FireLineConstructed"
                                                         :scatter-plot/color         "black"
                                                         :scatter-plot/x-coordinates fire-perimeter-points-X
                                                         :scatter-plot/y-coordinates fire-perimeter-points-Y}]}]}))

(rp/reg-event-fx
 :worksheet/add-surface-fire-shape-diagram
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_
                    ws-uuid
                    title
                    group-variable-uuid
                    row-id
                    elliptical-A
                    elliptical-B
                    direction-of-max-spread
                    wind-direction
                    _wind-speed
                    _elapsed-time]]
   (log "503:")
   (let [existing-eid    (d/q '[:find  ?d .
                                :in    $ ?uuid ?gv-uuid ?row-id
                                :where
                                [?ws :worksheet/uuid               ?uuid]
                                [?ws :worksheet/diagrams           ?d]
                                [?d  :worksheet.diagram/group-variable-uuid ?gv-uuid]
                                [?d  :worksheet.diagram/row-id              ?row-id]]
                              ds ws-uuid group-variable-uuid row-id)
         semi-major-axis (max elliptical-A elliptical-B )
         semi-minor-axis (min elliptical-A elliptical-B)]
     {:transact [(when existing-eid [:db.fn/retractEntity existing-eid])
                 {:worksheet/_diagrams                   [:worksheet/uuid ws-uuid]
                  :worksheet.diagram/title               title
                  :worksheet.diagram/group-variable-uuid group-variable-uuid
                  :worksheet.diagram/row-id              row-id
                  :worksheet.diagram/ellipses            [{:ellipse/legend-id       "SurfaceFire"
                                                           :ellipse/semi-major-axis semi-major-axis
                                                           :ellipse/semi-minor-axis semi-minor-axis
                                                           :ellipse/rotation        direction-of-max-spread
                                                           :ellipse/color           "red"}]
                  :worksheet.diagram/arrows              [{:arrow/legend-id "Wind"
                                                           :arrow/length    semi-major-axis
                                                           ;; :arrow/length   (* wind-speed elapsed-time)
                                                           ;; NOTE Using the wind speed converted to the
                                                           ;; chains makes this value possibly too large. The
                                                           ;; arrow points much further out of othe ellipse.
                                                           ;; Discuss if if we should use this or not.
                                                           :arrow/rotation  wind-direction
                                                           :arrow/color     "blue"}]}]})))
(rp/reg-event-fx
 :worksheet/add-wind-slope-spread-direction-diagram
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_
                    ws-uuid
                    title
                    group-variable-uuid
                    row-id
                    max-spread-dir
                    max-spread-rate
                    has-direction-of-interest?
                    interest-dir
                    interest-spread-rate
                    flanking-dir
                    flanking-spread-rate
                    backing-dir
                    backing-spread-rate
                    wind-dir
                    wind-speed]]
   (let [existing-eid (d/q '[:find  ?d .
                             :in    $ ?uuid ?gv-uuid ?row-id
                             :where
                             [?ws :worksheet/uuid               ?uuid]
                             [?ws :worksheet/diagrams           ?d]
                             [?d  :worksheet.diagram/group-variable-uuid ?gv-uuid]
                             [?d  :worksheet.diagram/row-id              ?row-id]]
                           ds ws-uuid group-variable-uuid row-id)]
     {:transact [(when existing-eid [:db.fn/retractEntity existing-eid])
                 {:worksheet/_diagrams                   [:worksheet/uuid ws-uuid]
                  :worksheet.diagram/title               title
                  :worksheet.diagram/group-variable-uuid group-variable-uuid
                  :worksheet.diagram/row-id              row-id
                  :worksheet.diagram/arrows              (cond-> [{:arrow/legend-id       "MaxSpread"
                                                                   :arrow/length   max-spread-rate
                                                                   :arrow/rotation max-spread-dir
                                                                   :arrow/color    "red"}

                                                                  {:arrow/legend-id       "Flanking1"
                                                                   :arrow/length   flanking-spread-rate
                                                                   :arrow/rotation flanking-dir
                                                                   :arrow/color    "#81c3cb"}

                                                                  {:arrow/legend-id       "Flanking2"
                                                                   :arrow/length   flanking-spread-rate
                                                                   :arrow/rotation (mod (+ flanking-dir 180) 360)
                                                                   :arrow/color    "#347da0"}

                                                                  {:arrow/legend-id       "Backing"
                                                                   :arrow/length   backing-spread-rate
                                                                   :arrow/rotation backing-dir
                                                                   :arrow/color    "orange"}

                                                                  (let [l (min max-spread-rate wind-speed)]
                                                                    {:arrow/legend-id       "Wind"
                                                                     ;;NOTE for visual purposes
                                                                     ;;make wind 10% larger than
                                                                     ;;max spread rate.
                                                                     ;; :arrow/length   wind-speed
                                                                     :arrow/length   (if (> wind-speed max-spread-rate)
                                                                                       (* l 1.1)
                                                                                       l)
                                                                     :arrow/rotation wind-dir
                                                                     :arrow/color    "blue"
                                                                     :arrow/dashed?  true})]

                                                           has-direction-of-interest?
                                                           (conj {:arrow/legend-id "Interest"
                                                                  :arrow/length    interest-spread-rate
                                                                  :arrow/rotation  interest-dir
                                                                  :arrow/color     "black"}))}]})))

(rf/reg-event-fx
 :worksheet/delete-existing-result-table
 [(rp/inject-cofx :ds)]
 (fn [{:keys [ds]} [_ ws-uuid]]
   (when-let [existing-eid (d/q '[:find  ?t .
                                  :in    $ ?ws-uuid
                                  :where
                                  [?ws :worksheet/uuid     ?ws-uuid]
                                  [?ws :worksheet/result-table ?t]]
                                ds ws-uuid)]
     {:transact [[:db.fn/retractEntity existing-eid]]})))

(rp/reg-event-fx
 :worksheet/clear-input-value
 (fn [_ [_ input-eid]]
   {:transact [[:db/retract input-eid :input/value]]}))

(rf/reg-event-fx
 :worksheet/proccess-conditonally-set-output-group-variables

 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet  ws-uuid]))
  (rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:wizard/conditionally-set-group-variables ws-uuid :output]))]

 (fn [{worksheet       :worksheet
       group-variables :wizard/conditionally-set-group-variables} [_ ws-uuid]]
   (let [reset-map   (zipmap (map :bp/uuid group-variables) (repeat false))
         enabled-map (->> group-variables
                          (mapcat (fn [{group-variable-uuid :bp/uuid
                                        actions             :group-variable/actions}]
                                    (for [{conditionals    :action/conditionals
                                           conditionals-op :action/conditionals-operator} actions
                                          :when                                           (all-conditionals-pass? worksheet conditionals-op conditionals)]
                                      [group-variable-uuid true])))
                          (into {}))
         merged-map  (merge reset-map enabled-map)
         payload     (mapv (fn [[gv-uuid v]] [:dispatch [:worksheet/upsert-output ws-uuid gv-uuid v]]) merged-map)]
     {:fx payload})))

(rf/reg-event-fx
 :worksheet/select-single-select-output

 [(rf/inject-cofx ::inject/sub (fn [[_ _ group-eid]] [:vms/entity-from-eid  group-eid]))]

 (fn [{group :vms/entity-from-eid} [_ ws-uuid _group-id target-group-variable-uuid]]
   (let [siblings (remove #(= (:bp/uuid %) target-group-variable-uuid)
                          (:group/group-variables group))]
     {:fx (into [[:dispatch [:worksheet/upsert-output ws-uuid target-group-variable-uuid true]]]
                (mapv #(identity
                        [:dispatch [:worksheet/upsert-output
                                    ws-uuid
                                    (:bp/uuid %)
                                    false]])
                      siblings))})))

(rf/reg-event-fx
 :worksheet/proccess-conditonally-set-input-group-variables
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:wizard/conditionally-set-input-data ws-uuid]))]
 (fn [{data :wizard/conditionally-set-input-data}
      [_ ws-uuid]]
   (let [payload (for [[group-uuid group-variable-uuid default-value] data
                       :when                                          default-value]
                   [:dispatch [:wizard/upsert-input-variable
                               ws-uuid group-uuid 0 group-variable-uuid default-value]])]
     {:fx payload})))
