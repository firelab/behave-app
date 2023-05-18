(ns behave-cms.subgroups.views
  (:require [clojure.set   :refer [difference]]
            [clojure.string :as str]
            [reagent.core  :as r]
            [re-frame.core :as rf]
            [data-utils.interface :refer [parse-int]]
            [string-utils.interface :refer [->kebab ->str]]
            [behave-cms.components.common          :refer [accordion dropdown simple-table window]]
            [behave-cms.components.entity-form     :refer [entity-form]]
            [behave-cms.help.views                 :refer [help-editor]]
            [behave-cms.components.sidebar         :refer [sidebar sidebar-width]]
            [behave-cms.components.translations    :refer [all-translations]]
            [behave-cms.components.variable-search :refer [variable-search]]
            [behave-cms.utils :as u]
            [behave-cms.subs]
            [behave-cms.events]))

;;; Private Views

(defn- subgroup-form [group-id subgroup-id]
  [entity-form {:entity        :group
                :parent-field  :group/_children
                :parent-id     group-id
                :id            subgroup-id
                :fields        [{:label     "Name"
                                 :required? true
                                 :field-key :group-name}]}])

(defn- manage-subgroup [{id :db/id}]
  (let [subgroup (rf/subscribe [:state :subgroup])]
    [subgroup-form id @subgroup]))

