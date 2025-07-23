(ns behave-cms.languages.views
  (:require [re-frame.core :as rf]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [reagent.core :as r]))

(defn list-languages-page
  "Page for managing languages"
  [_]
  (r/with-let [selected-language-atom (r/atom nil)]
    (let [loaded? (rf/subscribe [:state :loaded?])]
      (if @loaded?
        (let [languages @(rf/subscribe [:languages])]
          [:div.container
           [:div {:style {:height "500px"}}
            [table-entity-form
             {:title              "Languages"
              :entity             :language
              :entities           (sort-by :language/name languages)
              :on-select          #(reset! selected-language-atom @(rf/subscribe [:touch-entity (:db/id %)]))
              :table-header-attrs [:language/name :language/shortcode]
              :entity-form-fields [{:label     "Language"
                                    :required? true
                                    :field-key :language/name}
                                   {:label     "Shortcode"
                                    :field-key :language/shortcode
                                    :required? true}]}]]])
        [:div "Loading..."]))))
