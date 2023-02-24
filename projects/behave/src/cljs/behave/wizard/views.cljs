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
            [dom-utils.interface            :refer [input-int-value]]
            [goog.string                    :as gstring]
            [goog.string.format]
            [re-frame.core                  :refer [dispatch subscribe]]
            [reagent.core                   :as r]
            [string-utils.interface         :refer [->kebab]]))

;;; Components

(defmulti submodule-page (fn [io _ _] io))

(defmethod submodule-page :input [_ ws-uuid groups on-back on-next]
  [:<>
   (for [group groups]
     (let [variables (:group/group-variables group)]
       ^{:key (:db/id group)}
       [input-group ws-uuid group variables]))])

(defmethod submodule-page :output [_ ws-uuid groups on-back on-next]
  [:<>
   (for [group groups]
     (let [variables (:group/group-variables group)]
       ^{:key (:db/id group)}
       [output-group ws-uuid group variables]))])

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

(defn wizard-page [{:keys [module io submodule ws-uuid] :as params}]
  (let [*module                  (subscribe [:wizard/*module module])
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
      [submodule-page io ws-uuid @*groups on-back on-next]
      (when (true? @*warn-limit?)
        [:div.wizard-warning
         (gstring/format  @(<t (bp "warn_input_limit")) @*multi-value-input-count @*multi-value-input-limit)])]
     [wizard-navigation {:next-label     @(<t (bp "next"))
                         :on-next        on-next
                         :back-label     @(<t (bp "back"))
                         :on-back        on-back
                         :next-disabled? next-disabled?}]]))

(defn run-description [ws-uuid]
  (let [*values  (subscribe [:worksheet/get-attr ws-uuid :worksheet/run-description])]
    [:div.wizard-review__run-desciption
     [:div.wizard-review__run-description__header
      @(<t "behaveplus:run_description")]
     [:div.wizard-review-group__inputs
      [:div.wizard-review__run-description__input
       [c/text-input {:label       @(<t (bp "run_description"))
                      :placeholder @(<t (bp "type_description"))
                      :id          (->kebab @(<t (bp "run_description")))
                      :value       (or (first @*values) "")
                      :on-change   #(dispatch [:worksheet/update-attr
                                               ws-uuid
                                               :worksheet/run-description
                                               (-> % .-target .-value)])}]
       [:div.wizard-review__run-description__message
        [c/button {:label         (gstring/format "*%s"  @(<t (bp "optional")))
                   :variant       "transparent-highlight"
                   :icon-name     :help2
                   :icon-position "left"}]
        @(<t (bp "a_brief_phrase_documenting_the_run"))]]]]))

(defn wizard-review-page [{:keys [ws-uuid] :as params}]
  (let [*modules                 (subscribe [:worksheet/modules ws-uuid])
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
        (for [module-kw @*modules
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
                   :on-click      #(dispatch [:wizard/solve params])}]]]]]))

;; Wizard Results Settings

(defn y-axis-limit-table [ws-uuid]
  (letfn [(number-inputs [min-or-max y-axis-limits]
            (map (fn [[gv-uuid saved-min saved-max]]
                   (c/number-input {:disabled? false
                                    :on-change #(dispatch [:worksheet/update-y-axis-limit-attr
                                                           ws-uuid
                                                           gv-uuid
                                                           (if (= min-or-max :min)
                                                             :y-axis-limit/min
                                                             :y-axis-limit/max)
                                                           (input-int-value %)])
                                    :value     (if (= min-or-max :min) saved-min saved-max)
                                    :min       10     ;TODO compute from results
                                    :max       100})) ;TODO compute from results
                 y-axis-limits))]
    (let [*y-axis-limits  (subscribe [:worksheet/graph-settings-y-axis-limits ws-uuid])
          names           (map (fn uuid->name [[uuid _]]
                                 (:variable/name
                                  @(subscribe [:wizard/group-variable uuid])))
                               @*y-axis-limits)
          output-ranges   (repeat (count @*y-axis-limits) "TODO compute from results")
          y-axis-minimums (number-inputs :min @*y-axis-limits)
          y-axis-maximums (number-inputs :max @*y-axis-limits)
          column-keys     [:col1 :col2 :col3 :col4]
          row-data        (map (fn [& args]
                                 (into {}
                                       (map (fn [x y] [x y])
                                            column-keys args)))
                               names
                               output-ranges
                               y-axis-minimums
                               y-axis-maximums)]
      [:div.y-axis-limit-table
       (c/table {:title   "Graph and Axis Limit"
                 :headers ["GRAPH Y VARIABLES" "OUTPUT RANGE" "Y AXIS MINIMUM" "Y AXIS MAXIMUM"]
                 :columns column-keys
                 :rows    row-data})])))

(defn wizard-results-settings-page [{:keys [ws-uuid] :as params}]
  (let [*multi-value-input-uuids (subscribe [:worksheet/multi-value-input-uuids ws-uuid])
        group-variables          (map #(deref (subscribe [:wizard/group-variable %])) @*multi-value-input-uuids)
        *notes                   (subscribe [:wizard/notes ws-uuid])
        *show-notes?             (subscribe [:wizard/show-notes?])
        on-back                  #(dispatch [:wizard/prev-tab params])
        on-next                  #(dispatch [:navigate (path-for routes :ws/results :ws-uuid ws-uuid)])]
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
      [:div.accordion
       [:div.accordion__header
        [c/tab {:variant   "outline-primary"
                :selected? true
                :label     @(<t "behaveplus:working_area")}]]
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
          (let [enabled? (first @(subscribe [:worksheet/get-table-settings-attr
                                             ws-uuid
                                             :table-settings/enabled?]))]
            [c/checkbox {:label     "Display Table Results"
                         :checked?  (true? enabled?)
                         :on-change #(dispatch [:worksheet/toggle-table-settings ws-uuid])}])]]
        [:div.wizard-results__graph-settings
         [:div.wizard-results__graph-settings__header "Graph Settings"]
         [:div.wizard-results__graph-settings__content
          (let [enabled? (first @(subscribe [:worksheet/get-graph-settings-attr
                                             ws-uuid
                                             :graph-settings/enabled?]))]
            [:<>
             [c/checkbox {:label     "Display Graph Results"
                          :checked?  (true? enabled?)
                          :on-change #(dispatch [:worksheet/toggle-graph-settings ws-uuid])}]
             (when (true? enabled?)
               [:<>
                [radio-group {:label     "Select X axis variable:"
                              :attr      :graph-settings/x-axis-group-variable-uuid
                              :variables group-variables}]
                [radio-group {:label     "Select Z axis variable:"
                              :attr      :graph-settings/z-axis-group-variable-uuid
                              :variables group-variables}]
                [y-axis-limit-table ws-uuid]])])]]]
       [wizard-navigation {:next-label @(<t (bp "next"))
                           :on-next    on-next
                           :back-label @(<t (bp "back"))
                           :on-back    on-back}]])))

;; Wizard Results
(defn wizard-results-page [{:keys [ws-uuid] :as params}]
  (let [*notes         (subscribe [:wizard/notes ws-uuid])
        *tab-selected  (subscribe [:worksheet/results-tab-selected])
        *headers       (subscribe [:worksheet/result-table-headers-sorted ws-uuid])
        *cell-data     (subscribe [:worksheet/result-table-cell-data ws-uuid])
        graph-data     (->> @*cell-data
                            (group-by first)
                            (reduce (fn [acc [_row-id cell-data]]
                                      (conj acc
                                            (reduce (fn [acc [_row-id col-uuid value]]
                                                      (assoc acc
                                                             (-> (subscribe [:wizard/group-variable col-uuid])
                                                                 deref
                                                                 :variable/name)
                                                             value))
                                                    {}
                                                    cell-data)))
                                    []))
        table-enabled? (first @(subscribe [:worksheet/get-table-settings-attr
                                           ws-uuid
                                           :table-settings/enabled?]))]
    [:div.accordion
     [:div.accordion__header
      [c/tab {:variant   "outline-primary"
              :selected? true
              :label     @(<t "behaveplus:working_area")}]]
     [:div.wizard-page
      [:div.wizard-header
       [:div.wizard-header__banner {:style {:margin-top "20px"}}
        [:div.wizard-header__banner__icon
         [c/icon :modules]]
        "Results"]]
      [c/tab-group {:variant  "outline-highlight"
                    :on-click #(dispatch [:wizard/results-scroll-into-view :tab])
                    :tabs     [{:label     "Notes"
                                :tab       :notes
                                :selected? (= @*tab-selected :notes)
                                :icon-name :notes}
                               {:label     "Table"
                                :tab       :table
                                :icon-name :tables
                                :selected? (= @*tab-selected :table)}
                               {:label     "Graph"
                                :tab       :graph
                                :icon-name :graphs
                                :selected? (= @*tab-selected :graph)}]}]
      [:div.wizard-results__content
       (wizard-notes @*notes)
       (when (and table-enabled? (seq @*cell-data))
         [:div.wizard-results__table
          (c/table {:title   "Results Table"
                    :headers (mapv (fn resolve-uuid [[_order uuid units]]
                                     (gstring/format "%s (%s)"
                                                     (:variable/name @(subscribe [:wizard/group-variable uuid]))
                                                     units))
                                   @*headers)
                    :columns (map second @*headers)
                    :rows    (->> (group-by first @*cell-data)
                                  (sort-by key)
                                  (map (fn [[_ data]]
                                         (reduce (fn [acc [_row-id uuid value]]
                                                   (assoc acc (keyword uuid) value))
                                                 {}
                                                 data))))})])
       (chart {:data   graph-data
               :x      {:name (:variable/name @(subscribe [:wizard/group-variable "fbbf73f6-3a0e-4fdd-b913-dcc50d2db311"]))}       ;TODO read value from datahike
               :y      {:name  (:variable/name @(subscribe [:wizard/group-variable "b7873139-659e-4475-8d41-0cf6c36da893"]))
                        :scale [0 120]}                                                                                      ;TODO read value from datahike
               :z      {:name (:variable/name @(subscribe [:wizard/group-variable "41503286-dfe4-457a-9b68-41832e049cc9"]))} ;TODO read value from datahike
               :z2     {:name    (:variable/name @(subscribe [:wizard/group-variable "30493fc2-a231-41ee-a16a-875f00cf853f"]))
                        :columns 2}
               :width  500
               :height 500})]]
     [:div.wizard-navigation
      [c/button {:label    "Back"
                 :variant  "secondary"
                 :on-click #(dispatch [:wizard/prev-tab params])}]]]))

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
