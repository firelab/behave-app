(ns behave.wizard.views
  (:require [behave.components.core               :as c]
            [behave.components.input-group        :refer [input-group]]
            [behave.components.review-input-group :as review]
            [behave.components.navigation         :refer [wizard-navigation]]
            [behave.components.output-group       :refer [output-group]]
            [behave.components.results.diagrams   :refer [result-diagrams]]
            [behave.components.results.matrices   :refer [result-matrices]]
            [behave.components.results.graphs     :refer [result-graphs]]
            [behave.components.results.inputs.views :refer [inputs-table]]
            [behave.components.results.table      :refer [result-table-download-link]]
            [behave.tool.views                    :refer [tool tool-selector]]
            [behave-routing.main                  :refer [routes]]
            [behave.translate                     :refer [<t bp]]
            [behave.wizard.events]
            [behave.wizard.subs]
            [behave.components.results.inputs.subs]
            [bidi.bidi                            :refer [path-for]]
            [behave.worksheet.events]
            [behave.worksheet.subs]
            [dom-utils.interface                  :refer [input-int-value
                                                          input-float-value
                                                          input-value]]
            [goog.string                          :as gstring]
            [goog.string.format]
            [re-frame.core                        :refer [dispatch dispatch-sync subscribe]]
            [string-utils.interface               :refer [->kebab]]
            [reagent.core                         :as r]
            [behave.solver.core            :refer [solve-worksheet]]
            [string-utils.core :as s]
            [clojure.string :as str]))

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

