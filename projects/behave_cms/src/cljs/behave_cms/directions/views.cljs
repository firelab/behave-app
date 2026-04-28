(ns behave-cms.directions.views
  (:require [behave-cms.components.table-entity-form :refer [table-entity-form table-entity-form-on-select]]
            [behave-cms.directions.subs]
            [behave-cms.events]
            [re-frame.core                           :as rf]))

(defn list-directions-page [_]
  (let [selected-state-path [:selected :direction]
        editor-state-path   [:editors :direction]
        entities            (rf/subscribe [:directions])]
    [:div.container
     [:div {:style {:height "500px"}}
      [table-entity-form
       {:title              "Directions"
        :form-state-path    editor-state-path
        :entity             :direction
        :entities           @entities
        :on-select          (table-entity-form-on-select selected-state-path)
        :table-header-attrs [:direction/id :direction/color]
        :translation-config {:key-fn  :direction/translation-key
                             :text-fn #(name (:direction/id %))}
        :entity-form-fields [{:label     "Direction ID"
                              :required? true
                              :type      :radio
                              :field-key :direction/id
                              :options   [{:label "Heading" :value :heading}
                                          {:label "Flanking" :value :flanking}
                                          {:label "Backing" :value :backing}]}
                             {:label     "Color (hex)"
                              :required? true
                              :field-key :direction/color}
                             {:label     "Order"
                              :field-key :direction/order}]
        :translation-attrs  [{:label "Direction Label" :attr :direction/translation-key}]}]]]))
