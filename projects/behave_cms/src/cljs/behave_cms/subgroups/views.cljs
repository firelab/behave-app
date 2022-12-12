(ns behave-cms.subgroups.views
  (:require [reagent.core  :as r]
            [re-frame.core :as rf]
            [behave-cms.components.common          :refer [accordion simple-table window]]
            [behave-cms.components.entity-form     :refer [entity-form]]
            [behave-cms.components.help-editor     :refer [help-editor]]
            [behave-cms.components.sidebar         :refer [sidebar sidebar-width]]
            [behave-cms.components.translations    :refer [all-translations]]
            [behave-cms.components.variable-search :refer [variable-search]]
            [behave-cms.utils :as u]))

;;; Private Views

(defn- subgroup-form [group-id subgroup-id]
  [entity-form {:entity        :groups
                :parent-entity :group/children
                :parent-id     group-id
                :id            subgroup-id
                :fields        [{:label     "Name"
                                 :required? true
                                 :field-key :group-name}]}])

(defn- manage-subgroup [{id :db/id}]
  (let [subgroup (rf/subscribe [:state :subgroup])]
    [subgroup-form id @subgroup]))

(defn- subgroups-table [{id :db/id}]
  (r/with-let [subgroups (rf/subscribe [:entities :subgroups])]
    [simple-table
     [:group/name]
     (->> @subgroups (vals) (sort-by :group/name))
     {:on-select #(rf/dispatch [:state/set-state :subgroup (:id %)])
      :on-delete #(when (js/confirm (str "Are you sure you want to delete the subgroup " (:group/name %) "?"))
                    (rf/dispatch [:api/delete-entity :groups (:id %)]))}]))

(defn- variables-table [{id :db/id}]
  (r/with-let [group-variables (rf/subscribe [:group-variables id])]
    [simple-table
     [:variable/name]
     (->> @group-variables (vals) (sort-by :variable/order))
     {:on-delete   #(when (js/confirm (str "Are you sure you want to delete the variable " (:variable/name %) "?"))
                      (rf/dispatch [:api/delete-entity :group-variables %]))
      :on-increase #(rf/dispatch [:group-variable/reorder % :up @group-variables])
      :on-decrease #(rf/dispatch [:group-variable/reorder % :down @group-variables])}]))

(defn- add-variable [{id :db/id group-variables :group/variables}]
  (let [all-variables (rf/subscribe [:pull-with-attr :variable/name])]
    [:div.row
     [:h4 "Add Variable:"]
     [variable-search
      @all-variables
      (u/debounce #(rf/dispatch [:groups/search-variables %]) 1000)
      #(rf/dispatch [:api/update-entity
                     {:db/id           id
                      :group/variables [%]
                      :variable/order  (count @group-variables)}])
      #(rf/dispatch [:state/set-state [:search :variables] nil])]]))

;;; Public Views

(defn list-subgroups-page
  "Renders the subgroups page. Takes in a group UUID."
  [{:keys [id]}]
  (let [group           (rf/subscribe [:entity id])
        group-variables (rf/subscribe [:sidebar/variables id])]
    [:<>
     [sidebar
      "Variables"
      @group-variables
      "Groups"
      (str "/submodules/" (:submodule/rid @group))]
     [window
      sidebar-width
      [:div.container
       [:div.row.mb-3.mt-4
        [:h2 (:group/name @group)]]
       [accordion
        "Variables"
        [:div.col-6
         [variables-table @group]]
        [:div.col-6
         [add-variable @group]]]
       [:hr]
       [accordion
        "Translations"
        [all-translations (:group/translation-key @group)]]
       [:hr]
       [accordion
        "Help Page"
        [:div.col-12
         [help-editor (:group/help-key @group)]]]]]]))
