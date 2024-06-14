(ns behave-cms.components.conditionals
  (:require
   [clojure.spec.alpha           :as s]
   [clojure.string               :as str]
   [clojure.set                  :as set]
   [behave.schema.conditionals]
   [behave-cms.components.common :refer [dropdown checkboxes simple-table radio-buttons]]
   [behave-cms.utils             :as u]
   [re-frame.core                :as rf]
   [string-utils.interface       :refer [->str]]))

;;; Helpers

(defn- inverse-attr
  "Takes an attribute of the form `:parent/child` returns `:parent/_child`."
  [attr]
  (keyword (str/join "/_" (str/split (->str attr) #"/"))))

(defn- update-conditional! [conditional]
  (let [nid         [:bp/nid (:bp/nid conditional)]
        original    @(rf/subscribe [:pull '[*] nid])
        orig-values (set (:conditional/values original))
        values      (set (:conditional/values conditional))]
    (if (= values orig-values)
      (rf/dispatch [:api/update-entity conditional])
      (let [old-values (set/difference orig-values values)
            new-values (set/difference values orig-values)
            add-tx     (mapv (fn [v] [:db/add nid :conditional/values v]) new-values)
            retract-tx (mapv (fn [v] [:db/retract nid :conditional/values v]) old-values)]
        (rf/dispatch [:ds/transact
                        (concat [(dissoc conditional :conditional/values)]
                                add-tx
                                retract-tx)])))))

(defn- on-submit [entity-id cond-attr]
  (let [conditional @(rf/subscribe [:state [:editors :conditional cond-attr]])]
    (if (:bp/nid conditional)
      (update-conditional! conditional)
      (rf/dispatch [:api/create-entity
                    (merge conditional {(inverse-attr cond-attr) entity-id})])))
  (rf/dispatch [:state/set-state cond-attr nil])
  (rf/dispatch [:state/set-state :editors {}]))

(defn- toggle-item [x xs]
  (let [xs-set (set xs)]
    (vec (if (xs-set x)
           (remove #(= x %) xs)
           (conj xs x)))))

(toggle-item "surface" ["surface"])

(defn- update-draft [cond-attr conditional]
  (let [cond-path   [:editors :conditional cond-attr]
        conditional @(rf/subscribe [:pull '[*] [:bp/nid (:bp/nid conditional)]])]
    (if @(rf/subscribe [:state cond-path])
      (when (js/confirm (str "You have unsaved changes. Are you sure you want to remove those change?"))
        (rf/dispatch [:state/set-state cond-path conditional]))
      (rf/dispatch [:state/set-state cond-path conditional]))))

;;; Components

(defn conditionals-table
  "Table of conditionals for entity."
  [entity-id conditionals cond-attr cond-op-attr]
  (let [entity (rf/subscribe [:entity entity-id])]
    [:<>
     [dropdown
      {:label     "Combined Operator:"
       :selected  (->str (get @entity cond-op-attr))
       :on-select #(rf/dispatch [:api/update-entity
                                 {:db/id entity-id cond-op-attr (keyword (u/input-value %))}])
       :options   [{:value "and" :label "AND"}
                   {:value "or" :label "OR"}]}]
     [simple-table
      [:variable/name :conditional/operator :conditional/values]
      (sort-by :variable/name conditionals)
      {:on-select #(update-draft cond-attr %)
       :on-delete #(when (js/confirm (str "Are you sure you want to delete the conditional " (:variable/name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))}]]))

(defn manage-module-conditionals
  "Form to manage Module conditionals for entity."
  [entity-id cond-attr]
  (let [cond-path   [:editors :conditional cond-attr]
        set-field   (fn [path v] (rf/dispatch [:state/set-state path v]))
        get-field   #(rf/subscribe [:state %])
        module-path (conj cond-path :conditional/values)
        all-modules (rf/subscribe [:subgroup/app-modules entity-id])
        *modules    (get-field module-path)
        options     (map (fn [{label :module/name}]
                           {:value (str/lower-case label) :label label})
                         @all-modules)]
    (println [:MODULES @*modules])
    [:div
     [:h6 "Enabled with Modules:"]
     [checkboxes
      options
      @*modules
      #(set-field module-path (toggle-item (u/input-value %) @*modules))]]))

(defn manage-variable-conditionals
  "Form to manage Variable conditionals for entity."
  [entity-id cond-attr conditional]

  ;; Pre-select Conditional's Group-Variable Parents
  (when (:bp/nid conditional)
    (let [cond-gv-id               @(rf/subscribe [:bp/lookup (:conditional/group-variable-uuid conditional)])
          [module submodule group] @(rf/subscribe [:group-variable/module-submodule-group cond-gv-id])]
      (rf/dispatch [:state/set-state [:editors :variable-lookup] {:module module :submodule submodule :group group}])))

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
        multiple?   (= :in @(get-field (conj cond-path :conditional/operator)))
        reset-cond! #(set-field cond-path {:conditional/type :group-variable})]

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
       :selected  (:conditional/group-variable-uuid conditional)
       :on-select #(do
                     (set-field (conj cond-path :conditional/values) nil)
                     (set-field (conj cond-path :conditional/group-variable-uuid)
                                (u/input-value %)))
       :options   (map (fn [{value :bp/uuid label :variable/name}]
                         {:value value :label label}) @variables)}]

     [dropdown
      {:label     "Operator:"
       :selected  (->str (:conditional/operator conditional))
       :on-select #(set-field (conj cond-path :conditional/operator)
                              (keyword (u/input-value %)))
       :options   (filter some? [{:value "equal" :label "="}
                                 {:value "not-equal" :label "!="}
                                 (when-not @is-output? {:value "in" :label "IN"})])}]

     [dropdown
      {:label     "Value:"
       :selected  (:conditional/values conditional)
       :multiple? multiple?
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

  (let [cond-path   [:editors :conditional cond-attr]
        set-type    #(rf/dispatch [:state/set-state
                                   cond-path
                                   {:conditional/type     %
                                    :conditional/operator :equal}])
        conditional (rf/subscribe [:state cond-path])]

    [:form.row
     {:on-submit (u/on-submit #(on-submit entity-id cond-attr))}
     [:h4 "Manage Conditionals:"]

     [radio-buttons
      "Conditional Type:"
      (->str (:conditional/type @conditional))
      [{:label "Module" :value "module"}
       {:label "Variable" :value "group-variable"}]
      #(set-type (keyword (u/input-value %)))]

     (condp = (:conditional/type @conditional)
       :group-variable
       [manage-variable-conditionals entity-id cond-attr @conditional]

       :module
       [manage-module-conditionals entity-id cond-attr @conditional]

       [:<>])

     [:button.btn.btn-sm.btn-outline-primary.mt-4
      {:type     "submit"
       :disabled (not (s/valid? :behave/conditional @conditional))}
      "Save"]]))
