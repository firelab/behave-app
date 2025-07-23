(ns behave-cms.units.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.events]
            [behave-cms.subs]
            [reagent.core :as r]))

(defn units-page [_]
  (r/with-let [selected-dimension-atom (r/atom nil)
               selected-unit-atom (r/atom nil)]
    (let [dimensions @(rf/subscribe [:pull-with-attr :dimension/name])]
      [:div.container
       [:div {:style {:height "500px"}}
        [table-entity-form
         {:title              "Dimensions"
          :entity             :dimension
          :entities           (sort-by :dimension/name dimensions)
          :on-select          #(reset! selected-dimension-atom @(rf/subscribe [:touch-entity (:db/id %)]))
          :table-header-attrs [:dimension/name]
          :entity-form-fields [{:label     "Name"
                                :required? true :field-key :dimension/name}
                               {:label     "Enum"
                                :type      :select
                                :required? true
                                :options   @(rf/subscribe [:units/enum-options])
                                :field-key :dimension/cpp-enum-uuid}]}]]
       (when @selected-dimension-atom
         [:div {:style {:height "500px"}}
          (let [units        (:dimension/units @selected-dimension-atom)
                enum-members @(rf/subscribe [:units/enum-member-options (:db/id @selected-dimension-atom)])]
            [table-entity-form
             {:title              "Units"
              :entity             :unit
              :entities           (sort-by :unit/name units)
              :on-select          #(reset! selected-unit-atom @(rf/subscribe [:touch-entity (:db/id %)]))
              :table-header-attrs [:unit/name]
              :entity-form-fields [{:label     "Name"
                                    :required? true
                                    :field-key :unit/name}
                                   {:label     "Short Code"
                                    :required? true
                                    :field-key :unit/short-code}
                                   {:label     "Enum Member"
                                    :type      :select
                                    :field-key :unit/cpp-enum-member-uuid
                                    :options   enum-members}
                                   {:label     "Default"
                                    :type      :radio
                                    :field-key :unit/system
                                    :options   [{:label "Metric" :value :metric}
                                                {:label "English" :value :english}
                                                {:label "Time" :value :time}]}]}])])])))
