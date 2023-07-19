(ns behave-cms.users.views
  (:require [re-frame.core :as rf]
            [behave-cms.components.common :refer [simple-table]]
            [behave-cms.components.entity-form :refer [entity-form]]))

(def columns [:email :name])

(defn users-table []
  (let [users (rf/subscribe [:entities :users])
        on-select #(rf/dispatch [:state/set-state :user (:uuid %)])
        on-delete #(when (js/confirm (str "Are you sure you want to delete the user " (:user %) "?"))
                     (rf/dispatch [:api/delete-entity :users %]))]
    [simple-table
     columns
     (->> @users (vals) (sort-by :name))
     on-select
     on-delete]))

(defn user-form [uuid]
  [entity-form {:entity :users
                :uuid   uuid
                :fields [{:label     "Email"
                          :required? true
                          :field-key :user}
                         {:label     "Name"
                          :field-key :shortcode
                          :required? true}
                         {:label     "Password"
                          :field-key :shortcode
                          :type      "password"
                          :required? true}]}])

(defn list-users-page [{:keys [uuid]}]
  (let [user (rf/subscribe [:state :user])]
    (when (nil? @user)
      (rf/dispatch [:state/set-state :user uuid]))
    (rf/dispatch [:api/entities :users])
    [:div.container
     [:div.row
      [:div.col
       [:h3 "Users"]
       [users-table]]
      [:div.col
       [:h3 "Manage"]
       [user-form @user]]]]))
