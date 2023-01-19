(ns behave-cms.subgroups.views
  (:require [clojure.set   :refer [difference]]
            [reagent.core  :as r]
            [re-frame.core :as rf]
            [data-utils.interface :refer [parse-int]]
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
                :parent-entity :group/_children
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

(defn- variables-table [{group-id :db/id}]
  (r/with-let [group-variables (rf/subscribe [:group/variables group-id])]
    [simple-table
     [:variable/name]
     (sort-by :group-variable/order @group-variables)
     {:on-delete   #(when (js/confirm (str "Are you sure you want to delete the variable " (:variable/name %) "?"))
                      (rf/dispatch [:api/delete-entity %]))
      :on-increase #(rf/dispatch [:api/reorder % @group-variables :group-variable/order :inc])
      :on-decrease #(rf/dispatch [:api/reorder % @group-variables :group-variable/order :dec])}]))

(defn- add-variable [{id :db/id group-variables :group/group-variables}]
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
      #(rf/dispatch [:api/create-entity
                     {:group/_group-variables    id
                      :variable/_group-variables %
                      :group-variable/order      (count group-variables)}])
      #(rf/dispatch [:state/set-state [:search :variables] nil])]]))

;;; Public Views

(defn list-subgroups-page
  "Renders the subgroups page. Takes in a group UUID."
  [{:keys [id]}]
  (let [loaded? (rf/subscribe [:state :loaded?])]
    (if (not @loaded?)
      [:div "Loading..."]
      (let [group-id        (parse-int id)
            group           (rf/subscribe [:entity group-id '[* {:submodule/_groups [*]
                                                                 :group/group-variables [* {:variable/_group-variables [*]}]}]])
            group-variables (rf/subscribe [:sidebar/variables group-id])]
        [:<>
         [sidebar
          "Variables"
          @group-variables
          "Groups"
          (str "/submodules/" (get-in @group [:submodule/_groups 0 :db/id]))]
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
             [help-editor (:group/help-key @group)]]]]]]))))
