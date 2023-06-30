(ns behave.wizard.views
  (:require [behave.components.core         :as c]
            [behave.components.input-group  :refer [input-group]]
            [behave.components.chart         :refer [chart]]
            [behave.components.review-input-group  :as review]
            [behave.components.navigation   :refer [wizard-navigation]]
            [behave.components.output-group :refer [output-group]]
            [behave-routing.main            :refer [routes]]
            [behave.translate               :refer [<t bp]]
            [behave.wizard.events]
            [behave.wizard.subs]
            [bidi.bidi                      :refer [path-for]]
            [behave.worksheet.events]
            [behave.worksheet.subs]
            [dom-utils.interface            :refer [input-int-value input-value]]
            [goog.string                    :as gstring]
            [goog.string.format]
            [re-frame.core                  :refer [dispatch subscribe]]
            [string-utils.interface         :refer [->kebab]]
            [reagent.core :as r]))

;;; Components

(defn build-groups [ws-uuid groups component-fn]
  (when groups
    [:<>
     (doall
      (for [group groups]
        (let [show-group? @(subscribe [:wizard/show-group? ws-uuid
                                       (:db/id group)
                                       (:group/conditionals-operator group)])]
          ^{:key (:db/id group)}
          (when show-group?
            [:<>
             [component-fn ws-uuid group (:group/group-variables group)]
             [:div.wizard-subgroup__indent
              (build-groups ws-uuid (:group/children group) component-fn)]]))))]))

(defmulti submodule-page (fn [io _ _] io))

(defmethod submodule-page :input [_ ws-uuid groups]
  [:<> (build-groups ws-uuid groups input-group)])

(defmethod submodule-page :output [_ ws-uuid groups]
  [:<> (build-groups ws-uuid groups output-group)])

(defn- io-tabs [submodules {:keys [io] :as params}]
  (let [[i-subs o-subs] (partition-by #(:submodule/io %) submodules)
        first-submodule (:slug (first (if (= io :input) o-subs i-subs)))]
    [:div.wizard-header__io-tabs
     [c/tab-group {:variant   "outline-primary"
                   :flat-edge "top"
                   :align     "right"
                   :on-click  #(when (not= io (:tab %))
                                 (dispatch [:wizard/select-tab (assoc params
                                                                      :io (:tab %)
                                                                      :submodule first-submodule)]))
                   :tabs      [{:label "Outputs" :tab :output :selected? (= io :output)}
                               {:label "Inputs" :tab :input :selected? (= io :input)}]}]]))

