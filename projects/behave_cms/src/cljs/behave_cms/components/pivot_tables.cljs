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
  (rf/dispatch [:ds/transact
                (merge @(rf/subscribe [:state [:editors :pivot-table pivot-attr]])
                       {(inverse-attr pivot-attr) entity-id})])
  (rf/dispatch-sync [:state/set-state [:editors :variable-lookup] nil])
  (rf/dispatch [:state/set-state [:editors :pivot-table] {}]))

;;; Components

(defn radio-buttons
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
  [pivot-attr entity-id]
  (let [var-path     [:editors :variable-lookup]
        pivot-path   [:editors :pivot-table @pivot-attr]
        get-field    #(rf/subscribe [:state %])
        set-field    (fn [path v] (rf/dispatch [:state/set-state path v]))
        modules      (rf/subscribe [:module/app-modules entity-id])
        submodules   (rf/subscribe [:pull-children :module/submodules @(get-field (conj var-path :module))])
        groups       (rf/subscribe [:submodule/groups-w-subgroups @(get-field (conj var-path :submodule))])
        is-output?   (rf/subscribe [:submodule/is-output? @(get-field (conj var-path :submodule))])
        variables    (rf/subscribe [(if @is-output? :group/variables :group/discrete-variables)
                                    @(get-field (conj var-path :group))])
        reset-entry! #(set-field pivot-path {})]
    [:<>
     [dropdown
      {:label     "Module:"
       :selected  (get-field (conj var-path :module))
       :on-select #(do
                     (reset-entry!)
                     (set-field var-path {})
                     (set-field (conj var-path :module) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :module/name}]
                         {:value value :label label}) @modules)}]

     [dropdown
      {:label     "Submodule:"
       :selected  (get-field (conj var-path :submodule))
       :on-select #(do
                     (reset-entry!)
                     (set-field (conj var-path :group) nil)
                     (set-field (conj var-path :submodule) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :submodule/name io :submodule/io}]
                         {:value value :label (str label " (" (->str io) ")")})
                       (sort-by (juxt :submodule/io :submodule/name) @submodules))}]

     [dropdown
      {:label     "Group/Subgroup:"
       :selected  (get-field (conj var-path :group))
       :on-select #(do
                     (reset-entry!)
                     (set-field (conj var-path :group) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :group/name}]
                         {:value value :label label}) @groups)}]

     [dropdown
      {:label     "Variable:"
       :selected  (get-field (conj pivot-path (if (= @pivot-attr :pivot-table/rows)
                                                :pivot-row/group-variable-uuid
                                                :pivot-value/group-variable-uuid)))
       :on-select #(set-field (conj pivot-path (if (= @pivot-attr :pivot-table/rows)
                                                 :pivot-row/group-variable-uuid
                                                 :pivot-value/group-variable-uuid))
                              (u/input-value %))
       :options   (map (fn [{value :bp/uuid label :variable/name}]
                         {:value value :label label}) @variables)}]

     (when (= @pivot-attr :pivot-table/values)
       [dropdown
        {:label     "Function"
         :on-select #(set-field (conj pivot-path :pivot-value/function)
                                (keyword (u/input-value %)))
         :options   [{:value :sum :label "sum"}
                     {:value :min :label "min"}
                     {:value :max :label "max"}
                     {:value :max :label "count"}]}])]))

(defn manage-pivot-table
  "Component to manage a pivot table column for an pivot table entity. Takes:
   - module-id [int]: the ID of the module the pivot table belongs to
   - pivot-table-id [int]: the ID of the pivot table"
  [module-id pivot-table-id]
  (r/with-let [pivot-attr (r/atom :pivot-table/rows)]
    (let [pivot-path  [:editors :pivot-table @pivot-attr]
          set-type    #(do (reset! pivot-attr %)
                           (rf/dispatch-sync [:state/set-state [:editors :variable-lookup] nil])
                           (rf/dispatch-sync [:state/set-state pivot-path nil]))
          pivot-field (rf/subscribe [:state pivot-path])]
      [:form.row
       {:on-submit (u/on-submit #(on-submit pivot-table-id @pivot-attr))}
       [:h4 "Manage Pivot Table Rows/Values:"]

       [radio-buttons
        "Rows/Values"
        [{:label "Rows" :value "pivot-table/rows"}
         {:label "Values" :value "pivot-table/values"}]
        #(set-type (u/input-keyword %))]

       [column-field-selectors pivot-attr module-id]

       [:button.btn.btn-sm.btn-outline-primary.mt-4
        {:type     "submit"
         :disabled (if (= @pivot-attr :pivot-table/rows)
                     (not (s/valid? :behave/pivot-table-row @pivot-field))
                     (not (s/valid? :behave/pivot-table-value @pivot-field)))}
        "Save"]])))
