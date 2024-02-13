(ns behave-cms.components.actions
  (:require
   [behave.schema.conditionals]
   [behave-cms.components.common :refer [dropdown simple-table labeled-input]]
   [behave-cms.utils             :as u]
   [reagent.core                 :as r]
   [re-frame.core                :as rf]
   [string-utils.interface       :refer [->str]]))

;;; Helpers

(defn- on-submit [entity-id]
  (rf/dispatch [:ds/transact
                (merge @(rf/subscribe [:state [:editors :action]])
                       {:group-variable/_actions entity-id})])
  (rf/dispatch [:state/set-state :action nil])
  (rf/dispatch [:state/set-state :editors {}]))

(defn- get-list-options [gv-id]
  (get-in @(rf/subscribe [:pull '[{:variable/_group-variables
                                   [{:variable/list
                                     [{:list/options [*]}]}]}] gv-id])
          [:variable/_group-variables 0 :variable/list :list/options]))

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

(defn actions-table [actions]
  [simple-table
   [:action/name :action/type :action/target-value]
   (sort-by :action/name actions)
   {:on-select #(rf/dispatch [:state/set-state :action (:db/id %)])
    :on-delete #(when (js/confirm (str "Are you sure you want to delete the action " (:action/name %) "?"))
                  (rf/dispatch [:api/delete-entity %]))}])

(defn manage-conditional [group-id conditional idx]
  ;; Select Parents
  (when (:db/id conditional)
    (let [cond-gv-id                        @(rf/subscribe [:bp/lookup (:conditional/group-variable-uuid conditional)])
          [module submodule group] @(rf/subscribe [:group-variable/module-submodule-group cond-gv-id])]
      (rf/dispatch [:state/set-state [:editors :variable-lookup idx] {:module module :submodule submodule :group group}])))

  (let [var-path    [:editors :variable-lookup idx]
        cond-path   [:editors :action :action/conditionals idx]
        get-field   #(rf/subscribe [:state %])
        set-field   (fn [path v] (rf/dispatch [:state/set-state path v]))
        modules     (rf/subscribe [:subgroup/app-modules group-id])
        submodules  (rf/subscribe [:pull-children :module/submodules @(get-field (conj var-path :module))])
        groups      (rf/subscribe [:submodule/groups-w-subgroups @(get-field (conj var-path :submodule))])
        is-output?  (rf/subscribe [:submodule/is-output? @(get-field (conj var-path :submodule))]) 
        variables   (rf/subscribe [(if @is-output? :group/variables :group/discrete-variables) @(get-field (conj var-path :group))])
        options     (rf/subscribe [:group/discrete-variable-options @(get-field (conj cond-path :conditional/group-variable-uuid))])
        multiple?   (= :in @(get-field (conj cond-path :conditional/operator)))
        reset-cond! #(set-field cond-path {:conditional/type :group-variable})]

    [:div.mb-2
     {:key idx
      :style {:background "whitesmoke" :padding "1em"}}
     [:div.row
      [:div.col-4
       [dropdown
        {:label     "Module:"
         :selected  (get-field (conj var-path :module))
         :on-select #(do
                       (reset-cond!)
                       (set-field var-path {})
                       (set-field (conj var-path :module) (u/input-int-value %)))
         :options   (map (fn [{value :db/id label :module/name}]
                           {:value value :label label}) @modules)}]]

      [:div.col-4
       [dropdown
        {:label     "Submodule:"
         :selected  (get-field (conj var-path :submodule))
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
         :selected  (get-field (conj var-path :group))
         :on-select #(do
                       (reset-cond!)
                       (set-field (conj var-path :group) (u/input-int-value %)))
         :options   (map (fn [{value :db/id label :group/name}]
                           {:value value :label label}) @groups)}]]]

     [:div.row
      [:div.col-4
       [dropdown
        {:label     "Variable:"
         :selected  (get-field (conj cond-path :conditional/group-variable-uuid))
         :on-select #(do
                       (set-field (conj cond-path :conditional/values) nil)
                       (set-field (conj cond-path :conditional/group-variable-uuid)
                                  (u/input-value %)))
         :options   (map (fn [{value :bp/uuid label :variable/name}]
                           {:value value :label label}) @variables)}]]

      [:div.col-4
       [dropdown
        {:label     "Operator:"
         :selected  (get-field (conj cond-path :conditional/operator))
         :on-select #(set-field (conj cond-path :conditional/operator)
                                (keyword (u/input-value %)))
         :options   (filter some? [{:value "equal" :label "="}
                                   {:value "not-equal" :label "!="}
                                   (when-not @is-output? {:value :in :label "IN"})])}]]

      [:div.col-4
       [dropdown
        {:label     "Value:"
         :selected  (get-field (conj cond-path :conditional/values))
         :multiple? multiple?
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
               conditionals    (rf/subscribe [:state (conj cond-path :action/conditionals)])
               cond-op         (rf/subscribe [:state (conj cond-path :action/conditionals-operator)])
               add-conditional #(rf/dispatch [:state/set-state
                                              (conj cond-path :action/conditionals)
                                              (if (pos? (count @conditionals)) (conj @conditionals {}) [{}])])
               set-field       (fn [attr v]
                                 (rf/dispatch [:state/set-state (conj cond-path attr) v]))]

    ;; Find the relevant modules for a variable
    [:<>
     [:h5 "Conditionals"]
     [dropdown
      {:label     "Conditional Operator:"
       :selected  @cond-op
       :on-select #(set-field :action/conditionals-operator (keyword (u/input-value %)))
       :options   [{:label "AND" :value "and"}
                   {:label "OR" :value "or"}]}]

     (when (= 0 (count @conditionals))
       [:p "No conditionals have been set."])

     (doall
      (for [idx (range (count @conditionals))]
        ^{:key idx}
        [manage-conditional group-id (nth @conditionals idx) idx]))

     [:a.btn.btn-sm.btn-outline-secondary
      {:href "" :on-click #(do (.preventDefault %)
                               (add-conditional))}
      "Add Conditional"]]))

(defn manage-action [gv-id action-id is-output?]
  ;; Set editing state to action
  (when-not (nil? action-id)
    (rf/dispatch [:state/set-state [:editors :action] @(rf/subscribe [:pull '[* {:action/conditionals [*]}] action-id])]))

  (let [action-path  [:editors :action]
        action-id    (rf/subscribe [:state :action])
        get-field    (fn [attr] (rf/subscribe [:state (conj action-path attr)]))
        set-field    (fn [attr v]
                       (rf/dispatch [:state/set-state (conj action-path attr) v]))
        target-value (get-field :action/target-value)
        list-options (get-list-options gv-id)]

    [:form.row
     {:on-submit (u/on-submit #(on-submit gv-id))}
     [:h4 (str (if @action-id "Edit" "Add") " Action:")]
     [:<> 
      [labeled-input
       "Action Name:"
       (get-field :action/name)
       {:call-back #(set-field :action/name (u/input-value %))}]

      [radio-buttons
       "Action Type:"
       [{:label "Select" :value "select"}
        {:label "Disable" :value "disable"}]
       (get-field :action/type)
       #(set-field :action/type (keyword (u/input-value %)))]

      (when-not is-output?
        [dropdown
         {:label     "Option:"
          :selected  @target-value
          :on-select #(set-field :action/target-value (u/input-value %))
          :options   (map (fn [{value :list-option/value label :list-option/name}]
                            {:value (str value) :label label}) list-options)}])

      [add-conditionals gv-id]

      [:button.btn.btn-sm.btn-outline-primary.mt-4
       {:type "submit"}
       "Save"]]]))
