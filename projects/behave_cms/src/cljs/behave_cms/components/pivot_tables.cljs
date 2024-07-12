(ns behave-cms.components.pivot-tables
  (:require
   [clojure.spec.alpha           :as s]
   [clojure.string               :as str]
   [behave.schema.conditionals]
   [behave-cms.components.common :refer [dropdown checkboxes simple-table]]
   [behave-cms.utils             :as u]
   [reagent.core                 :as r]
   [re-frame.core                :as rf]
   [string-utils.interface       :refer [->str]]))


;;; Helpers

(defn inverse-attr
  [attr]
  (->> (str/split (->str attr) "/")
       (str/join "/_")
       keyword))

(defn on-submit [entity-id cond-attr]
  (prn "data:" (merge @(rf/subscribe [:state [:editors :conditional cond-attr]])
                      {(inverse-attr cond-attr) entity-id}))
  (rf/dispatch [:ds/transact
                (merge @(rf/subscribe [:state [:editors :conditional cond-attr]])
                       {(inverse-attr cond-attr) entity-id})])
  (rf/dispatch-sync [:state/set-state [:editors :variable-lookup] nil])
  (rf/dispatch [:state/set-state [:editors :conditional] {}]))

(comment
  {:pivot-table/_rows             -1
   :pivot-row/group-variable-uuid "some-uuid"})

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

(defn manage-variable-conditionals [cond-attr entity-id]
  (let [var-path    [:editors :variable-lookup]
        cond-path   [:editors :conditional @cond-attr]
        get-field   #(rf/subscribe [:state %])
        set-field   (fn [path v] (rf/dispatch [:state/set-state path v]))
        modules     (rf/subscribe [:module/app-modules entity-id])
        submodules  (rf/subscribe [:pull-children :module/submodules @(get-field (conj var-path :module))])
        groups      (rf/subscribe [:submodule/groups-w-subgroups @(get-field (conj var-path :submodule))])
        is-output?  (rf/subscribe [:submodule/is-output? @(get-field (conj var-path :submodule))])
        variables   (rf/subscribe [(if @is-output? :group/variables :group/discrete-variables)
                                   @(get-field (conj var-path :group))])
        reset-cond! #(set-field cond-path {})]
    [:<>
     [dropdown
      {:label     "Module:"
       :selected  (get-field (conj var-path :module))
       :on-select #(do
                     (reset-cond!)
                     (set-field var-path {})
                     (set-field (conj var-path :module) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :module/name}]
                         {:value value :label label}) @modules)}]

     [dropdown
      {:label     "Submodule:"
       :selected  (get-field (conj var-path :submodule))
       :on-select #(do
                     (reset-cond!)
                     (set-field (conj var-path :group) nil)
                     (set-field (conj var-path :submodule) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :submodule/name io :submodule/io}]
                         {:value value :label (str label " (" (->str io) ")")})
                       (sort-by (juxt :submodule/io :submodule/name) @submodules))}]

     [dropdown
      {:label     "Group/Subgroup:"
       :selected  (get-field (conj var-path :group))
       :on-select #(do
                     (reset-cond!)
                     (set-field (conj var-path :group) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :group/name}]
                         {:value value :label label}) @groups)}]

     [dropdown
      {:label     "Variable:"
       :selected  (get-field (conj cond-path (if (= @cond-attr :pivot-table/rows)
                                               :pivot-row/group-variable-uuid
                                               :pivot-value/group-variable-uuid)))
       :on-select #(do
                     (set-field (conj cond-path (if (= @cond-attr :pivot-table/rows)
                                                  :pivot-row/group-variable-uuid
                                                  :pivot-value/group-variable-uuid))
                                (u/input-value %)))
       :options   (map (fn [{value :bp/uuid label :variable/name}]
                         {:value value :label label}) @variables)}]

     (when (= @cond-attr :pivot-table/values)
       [dropdown
        {:label     "Function"
         :on-select #(set-field (conj cond-path :pivot-value/function)
                                (keyword (u/input-value %)))
         :options   [{:value :sum :label "sum"}
                     {:value :max :label "max"}
                     {:value :min :label "min"}]}])]))

(defn manage-conditionals
  "Component to manage conditional for an entity. Takes:
   - entity-id [int]: the ID of the entity
   - cond-attr [keyword]: the attribute name of the conditionals (e.g. `:group/conditionals`)"
  [module-id pivot-table-id]
  (r/with-let [state     (r/atom :pivot-table/rows)
               cond-attr @state
               cond-path [:editors :conditional @state]
               set-type  #(do (reset! state %)
                              (rf/dispatch-sync [:state/set-state [:editors :variable-lookup] nil])
                              (rf/dispatch-sync [:state/set-state cond-path nil]))
               conditional (rf/subscribe [:state cond-path])]
    [:form.row
     {:on-submit (u/on-submit #(on-submit pivot-table-id @state))}
     [:h4 "Manage Pivot Table Rows/Values:"]

     [radio-buttons
      "Rows/Values"
      [{:label "Rows" :value "pivot-table/rows"}
       {:label "Values" :value "pivot-table/values"}]
      #(do (set-type (u/input-keyword %)))]

     [manage-variable-conditionals state module-id]

     [:button.btn.btn-sm.btn-outline-primary.mt-4
      {:type     "submit"
       ;; :disabled (or (not (s/valid? :behave/pivot-table-value @conditional))
       ;;               (not (s/valid? :behave/pivot-table @conditional)))
       }
      "Save"]]))

(defn pivot-table-editor [pivot-tables]
  [:div [manage-conditionals nil nil]])
