(ns behave-cms.groups.views
  (:require [re-frame.core :as rf]
            [string-utils.interface :refer [->str]]
            [behave-cms.components.common          :refer [accordion checkbox simple-table window]]
            [behave-cms.components.conditionals    :refer [conditionals-graph manage-conditionals]]
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

;;; Settings

(defn- bool-setting [label attr entity]
  (let [{id :db/id} entity
        *value?     (atom (get entity attr))
        update!     #(rf/dispatch [:api/update-entity {:db/id id attr @*value?}])]
    [:div.mt-1
     [checkbox
      label
      @*value?
      #(do (swap! *value? not)
           (update!))]]))

(defn- settings [submodule]
  [:div.row.mt-2
   [bool-setting "Research Submodule?" :submodule/research? submodule]])

(defn list-groups-page
  "Component for groups page. Takes a single map with:
   - :id [int] - Submodule Entity ID"
  [{nid :nid}]
  (let [submodule           (rf/subscribe [:entity [:bp/nid nid] '[* {:module/_submodules [*]}]])
        submodule-eid       (:db/id @submodule)
        sidebar-groups      (rf/subscribe [:sidebar/groups submodule-eid])
        var-conditionals    (rf/subscribe [:submodule/variable-conditionals submodule-eid])
        module-conditionals (rf/subscribe [:submodule/module-conditionals submodule-eid])
        parent-module       (:module/_submodules @submodule)]
    [:<>
     [sidebar
      "Groups"
      @sidebar-groups
      (str (:module/name parent-module) " Submodules")
      (str "/modules/" (:bp/nid parent-module))]
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
        [:div.col-9
         [conditionals-graph submodule-eid submodule-eid (concat @var-conditionals @module-conditionals) :submodule/conditionals :submodule/conditionals-operator]]
        [:div.col-3
         [manage-conditionals submodule-eid :submodule/conditionals]]]

       [:hr]
       [accordion
        "Translations"
        [:div.col-12
         [all-translations (:submodule/translation-key @submodule)]]]

       [:hr]
       [accordion
        "Help Page"
        [help-editor (:submodule/help-key @submodule)]]

       [:hr]
       [accordion
        "Settings"
        [settings @submodule]]]]]))
