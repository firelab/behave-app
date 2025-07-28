(ns behave-cms.units.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.table-entity-form :refer [table-entity-form on-select]]
            [behave-cms.events]
            [behave-cms.subs]))

(defn- units-table [selected-state-path editor-state-path selected-dimension-path]
  (let [selected-dimension (rf/subscribe [:state selected-dimension-path])
        units              (:dimension/units @selected-dimension)
        enum-members       @(rf/subscribe [:units/enum-member-options (:db/id @selected-dimension)])]
    [table-entity-form
     {:title              "Units"
      :form-state-path    editor-state-path
      :entity             :unit
      :entities           (sort-by :unit/name units)
      :on-select          (on-select selected-state-path)
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
                                        {:label "Time" :value :time}]}]}]))

(defn- dimensions-table [selected-state-path editor-state-path & other-state-paths-to-clear]
  (let [entities (rf/subscribe [:pull-with-attr :dimension/name])]
    [table-entity-form
     {:title              "Dimensions"
      :form-state-path    editor-state-path
      :entity             :dimension
      :entities           (sort-by :dimension/name @entities)
      :on-select          (on-select selected-state-path other-state-paths-to-clear)
      :table-header-attrs [:dimension/name]
      :entity-form-fields [{:label     "Name"
                            :required? true :field-key :dimension/name}
                           {:label     "Enum"
                            :type      :select
                            :required? true
                            :options   @(rf/subscribe [:units/enum-options])
                            :field-key :dimension/cpp-enum-uuid}]}]))

(defn units-page
  "Page to manage Dimensions and Units"
  [_]
  (if @(rf/subscribe [:state :loaded?])
    (let [selected-dimension-state-path [:selected :dimension]
          dimension-editor-state-path   [:editors  :dimension]
          selected-unit-state-path      [:selected :unit]
          unit-editor-path              [:editors  :unit]
          selected-dimension            (rf/subscribe [:state selected-dimension-state-path])]
      [:div.container
       [:div {:style {:height "500px"}}
        [dimensions-table selected-dimension-state-path dimension-editor-state-path]]
       (when @selected-dimension
         [:div {:style {:height "500px"}}
          [units-table selected-unit-state-path unit-editor-path selected-dimension-state-path]])])
    [:div "Loading..."]))
