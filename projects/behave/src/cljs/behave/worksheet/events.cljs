(ns behave.worksheet.events
  (:require [re-frame.core    :as rf]
            [re-posh.core     :as rp]
            [datascript.core  :as d]
            [behave.components.toolbar :refer [get-step-number step-kw->number get-step-kw]]
            [behave.importer           :refer [import-worksheet]]
            [behave.logger             :refer [log]]
            [behave.solver.core        :refer [solve-worksheet]]
            [vimsical.re-frame.cofx.inject :as inject]
            [clojure.string :as str]))

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
   (let [tx {:worksheet/uuid    (or uuid (str (d/squuid)))
             :worksheet/modules modules
             :worksheet/created (.now js/Date)}]
     {:transact [(merge tx (when name {:worksheet/name name}))]})))

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
 :worksheet/toggle-graph-settings
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet/multi-value-input-uuids ws-uuid]))
  (rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet ws-uuid]))]
 (fn [{worksheet               :worksheet
       multi-value-input-uuids :worksheet/multi-value-input-uuids} _]
   (let [graph-setting-id (get-in worksheet [:worksheet/graph-settings :db/id])
         enabled?         (get-in worksheet [:worksheet/graph-settings :graph-settings/enabled?])]
     {:transact [(cond-> {:db/id                   graph-setting-id
                          :graph-settings/enabled? (not enabled?)}

                   ;; sets default x-axis selection if available
                   (first multi-value-input-uuids)
                   (assoc :graph-settings/x-axis-group-variable-uuid (first multi-value-input-uuids))

                   ;; sets default z-axis selection if available
                   (second multi-value-input-uuids)
                   (assoc :graph-settings/z-axis-group-variable-uuid (second multi-value-input-uuids))

                   ;; sets default z2-axis selection if available
                   (nth multi-value-input-uuids 2 nil)
                   (assoc :graph-settings/z2-axis-group-variable-uuid (nth multi-value-input-uuids 2)))]})))

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
                    (let [min-val (get output-uuid->result-min-values gv-uuid)
                          max-val (get output-uuid->result-max-values gv-uuid)]
                      (-> acc
                          (conj [:dispatch [:worksheet/update-y-axis-limit-attr
                                            ws-uuid
                                            gv-uuid
                                            :y-axis-limit/min
                                            (.floor js/Math min-val)]])
                          (conj [:dispatch [:worksheet/update-y-axis-limit-attr
                                            ws-uuid
                                            gv-uuid
                                            :y-axis-limit/max
                                            (.ceil js/Math max-val)]]))))
                  []
                  gv-uuids)})))

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
 [(rf/inject-cofx ::inject/sub (fn [[_ ws-uuid]] [:worksheet ws-uuid]))]
 (fn [{:keys [worksheet]} [_ ws-uuid gv-uuid]]
   (let [filter {:table-filter/group-variable-uuid gv-uuid
                 :table-filter/enabled?            false}]
     (if-let [id (get-in worksheet [:worksheet/table-settings :db/id])]
       {:transact [{:db/id                  id
                    :table-settings/filters [filter]}]}
       {:transact [{:worksheet/_table-settings [:worksheet/uuid ws-uuid]
                    :table-settings/filters    [filter]}]}))))

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
                                        (.floor js/Math min-val)]])
                      (conj [:dispatch [:worksheet/update-table-filter-attr
                                        ws-uuid
                                        gv-uuid
                                        :table-filter/max
                                        (.ceil js/Math max-val)]])))
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
