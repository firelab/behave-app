(ns behave.wizard.views
  (:require [behave.components.core               :as c]
            [behave.components.input-group        :refer [input-group]]
            [behave.components.review-input-group :as review]
            [behave.components.navigation         :refer [wizard-navigation]]
            [behave.components.output-group       :refer [output-group]]
            [behave.components.results.diagrams   :refer [result-diagrams]]
            [behave.components.results.matrices   :refer [result-matrices]]
            [behave.components.results.graphs     :refer [result-graphs]]
            [behave.components.results.table      :refer [result-table]]
            [behave.tool.views                    :refer [tool tool-selector]]
            [behave-routing.main                  :refer [routes]]
            [behave.translate                     :refer [<t bp]]
            [behave.wizard.events]
            [behave.wizard.subs]
            [bidi.bidi                            :refer [path-for]]
            [behave.worksheet.events]
            [behave.worksheet.subs]
            [dom-utils.interface                  :refer [input-int-value input-value]]
            [goog.string                          :as gstring]
            [goog.string.format]
            [re-frame.core                        :refer [dispatch dispatch-sync subscribe]]
            [string-utils.interface               :refer [->kebab]]
            [reagent.core                         :as r]))

;;; Components

(defn build-groups [ws-uuid groups component-fn & [level]]
  (let [level (if (nil? level) 0 level)]
    (when groups
      [:<>
       (doall
        (for [group groups]
          ^{:key (:db/id group)}
          (when (and (not (:group/research? group)) ;; TODO: Remove when "Research Mode" is enabled
                     @(subscribe [:wizard/show-group?
                                  ws-uuid
                                  (:db/id group)
                                  (:group/conditionals-operator group)]))
            (let [variables (->> group (:group/group-variables) (sort-by :group-variable/variable-order))]
              [:<>
               [component-fn ws-uuid group variables level]
               [:div.wizard-subgroup__indent
                (build-groups ws-uuid (sort-by :group/order (:group/children group)) component-fn (inc level))]]))))])))

(defmulti submodule-page (fn [io _ _] io))

(defmethod submodule-page :input [_ ws-uuid groups]
  [:<> (build-groups ws-uuid groups input-group)])

(defmethod submodule-page :output [_ ws-uuid groups]
  [:<> (build-groups ws-uuid groups output-group)])

