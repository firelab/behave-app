(ns behave-cms.components.actions
  (:require
   [behave.schema.conditionals]
   [behave-cms.components.common :refer [dropdown simple-table]]
   [behave-cms.utils             :as u]
   [reagent.core                 :as r]
   [re-frame.core                :as rf]
   [string-utils.interface       :refer [->str]]))

;;; Helpers

(defn- on-submit [entity-id]
  (rf/dispatch [:ds/transact
                (merge @(rf/subscribe [:state [:editors :action]])
                       {:group-variable/_actions entity-id})])
  (rf/dispatch [:state/set-state [:editors :action] {}]))

(defn- get-list-options [gv-id]
  (get-in @(rf/subscribe [:pull '[{:variable/_group-variables
                                   [{:variable/list
                                     [{:list/options [*]}]}]}] gv-id])
          [:variable/_group-variables 0 :variable/list :list/options]))

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

(defn actions-table [actions]
  [simple-table
   [:action/name :action/type :action/target-value]
   (sort-by :action/name actions)
   {:on-select #(rf/dispatch [:state/set-state [:editors :action] %])
    :on-delete #(when (js/confirm (str "Are you sure you want to delete the action " (:action/name %) "?"))
                  (rf/dispatch [:api/delete-entity %]))}])

(defn manage-conditional [group-id idx conditional]
  (println [:MNG idx conditional])
  (let [var-path    [:editors :variable-lookup]
        cond-path   [:editors :conditional :action/conditionals idx]
        get-field   #(rf/subscribe [:state %])
        set-field   (fn [path v] (rf/dispatch [:state/set-state path v]))
        modules     (rf/subscribe [:subgroup/app-modules group-id])
        submodules  (rf/subscribe [:pull-children :module/submodules @(get-field (conj var-path :module))])
        groups      (rf/subscribe [:submodule/groups-w-subgroups @(get-field (conj var-path :submodule))])
        is-output?  (rf/subscribe [:submodule/is-output? @(get-field (conj var-path :submodule))]) 
        variables   (rf/subscribe [(if @is-output? :group/variables :group/discrete-variables) @(get-field (conj var-path :group))])
        options     (rf/subscribe [:group/discrete-variable-options @(get-field (conj cond-path :conditional/group-variable-uuid))])
        reset-cond! #(set-field cond-path {:conditional/type :group-variable})]

    [:<>
     [:div.row
      [:div.col-4
       [dropdown
        {:label     "Module:"
         :on-select #(do
                       (reset-cond!)
                       (set-field var-path {})
                       (set-field (conj var-path :module) (u/input-int-value %)))
         :options   (map (fn [{value :db/id label :module/name}]
                           {:value value :label label}) @modules)}]]

      [:div.col-4
       [dropdown
        {:label     "Submodule:"
         :on-select #(do
                       (reset-cond!)
                       (set-field (conj var-path :group) nil)
                       (set-field (conj var-path :submodule) (u/input-int-value %)))
         :options   (map (fn [{value :db/id label :submodule/name io :submodule/io}]
                           {:value value :label (str label " (" (->str io) ")")})
                         (sort-by (juxt :submodule/io :submodule/name) @submodules))}]]

      [:div.col-4
       [dropdown
        {:label     "Group/Subgroup:"
         :on-select #(do
                       (reset-cond!)
                       (set-field (conj var-path :group) (u/input-int-value %)))
         :options   (map (fn [{value :db/id label :group/name}]
                           {:value value :label label}) @groups)}]]]

     [:div.row
      [:div.col-4
       [dropdown
        {:label     "Variable:"
         :on-select #(do
                       (set-field (conj cond-path :conditional/values) nil)
                       (set-field (conj cond-path :conditional/group-variable-uuid)
                                  (u/input-value %)))
         :options   (map (fn [{value :bp/uuid label :variable/name}]
                           {:value value :label label}) @variables)}]]

      [:div.col-4
       [dropdown
        {:label     "Operator:"
         :on-select #(set-field (conj cond-path :conditional/operator)
                                (keyword (u/input-value %)))
         :options   (filter some? [{:value :equal :label "="}
                                   {:value :not-equal :label "!="}
                                   (when-not @is-output? {:value :in :label "IN"})])}]]

      [:div.col-4
       [dropdown
        {:label     "Value:"
         :multiple? (= :in @(get-field (conj cond-path :conditional/operator)))
         :on-select #(let [vs (u/input-multi-select %)]
                       (set-field (conj cond-path :conditional/values) vs))
         :options   (if @is-output?
                      [{:value "true" :label "True"}
                       {:value "false" :label "False"}]
                      (map (fn [{value :list-option/value label :list-option/name}]
                             {:value value :label label}) @options))}]]]]))

(defn add-conditionals [gv-id]
  (r/with-let [group-id        (get-in @(rf/subscribe [:pull '[{:group/_group-variables [*]}] gv-id]) [:group/_group-variables 0 :db/id])
               cond-path       [:editors :action]
               conditionals    (rf/subscribe [:state :action :action/conditionals])
               add-conditional #(rf/dispatch [:state/set-state cond-path (if @conditionals (conj @conditionals {}) [])])
               set-field       (fn [attr v]
                                 (rf/dispatch [:state/set-state (conj cond-path attr) v]))]

    ;; Find the relevant modules for a variable
    [:<>
     [:h5 "Conditionals"]
     [dropdown
      {:label     "Conditional Operator:"
       :on-select #(set-field :action/conditional-operator (keyword (u/input-value %)))
       :options   [{:label "AND" :value "and"}
                   {:label "OR" :value "or"}]}]

     (when (= 0 (count @conditionals))
       [:p "No conditionals have been set."])

     (doall
      (for [conditional @conditionals idx (range @conditionals)]
        [manage-conditional group-id idx conditional]))

     [:a.btn.btn-sm.btn-outline-secondary
      {:on-click add-conditional}
      "Add Conditional"]]))

(defn manage-action [gv-id is-output?]
  ;; Look up variable's list options
  (let [action-path  [:editors :action]
        action-id    (rf/subscribe [:state :action])
        set-field    (fn [attr v]
                       (rf/dispatch [:state/set-state (conj action-path attr) v]))
        list-options (get-list-options gv-id)]

    [:form.row
     {:on-submit (u/on-submit #(on-submit gv-id))}
     [:h4 (str (if @action-id "Edit" "Add") " Action:")]
     [:<> 
      [radio-buttons
       "Action Type:"
       [{:label "Select" :value "select"}
        {:label "Disable" :value "disable"}]
       #(set-field :action/type (keyword (u/input-value %)))]

      [dropdown
       {:label     "Option:"
        :on-select #(set-field :action/target-value (u/input-value %))
        :options   (map (fn [{value :list-option/value label :list-option/name}]
                          {:value value :label label}) list-options)}]

      [add-conditionals gv-id]

      [:button.btn.btn-sm.btn-outline-primary.mt-4
       {:type "submit"}
       "Save"]]]))


(comment
  (rf/subscribe [:state [:editors :action]])

  )
