(ns behave-cms.components.pivot-tables
  (:require
   [clojure.spec.alpha           :as s]
   [clojure.string               :as str]
   [behave.schema.conditionals]
   [behave-cms.components.common :refer [dropdown]]
   [behave-cms.utils             :as u]
   [reagent.core                 :as r]
   [re-frame.core                :as rf]
   [string-utils.interface       :refer [->str]]))


;;; Helpers

(defn- inverse-attr
  [attr]
  (->> (str/split (->str attr) "/")
       (str/join "/_")
       keyword))

(defn- on-submit [entity-id pivot-attr]
  (rf/dispatch-sync [:ds/transact
                     (merge @(rf/subscribe [:state [:editors :pivot-table entity-id]])
                            {(inverse-attr pivot-attr) entity-id})])
  (rf/dispatch-sync [:state/set-state [:editors :variable-lookup] nil])
  (rf/dispatch-sync [:state/set-state [:editors :pivot-table entity-id] {}])
  (rf/dispatch-sync [:state/set-state :pivot-column-id nil]))

;;; Components

(defn radio-buttons
  "A component for radio button."
  [group-label options selected on-change]
  (let [selected (if (nil? @selected) nil (name @selected))]
    [:div.mb-3
     [:label.form-label group-label]
     (for [{:keys [label value]} options]
       ^{:key value}
       [:div.form-check
        [:input.form-check-input
         {:type      "radio"
          :name      (u/sentence->kebab group-label)
          :checked   (= selected value)
          :id        value
          :value     (name value)
          :on-change on-change}]
        [:label.form-check-label {:for value} label]])]))

#_(defn radio-buttons
    "A component for radio button."
    [group-label options on-change]
    [:div.mb-3
     [:label.form-label group-label]
     (for [{:keys [label value]} options]
       [:div.form-check
        [:input.form-check-input
         {:type      "radio"
          :name      (u/sentence->kebab group-label)
          :id        value
          :value     value
          :on-change on-change}]
        [:label.form-check-label {:for value} label]])])

(defn column-field-selectors
  "a commponent to select the necessary fields for a pivot column"
  [state module-id pivot-table-id pivot-column]
  (when (:pivot-column/group-variable-uuid pivot-column)
    (let [gv-id                    @(rf/subscribe [:bp/lookup (:pivot-column/group-variable-uuid pivot-column)])
          [module submodule group] @(rf/subscribe [:group-variable/module-submodule-group gv-id])]
      (rf/dispatch [:state/set-state [:editors :variable-lookup] {:module module :submodule submodule :group group}])))
  (let [var-path          [:editors :variable-lookup]
        pivot-column-path [:editors :pivot-table pivot-table-id]
        get-field         #(rf/subscribe [:state %])
        set-field         (fn [path v] (rf/dispatch [:state/set-state path v]))
        modules           (rf/subscribe [:module/app-modules module-id])
        submodules        (rf/subscribe [:pull-children :module/submodules @(get-field (conj var-path :module))])
        groups            (rf/subscribe [:submodule/groups-w-subgroups @(get-field (conj var-path :submodule))])
        variables         (rf/subscribe [:group/variables @(get-field (conj var-path :group))])
        reset-gv-uuid!    #(do
                             (rf/dispatch-sync [:state/set-state :pivot-column-id nil])
                             (set-field pivot-column-path (dissoc pivot-column :pivot-column/group-variable-uuid)))
        reset-function!   #(do
                             (rf/dispatch-sync [:state/set-state :pivot-column-id nil])
                             (set-field pivot-column-path (dissoc pivot-column :pivot-column/function)))]
    [:<>
     [dropdown
      {:label     "Module:"
       :selected  (get-field (conj var-path :module))
       :on-select #(do
                     (reset-gv-uuid!)
                     (set-field var-path {})
                     (set-field (conj var-path :module) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :module/name}]
                         {:value value :label label}) @modules)}]

     [dropdown
      {:label     "Submodule:"
       :selected  (get-field (conj var-path :submodule))
       :on-select #(do
                     (reset-gv-uuid!)
                     (set-field (conj var-path :group) nil)
                     (set-field (conj var-path :submodule) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :submodule/name io :submodule/io}]
                         {:value value :label (str label " (" (->str io) ")")})
                       (sort-by (juxt :submodule/io :submodule/name) @submodules))}]

     [dropdown
      {:label     "Group/Subgroup:"
       :selected  (get-field (conj var-path :group))
       :on-select #(do
                     (reset-gv-uuid!)
                     (set-field (conj var-path :group) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :group/name}]
                         {:value value :label label}) @groups)}]

     [dropdown
      {:label     "Variable:"
       :selected  (get-field (conj pivot-column-path :pivot-column/group-variable-uuid))
       :on-select #(set-field (conj pivot-column-path :pivot-column/group-variable-uuid)
                              (u/input-value %))
       :options   (map (fn [{value :bp/uuid label :variable/name}]
                         {:value value :label label}) @variables)}]

     (when (= @state "value")
       [dropdown
        {:label     "Function"
         :selected  (get-field (conj pivot-column-path :pivot-column/function))
         :on-select #(do
                       (reset-function!)
                       (set-field (conj pivot-column-path :pivot-column/function)
                                  (keyword (u/input-value %))))
         :options   [{:value "sum" :label "sum"}
                     {:value "min" :label "min"}
                     {:value "max" :label "max"}
                     {:value "count" :label "count"}]}])]))

(defn manage-pivot-table
  "Component to manage a pivot table column for an pivot table entity. Takes:
   - module-id [int]: the ID of the module the pivot table belongs to
   - pivot-table-id [int]: the ID of the pivot table"
  [module-id pivot-table-id]
  (let [pivot-column-id @(rf/subscribe [:state :pivot-column-id])]
    (r/with-let [state (r/atom "field")
                 pivot-column-path  [:editors :pivot-table pivot-table-id]
                 set-type    #(do (reset! state %)
                                  (rf/dispatch-sync [:state/set-state [:editors :variable-lookup] nil])
                                  (rf/dispatch-sync [:state/set-state pivot-column-path nil])
                                  (rf/dispatch-sync [:state/set-state
                                                     (conj pivot-column-path :pivot-column/type)
                                                     (if (= % "field") :field :value)]))
                 get-field (fn [attr] (rf/subscribe [:state (conj pivot-column-path attr)]))
                 pivot-column (rf/subscribe [:state pivot-column-path])]
      (when-not (nil? pivot-column-id)
        (let [existing-pivot-column @(rf/subscribe [:entity pivot-column-id])]
          (rf/dispatch [:state/set-state [:editors :pivot-table pivot-table-id] existing-pivot-column])
          (when (= (:pivot-column/type existing-pivot-column) :value)
            (reset! state "value"))))
      [:form.row
       {:on-submit (u/on-submit #(on-submit pivot-table-id :pivot-table/columns))}
       [:h4 "Manage Pivot Table Columns:"]
       [radio-buttons
        "Column Type"
        [{:label "Field" :value "field"}
         {:label "Value" :value "value"}]
        (get-field :pivot-column/type)
        #(set-type (u/input-value %))]

       [column-field-selectors state module-id pivot-table-id @pivot-column]

       [:button.btn.btn-sm.btn-outline-primary.mt-4
        {:type "submit"
         :disabled (if (= @state "field")
                     (not (s/valid? :behave/pivot-table-column-field @pivot-column))
                     (not (s/valid? :behave/pivot-table-column-value @pivot-column)))
         }
        (if (:db/id @pivot-column) "Save" "Create")]])))