(defn- io-tabs [module-id {:keys [ws-uuid io] :as params}]
  (let [i-subs          @(subscribe [:wizard/submodules-conditionally-filtered ws-uuid module-id :input])
        o-subs          @(subscribe [:wizard/submodules-conditionally-filtered ws-uuid module-id :output])
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

(defn- wizard-header [{module-name :module/name
                       module-id   :db/id}
                      {:keys [ws-uuid io submodule] :as params}]
  (let [*submodules         (if (= io :output)
                              (subscribe [:wizard/submodules-io-output-only module-id])
                              (subscribe [:wizard/submodules-io-input-only module-id]))
        ;;(Kcheung) Not able to use :wizard/submodules-conditionally-filtered because the submodule tabs
        ;;would not rerender when conditionals are met without hard refresh. so had to filter
        ;;:wizard/show-submodule? outside of the subscription.
        submodules-filtered (filter (fn [{id :db/id
                                          op :submodule/conditionals-operator}]
                                      @(subscribe [:wizard/show-submodule? ws-uuid id op]))
                                    @*submodules)
        *show-notes?        (subscribe [:wizard/show-notes?])]
    [:div.wizard-header
     [io-tabs module-id params]
     [:div.wizard-header__banner
      [:div.wizard-header__banner__icon
       [c/icon :modules]]
      [:div.wizard-header__banner__title
       (str module-name " Module")]
      [:div.wizard-header__banner__notes-button
       (show-or-close-notes-button @*show-notes?)]]
     [:div.wizard-header__submodule-tabs
      {:data-theme-color module-name}
      [c/tab-group {:variant  "outline-primary"
                    :on-click #(dispatch [:wizard/select-tab (assoc params :submodule (:tab %))])
                    :tabs     (map (fn [{s-name :submodule/name slug :slug}]
                                     {:label     s-name
                                      :tab       slug
                                      :selected? (= submodule slug)})
                                   submodules-filtered)}]]]))

(defn wizard-note
  [{:keys [note display-submodule-headers?]}]
  (r/with-let [[note-id
                note-name
                note-content
                submodule-name
                submodule-io] note
               content-atom   (r/atom note-content)
               title-atom     (r/atom note-name)]
    (if @(subscribe [:wizard/edit-note? note-id])
      [:div.wizard-note
       [:div.wizard-note__content
        (when display-submodule-headers?
          [:div.wizard-note__module (str "Note: " submodule-name "'s " (name submodule-io))])
        [c/note {:title-label       "Note's Name / Category"
                 :title-placeholder "Enter note's name or category"
                 :title-value       @title-atom
                 :body-value        @content-atom
                 :on-save           #((reset! content-atom (:body %))
                                      (reset! title-atom (:title %))
                                      (dispatch [:wizard/update-note note-id %]))}]]]
      [:div.wizard-note
       (when display-submodule-headers?
         [:div.wizard-note__module (str "Note: " submodule-name "'s " (name submodule-io))])
       [:div.wizard-note__name @title-atom]
       [:div.wizard-note__content @content-atom]
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
              [wizard-note {:note                       n
                            :display-submodule-headers? false}]))]))

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
        ;; *all-inputs-entered?     (subscribe [:worksheet/all-inputs-entered? ws-uuid module-id submodule])
        ;; *some-outputs-entered?   (subscribe [:worksheet/some-outputs-entered? ws-uuid module-id submodule])
        ;; next-disabled?           (not (if (= io :input) @*all-inputs-entered? @*some-outputs-entered?))
        ]
    [:div.wizard-page
     [:div
      [wizard-header @*module params]
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
          [wizard-notes @*notes]])
       [:div
        {:data-theme-color module}
        [submodule-page io ws-uuid @*groups]]
       (when (true? @*warn-limit?)
         [:div.wizard-warning
          (gstring/format  @(<t (bp "warn_input_limit")) @*multi-value-input-count @*multi-value-input-limit)])]]
     [wizard-navigation {:next-label @(<t (bp "next"))
                         :on-next    on-next
                         :back-label @(<t (bp "back"))
                         :on-back    on-back
                         ;;TODO to discuss or refine at a later date (2023-10-15 Kcheung)
                         ;; :next-disabled? next-disabled?
                         }]]))

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
        *show-notes?             (subscribe [:wizard/show-notes?])
        show-tool-selector?      @(subscribe [:tool/show-tool-selector?])
        selected-tool-uuid       @(subscribe [:tool/selected-tool-uuid])]
    [:<>
     (when show-tool-selector?
       [tool-selector])
     (when (some? selected-tool-uuid)
       [tool selected-tool-uuid])
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
              {:data-theme-color module-name}
              (gstring/format "%s Inputs"  @(<t (:module/translation-key module)))]
             [:div.wizard-review__submodule
              (for [submodule @(subscribe [:wizard/submodules-io-input-only (:db/id module)])
                    :let      [edit-route (path-for routes
                                                    :ws/wizard
                                                    :ws-uuid   ws-uuid
                                                    :module    module-name
                                                    :io        :input
                                                    :submodule (:slug submodule))]]
                [:<>
                 [:div.wizard-review__submodule-header (:submodule/name submodule)]
                 (build-groups  ws-uuid
                                (:submodule/groups submodule)
                                (partial review/input-group edit-route))])]])]
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
                     :on-click      #(do (dispatch-sync [:wizard/before-solve params])
                                         (dispatch-sync [:wizard/during-solve params])
                                         (dispatch-sync [:wizard/after-solve params]))}]]]]]]]))

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
        group-variables          (->> @*multi-value-input-uuids
                                      (map #(deref (subscribe [:wizard/group-variable %]))))
        enabled?                 (first @(subscribe [:worksheet/get-graph-settings-attr
                                                     ws-uuid
                                                     :graph-settings/enabled?]))
        multi-valued-input-uuids @(subscribe [:worksheet/multi-value-input-uuids ws-uuid])
        multi-valued-input-count (count multi-valued-input-uuids)]
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
         (cond-> [:<>]
           (>= multi-valued-input-count 1)
           (conj [radio-group {:label     "Select X axis variable:"
                               :attr      :graph-settings/x-axis-group-variable-uuid
                               :variables group-variables}])

           (>= multi-valued-input-count 2)
           (conj [radio-group {:label     "Select Z axis variable:"
                               :attr      :graph-settings/z-axis-group-variable-uuid
                               :variables group-variables}])
           (>= multi-valued-input-count 3)
           (conj [radio-group {:label     "Select Z2 axis variable:"
                               :attr      :graph-settings/z2-axis-group-variable-uuid
                               :variables group-variables}])
           :always
           (conj [settings-form {:ws-uuid     ws-uuid
                                 :title       "Graph and Axis Limit"
                                 :headers     ["GRAPH Y VARIABLES" "OUTPUT RANGE" "Y AXIS MINIMUM" "Y AXIS MAXIMUM"]
                                 :rf-event-id :worksheet/update-y-axis-limit-attr
                                 :rf-sub-id   :worksheet/graph-settings-y-axis-limits
                                 :min-attr-id :y-axis-limit/min
                                 :max-attr-id :y-axis-limit/max}])))])))

