(ns behave-cms.languages.views
  (:require [re-frame.core :as rf]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]))

(defn list-languages-page
  "Page for managing languages"
  [_]
  (if @(rf/subscribe [:state :loaded?])
    (let [language-editor-state-path [:editors :language]
          languages                  @(rf/subscribe [:languages])]
      [:div.container
       [:div {:style {:height "500px"}}
        [table-entity-form
         {:title              "Languages"
          :form-state-path    language-editor-state-path
          :entity             :language
          :entities           (sort-by :language/name languages)
          :table-header-attrs [:language/name :language/shortcode]
          :entity-form-fields [{:label     "Language"
                                :required? true
                                :field-key :language/name}
                               {:label     "Shortcode"
                                :field-key :language/shortcode
                                :required? true}]}]]])
    [:div "Loading..."]))