(defn- io-tabs [first-module {:keys [ws-uuid io] :as params}]
  (let [module-id       (:db/id first-module)
        module-name     (str/lower-case (:module/name first-module))
        i-subs          @(subscribe [:wizard/submodules-conditionally-filtered ws-uuid module-id :input])
        o-subs          @(subscribe [:wizard/submodules-conditionally-filtered ws-uuid module-id :output])
        first-submodule (:slug (first (if (= io :input) o-subs i-subs)))]
    [:div.wizard-header__io-tabs
     [c/tab-group {:variant   "outline-primary"
                   :flat-edge "top"
                   :align     "right"
                   :on-click  #(when (not= io (:tab %))
                                 (dispatch [:wizard/select-tab (merge params
                                                                      {:module    module-name
                                                                       :io        (:tab %)
                                                                       :submodule first-submodule})]))
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

(defn- wizard-header [{:keys [ws-uuid io submodule module] :as params} modules]
  (let [*show-notes? (subscribe [:wizard/show-notes?])]
    [:div.wizard-header
     [io-tabs (first modules) params]
     [:div.wizard-header__banner
      [:div.wizard-header__banner__icon
       [c/icon :modules]]
      [:div.wizard-header__banner__title
       (str/join " and " (map :module/name modules))]
      [:div.wizard-header__banner__notes-button
       (show-or-close-notes-button @*show-notes?)]]
     [:div.wizard-header__submodules
      (for [m    modules
            :let [submodules (if (= io :output)
                                 (->> @(subscribe [:wizard/submodules-io-output-only (:db/id m)])
                                      (filter (fn [{id :db/id
                                                    op :submodule/conditionals-operator}]
                                                @(subscribe [:wizard/show-submodule? ws-uuid id op]))))
                                 (->> @(subscribe [:wizard/submodules-io-input-only (:db/id m)])
                                      (filter (fn [{id :db/id
                                                    op :submodule/conditionals-operator}]
                                                @(subscribe [:wizard/show-submodule? ws-uuid id op])))))
                    module-name (str/lower-case (:module/name m))]]
        [:div.wizard-header__submodules__group
         {:data-theme-color module-name}
         [c/tab-group {:variant  "outline-primary"
                       :on-click #(dispatch [:wizard/select-tab
                                             (merge params {:submodule (:tab %)
                                                            :module    module-name})])
                       :tabs     (map (fn [{s-name         :submodule/name
                                            submodule-slug :slug}]
                                        {:label     s-name
                                         :tab       submodule-slug
                                         :selected? (= (str module "-" submodule)
                                                       (str (str/lower-case module-name)
                                                            "-"
                                                            submodule-slug))})
                                      submodules)}]])]]))

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
  (dispatch-sync [:worksheet/update-furthest-visited-step ws-uuid route-handler io])
  (let [modules                  @(subscribe [:worksheet/modules ws-uuid])
        *module                  (subscribe [:wizard/*module module])
        module-id                (:db/id @*module)
        *submodule               (subscribe [:wizard/*submodule module-id submodule io])
        submodule-uuid           (:bp/uuid @*submodule)
        *notes                   (subscribe [:wizard/notes ws-uuid submodule-uuid])
        *groups                  (subscribe [:wizard/groups (:db/id @*submodule)])
        *warn-limit?             (subscribe [:wizard/warn-limit? ws-uuid])
        *multi-value-input-limit (subscribe [:wizard/multi-value-input-limit])
        *multi-value-input-count (subscribe [:wizard/multi-value-input-count ws-uuid])
        *show-notes?             (subscribe [:wizard/show-notes?])
        *show-add-note-form?     (subscribe [:wizard/show-add-note-form?])
        on-back                  #(dispatch [:wizard/back ws-uuid])
        on-next                  #(dispatch [:wizard/next ws-uuid])
        ;; *all-inputs-entered?     (subscribe [:worksheet/all-inputs-entered? ws-uuid module-id submodule])
        ;; *some-outputs-entered?   (subscribe [:worksheet/some-outputs-entered? ws-uuid module-id submodule])
        ;; next-disabled?           (not (if (= io :input) @*all-inputs-entered? @*some-outputs-entered?))
        ]
    [:div.wizard-page
     [:div
      [wizard-header params modules]
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

(defn wizard-review-page [{:keys [route-handler ws-uuid] :as params}]
  (dispatch-sync [:worksheet/update-furthest-visited-step ws-uuid route-handler nil])
  (let [modules                  @(subscribe [:worksheet/modules ws-uuid])
        *warn-limit?             (subscribe [:wizard/warn-limit? ws-uuid])
        *multi-value-input-limit (subscribe [:wizard/multi-value-input-limit])
        *multi-value-input-count (subscribe [:wizard/multi-value-input-count ws-uuid])
        *notes                   (subscribe [:wizard/notes ws-uuid])
        *show-notes?             (subscribe [:wizard/show-notes?])
        show-tool-selector?      @(subscribe [:tool/show-tool-selector?])
        selected-tool-uuid       @(subscribe [:tool/selected-tool-uuid])
        computing?               @(subscribe [:state :worksheet-computing?])]
    [:<>
     (when show-tool-selector?
       [tool-selector])
     (when (some? selected-tool-uuid)
       [tool selected-tool-uuid])
     (when computing?
       [:h2 "Computing..."])
     (when (not computing?)
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
            (for [module modules
                  :let   [module-name (:module/name module)]]
              [:div
               [:div.wizard-review__module
                {:data-theme-color module-name}
                (gstring/format "%s Inputs"  @(<t (:module/translation-key module)))]
               [:div.wizard-review__submodule
                (for [submodule @(subscribe [:wizard/submodules-conditionally-filtered
                                             ws-uuid
                                             (:db/id module)
                                             :input])
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
                       :on-click #(dispatch [:wizard/back ws-uuid])}]
            [c/button {:label         "Run"
                       :disabled?     @*warn-limit?
                       :variant       "highlight"
                       :icon-name     "arrow2"
                       :icon-position "right"
                       :on-click      #(do (dispatch-sync [:wizard/before-solve params])
                                           (js/setTimeout
                                            (fn []
                                              (dispatch-sync [:wizard/solve params])
                                              (dispatch-sync [:wizard/after-solve params]))
                                            300))}]]]]]])]))

;; Wizard Results Settings

(defn update-setting-input [ws-uuid rf-event-id attr-id gv-uuid value]
  (dispatch [rf-event-id ws-uuid gv-uuid attr-id value]))

(defn- number-inputs
  [{:keys [saved-entries on-change default-values]}]
  (map (fn [[gv-uuid saved-value enabled?]]
         (let [value-atom            (r/atom saved-value)
               show-tenth-precision? (< (get default-values gv-uuid) 1)]
           [c/number-input (cond-> {:disabled?  (if (some? enabled?)
                                                  (not enabled?)
                                                  false)
                                    :on-change  #(let [v (if show-tenth-precision?
                                                           (input-float-value %)
                                                           (input-int-value %))]
                                                   (reset! value-atom v))
                                    :on-blur    #(on-change gv-uuid @value-atom)
                                    :value-atom value-atom}
                             show-tenth-precision?
                             (assoc :step "0.1"))]))
       saved-entries))

(defn settings-form
  [{:keys [ws-uuid title headers rf-event-id rf-sub-id min-attr-id max-attr-id]}]
  (let [*gv-uuid+min+max-entries       (subscribe [rf-sub-id ws-uuid])
        *gv-order                      (subscribe [:vms/group-variable-order ws-uuid])
        gv-uuid+min+max-entries-sorted (->> @*gv-uuid+min+max-entries
                                            (sort-by #(.indexOf @*gv-order (first %))))
        *default-max-values            (subscribe [:worksheet/output-uuid->result-max-values ws-uuid])
        *default-min-values            (subscribe [:worksheet/output-uuid->result-min-values ws-uuid])
        maximums                       (number-inputs {:saved-entries  (map (fn remove-min-val[[gv-uuid _min-val max-val enabled?]]
                                                                              [gv-uuid max-val enabled?])
                                                                            gv-uuid+min+max-entries-sorted)
                                                       :on-change      #(update-setting-input ws-uuid rf-event-id max-attr-id %1 %2)
                                                       :default-values @*default-max-values})
        minimums                       (number-inputs {:saved-entries  (map (fn remove-max-val [[gv-uuid min-val _max-val enabled?]]
                                                                              [gv-uuid min-val enabled?])
                                                                            gv-uuid+min+max-entries-sorted)
                                                       :min-attr-id    max-attr-id
                                                       :on-change      #(update-setting-input ws-uuid rf-event-id min-attr-id %1 %2)
                                                       :default-values @*default-min-values})
        output-ranges                  (map (fn [[gv-uuid & _rest]]
                                              (let [min-val     (get @*default-min-values gv-uuid)
                                                    min-val-fmt (if (< min-val 1) "%.1f" "%d")
                                                    max-val     (get @*default-max-values gv-uuid)
                                                    max-val-fmt (if (< max-val 1) "%.1f" "%d")
                                                    fmt         (gstring/format "%s - %s" min-val-fmt max-val-fmt)]
                                                (gstring/format fmt min-val max-val)))
                                            gv-uuid+min+max-entries-sorted)
        names                          (map (fn get-variable-name [[gv-uuid _min _max]]
                                              @(subscribe [:wizard/gv-uuid->resolve-result-variable-name gv-uuid]))
                                            gv-uuid+min+max-entries-sorted)
        enabled-check-boxes            (when (= rf-event-id :worksheet/update-table-filter-attr)
                                         (map (fn [[gv-uuid _min _max enabled?]]
                                                [c/checkbox {:checked?  enabled?
                                                             :on-change #(dispatch [:worksheet/toggle-enable-filter ws-uuid gv-uuid])}])
                                              gv-uuid+min+max-entries-sorted))
        column-keys                    (mapv (fn [idx] (keyword (str "col" idx))) (range (count headers)))
        row-data                       (if enabled-check-boxes
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
    (letfn [(radio-group [{:keys [label attr variables on-change]}]
              (let [*values   (subscribe [:worksheet/get-graph-settings-attr ws-uuid attr])
                    selected? (first @*values)]
                [c/radio-group {:label   label
                                :options (mapv (fn [{group-var-uuid :bp/uuid}]
                                                 (let [var-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name group-var-uuid])]
                                                   {:value     var-name
                                                    :label     var-name
                                                    :on-change #(do (dispatch [:worksheet/update-graph-settings-attr
                                                                               ws-uuid
                                                                               attr
                                                                               group-var-uuid])
                                                                    (on-change group-var-uuid))
                                                    :checked?  (= selected? group-var-uuid)}))
                                               variables)}]))]
      [:<>
       [c/checkbox {:label     @(<t (bp "display_graph_results"))
                    :checked?  enabled?
                    :on-change #(dispatch [:worksheet/toggle-graph-settings ws-uuid])}]
       (when enabled?
         (cond-> [:<>]
           (>= multi-valued-input-count 1)
           (conj [radio-group {:label     (str @(<t (bp "select_x_axis_variable")) ":")
                               :attr      :graph-settings/x-axis-group-variable-uuid
                               :variables group-variables
                               :on-change #(dispatch [:worksheet/upsert-x-axis-limit ws-uuid %])}])

           (>= multi-valued-input-count 2)
           (conj [radio-group {:label     (str @(<t (bp "select_z_axis_variable")) ":")
                               :attr      :graph-settings/z-axis-group-variable-uuid
                               :variables group-variables}])

           (>= multi-valued-input-count 3)
           (conj [radio-group {:label     (str @(<t (bp "select_z2_axis_variable")) ":")
                               :attr      :graph-settings/z2-axis-group-variable-uuid
                               :variables group-variables}])

           (>= multi-valued-input-count 1)
           (conj (let [[gv-uuid
                        min-val
                        max-val]                 (first @(subscribe [:worksheet/graph-settings-x-axis-limits ws-uuid]))
                       v-name                    @(subscribe [:wizard/gv-uuid->resolve-result-variable-name gv-uuid])
                       [default-min default-max] @(subscribe [:wizard/x-axis-limit-min+max-defaults ws-uuid gv-uuid])]
                   [:div.settings-form
                    (c/table {:title   @(<t (bp "x_graph_and_axis_limits"))
                              :headers (->> [@(<t (bp "input_variable"))
                                             @(<t (bp "range"))
                                             @(<t (bp "minimum"))
                                             @(<t (bp "maximum"))]
                                            (mapv #(str/upper-case %)))
                              :columns [:v-name :input-range :min :max]
                              :rows    [{:v-name      v-name
                                         :input-range (str default-min "-" default-max)
                                         :min         (let [value-atom (r/atom min-val)]
                                                        [c/number-input {:enabled?   enabled?
                                                                         :on-change  #(let [v (input-int-value %)]
                                                                                        (reset! value-atom v))
                                                                         :on-blur    #(dispatch [:worksheet/update-x-axis-limit-attr
                                                                                                 ws-uuid
                                                                                                 :x-axis-limit/min
                                                                                                 @value-atom])
                                                                         :value-atom value-atom}])
                                         :max         (let [value-atom (r/atom max-val)]
                                                        [c/number-input {:enabled?   enabled?
                                                                         :on-change  #(let [v (input-int-value %)]
                                                                                        (reset! value-atom v))
                                                                         :on-blur    #(dispatch [:worksheet/update-x-axis-limit-attr
                                                                                                 ws-uuid
                                                                                                 :x-axis-limit/max
                                                                                                 @value-atom])
                                                                         :value-atom value-atom}])}]})]))

           (>= multi-valued-input-count 1)
           (conj [settings-form {:ws-uuid     ws-uuid
                                 :title       @(<t (bp "y_graph_and_axis_limits"))
                                 :headers     (->> [@(<t (bp "output_variable"))
                                                    @(<t (bp "range"))
                                                    @(<t (bp "minimum"))
                                                    @(<t (bp "maximum"))]
                                                   (mapv #(str/upper-case %)))
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
      [c/radio-group {:label   (s/capitalize-words @(<t (bp "map_representative_fraction")))
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
      [c/number-input {:label      (str (s/capitalize-words @(<t (bp "map_representative_fraction")))
                                        " (1:x)")
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
    [:<> [c/checkbox {:label     (s/capitalize-words @(<t (bp "display_table_results")))
                      :checked?  table-enabled?
                      :on-change #(dispatch [:worksheet/toggle-table-settings ws-uuid])}]
     (when table-enabled?
       [:div.table-settings
        [c/checkbox {:label     (s/capitalize-words @(<t (bp "convert_to_map_units")))
                     :checked?  map-units-enabled?
                     :on-change #(dispatch [:worksheet/toggle-map-units-settings ws-uuid])}]
        (when map-units-enabled?
          [map-units-form ws-uuid])
        [settings-form {:ws-uuid     ws-uuid
                        :title       @(<t (bp "table_shading_filters"))
                        :headers     (mapv
                                      #(str/upper-case %)
                                      [(str @(<t (bp "enabled")) "?")
                                       @(<t (bp "output_variable"))
                                       @(<t (bp "range"))
                                       @(<t (bp "minimum"))
                                       @(<t (bp "maximum"))])
                        :rf-event-id :worksheet/update-table-filter-attr
                        :rf-sub-id   :worksheet/table-settings-filters
                        :min-attr-id :table-filter/min
                        :max-attr-id :table-filter/max}]])]))

(defn wizard-results-settings-page [{:keys [route-handler io ws-uuid] :as params}]
  (dispatch-sync [:worksheet/update-furthest-visited-step ws-uuid route-handler io])
  (let [*notes              (subscribe [:wizard/notes ws-uuid])
        *show-notes?        (subscribe [:wizard/show-notes?])
        on-back             #(dispatch [:wizard/back ws-uuid])
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
  (dispatch-sync [:worksheet/update-furthest-visited-step ws-uuid route-handler io])
  (let [*worksheet          (subscribe [:worksheet ws-uuid])
        *ws-date            (subscribe [:wizard/worksheet-date ws-uuid])
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
                        :tabs     (cond-> [{:label     "Notes"
                                            :tab       :notes
                                            :icon-name :notes
                                            :selected? (= @*tab-selected :notes)}
                                           {:label     "Inputs"
                                            :tab       :inputs
                                            :icon-name :tables
                                            :selected? (= @*tab-selected :inputs)}
                                           {:label     "Outputs"
                                            :tab       :outputs
                                            :icon-name :tables
                                            :selected? (= @*tab-selected :outputs)}]

                                    (get-in @*worksheet [:worksheet/graph-settings :graph-settings/enabled?])
                                    (conj {:label     "Graph"
                                           :tab       :graph
                                           :icon-name :graphs
                                           :selected? (= @*tab-selected :graph)})

                                    (seq (:worksheet/diagrams @*worksheet))
                                    (conj {:label     "Diagram"
                                           :tab       :diagram
                                           :icon-name :graphs
                                           :selected? (= @*tab-selected :diagram)} ))}]]]
        [:div.wizard-page__body
         [:div.wizard-results__notes {:id "notes"}
          (wizard-notes @*notes)]
         [:div.wizard-notes__header {:id "inputs"}
          @(<t (bp "inputs_table"))]
         [inputs-table ws-uuid]
         (when (and table-enabled? (seq @*cell-data))
           [:div.wizard-results__table {:id "table"}
            [:div.wizard-notes__header @(<t (bp "table"))]
            [result-matrices ws-uuid]
            [:div.wizard-notes__header @(<t (bp "runs_table"))]
            [result-table-download-link ws-uuid]])
         (result-graphs ws-uuid @*cell-data)
         (result-diagrams ws-uuid)]]
       [:div.wizard-navigation
        [c/button {:label    "Back"
                   :variant  "secondary"
                   :on-click #(dispatch [:wizard/back ws-uuid])}]]]]]))

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
