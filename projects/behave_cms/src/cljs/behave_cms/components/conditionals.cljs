(ns behave-cms.components.conditionals
  (:require
   [behave-cms.components.common :refer [btn-sm checkboxes dropdown labeled-float-input radio-buttons]]
   [behave-cms.utils             :as u]
   [clojure.set                  :as set]
   [clojure.spec.alpha           :as s]
   [clojure.string               :as str]
   [re-frame.core                :as rf]
   [string-utils.interface       :refer [->str]]))

;;; Helpers

(defn- clear-editor []
  (rf/dispatch [:state/set-state :editors {}]))

(defn- clear-show-sub-conditional-editor []
  (rf/dispatch [:state/set-state :show-sub-conditional-editor {}]))

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
  (clear-editor))

(defn- toggle-item [x xs]
  (let [xs-set (set xs)]
    (vec (if (xs-set x)
           (remove #(= x %) xs)
           (conj xs x)))))

(defn- update-draft [cond-attr conditional]
  (let [cond-path   [:editors :conditional cond-attr]
        conditional @(rf/subscribe [:pull '[*] [:bp/nid (:bp/nid conditional)]])]
    (if @(rf/subscribe [:state cond-path])
      (when (js/confirm (str "You have unsaved changes. Are you sure you want to remove those change?"))
        (rf/dispatch [:state/set-state cond-path conditional]))
      (rf/dispatch [:state/set-state cond-path conditional]))))

;;; Components

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
        *group      (get-field (conj var-path :group))
        variables   (rf/subscribe [:group/variables @*group])
        *gv-uuid    (get-field (conj cond-path :conditional/group-variable-uuid))
        *gv-kind    (rf/subscribe [:group-variable/kind @*gv-uuid])
        options     (rf/subscribe [:group/discrete-variable-options @*gv-uuid])
        *operator   (get-field (conj cond-path :conditional/operator))
        multiple?   (= :in @*operator)
        reset-cond! #(set-field cond-path {:conditional/type :group-variable})
        *values     (get-field (conj cond-path :conditional/values))]

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
       :selected  (->str @*operator)
       :on-select #(set-field (conj cond-path :conditional/operator)
                              (keyword (u/input-value %)))
       :options   (filter some? [{:value "equal" :label "="}
                                 {:value "not-equal" :label "!="}
                                 (when (and (not @is-output?) (= @*gv-kind :discrete))
                                   {:value :in :label "IN"})])}]

     (if (or (nil? @*gv-uuid) @is-output? (= @*gv-kind :discrete))
       [dropdown
        {:label     "Value:"
         :selected  @*values
         :multiple? multiple?
         :on-select #(let [vs (u/input-multi-select %)]
                       (set-field (conj cond-path :conditional/values) vs))
         :options   (if @is-output?
                      [{:value "true" :label "True"}
                       {:value "false" :label "False"}]
                      (map (fn [{value :list-option/value label :list-option/name}]
                             {:value value :label label}) @options))}]

       [labeled-float-input
        "Value:"
        (u/->text-input-value @*values)
        #(set-field (conj cond-path :conditional/values) [(str %)])
        {:zero-margin? true}])]))

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
      #(do (clear-show-sub-conditional-editor)
           (set-type (keyword (u/input-value %))))]

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

(defn add-sub-conditionals
  "Component to manage conditional for an entity. Takes:
   - entity-id [int]: the ID of the entity
   - cond-attr [keyword]: the attribute name of the conditionals (e.g. `:group/conditionals`)"
  [entity-id cond-attr sub-conditional-eid]

  (let [cond-path   [:editors :conditional cond-attr]
        set-type    #(rf/dispatch [:state/set-state
                                   cond-path
                                   {:conditional/type     %
                                    :conditional/operator :equal}])
        conditional (rf/subscribe [:state cond-path])]
    [:form.row
     {:on-submit (u/on-submit
                  #(let [conditional @(rf/subscribe [:state [:editors :conditional cond-attr]])]
                     (if (:bp/nid conditional)
                       (update-conditional! conditional)
                       (do
                         (when (nil? (:conditional/sub-conditional-operator
                                      @(rf/subscribe [:entity sub-conditional-eid])))
                           (rf/dispatch [:ds/transact {:db/id                                sub-conditional-eid
                                                       :conditional/sub-conditional-operator :and}]))
                         (rf/dispatch [:api/create-entity
                                       (merge conditional
                                              {(inverse-attr cond-attr) sub-conditional-eid})])))
                     (rf/dispatch [:state/set-state cond-attr nil])
                     (clear-editor)
                     (clear-show-sub-conditional-editor)))}
     [:h4 "Add Sub Conditional"]
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

