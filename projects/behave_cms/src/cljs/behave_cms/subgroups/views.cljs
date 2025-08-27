(ns behave-cms.subgroups.views
  (:require [clojure.set   :refer [difference]]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [string-utils.interface :refer [->kebab]]
            [behave-cms.components.common          :refer [accordion checkbox simple-table window]]
            [behave-cms.components.conditionals.views    :refer [conditionals-graph manage-conditionals]]
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
  [entity-form {:entity       :group
                :parent-field :group/_children
                :parent-id    group-id
                :id           subgroup-id
                :fields       [{:label     "Name"
                                :required? true
                                :field-key :group/name}]}])

(defn- manage-subgroup [group-id]
  (let [subgroup (rf/subscribe [:state :subgroup])]
    [subgroup-form group-id @subgroup]))

(defn- subgroups-table [group-id]
  (let [subgroups (rf/subscribe [:group/subgroups group-id])]
    [simple-table
     [:group/name]
     (sort-by :group/order @subgroups)
     {:on-select   #(rf/dispatch [:state/set-state :subgroup (:db/id %)])
      :on-delete   #(when (js/confirm (str "Are you sure you want to delete the subgroup " (:group/name %) "?"))
                      (rf/dispatch [:api/delete-entity %]))
      :on-increase #(rf/dispatch [:api/reorder % @subgroups :group/order :inc])
      :on-decrease #(rf/dispatch [:api/reorder % @subgroups :group/order :dec])}]))

(defn- variables-table [group-id]
  (let [group-variables (rf/subscribe [:group/variables group-id])]
    [simple-table
     [:variable/name :variable/domain-uuid :group-variable/conditionally-set?]
     (sort-by :group-variable/order @group-variables)
     {:on-delete   #(when (js/confirm (str "Are you sure you want to delete the variable " (:variable/name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))
      :on-increase #(rf/dispatch [:api/reorder % @group-variables :group-variable/order :inc])
      :on-decrease #(rf/dispatch [:api/reorder % @group-variables :group-variable/order :dec])
      :on-select   #(rf/dispatch [:subgroups/edit-variables (first (:variable/_group-variables %))])}]))

(defn- add-variable [group-id]
  (let [translation-key  (rf/subscribe [:entity-attr group-id :group/translation-key])
        group-variables  (rf/subscribe [:group/variables group-id])
        query            (rf/subscribe [:state [:search :variables]])
        all-variables    (rf/subscribe [:group/search-variables @query])
        all-variable-ids (set (map :db/id @all-variables))
        gv-ids           (set (map #(get-in % [:variable/_group-variables 0 :db/id]) @group-variables))
        remaining-ids    (difference all-variable-ids gv-ids)
        remaining        (filter #(-> % (:db/id) (remaining-ids)) @all-variables)]
    [:div.row
     [:h4 "Add Variable:"]
     [variable-search
      {:results   remaining
       :on-change (u/debounce #(rf/dispatch [:state/set-state [:search :variables] %]) 1000)
       :on-select #(let [variable @(rf/subscribe [:pull '[:variable/name] %])]
                     (rf/dispatch [:api/create-entity
                                   {:group/_group-variables                group-id
                                    :variable/_group-variables             %
                                    :group-variable/translation-key        (str @translation-key ":" (->kebab (:variable/name variable)))
                                    :group-variable/result-translation-key (-> (str/replace @translation-key #":input:|:output:" ":result:")
                                                                               (str ":" (->kebab (:variable/name variable))))
                                    :group-variable/help-key               (str @translation-key ":" (->kebab (:variable/name variable)) ":help")
                                    :group-variable/order                  (count @group-variables)}]))
       :on-blur   #(rf/dispatch [:state/set-state [:search :variables] nil])}]]))

;;; Settings

(defn- bool-setting [label attr group]
  (let [{id :db/id} group
        *value?     (atom (get group attr))
        update!     #(rf/dispatch [:api/update-entity
                                   {:db/id id attr @*value?}])]
    [:div.mt-1
     [checkbox
      label
      @*value?
      #(do (swap! *value? not)
           (update!))]]))

(defn- group-settings [group]
  [:div.row.mt-2
   [bool-setting "Repeat Group?" :group/repeat? group]
   [bool-setting "Research Group?" :group/research? group]
   [bool-setting "Hide Group?" :group/hidden? group]
   [bool-setting "Single Select Group?" :group/single-select? group]])

;;; Public Views

(defn list-subgroups-page
  "Renders the subgroups page. Takes in a group UUID."
  [{:keys [nid]}]
  (let [group               (rf/subscribe [:entity [:bp/nid nid] '[* {:submodule/_groups [:db/id :submodule/name :bp/nid]}]])
        id                  (:db/id @group)
        parent-group        (rf/subscribe [:subgroup/parent id])
        parent-submodule    (:submodule/_groups @group)
        group-variables     (rf/subscribe [:sidebar/variables id])
        subgroups           (rf/subscribe [:sidebar/subgroups id])
        var-conditionals    (rf/subscribe [:group/variable-conditionals id])
        module-conditionals (rf/subscribe [:group/module-conditionals id])]
    [:div
     {:id (str id)}
     [sidebar
      "Variables"
      @group-variables
      (if @parent-group
        (:group/name @parent-group)
        (str (:submodule/name parent-submodule) " Groups"))
      (if @parent-group
        (str "/groups/" (:bp/nid @parent-group))
        (str "/submodules/" (:bp/nid parent-submodule)))
      "Subgroups"
      (when (seq @subgroups) @subgroups)]
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
         [variables-table id]]
        [:div.col-6
         [add-variable id]]]
       [:hr]
       ^{:key "subgroups"}
       [accordion
        "Subgroups"
        [:div.col-6
         [subgroups-table id]]
        [:div.col-6
         [manage-subgroup id]]]
       [:hr]
       ^{:key "conditionals"}
       [accordion
        "Conditionals"
        [:div.col-9
         [conditionals-graph id id :group/conditionals :group/conditionals-operator]]
        [:div.col-3
         [manage-conditionals id :group/conditionals]]]
       [:hr]
       ^{:key "translations"}
       [accordion
        "Translations"
        [:h5 "Worksheet Translations"]
        [all-translations (:group/translation-key @group)]
        [:h5 "Result Translations"]
        [all-translations (:group/result-translation-key @group)]]
       [:hr]
       ^{:key "help"}
       [accordion
        "Help Page"
        [:div.col-12
         [help-editor (:group/help-key @group)]]]
       [:hr]
       ^{:key "settings"}
       [accordion
        "Settings"
        [:div.col-12
         [group-settings @group]]]]]]))
