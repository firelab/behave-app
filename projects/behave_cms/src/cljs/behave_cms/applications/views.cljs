(ns behave-cms.applications.views
  (:require [re-frame.core :as rf]
            [behave-cms.components.common      :refer [simple-table window]]
            [behave-cms.components.sidebar     :refer [sidebar sidebar-width]]
            [behave-cms.components.entity-form :refer [entity-form]]
            [behave-cms.applications.subs]))

(def columns [:application/name :application/version])

(defn add-version! [application]
  (assoc application
         :application/version
         (str
           (:application/version-major application)
           "."
           (:application/version-minor application)
           "."
           (:application/version-patch application))))

(defn applications-table []
  (let [applications (rf/subscribe [:applications])
        on-select    #(rf/dispatch [:state/set-state :application (:db/id %)])
        on-delete    #(when (js/confirm (str "Are you sure you want to delete the application " (:application/name %) "?"))
                        (rf/dispatch [:api/delete-entity %]))]

    [simple-table
     columns
     (->> @applications
          (map add-version!)
          (sort-by :application/name))
     {:on-select on-select
      :on-delete on-delete}]))

(defn application-form [id]
  [entity-form {:entity :applications
                :id     id
                :fields [{:label     "Name"
                          :required? true
                          :field-key :application/name}
                         {:label     "Major Version"
                          :field-key :application/version-major
                          :type      :number
                          :required? true}
                         {:label     "Minor Version"
                          :field-key :application/version-minor
                          :type      :number
                          :required? true}
                         {:label     "Patch Version"
                          :field-key :application/version-patch
                          :type      :number
                          :required? true}]}])

(defn list-applications-page [{:keys [id]}]
  (let [loading? (rf/subscribe [:state :loading?])
        application (rf/subscribe [:state :application])]
    (when (and id (nil? @application))
      (rf/dispatch [:state/set-state :application id]))
    (if @loading?
      [:div "Loading..."]
      [:<>
       [sidebar "Applications" @(rf/subscribe [:sidebar/applications])]
       [window sidebar-width
        [:div.container
         [:div.row
          [:div.col-6
           [:h3 "Applications"]
           [applications-table]]
          [:div.col-6
           [:h3 "Manage"]
           [application-form @application]]]]]])))
