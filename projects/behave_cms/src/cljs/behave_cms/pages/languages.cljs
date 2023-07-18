(ns behave-cms.pages.languages
  (:require [re-frame.core :as rf]
            [behave-cms.components.common :refer [simple-table]]
            [behave-cms.components.entity-form :refer [entity-form]]))

(def columns [:shortcode :language ])

(defn languages-table []
  (let [languages (rf/subscribe [:entities :languages])
        on-select #(rf/dispatch [:state/set-state :language (:uuid %)])
        on-delete #(when (js/confirm (str "Are you sure you want to delete the language " (:language %) "?"))
                     (rf/dispatch [:api/delete-entity :languages %]))]
    [simple-table
     columns
     (->> @languages (vals) (sort-by :shortcode))
     on-select
     on-delete]))

(defn language-form [uuid]
  [entity-form {:entity :languages
                :uuid   uuid
                :fields [{:label     "Language"
                          :required? true
                          :field-key :language}
                         {:label     "Shortcode"
                          :field-key :shortcode
                          :required? true}]}])

(defn root-component [{:keys [uuid]}]
  (let [language (rf/subscribe [:state :language])]
    (when (nil? @language)
      (rf/dispatch [:state/set-state :language uuid]))
    (rf/dispatch [:api/entities :languages])
    [:div.container
     [:div.row
      [:div.col
       [:h3 "Languages"]
       [languages-table]]
      [:div.col
       [:h3 "Manage"]
       [language-form @language]]]]))