(defn conditionals-table
  "Table of conditionals for entity.
  - `parent-eid`: Used to keep track of the first-parent entity so
    `manage-conditionals` can lookup the proper modules, submodules, etc

  - `this-eid`: Starts as the same as the `parent-eid`, but on each recursive call
    is updated to the conditional's entity id

  - `conditionals`: sequence of conditional data for `this-eid`

  - `cond-attr`: keyword. May start as `submodule/conditonals`,
    `:group/conditionals`, etc. Recursive calls is updated to
    `:conditional/subconditionals`

  - `cond-attr`: keyword. May start as `:submodule/conditionals-operator`,
    `group/conditional-operator`, etc. Recursive calls is updated to
    `:conditional/subconditional-operator`"

  [parent-eid this-eid conditionals cond-attr cond-op-attr]
  (when (seq conditionals)
    (let [entity (rf/subscribe [:entity this-eid])]
      [:div.conditionals-table
       [:div.line]
       [:div.conditionals-table__operator
        [dropdown
         {:selected  (->str (get @entity cond-op-attr))
          :on-select #(rf/dispatch [:api/update-entity
                                    {:db/id this-eid cond-op-attr (keyword (u/input-value %))}])
          :options   [{:value "and" :label "AND"}
                      {:value "or" :label "OR"}]}]]
       [:div.line]
       [:div.conditionals-table__entries
        (doall
         (map
          (fn [{gv-uuid          :conditional/group-variable-uuid
                conditional-type :conditional/type
                op               :conditional/operator
                values           :conditional/values
                conditional-eid  :db/id
                :as              conditional-entity}]
            (let [sub-conditionals (:conditional/sub-conditionals @(rf/subscribe [:entity conditional-eid]))
                  gv-id            @(rf/subscribe [:bp/lookup gv-uuid])
                  [module-id]      @(rf/subscribe [:group-variable/module-submodule-group gv-id])
                  module-name      @(rf/subscribe [:entity-attr module-id :module/name])
                  v-name           (if (= conditional-type :module)
                                     "Module"
                                     @(rf/subscribe [:gv-uuid->variable-name gv-uuid]))]
              [:div.conditionals-table__row
               (when (seq sub-conditionals)
                 [:div.conditionals-table__operator
                  [:div.line]
                  [dropdown
                   {:selected  "and"
                    :disabled? true
                    :options   [{:value "and" :label "AND"}]}]
                  [:div.line]])
               [:div {:class ["conditionals-table__row__conditional"
                              (when (seq sub-conditionals)
                                "conditionals-table__row__conditional__with-sub-conditionals")]}
                [:div.conditionals-table__values
                 (when (= conditional-type :group-variable)
                   [:div.conditionals-table__values__module-name module-name])
                 [:div.conditionals-table__values__var-name "\"" v-name "\""]
                 [:div.conditionals-table__values__op op]
                 [:div.conditionals-table__values__values (str values)]
                 [:div.conditionals-table__entry__manage
                  [btn-sm :outline-secondary "Edit"   #(do
                                                         (clear-editor)
                                                         (clear-show-sub-conditional-editor)
                                                         (update-draft cond-attr conditional-entity))]
                  [btn-sm :outline-danger    "Delete" #(when
                                                           (js/confirm
                                                            (str "Are you sure you want to delete the conditional "
                                                                 (:variable/name %)
                                                                 "?"))
                                                         (rf/dispatch [:api/delete-entity conditional-entity]))]
                  [btn-sm :outline-secondary "Add Sub Conditional"
                   #(do
                      (clear-editor)
                      (if @(rf/subscribe [:state [:show-sub-conditional-editor conditional-eid]])
                        (rf/dispatch [:state/update
                                      [:show-sub-conditional-editor conditional-eid]
                                      (fn [state] (not state))])
                        (rf/dispatch [:state/set-state
                                      :show-sub-conditional-editor
                                      {conditional-eid true}])))]]]
                (when @(rf/subscribe [:state [:show-sub-conditional-editor conditional-eid]])
                  [add-sub-conditionals
                   parent-eid
                   :conditional/sub-conditionals
                   conditional-eid])
                (when (seq sub-conditionals)
                  [:div.conditionals-table__row__sub-conditionals
                   [conditionals-table
                    parent-eid
                    conditional-eid
                    sub-conditionals
                    :group/conditionals
                    :conditional/sub-conditional-operator]])]]))
          (sort-by :variable/name conditionals)))]])))
