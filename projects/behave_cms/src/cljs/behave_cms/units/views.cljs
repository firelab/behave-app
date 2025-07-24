(ns behave-cms.units.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.events]
            [behave-cms.subs]))

(defn units-page
  "Page to manage Dimensions and Units"
  [_]
  (if @(rf/subscribe [:state :loaded?])
    (let [selected-dimension-state-path [:selected :dimension]
          selected-unit-state-path      [:selected :unit]
          dimension-editor-path         [:editors  :dimension]
          unit-editor-path              [:editors  :unit]
          selected-dimension            (rf/subscribe [:state selected-dimension-state-path])
          dimensions                    (rf/subscribe [:pull-with-attr :dimension/name])]
      [:div.container
       [:div {:style {:height "500px"}}
        [table-entity-form
         {:title              "Dimensions"
          :form-state-path    dimension-editor-path
          :entity             :dimension
          :entities           (sort-by :dimension/name @dimensions)
          :on-select          #(if (= (:db/id %) (:db/id @selected-dimension))
                                 (do (rf/dispatch [:state/set-state selected-dimension-state-path nil])
                                     (rf/dispatch [:state/set-state selected-dimension-state-path nil]))
                                 (rf/dispatch [:state/set-state selected-dimension-state-path
                                               @(rf/subscribe [:re-entity (:db/id %)])]))
          :table-header-attrs [:dimension/name]
          :entity-form-fields [{:label     "Name"
                                :required? true :field-key :dimension/name}
                               {:label     "Enum"
                                :type      :select
                                :required? true
                                :options   @(rf/subscribe [:units/enum-options])
                                :field-key :dimension/cpp-enum-uuid}]}]]
       (when @selected-dimension
         [:div {:style {:height "500px"}}
          (let [units        (:dimension/units @selected-dimension)
                enum-members @(rf/subscribe [:units/enum-member-options (:db/id @selected-dimension)])]
            [table-entity-form
             {:title              "Units"
              :form-state-path    unit-editor-path
              :entity             :unit
              :entities           (sort-by :unit/name units)
              :on-select          #(rf/dispatch [:state/set-state selected-unit-state-path
                                                 @(rf/subscribe [:re-entity (:db/id %)])])
              :parent-id          (:db/id @selected-dimension)
              :parent-field       :dimension/_units
              :table-header-attrs [:unit/name :unit/short-code :unit/system]
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
                                                {:label "Time" :value :time}]}]}])])])
    [:div "Loading..."]))
