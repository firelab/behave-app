(ns behave-cms.groups.views
  (:require [re-frame.core :as rf]
            [string-utils.interface :refer [->str]]
            [behave-cms.components.common          :refer [accordion simple-table window]]
            [behave-cms.components.conditionals    :refer [conditionals-table manage-conditionals]]
            [behave-cms.components.entity-form     :refer [entity-form]]
            [behave-cms.components.sidebar         :refer [sidebar sidebar-width]]
            [behave-cms.components.translations    :refer [all-translations]]
            [behave-cms.help.views                 :refer [help-editor]]
            [behave-cms.groups.subs]))

(defn- group-form [submodule-id group-id num-groups]
  [entity-form {:entity        :group
                :parent-field  :submodule/_groups
                :parent-id     submodule-id
                :id            group-id
                :fields        [{:label     "Name"
                                 :required? true
                                 :field-key :group/name}]
                :on-create     #(assoc % :group/order num-groups)}])

(defn- manage-group [{submodule-id :db/id}]
  (let [groups (rf/subscribe [:groups submodule-id])
        *group (rf/subscribe [:state :group])]
    [:div.col-6
     [:h3 (str (if @*group "Update" "Add") " Group")
      [group-form submodule-id @*group (count @groups)]]]))

(defn- groups-table [{submodule-id :db/id}]
  (let [groups (rf/subscribe [:groups submodule-id])]
    [:div.col-6
     [simple-table
      [:group/name]
      (sort-by :group/order @groups)
      {:on-select   #(rf/dispatch [:state/set-state :group (:db/id %)])
       :on-delete   #(when (js/confirm (str "Are you sure you want to delete the group " (:group/name %) "?"))
                       (rf/dispatch [:api/delete-entity %]))
       :on-increase #(rf/dispatch [:api/reorder % @groups :group/order :inc])
       :on-decrease #(rf/dispatch [:api/reorder % @groups :group/order :dec])}]]))

(defn list-groups-page
  "Component for groups page. Takes a single map with:
   - :id [int] - Submodule Entity ID"
  [{submodule-eid :id}]
  (let [submodule           (rf/subscribe [:entity submodule-eid '[* {:module/_submodules [*]}]])
        sidebar-groups      (rf/subscribe [:sidebar/groups submodule-eid])
        var-conditionals    (rf/subscribe [:submodule/variable-conditionals submodule-eid])
        module-conditionals (rf/subscribe [:submodule/module-conditionals submodule-eid])]
    [:<>
     [sidebar
      "Groups"
      @sidebar-groups
      "Submodules"
      (str "/modules/" (get-in @submodule [:module/_submodules 0 :db/id]))]
     [window sidebar-width
      [:div.container
       [:div.row.mb-3.mt-4
        [:h2 (str (:submodule/name @submodule) " (" (->str (:submodule/io @submodule)) ")")]]
       [accordion
        "Groups"
        [groups-table @submodule]
        [manage-group @submodule]]
       [:hr]
       ^{:key "conditionals"}
       [accordion
        "Conditionals"
        [:div.col-6
         [conditionals-table submodule-eid (concat @var-conditionals @module-conditionals) :submodule/conditionals :submodule/conditionals-operator]]
        [:div.col-6
         [manage-conditionals submodule-eid :submodule/conditionals]]]
       [:hr]
       [accordion
        "Translations"
        [:div.col-12
         [all-translations (:submodule/translation-key @submodule)]]]
       [:hr]
       [accordion
        "Help Page"
        [help-editor (:submodule/help-key @submodule)]]]]]))
