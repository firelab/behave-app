(ns behave-cms.languages.views
  (:require [re-frame.core :as rf]
            [behave-cms.components.common :refer [simple-table]]
            [behave-cms.components.entity-form :refer [entity-form]]))

(def columns [:language/name :language/shortcode])

(defn languages-table [languages]
  (let [on-select #(rf/dispatch [:state/set-state :language (:db/id %)])
        on-delete #(when (js/confirm (str "Are you sure you want to delete the language " (:language %) "?"))
                     (rf/dispatch [:api/delete-entity %]))]
    [simple-table
     columns
     (sort-by :language/shortcode languages)
     on-select
     on-delete]))

(defn language-form [id]
  [entity-form {:entity :languages
                :id     id
                :fields [{:label     "Language"
                          :required? true
                          :field-key :language/name}
                         {:label     "Shortcode"
                          :field-key :language/shortcode
                          :required? true}]}])

(defn list-languages-page [{:keys [id]}]
  (let [loaded? (rf/subscribe [:state :loaded?])]
    (if @loaded?
      (let [languages (rf/subscribe [:languages])
            language  (rf/subscribe [:state :language])]
        (when (nil? @language)
          (rf/dispatch [:state/set-state :language id]))
        [:div.container
         [:div.row
          [:div.col
           [:h3 "Languages"]
           [languages-table @languages]]
          [:div.col
           [:h3 "Manage"]
           [language-form @language]]]])
      [:div "Loading..."])))
