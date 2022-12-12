(ns behave-cms.pages.groups
  (:require [re-frame.core :as rf]
            [behave-cms.components.common :refer [simple-table]]
            [behave-cms.components.entity-form :refer [entity-form]]))

(def columns [:group_name])

(defn groups-table []
  (let [groups (rf/subscribe [:entities :groups])
        on-select #(rf/dispatch [:state/set-state :group (:uuid %)])
        on-delete #(when (js/confirm (str "Are you sure you want to delete the group " (:group_name %) "?"))
                     (rf/dispatch [:api/delete-entity :groups %]))]
    [simple-table
     columns
     (->> @groups (vals) (sort-by :group_name))
     on-select
     on-delete]))

(defn group-form [submodule-uuid & [uuid]]
  [entity-form {:entity        :groups
                :parent-entity :submodules
                :parent-uuid   submodule-uuid
                :uuid          uuid
                :fields        [{:label     "Name"
                                 :required? true
                                 :field-key :group-name}
                                {:label     "Outputs"
                                 :field-key :input-output
                                 :type      :checkbox
                                 :options   [{:label "Input" :value "input"}
                                             {:label "Output" :value "output"}]}]}])

(defn root-component [submodule-uuid]
  (let [submodule   (rf/subscribe [:state [:submodule]])
        group       (rf/subscribe [:state [:group]])]
    (rf/dispatch [:api/entities :groups {:submodule-uuid submodule-uuid}])
    [:div.container
     [:div.row
      [:div.col
       [:h3 "Users"]
       [groups-table]]
      [:div.col
       [:h3 "Manage"]
       [group-form @group]]]]))