(defn- show-or-close-notes-button [show-notes?]
  (if show-notes?
    [:div.wizard-header__banner__notes-button--minus
     [c/button {:label         "Close Notes"
                :variant       "secondary"
                :icon-name     :minus
                :icon-position "left"
                :on-click      #(dispatch [:wizard/toggle-show-notes])}]]
    [:div.wizard-header__banner__notes-button--plus
     [c/button {:label         "Show Notes"
                :variant       "outline-primary"
                :icon-name     :plus
                :icon-position "left"
                :on-click      #(dispatch [:wizard/toggle-show-notes])}]]))

(defn- wizard-header [{module-name :module/name} all-submodules {:keys [io submodule] :as params}]
  (let [submodules   (filter #(= (:submodule/io %) io) all-submodules)
        *show-notes? (subscribe [:wizard/show-notes?])]
    [:div.wizard-header
     [io-tabs all-submodules params]
     [:div.wizard-header__banner
      [:div.wizard-header__banner__icon
       [c/icon :modules]]
      [:div.wizard-header__banner__title
       (str module-name " Module")]
      [:div.wizard-header__banner__notes-button
       (show-or-close-notes-button @*show-notes?)]]
     [:div.wizard-header__submodule-tabs
      [c/tab-group {:variant  "outline-primary"
                    :on-click #(dispatch [:wizard/select-tab (assoc params :submodule (:tab %))])
                    :tabs     (map (fn [{s-name :submodule/name slug :slug}]
                                     {:label     s-name
                                      :tab       slug
                                      :selected? (= submodule slug)}) submodules)}]]]))

(defn wizard-note
  [{:keys [note display-submodule-headers?]}]
  (let [[note-id note-name note-content submodule-name submodule-io] note]
    (if @(subscribe [:wizard/edit-note? note-id])
      [:div.wizard-note
       [:div.wizard-note__content
        (when display-submodule-headers?
          [:div.wizard-note__module (str "Note: " submodule-name "'s " (name submodule-io))])
        [c/note {:title-label       "Note's Name / Category"
                 :title-placeholder "Enter note's name or category"
                 :title-value       note-name
                 :body-value        note-content
                 :on-save           #(dispatch [:wizard/update-note note-id %])}]]]
      [:div.wizard-note
       (when display-submodule-headers?
         [:div.wizard-note__module (str "Note: " submodule-name "'s " (name submodule-io))])
       [:div.wizard-note__name note-name]
       [:div.wizard-note__content note-content]
       [:div.wizard-note__manage
        [c/button {:variant   "primary"
                   :label     @(<t (bp "delete"))
                   :size      "small"
                   :icon-name "delete"
                   :on-click  #(dispatch [:worksheet/delete-note note-id])}]
        [c/button {:variant   "secondary"
                   :label     @(<t (bp "edit"))
                   :size      "small"
                   :icon-name "edit"
                   :on-click  #(dispatch [:wizard/edit-note note-id])}]]])))

(defn wizard-notes [notes]
  (when (seq notes)
    [:div.wizard-notes
     [:div.wizard-notes__header "Run's Notes"]
     (doall (for [[id & _rest :as n] notes]
              ^{:key id}
              (wizard-note {:note                       n
                            :display-submodule-headers? false})))]))

(defn wizard-page [{:keys [module io submodule route-handler ws-uuid] :as params}]
  (let [_                        (dispatch [:worksheet/update-furthest-visited-step ws-uuid route-handler io])
        *module                  (subscribe [:wizard/*module module])
        module-id                (:db/id @*module)
        *submodules              (subscribe [:wizard/submodules module-id])
        *submodule               (subscribe [:wizard/*submodule module-id submodule io])
        submodule-uuid           (:bp/uuid @*submodule)
        *notes                   (subscribe [:wizard/notes ws-uuid submodule-uuid])
        *groups                  (subscribe [:wizard/groups (:db/id @*submodule)])
        *warn-limit?             (subscribe [:wizard/warn-limit? ws-uuid])
        *multi-value-input-limit (subscribe [:wizard/multi-value-input-limit])
        *multi-value-input-count (subscribe [:wizard/multi-value-input-count ws-uuid])
        *show-notes?             (subscribe [:wizard/show-notes?])
        *show-add-note-form?     (subscribe [:wizard/show-add-note-form?])
        on-back                  #(dispatch [:wizard/prev-tab params])
        on-next                  #(dispatch [:wizard/next-tab @*module @*submodule @*submodules params])
        *all-inputs-entered?     (subscribe [:worksheet/all-inputs-entered? ws-uuid module-id submodule])
        *some-outputs-entered?   (subscribe [:worksheet/some-outputs-entered? ws-uuid module-id submodule])
        next-disabled?           (not (if (= io :input) @*all-inputs-entered? @*some-outputs-entered?))]
    [:div.wizard-page
     [wizard-header @*module @*submodules params]
     [:div.wizard-page__body
      (when @*show-notes?
        [:<>
         [:div.wizard-add-notes
          [c/button {:label         "Add Notes"
                     :variant       "outline-primary"
                     :icon-name     :plus
                     :icon-position "left"
                     :on-click      #(dispatch [:wizard/toggle-show-add-note-form])}]
          (when @*show-add-note-form?
            [c/note {:title-label       "Note's Name / Category"
                     :title-placeholder "Enter note's name or category"
                     :title-value       ""
                     :body-value        ""
                     :on-save           #(dispatch [:wizard/create-note
                                                    ws-uuid
                                                    submodule-uuid
                                                    (:submodule/name @*submodule)
                                                    (name (:submodule/io @*submodule))
                                                    %])}])]
         (wizard-notes @*notes)])
      [submodule-page io ws-uuid @*groups]
      (when (true? @*warn-limit?)
        [:div.wizard-warning
         (gstring/format  @(<t (bp "warn_input_limit")) @*multi-value-input-count @*multi-value-input-limit)])]
     [wizard-navigation {:next-label     @(<t (bp "next"))
                         :on-next        on-next
                         :back-label     @(<t (bp "back"))
                         :on-back        on-back
                         :next-disabled? next-disabled?}]]))

(defn run-description [ws-uuid]
  (let [*worksheet  (subscribe [:worksheet ws-uuid])
        description (:worksheet/run-description @*worksheet)
        value-atom  (r/atom (or description ""))]
    [:div.wizard-review__run-desciption
     [:div.wizard-review__run-description__header
      @(<t "behaveplus:run_description")]
     [:div.wizard-review-group__inputs
      [:div.wizard-review__run-description__input
       [c/text-input {:label       @(<t (bp "run_description"))
                      :placeholder @(<t (bp "type_description"))
                      :id          (->kebab @(<t (bp "run_description")))
                      :value-atom  value-atom
                      :on-change   #(reset! value-atom (input-value %))
                      :on-blur     #(dispatch [:worksheet/update-attr
                                             ws-uuid
                                             :worksheet/run-description
                                             (-> % .-target .-value)])}]
       [:div.wizard-review__run-description__message
        [c/button {:label         (gstring/format "*%s"  @(<t (bp "optional")))
                   :variant       "transparent-highlight"
                   :icon-name     :help2
                   :icon-position "left"}]
        @(<t (bp "a_brief_phrase_documenting_the_run"))]]]]))

(defn wizard-review-page [{:keys [io route-handler ws-uuid] :as params}]
  (let [_                        (dispatch [:worksheet/update-furthest-visited-step ws-uuid route-handler io])
        *worksheet               (subscribe [:worksheet ws-uuid])
        modules                  (:worksheet/modules @*worksheet)
        *warn-limit?             (subscribe [:wizard/warn-limit? ws-uuid])
        *multi-value-input-limit (subscribe [:wizard/multi-value-input-limit])
        *multi-value-input-count (subscribe [:wizard/multi-value-input-count ws-uuid])
        *notes                   (subscribe [:wizard/notes ws-uuid])
        *show-notes?             (subscribe [:wizard/show-notes?])]
    [:div.accordion
     [:div.accordion__header
      [c/tab {:variant   "outline-primary"
              :selected? true
              :label     @(<t (bp "working_area"))}]]

     [:div.wizard
      [:div.wizard-page
       [:div.wizard-header
        [:div.wizard-header__banner {:style {:margin-top "20px"}}
         [:div.wizard-header__banner__icon
          [c/icon :modules]]
         [:div.wizard-header__banner__title "Review Modules"]
         (show-or-close-notes-button @*show-notes?)]
        [:div.wizard-review
         [run-description ws-uuid]
         (when @*show-notes?
           (wizard-notes @*notes))
         (for [module-kw modules
               :let      [module-name (name module-kw)
                          module @(subscribe [:wizard/*module module-name])]]
           [:div
            [:div.wizard-review__module
             (gstring/format "%s Inputs"  @(<t (:module/translation-key module)))]
            [:div.wizard-review__submodule
             (for [submodule @(subscribe [:wizard/submodules-io-input-only (:db/id module)])]
               [:<>
                [:div.wizard-review__submodule-header (:submodule/name submodule)]
                (for [group (:submodule/groups submodule)
                      :when (seq (:group/group-variables group))
                      :let  [variables  (:group/group-variables group)
                             edit-route (path-for routes
                                                  :ws/wizard
                                                  :ws-uuid   ws-uuid
                                                  :module    module-name
                                                  :io        :input
                                                  :submodule (:slug submodule))]]
                  ^{:key (:db/id group)}
                  [review/input-group ws-uuid group variables edit-route])])]])]
        (when (true? @*warn-limit?)
          [:div.wizard-warning
           (gstring/format  @(<t (bp "warn_input_limit")) @*multi-value-input-count @*multi-value-input-limit)])
        [:div.wizard-navigation
         [c/button {:label    "Back"
                    :variant  "secondary"
                    :on-click #(dispatch [:wizard/prev-tab params])}]
         [c/button {:label         "Run"
                    :disabled?     @*warn-limit?
                    :variant       "highlight"
                    :icon-name     "arrow2"
                    :icon-position "right"
                    :on-click      #(dispatch [:wizard/solve params])}]]]]]]))

;; Wizard Results Settings

(defn update-setting-input [ws-uuid rf-event-id attr-id gv-uuid value]
  (dispatch [rf-event-id ws-uuid gv-uuid attr-id value]))

(defn number-input [{:keys [enabled? on-change value-atom default-value]}]
  (c/number-input {:disabled?  (if (some? enabled?)
                                 (not enabled?)
                                 false)
                   :on-change  #(let [v (input-int-value %)]
                                  (reset! value-atom (if (js/isNaN v) default-value v)))
                   :on-blur    #(on-change @value-atom)
                   :value-atom value-atom}))

(defn number-inputs
  [{:keys [saved-entries on-change uuid->default-values]}]
  (map (fn [[gv-uuid saved-value enabled?]]
         [number-input {:enabled?      enabled?
                        :default-value (uuid->default-values gv-uuid)
                        :on-change     #(on-change gv-uuid %)
                        :value-atom    (r/atom saved-value)}])
       saved-entries))

(defn settings-form
  [{:keys [ws-uuid title headers rf-event-id rf-sub-id min-attr-id max-attr-id]}]
  (let [*gv-uuid+min+max-entries   (subscribe [rf-sub-id ws-uuid])
        *default-max-values        (subscribe [:worksheet/output-uuid->result-max-values ws-uuid])
        *default-min-values        (subscribe [:worksheet/output-uuid->result-min-values ws-uuid])
        default-max-values-rounded (into {}
                                         (map (fn round-down [[uuid value]]
                                                [uuid (.ceil js/Math value)]))
                                         @*default-max-values)
        default-min-values-rounded (into {}
                                         (map (fn round-up [[uuid value]]
                                                [uuid (.floor js/Math value)]))
                                         @*default-min-values)
        maximums                   (number-inputs {:saved-entries        (map (fn remove-min-val[[gv-uuid _min-val max-val enabled?]]
                                                                                [gv-uuid max-val enabled?])
                                                                              @*gv-uuid+min+max-entries)
                                                   :uuid->default-values default-max-values-rounded
                                                   :on-change            #(update-setting-input ws-uuid rf-event-id max-attr-id %1 %2)})
        minimums                   (number-inputs {:saved-entries        (map (fn remove-max-val [[gv-uuid min-val _max-val enabled?]]
                                                                                [gv-uuid min-val enabled?])
                                                                              @*gv-uuid+min+max-entries)
                                                   :uuid->default-values default-min-values-rounded
                                                   :min-attr-id          max-attr-id
                                                   :on-change            #(update-setting-input ws-uuid rf-event-id min-attr-id %1 %2)})
        output-ranges              (map (fn [[gv-uuid & _rest]]
                                          (let [min-val (get @*default-min-values gv-uuid)
                                                max-val (get @*default-max-values gv-uuid)]
                                            (gstring/format "%.2f - %.2f" min-val max-val))) ;TODO BHP1-257: Worksheet Settings for units and decimals
                                        @*gv-uuid+min+max-entries)
        names                      (map (fn get-variable-name [[uuid _min _max]]
                                          (->> (subscribe [:wizard/group-variable uuid])
                                               deref
                                               :variable/name))
                                        @*gv-uuid+min+max-entries)
        enabled-check-boxes        (when (= rf-event-id :worksheet/update-table-filter-attr)
                                     (map (fn [[uuid _min _max enabled?]]
                                            [c/checkbox {:checked?  enabled?
                                                         :on-change #(dispatch [:worksheet/toggle-enable-filter ws-uuid uuid])}])
                                          @*gv-uuid+min+max-entries))
        column-keys                (mapv (fn [idx] (keyword (str "col" idx))) (range (count headers)))
        row-data                   (if enabled-check-boxes
                                     (map (fn build-row [& args]
                                            (into {}
                                                  (map (fn [x y] [x y])
                                                       column-keys args)))
                                          enabled-check-boxes
                                          names
                                          output-ranges
                                          minimums
                                          maximums)
                                     (map (fn build-row [& args]
                                            (into {}
                                                  (map (fn [x y] [x y])
                                                       column-keys args)))
                                          names
                                          output-ranges
                                          minimums
                                          maximums))]
    [:div.settings-form
     (c/table {:title   title
               :headers headers
               :columns column-keys
               :rows    row-data})]))

(defn- graph-settings [ws-uuid]
  (let [*multi-value-input-uuids (subscribe [:worksheet/multi-value-input-uuids ws-uuid])
        group-variables          (map #(deref (subscribe [:wizard/group-variable %])) @*multi-value-input-uuids)
        enabled?                 (first @(subscribe [:worksheet/get-graph-settings-attr
                                                     ws-uuid
                                                     :graph-settings/enabled?]))]
    (letfn [(radio-group [{:keys [label attr variables]}]
              (let [*values   (subscribe [:worksheet/get-graph-settings-attr ws-uuid attr])
                    selected? (first @*values)]
                [c/radio-group {:label   label
                                :options (mapv (fn [{group-var-uuid :bp/uuid
                                                     var-name       :variable/name}]
                                                 {:value     var-name
                                                  :label     var-name
                                                  :on-change #(dispatch [:worksheet/update-graph-settings-attr
                                                                         ws-uuid
                                                                         attr
                                                                         group-var-uuid])
                                                  :checked?  (= selected? group-var-uuid)})
                                               variables)}]))]
      [:<>
       [c/checkbox {:label     "Display Graph Results"
                    :checked?  enabled?
                    :on-change #(dispatch [:worksheet/toggle-graph-settings ws-uuid])}]
       (when enabled?
         [:<>
          [radio-group {:label     "Select X axis variable:"
                        :attr      :graph-settings/x-axis-group-variable-uuid
                        :variables group-variables}]
          [radio-group {:label     "Select Z axis variable:"
                        :attr      :graph-settings/z-axis-group-variable-uuid
                        :variables group-variables}]
          [radio-group {:label     "Select Z2 axis variable:"
                        :attr      :graph-settings/z2-axis-group-variable-uuid
                        :variables group-variables}]
          [settings-form {:ws-uuid     ws-uuid
                          :title       "Graph and Axis Limit"
                          :headers     ["GRAPH Y VARIABLES" "OUTPUT RANGE" "Y AXIS MINIMUM" "Y AXIS MAXIMUM"]
                          :rf-event-id :worksheet/update-y-axis-limit-attr
                          :rf-sub-id   :worksheet/graph-settings-y-axis-limits
                          :min-attr-id :y-axis-limit/min
                          :max-attr-id :y-axis-limit/max}]])])))

(defn- table-settings [ws-uuid]
  (let [*worksheet     (subscribe [:worksheet ws-uuid])
        table-settings (:worksheet/table-settings @*worksheet)
        enabled?       (:table-settings/enabled? table-settings)]
    [:<> [c/checkbox {:label     "Display Table Results"
                      :checked?  enabled?
                      :on-change #(dispatch [:worksheet/toggle-table-settings ws-uuid])}]
     (when enabled?
       [settings-form {:ws-uuid     ws-uuid
                       :title       "Table Filters"
                       :headers     ["Enabled?" "OUTPUT VARIABLES" "OUTPUT RESULTS RANGE" "MINIMUM" "MAXIMUM"]
                       :rf-event-id :worksheet/update-table-filter-attr
                       :rf-sub-id   :worksheet/table-settings-filters
                       :min-attr-id :table-filter/min
                       :max-attr-id :table-filter/max}])]))

(defn wizard-results-settings-page [{:keys [route-handler io ws-uuid] :as params}]
  (let [_            (dispatch [:worksheet/update-furthest-visited-step ws-uuid route-handler io])
        *notes       (subscribe [:wizard/notes ws-uuid])
        *show-notes? (subscribe [:wizard/show-notes?])
        on-back      #(dispatch [:wizard/prev-tab params])
        on-next      #(dispatch [:navigate (path-for routes :ws/results :ws-uuid ws-uuid)])]
    [:div.accordion
     [:div.accordion__header
      [c/tab {:variant   "outline-primary"
              :selected? true
              :label     @(<t "behaveplus:working_area")}]]
     [:div.wizard
      [:div.wizard-page
       [:div.wizard-header
        [:div.wizard-header__banner {:style {:margin-top "20px"}}
         [:div.wizard-header__banner__icon
          [c/icon :modules]]
         [:div.wizard-header__banner__title
          "Results Selection"]
         (show-or-close-notes-button @*show-notes?)]]
       (when @*show-notes?
         (wizard-notes @*notes))
       [:div.wizard-results__table-settings
        [:div.wizard-results__table-settings__header "Table Settings"]
        [:div.wizard-results__table-settings__content
         [table-settings ws-uuid]]]
       [:div.wizard-results__graph-settings
        [:div.wizard-results__graph-settings__header "Graph Settings"]
        [:div.wizard-results__graph-settings__content
         [graph-settings ws-uuid]]]]]
     [wizard-navigation {:next-label @(<t (bp "next"))
                         :on-next    on-next
                         :back-label @(<t (bp "back"))
                         :on-back    on-back}]]))

(defn- wizard-graph [ws-uuid cell-data]
  (letfn [(uuid->variable-name [uuid]
            (:variable/name @(subscribe [:wizard/group-variable uuid])))]
    (when-let [graph-settings @(subscribe [:worksheet/graph-settings ws-uuid])]
      (let [*output-uuids (subscribe [:worksheet/all-output-uuids ws-uuid])
            graph-data    (->> cell-data
                               (group-by first)
                               (reduce (fn [acc [_row-id cell-data]]
                                         (conj acc
                                               (reduce (fn [acc [_row-id col-uuid _repeat-id value]]
                                                         (assoc acc
                                                                (-> (subscribe [:wizard/group-variable col-uuid])
                                                                    deref
                                                                    :variable/name)
                                                                value))
                                                       {}
                                                       cell-data)))
                                       []))]
        [:div.wizard-results__graphs {:id "graph"}
         [:div.wizard-notes__header "Graph"]
         (for [output-uuid @*output-uuids
               :let        [y-axis-limit (->> (:graph-settings/y-axis-limits graph-settings)
                                              (filter #(= output-uuid (:y-axis-limit/group-variable-uuid %)))
                                              (first))
                            y-min (:y-axis-limit/min y-axis-limit)
                            y-max (:y-axis-limit/max y-axis-limit)]]
           [:div.wizard-results__graph
            (chart {:data   graph-data
                    :x      {:name (-> (:graph-settings/x-axis-group-variable-uuid graph-settings)
                                       (uuid->variable-name))}
                    :y      {:name  (:variable/name @(subscribe [:wizard/group-variable output-uuid]))
                             :scale [y-min y-max]}
                    :z      {:name (-> (:graph-settings/z-axis-group-variable-uuid graph-settings)
                                       (uuid->variable-name))}
                    :z2     {:name    (-> (:graph-settings/z2-axis-group-variable-uuid graph-settings)
                                          (uuid->variable-name))
                             :columns 2}
                    :width  250
                    :height 250})])]))))

;; Wizard Results
(defn wizard-results-page [{:keys [route-handler io ws-uuid] :as params}]
  (let [*worksheet            (subscribe [:worksheet ws-uuid])
        *ws-date              (subscribe [:wizard/worksheet-date ws-uuid])
        _                     (dispatch [:worksheet/update-furthest-visited-step ws-uuid route-handler io])
        *notes                (subscribe [:wizard/notes ws-uuid])
        *tab-selected         (subscribe [:wizard/results-tab-selected])
        *headers              (subscribe [:worksheet/result-table-headers-sorted ws-uuid])
        *cell-data            (subscribe [:worksheet/result-table-cell-data ws-uuid])
        table-enabled?        (get-in @*worksheet [:worksheet/table-settings :table-settings/enabled?])
        table-setting-filters (subscribe [:worksheet/table-settings-filters ws-uuid])]
    [:div.accordion
     [:div.accordion__header
      [c/tab {:variant   "outline-primary"
              :selected? true
              :label     @(<t "behaveplus:working_area")}]]
     [:div.wizard
      [:div.wizard-page
       [:div.wizard-header
        [:div.wizard-header__banner {:style {:margin-top "20px"}}
         [:div.wizard-header__banner__icon
          [c/icon :modules]]
         "Results"]
        [:div.wizard-header__results-toolbar
         [:div.wizard-header__results-toolbar__file-name
          [:div.wizard-header__results-toolbar__file-name__label (str @(<t (bp "file_name")) ":")]
          [:div.wizard-header__results-toolbar__file-name__value (:worksheet/name @*worksheet)]]
         [:div.wizard-header__results-toolbar__date
          [:div.wizard-header__results-toolbar__date__label (str @(<t (bp "run_date")) ":")]
          [:div.wizard-header__results-toolbar__date__value @*ws-date]]]
        [:div.wizard-header__results-tabs
         [c/tab-group {:variant  "outline-highlight"
                       :on-click #(dispatch [:wizard/results-select-tab %])
                       :tabs     [{:label     "Notes"
                                   :tab       :notes
                                   :icon-name :notes
                                   :selected? (= @*tab-selected :notes)}
                                  {:label     "Table"
                                   :tab       :table
                                   :icon-name :tables
                                   :selected? (= @*tab-selected :table)}
                                  {:label     "Graph"
                                   :tab       :graph
                                   :icon-name :graphs
                                   :selected? (= @*tab-selected :graph)}]}]]]
       [:div.wizard-page__body
        [:div.wizard-results__notes {:id "notes"}
         (wizard-notes @*notes)]
        (when (and table-enabled? (seq @*cell-data))
          [:div.wizard-results__table {:id "table"}
           [:div.wizard-notes__header "Table"]
           (c/table {:title   "Results Table"
                     :headers (mapv (fn resolve-uuid [[_order uuid _repeat-id units]]
                                      (gstring/format "%s (%s)"
                                                      (:variable/name @(subscribe [:wizard/group-variable uuid]))
                                                      units))
                                    @*headers)
                     :columns (mapv (fn [[_order uuid repeat-id _units]]
                                      (keyword (str uuid "-" repeat-id))) @*headers)
                     :rows    (->> (group-by first @*cell-data)
                                   (sort-by key)
                                   (map (fn [[_ data]]
                                          (reduce (fn [acc [_row-id uuid repeat-id value]]
                                                    (let [[_ min max enabled?] (first (filter
                                                                              (fn [[gv-uuid]]
                                                                                (= gv-uuid uuid))
                                                                              @table-setting-filters))]
                                                      (cond-> acc
                                                        (and min max (not (<= min value max)) enabled?)
                                                        (assoc :shaded? true)

                                                        :always
                                                        (assoc (keyword (str uuid "-" repeat-id)) value))))
                                                  {}
                                                  data))))})])
        (wizard-graph ws-uuid @*cell-data)]]
      [:div.wizard-navigation
       [c/button {:label    "Back"
                  :variant  "secondary"
                  :on-click #(dispatch [:wizard/prev-tab params])}]]]]))

;; TODO Might want to set this in a config file to the application
(def ^:const multi-value-input-limit 3)

;;; Public Components
(defn root-component [params]
  (let [loaded? (subscribe [:app/loaded?])]
    [:div.accordion
     [:div.accordion__header
      [c/tab {:variant   "outline-primary"
              :selected? true
              :label     @(<t "behaveplus:working_area")}]]
     [:div.wizard
      (if @loaded?
        [wizard-page params]
        [:div.wizard__loading
         [:h2 "Loading..."]])]]))