(defn- subgroups-table [{group-id :db/id}]
  (r/with-let [subgroups (rf/subscribe [:group/subgroups group-id])]
    [simple-table
     [:group/name]
     (sort-by :group/name @subgroups)
     {:on-select #(rf/dispatch [:state/set-state :subgroup (:db/id %)])
      :on-delete #(when (js/confirm (str "Are you sure you want to delete the subgroup " (:group/name %) "?"))
                    (rf/dispatch [:api/delete-entity %]))
      :on-increase #(rf/dispatch [:api/reorder % @subgroups :group/order :inc])
      :on-decrease #(rf/dispatch [:api/reorder % @subgroups :group/order :dec])}]))

(defn- variables-table [{group-id :db/id translation-key :group/translation-key}]
  (r/with-let [group-variables (rf/subscribe [:group/variables group-id])]
    [simple-table
     [:variable/name]
     (sort-by :group-variable/order @group-variables)
     {:on-delete   #(when (js/confirm (str "Are you sure you want to delete the variable " (:variable/name %) "?"))
                      (rf/dispatch [:api/delete-entity %]))
      :on-increase #(rf/dispatch [:api/reorder % @group-variables :group-variable/order :inc])
      :on-decrease #(rf/dispatch [:api/reorder % @group-variables :group-variable/order :dec])}]))

(defn- add-variable [{id :db/id group-variables :group/group-variables translation-key :group/translation-key}]
  (let [query            (rf/subscribe [:state [:search :variables]])
        all-variables    (rf/subscribe [:group/search-variables @query])
        all-variable-ids (set (map :db/id @all-variables))
        gv-ids           (set (map #(get-in % [:variable/_group-variables 0 :db/id]) group-variables))
        remaining-ids    (difference all-variable-ids gv-ids)
        remaining        (filter #(-> % (:db/id) (remaining-ids)) @all-variables)]
    [:div.row
     [:h4 "Add Variable:"]
     [variable-search
      remaining
      (u/debounce #(rf/dispatch [:state/set-state [:search :variables] %]) 1000)
      #(let [variable @(rf/subscribe [:pull '[:variable/name] %])]
         (rf/dispatch [:api/create-entity
                       {:group/_group-variables         id
                        :variable/_group-variables      %
                        :group-variable/translation-key (str translation-key ":" (->kebab (:variable/name variable)))
                        :group-variable/help-key        (str translation-key ":" (->kebab (:variable/name variable)) ":help")
                        :group-variable/order           (count group-variables)}]))
      #(rf/dispatch [:state/set-state [:search :variables] nil])]]))

;;; Conditionals

(defn- conditionals-table [{group-id :db/id conditional :group/conditionals-operator}]
  (r/with-let [group-variables (rf/subscribe [:group/conditionals group-id])]
    [:<>
     [dropdown
      {:label     "Combined Operator:"
       :selected  conditional
       :on-select #(rf/dispatch [:api/update-entity
                                 {:db/id group-id :group/conditionals-operator (keyword (u/input-value %))}])
       :options   [{:value :and :label "AND"}
                   {:value :or :label "OR"}]}]
     [simple-table
      [:variable/name :conditional/operator :conditional/values]
      (sort-by :variable/name @group-variables)
      {:on-delete #(when (js/confirm (str "Are you sure you want to delete the conditional " (:variable/name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))}]]))

(defn- manage-conditionals [{id :db/id}]
  (let [var-path   [:editors :variable-lookup]
        group-path [:editors :groups]
        cond-path  [:editors :groups :group/conditional]

        get-field  #(rf/subscribe [:state %])
        set-field  (fn [path v] (rf/dispatch [:state/set-state path v]))
        on-submit  #(rf/dispatch [:api/create-entity
                                  (merge @(rf/subscribe [:state cond-path])
                                         {:group/_conditionals id})])

        modules    (rf/subscribe [:subgroup/app-modules id])
        submodules (rf/subscribe [:pull-children :module/submodules @(get-field (conj var-path :module))])
        groups     (rf/subscribe [:pull-children :submodule/group @(get-field (conj var-path :submodule))])
        is-output? (rf/subscribe [:submodule/is-output? @(get-field (conj var-path :submodule))]) 
        variables  (rf/subscribe [(if @is-output? :group/variables :group/discrete-variables) @(get-field (conj var-path :group))])
        options    (rf/subscribe [:group/discrete-variable-options @(get-field (conj cond-path :conditional/group-variable-uuid))])]

    [:div.row
     [:h4 "Manage Conditionals:"]
     [:form
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
        :options  (if @is-output?
                    [{:value "true" :label "True"}
                     {:value "false" :label "False"}]
                    (map (fn [{value :list-option/index label :list-option/name}]
                           {:value value :label label}) @options))}]


      [:button.btn.btn-sm.btn-outline-primary.mt-4
       {:on-click (u/on-submit on-submit)}
       "Save"]]]))

;;; Public Views

(defn list-subgroups-page
  "Renders the subgroups page. Takes in a group UUID."
  [{:keys [id]}]
  (let [loaded? (rf/subscribe [:state :loaded?])]
    (if (not @loaded?)
      [:div "Loading..."]
      (let [group           (rf/subscribe [:entity id '[* {:submodule/_groups     [*]
                                                           :group/group-variables [* {:variable/_group-variables [*]}]}]])
            group-variables (rf/subscribe [:sidebar/variables id])]
        [:<>
         [sidebar
          "Variables"
          @group-variables
          "Groups"
          (str "/submodules/" (get-in @group [:submodule/_groups 0 :db/id]))]
         [window
          sidebar-width
          [:div.container
           ^{:key "name"}
           [:div.row.mb-3.mt-4
            [:h2 (:group/name @group)]]
           ^{:key "variables"}
           [accordion
            "Variables"
            [:div.col-6
             [variables-table @group]]
            [:div.col-6
             [add-variable @group]]]
           [:hr]
           ^{:key "conditionals"}
           [accordion
            "Conditionals"
            [:div.col-6
             [conditionals-table @group]]
            [:div.col-6
             [manage-conditionals @group]]]
           [:hr]
           ^{:key "translations"}
           [accordion
            "Translations"
            [all-translations (:group/translation-key @group)]]
           [:hr]
           ^{:key "help"}
           [accordion
            "Help Page"
            [:div.col-12
             [help-editor (:group/help-key @group)]]]]]]))))
