(ns behave-cms.components.conditionals
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
  (keyword (str/join "/_" (str/split (->str attr) #"/"))))

(defn on-submit [entity-id cond-attr]
  (rf/dispatch [:ds/transact
                (merge @(rf/subscribe [:state [:editors :conditional cond-attr]])
                       {(inverse-attr cond-attr) entity-id})])
  (rf/dispatch [:state/set-state [:editors :conditional] {}]))

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

(defn conditionals-table [entity-id conditionals cond-attr cond-op-attr]
  (let [entity (rf/subscribe [:entity entity-id])]
    [:<>
     [dropdown
      {:label     "Combined Operator:"
       :selected  (get @entity cond-op-attr)
       :on-select #(rf/dispatch [:api/update-entity
                                 {:db/id entity-id cond-op-attr (keyword (u/input-value %))}])
       :options   [{:value :and :label "AND"}
                   {:value :or :label "OR"}]}]
     [simple-table
      [:variable/name :conditional/operator :conditional/values]
      (sort-by :variable/name conditionals)
      {:on-delete #(when (js/confirm (str "Are you sure you want to delete the conditional " (:variable/name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))}]]))

(defn toggle-item [x xs]
  (let [xs-set (set xs)]
    (vec (if (xs-set x)
           (remove #(= x %) xs)
           (conj xs x)))))

(defn manage-module-conditionals [entity-id cond-attr]
  (let [cond-path   [:editors :conditional cond-attr]
        set-field   (fn [path v] (rf/dispatch [:state/set-state path v]))
        get-field   #(rf/subscribe [:state %])
        module-path (conj cond-path :conditional/values)
        all-modules (rf/subscribe [:subgroup/app-modules entity-id])
        *modules    (get-field module-path)
        options     (map (fn [{label :module/name}]
                           {:value (str/lower-case label) :label label})
                         @all-modules)]
    [:div
     [:h6 "Enabled with Modules:"]
     [checkboxes
      options
      (get-field module-path)
      #(set-field module-path (toggle-item (u/input-value %) @*modules))]]))

(defn manage-variable-conditionals [entity-id cond-attr]
  (let [var-path    [:editors :variable-lookup]
        cond-path   [:editors :conditional cond-attr]
        get-field   #(rf/subscribe [:state %])
        set-field   (fn [path v] (rf/dispatch [:state/set-state path v]))
        modules     (rf/subscribe [:subgroup/app-modules entity-id])
        submodules  (rf/subscribe [:pull-children :module/submodules @(get-field (conj var-path :module))])
        groups      (rf/subscribe [:submodule/groups-w-subgroups @(get-field (conj var-path :submodule))])
        is-output?  (rf/subscribe [:submodule/is-output? @(get-field (conj var-path :submodule))]) 
        variables   (rf/subscribe [(if @is-output? :group/variables :group/discrete-variables) @(get-field (conj var-path :group))])
        options     (rf/subscribe [:group/discrete-variable-options @(get-field (conj cond-path :conditional/group-variable-uuid))])
        reset-cond! #(set-field cond-path {:conditional/type :group-variable})]

    [:<> 
     [dropdown
      {:label     "Module:"
       :on-select #(do
                     (reset-cond!)
                     (set-field var-path {})
                     (set-field (conj var-path :module) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :module/name}]
                         {:value value :label label}) @modules)}]

     [dropdown
      {:label     "Submodule:"
       :on-select #(do
                     (reset-cond!)
                     (set-field (conj var-path :group) nil)
                     (set-field (conj var-path :submodule) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :submodule/name io :submodule/io}]
                         {:value value :label (str label " (" (->str io) ")")})
                       (sort-by (juxt :submodule/io :submodule/name) @submodules))}]

     [dropdown
      {:label     "Group/Subgroup:"
       :on-select #(do
                     (reset-cond!)
                     (set-field (conj var-path :group) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :group/name}]
                         {:value value :label label}) @groups)}]

     [dropdown
      {:label     "Variable:"
       :on-select #(do
                     (set-field (conj cond-path :conditional/values) nil)
                     (set-field (conj cond-path :conditional/group-variable-uuid)
                                (u/input-value %)))
       :options   (map (fn [{value :bp/uuid label :variable/name}]
                         {:value value :label label}) @variables)}]

     [dropdown
      {:label     "Operator:"
       :on-select #(set-field (conj cond-path :conditional/operator)
                              (keyword (u/input-value %)))
       :options   (filter some? [{:value :equal :label "="}
                                 {:value :not-equal :label "!="}
                                 (when-not @is-output? {:value :in :label "IN"})])}]

     [dropdown
      {:label     "Value:"
       :multiple? (= :in @(get-field (conj cond-path :conditional/operator)))
       :on-select #(let [vs (u/input-multi-select %)]
                     (set-field (conj cond-path :conditional/values) vs))
       :options   (if @is-output?
                    [{:value "true" :label "True"}
                     {:value "false" :label "False"}]
                    (map (fn [{value :list-option/value label :list-option/name}]
                           {:value value :label label}) @options))}]]))

(defn manage-conditionals
  "Component to manage conditional for an entity. Takes:
   - entity-id [int]: the ID of the entity
   - cond-attr [keyword]: the attribute name of the conditionals (e.g. `:group/conditionals`)"
  [entity-id cond-attr]
  (r/with-let [state     (r/atom "variable")
               cond-path [:editors :conditional cond-attr]
               set-type  #(do (reset! state %)
                              (rf/dispatch-sync [:state/set-state cond-path nil])
                              (rf/dispatch-sync [:state/set-state
                                                 (conj cond-path :conditional/operator)
                                                 :equal])
                              (rf/dispatch-sync [:state/set-state
                                                 (conj cond-path :conditional/type)
                                                 (if (= % "module") :module :group-variable)]))
               conditional (rf/subscribe [:state cond-path])]

    [:form.row
     {:on-submit (u/on-submit #(on-submit entity-id cond-attr))}
     [:h4 "Manage Conditionals:"]

     [radio-buttons
      "Conditional Type:"
      [{:label "Module" :value "module"}
       {:label "Variable" :value "variable"}]
      #(set-type (u/input-value %))]

     (when (:conditional/type @conditional)
       (condp = @state
         "variable"
         [manage-variable-conditionals entity-id cond-attr]

         "module"
         [manage-module-conditionals entity-id cond-attr]))

     [:button.btn.btn-sm.btn-outline-primary.mt-4
      {:type     "submit"
       :disabled (not (s/valid? :behave/conditional @conditional))}
      "Save"]]))