(defn- map-units-form
  [ws-uuid]
  (let [map-units-settings-entity @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        units                     (:map-units-settings/units map-units-settings-entity)
        map-rep-frac              (:map-units-settings/map-rep-fraction map-units-settings-entity)
        map-rep-frac-atom         (r/atom map-rep-frac)]
    [:div.table-settings__map-units-form
     [:div.table-settings__map-units-form__units
      [c/radio-group {:label   "Map Units"
                      :options (mapv (fn [option]
                                       {:value     option
                                        :label     option
                                        :on-change #(dispatch [:worksheet/upsert-table-setting-map-units
                                                               ws-uuid
                                                               :map-units-settings/units
                                                               option])
                                        :checked?  (= units option)})
                                     ["in" "cm"])}]]
     [:div.table-settings__map-units-form__map-rep-fraction
      [c/number-input {:label      "Map Representative Fraction (1:x)"
                       :value-atom map-rep-frac-atom
                       :required?  true
                       :on-change  #(reset! map-rep-frac-atom (input-value %))
                       :on-blur    #(dispatch [:worksheet/upsert-table-setting-map-units
                                               ws-uuid
                                               :map-units-settings/map-rep-fraction
                                               (long @map-rep-frac-atom)])}]]]))

(defn- table-settings [ws-uuid]
  (let [*worksheet                (subscribe [:worksheet ws-uuid])
        table-settings            (:worksheet/table-settings @*worksheet)
        table-enabled?            (:table-settings/enabled? table-settings)
        map-units-settings-entity @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units-enabled?        (:map-units-settings/enabled? map-units-settings-entity)]
    [:<> [c/checkbox {:label     "Display Table Results"
                      :checked?  table-enabled?
                      :on-change #(dispatch [:worksheet/toggle-table-settings ws-uuid])}]
     (when table-enabled?
       [:div.table-settings
        [c/checkbox {:label     "Convert to Map Units"
                     :checked?  map-units-enabled?
                     :on-change #(dispatch [:worksheet/toggle-map-units-settings ws-uuid])}]
        (when map-units-enabled?
          [map-units-form ws-uuid])
        [settings-form {:ws-uuid     ws-uuid
                        :title       "Table Filters"
                        :headers     ["Enabled?" "OUTPUT VARIABLES" "OUTPUT RESULTS RANGE" "MINIMUM" "MAXIMUM"]
                        :rf-event-id :worksheet/update-table-filter-attr
                        :rf-sub-id   :worksheet/table-settings-filters
                        :min-attr-id :table-filter/min
                        :max-attr-id :table-filter/max}]])]))

(defn wizard-results-settings-page [{:keys [route-handler io ws-uuid] :as params}]
  (let [_                   (dispatch [:worksheet/update-furthest-visited-step ws-uuid route-handler io])
        *notes              (subscribe [:wizard/notes ws-uuid])
        *show-notes?        (subscribe [:wizard/show-notes?])
        on-back             #(dispatch [:wizard/prev-tab params])
        on-next             #(dispatch [:navigate (path-for routes :ws/results :ws-uuid ws-uuid)])
        show-tool-selector? @(subscribe [:tool/show-tool-selector?])
        selected-tool-uuid  @(subscribe [:tool/selected-tool-uuid])]
    [:<>
     (when show-tool-selector?
       [tool-selector])
     (when (some? selected-tool-uuid)
       [tool selected-tool-uuid])
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
                          :on-back    on-back}]]]))

;; Wizard Results

(defn wizard-results-page [{:keys [route-handler io ws-uuid] :as params}]
  (let [*worksheet          (subscribe [:worksheet ws-uuid])
        *ws-date            (subscribe [:wizard/worksheet-date ws-uuid])
        _                   (dispatch [:worksheet/update-furthest-visited-step ws-uuid route-handler io])
        *notes              (subscribe [:wizard/notes ws-uuid])
        *tab-selected       (subscribe [:wizard/results-tab-selected])
        *cell-data          (subscribe [:worksheet/result-table-cell-data ws-uuid])
        table-enabled?      (get-in @*worksheet [:worksheet/table-settings :table-settings/enabled?])
        show-tool-selector? @(subscribe [:tool/show-tool-selector?])
        selected-tool-uuid  @(subscribe [:tool/selected-tool-uuid])]
    [:<>
     (when show-tool-selector?
       [tool-selector])
     (when (some? selected-tool-uuid)
       [tool selected-tool-uuid])
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
                                    :selected? (= @*tab-selected :graph)}
                                   {:label     "Diagram"
                                    :tab       :diagram
                                    :icon-name :graphs
                                    :selected? (= @*tab-selected :diagram)}]}]]]
        [:div.wizard-page__body
         [:div.wizard-results__notes {:id "notes"}
          (wizard-notes @*notes)]
         (when (and table-enabled? (seq @*cell-data))
           [:div.wizard-results__table {:id "table"}
            [:div.wizard-notes__header @(<t (bp "outputs_table"))]
            [result-matrices ws-uuid]
            [:div.wizard-notes__header @(<t (bp "runs_table"))]
            [result-table ws-uuid]])
         (result-graphs ws-uuid @*cell-data)
         (result-diagrams ws-uuid)]]
       [:div.wizard-navigation
        [c/button {:label    "Back"
                   :variant  "secondary"
                   :on-click #(dispatch [:wizard/prev-tab params])}]]]]]))

;; TODO Might want to set this in a config file to the application
(def ^:const multi-value-input-limit 3)

;;; Public Components
(defn root-component [params]
  (let [loaded?             (subscribe [:app/loaded?])
        show-tool-selector? @(subscribe [:tool/show-tool-selector?])
        selected-tool-uuid  @(subscribe [:tool/selected-tool-uuid])]
    [:<>
     (when show-tool-selector?
       [tool-selector])
     (when (some? selected-tool-uuid)
       [tool selected-tool-uuid])
     [:div.accordion
      [:div.accordion__header
       [c/tab {:variant   "outline-primary"
               :selected? true
               :label     @(<t "behaveplus:working_area")}]]
      [:div.wizard
       (if @loaded?
         [wizard-page params]
         [:div.wizard__loading
          [:h2 "Loading..."]])]]]))
