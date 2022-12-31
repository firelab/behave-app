(ns behave-cms.groups.views
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [data-utils.interface :refer [parse-int]]
            [behave-cms.components.common          :refer [accordion simple-table window]]
            [behave-cms.components.entity-form     :refer [entity-form]]
            [behave-cms.components.sidebar         :refer [sidebar sidebar-width]]
            [behave-cms.components.translations    :refer [all-translations]]
            [behave-cms.help.views                 :refer [help-editor]]))

(defn group-form [submodule-id group-id]
  [entity-form {:entity        :groups
                :parent-field  :submodule/_groups
                :parent-id     submodule-id
                :id            group-id
                :fields        [{:label     "Name"
                                 :required? true
                                 :field-key :group/name}]}])

(defn manage-group [{submodule-id :db/id}]
  (let [group (rf/subscribe [:state :group])]
    [:div.col-6
     [:h3 (str (if @group "Update" "Add") " Group")
      [group-form submodule-id @group]]]))

(defn groups-table [{submodule-id :db/id}]
  (let [groups (rf/subscribe [:groups submodule-id])]
    [:div.col-6
     [simple-table
      [:group/name]
      (sort-by :group/order @groups)
      {:on-select   #(rf/dispatch [:state/set-state :group (:db/id %)])
       :on-delete   #(when (js/confirm (str "Are you sure you want to delete the group " (:group/name %) "?"))
                       (rf/dispatch [:api/delete-entity %]))
       :on-increase #(rf/dispatch [:group/reorder % :up @groups])
       :on-decrease #(rf/dispatch [:group/reorder % :down @groups])}]]))

(defn list-groups-page [{id :id}]
  (let [loaded? (rf/subscribe [:state :loaded?])]
    (if (not loaded?)
      [:div "Loading ..."]
      (let [submodule-id (parse-int id)
            submodule    (rf/subscribe [:entity submodule-id '[* {:module/_submodules [*]}]])
            groups       (rf/subscribe [:sidebar/groups submodule-id])]
        [:<>
         [sidebar
          "Groups"
          @groups
          "Submodules"
          (str "/modules/" (get-in @submodule [:module/_submodules 0 :db/id]))]
         [window sidebar-width
          [:div.container
           [:div.row.mb-3.mt-4
            [:h2 (str (:submodule/name @submodule) " (" (name (:submodule/io @submodule)) ")")]]
           [accordion
            "Groups"
            [groups-table @submodule]
            [manage-group @submodule]]
           [:hr]
           [accordion
            "Translations"
            [:div.col-12
             [all-translations (:submodule/translation-key @submodule)]]]
           [:hr]
           [accordion
            "Help Page"
            [help-editor (:submodule/help-key @submodule)]]]]]))))
