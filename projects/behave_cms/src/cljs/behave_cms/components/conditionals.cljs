(ns behave-cms.components.conditionals
  (:require
   [behave-cms.components.common :refer [dropdown checkboxes simple-table]]
   [behave-cms.utils             :as u]
   [clojure.string               :as str]
   [reagent.core                 :as r]
   [re-frame.core                :as rf]
   [string-utils.interface       :refer [->str]]))

;;; Helpers

(defn inverse-attr
  [attr]
  (keyword (str/join "/_" (str/split (->str attr) #"/"))))

;;; 

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

;;; Components

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
      {:on-select cond-attr
       :on-delete #(when (js/confirm (str "Are you sure you want to delete the conditional " (:variable/name %) "?"))
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
  (let [var-path   [:editors :variable-lookup]
        cond-path  [:editors :conditional cond-attr]
        get-field  #(rf/subscribe [:state %])
        set-field  (fn [path v] (rf/dispatch [:state/set-state path v]))
        on-submit  #(rf/dispatch [:api/create-entity
                                 (merge @(rf/subscribe [:state cond-path])
                                        {(inverse-attr cond-attr) entity-id})])
        modules    (rf/subscribe [:subgroup/app-modules entity-id])
        submodules (rf/subscribe [:pull-children :module/submodules @(get-field (conj var-path :module))])
        groups     (rf/subscribe [:pull-children :submodule/groups @(get-field (conj var-path :submodule))])
        is-output? (rf/subscribe [:submodule/is-output? @(get-field (conj var-path :submodule))]) 
        variables  (rf/subscribe [(if @is-output? :group/variables :group/discrete-variables) @(get-field (conj var-path :group))])
        options    (rf/subscribe [:group/discrete-variable-options @(get-field (conj cond-path :conditional/group-variable-uuid))])]

    [:<> 
     [dropdown
      {:label     "Module:"
       :on-select #(do
                     (set-field cond-path {})
                     (set-field var-path {})
                     (set-field (conj var-path :module) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :module/name}]
                         {:value value :label label}) @modules)}]

     [dropdown
      {:label     "Submodule:"
       :on-select #(do
                     (set-field cond-path {})
                     (set-field (conj var-path :group) nil)
                     (set-field (conj var-path :submodule) (u/input-int-value %)))
       :options   (map (fn [{value :db/id label :submodule/name io :submodule/io}]
                         {:value value :label (str label " (" (->str io) ")")})
                       (sort-by (juxt :submodule/io :submodule/name) @submodules))}]

     [dropdown
      {:label     "Group:"
       :on-select #(do
                     (set-field cond-path {})
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
                     (println "--- GOT " vs)
                     (set-field (conj cond-path :conditional/values)
                                (str/join "," vs)))
       :options   (if @is-output?
                    [{:value "true" :label "True"}
                     {:value "false" :label "False"}]
                    (map (fn [{value :list-option/index label :list-option/name}]
                           {:value value :label label}) @options))}]]))

(defn manage-conditionals [entity-id cond-attr cond-attr-op]
  (r/with-let [state     (r/atom "variable")
               cond-path [:editors :conditional cond-attr]
               set-type  #(do (reset! state %)
                              (rf/dispatch-sync [:state/set-state cond-path nil])
                              (rf/dispatch-sync [:state/set-state 
                                                 (conj cond-path :conditional/operator)
                                                 (if (= % "module") :in :equals)])
                              (rf/dispatch-sync [:state/set-state
                                                 (conj cond-path :conditional/type)
                                                 (if (= % "module") :module :group-variable)]))
               type       (rf/subscribe [:state (conj cond-path :conditional/type)])
               on-submit #(do
                            (rf/dispatch [:ds/transact
                                        (merge @(rf/subscribe [:state [:editors :conditional cond-attr]])
                                               {(inverse-attr cond-attr) entity-id})])
                            (rf/dispatch [:state/set-state [:editors :conditional] {}]))]
    [:form.row
     {:on-submit (u/on-submit on-submit)}
     [:h4 "Manage Conditionals:"]

     [radio-buttons
      "Conditional Type:"
      [{:label "Module" :value "module"}
       {:label "Variable" :value "variable"}]
      #(set-type (u/input-value %))]

     (when @type
       (condp = @state
         "variable"
         [manage-variable-conditionals entity-id cond-attr]

         "module"
         [manage-module-conditionals entity-id cond-attr]))

     [:button.btn.btn-sm.btn-outline-primary.mt-4 {:type "submit"} "Save"]]))
