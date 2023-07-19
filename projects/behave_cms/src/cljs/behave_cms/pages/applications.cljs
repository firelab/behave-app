(ns behave-cms.pages.applications
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [behave-cms.components.common :refer [simple-table]]
            [behave-cms.components.sidebar :refer [sidebar sidebar-width]]
            [behave-cms.components.entity-form :refer [entity-form]]))

(def columns [:application_name :version])

(defn applications-table []
  (let [applications (rf/subscribe [:entities :applications])
        on-select #(rf/dispatch [:state/set-state :application (:uuid %)])
        on-delete #(when (js/confirm (str "Are you sure you want to delete the application " (:application_name %) "?"))
                     (rf/dispatch [:api/delete-entity :applications %]))]
    [simple-table
     columns
     (->> @applications
          (vals)
          (sort-by :application_name)
          (map #(assoc % :version (str "v"
                                       (:version_major %)
                                       "."
                                       (:version_minor %)
                                       "."
                                       (:version_patch %)))))
     on-select
     on-delete]))

(defn application-form [uuid]
  [entity-form {:entity :applications
                :uuid   uuid
                :fields [{:label     "Name"
                          :required? true
                          :field-key :application_name}
                         {:label     "Major Version"
                          :field-key :version_major
                          :required? true}
                         {:label     "Minor Version"
                          :field-key :version_minor
                          :required? true}
                         {:label     "Patch Version"
                          :field-key :version_patch
                          :required? true}]}])

(defn module-form [application-uuid & [uuid]]
  [entity-form {:entity        :modules
                :parent-entity :applications
                :parent-uuid   application-uuid
                :uuid          uuid
                :fields        [{:label     "Name"
                                 :required? true
                                 :field-key :module-name}]}])

(defn application-view [{:keys [uuid]}]
  (rf/dispatch [:state/set-state [:sidebar :application] uuid])
  (rf/dispatch [:api/entity :applications {:uuid uuid}])
  (rf/dispatch [:api/entities :modules {:application-uuid uuid}])
  (r/with-let [application (rf/subscribe [:entity :applications uuid])
               module      (rf/subscribe [:state :module])
               modules     (rf/subscribe [:entities :modules])]
    [:<>
     [sidebar]
     [:div.window {:style {:position "fixed" :top "50px" :left sidebar-width :right "0px" :bottom "0px"}}
      [:div.container
       [:div.row
        [:div.col-6
         [:h3 (:application_name @application) " Modules"]
         [simple-table
          [:module_name]
          (->> @modules (vals) (sort-by :module_name))
          #(rf/dispatch [:state/set-state :module (:uuid %)])]]
        [:div.col-6
         [:h3 "Manage"
          [module-form uuid @module]]]]]]]))

(defn root-component [{:keys [uuid]}]
  (let [application (rf/subscribe [:state :application])]
    (when (nil? @application)
      (rf/dispatch [:state/set-state :application uuid]))
    (rf/dispatch [:api/entities :applications])
    [:<>
     [sidebar]
     [:div.window {:style {:position "fixed" :top "50px" :left sidebar-width :right "0px" :bottom "0px"}}
      [:div.container
       [:div.row
        [:div.col-6
         [:h3 "Applications"]
         [applications-table]]
        [:div.col-6
         [:h3 "Manage"]
         [application-form @application]]]]]]))
