(ns behave-cms.subgroups.views
  (:require [clojure.set   :refer [difference]]
            [clojure.string :as str]
            [reagent.core  :as r]
            [re-frame.core :as rf]
            [data-utils.interface :refer [parse-int]]
            [string-utils.interface :refer [->kebab ->str]]
            [behave-cms.components.common          :refer [accordion dropdown simple-table window]]
            [behave-cms.components.conditionals    :refer [conditionals-table manage-conditionals]]
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
                                 :field-key :group/name}]}])

(defn- manage-subgroup [group-id]
  (let [subgroup (rf/subscribe [:state :subgroup])]
    [subgroup-form group-id @subgroup]))

(defn- subgroups-table [group-id]
  (let [subgroups (rf/subscribe [:group/subgroups group-id])]
    [simple-table
     [:group/name]
     (sort-by :group/name @subgroups)
     {:on-select #(rf/dispatch [:state/set-state :subgroup (:db/id %)])
      :on-delete #(when (js/confirm (str "Are you sure you want to delete the subgroup " (:group/name %) "?"))
                    (rf/dispatch [:api/delete-entity %]))
      :on-increase #(rf/dispatch [:api/reorder % @subgroups :group/order :inc])
      :on-decrease #(rf/dispatch [:api/reorder % @subgroups :group/order :dec])}]))

(defn- variables-table [group-id]
  (let [translation-key (rf/subscribe [:entity group-id] '[:group/translation-key])
        group-variables (rf/subscribe [:group/variables group-id])]
    [simple-table
     [:variable/name]
     (sort-by :group-variable/order @group-variables)
     {:on-delete   #(when (js/confirm (str "Are you sure you want to delete the variable " (:variable/name %) "?"))
                      (rf/dispatch [:api/delete-entity %]))
      :on-increase #(rf/dispatch [:api/reorder % @group-variables :group-variable/order :inc])
      :on-decrease #(rf/dispatch [:api/reorder % @group-variables :group-variable/order :dec])}]))

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
      remaining
      (u/debounce #(rf/dispatch [:state/set-state [:search :variables] %]) 1000)
      #(let [variable @(rf/subscribe [:pull '[:variable/name] %])]
         (rf/dispatch [:api/create-entity
                       {:group/_group-variables         group-id
                        :variable/_group-variables      %
                        :group-variable/translation-key (str @translation-key ":" (->kebab (:variable/name variable)))
                        :group-variable/help-key        (str @translation-key ":" (->kebab (:variable/name variable)) ":help")
                        :group-variable/order           (count @group-variables)}]))
      #(rf/dispatch [:state/set-state [:search :variables] nil])]]))

;;; Public Views

(defn list-subgroups-page
  "Renders the subgroups page. Takes in a group UUID."
  [{:keys [id]}]
  (let [parent-group        (rf/subscribe [:subgroup/parent id])
        group               (rf/subscribe [:entity id '[:group/name
                                                        :group/help-key
                                                        :group/translation-key
                                                        {:submodule/_groups [:db/id]}]])
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
        "Groups")
      (if @parent-group
        (str "/groups/" (:db/id @parent-group))
        (str "/submodules/" (get-in @group [:submodule/_groups 0 :db/id])))
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
        [:div.col-6
         [conditionals-table id (concat @var-conditionals @module-conditionals) :group/conditionals :group/conditionals-operator]]
        [:div.col-6
         [manage-conditionals id :group/conditionals]]]
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
         [help-editor (:group/help-key @group)]]]]]]))

(def list-subsubgroups-page #'list-subgroups-page)
