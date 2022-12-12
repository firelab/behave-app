(ns behave-cms.pages.variables
  (:require [re-frame.core :as rf]
            [behave-cms.components.common :refer [simple-table]]
            [behave-cms.components.entity-form :refer [entity-form]]))

(def columns [:variable_name])

(defn variables-table []
  (let [variables (rf/subscribe [:entities :variables])
        on-select #(rf/dispatch [:state/set-state :variable (:uuid %)])
        on-delete #(when (js/confirm (str "Are you sure you want to delete the variable " (:variable_name %) "?"))
                     (rf/dispatch [:api/delete-entity :variables %]))]
    [simple-table
     columns
     (->> @variables (vals) (sort-by :name))
     on-select
     on-delete]))

(defn variable-form [uuid]
  [entity-form {:entity :variables
                :uuid   uuid
                :fields [{:label     "Name"
                          :required? true
                          :field-key :variable_name}]}])

(defn root-component [{:keys [uuid]}]
  (let [variable (rf/subscribe [:state :variable])]
    (when (nil? @variable)
      (rf/dispatch [:state/set-state :variable uuid]))
    (rf/dispatch [:api/entities :variables])
    [:div.container
     [:div.row
      [:div.col
       [:h3 "Variables"]
       [variables-table]]
      [:div.col
       [:h3 "Manage"]
       [variable-form @variable]]]]))
