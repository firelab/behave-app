(ns behave-cms.units.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.common      :refer [simple-table]]
            [behave-cms.components.entity-form :refer [entity-form]]
            [behave-cms.events]
            [behave-cms.subs]))

(defn- unit-editor [dimension-eid unit-eid]
  (let [enum-members (rf/subscribe [:units/enum-member-options dimension-eid])]
    [:<>
     [:h3 (if unit-eid "Edit Unit" "Add Unit")]
     [entity-form {:entity       :units
                   :parent-field :dimension/_units
                   :parent-id    dimension-eid
                   :id           unit-eid
                   :fields       [{:label     "Name"
                                   :required? true
                                   :field-key :unit/name}
                                  {:label     "Short Code"
                                   :required? true
                                   :field-key :unit/short-code}
                                  {:label     "Enum Member"
                                   :required? true
                                   :type      :select
                                   :field-key :unit/cpp-enum-member-uuid
                                   :options   @enum-members}
                                  {:label     "Default"
                                   :type      :radio
                                   :field-key :unit/system
                                   :options   [{:label "Metric"  :value :metric}
                                               {:label "English" :value :english}
                                               {:label "Time"    :value :time}]}]
                   :on-create #(assoc % :unit/system (keyword (:unit/system %)))}]]))

(defn- units-table [dimension-eid]
  (when dimension-eid 
    (let [units     (rf/subscribe [:pull-children :dimension/units dimension-eid])
          on-select #(rf/dispatch [:state/select :units (:db/id %)])
          on-delete
          #(when (js/confirm (str "Are you sure you want to delete the unit " (:unit/name %) "?"))
             (rf/dispatch [:api/delete-entity %]))
          ]
      [:div
       {:style {:height "400px" :overflow-y "scroll"}}
       [simple-table
        [:unit/name]
        (sort-by :unit/name @units)
        {:on-select on-select
         :on-delete on-delete}]])))

;;; Dimension

(defn- dimension-editor [dimension-eid unit-eid]
  (let [enums (rf/subscribe [:units/enum-options])]
    [:<>
     [:div.row
      [:h3 (str (if dimension-eid "Edit" "Add") " Dimension")]]
     [:div.row
      [:div.col-6
       [entity-form {:entity    :dimensions
                     :id        dimension-eid
                     :disabled? (boolean unit-eid)
                     :fields    [
                                 {:label     "Name"
                                  :required? true
                                  :field-key :dimension/name}
                                 {:label     "Enum"
                                  :type      :select
                                  :required? true
                                  :options   @enums
                                  :field-key :dimension/cpp-enum-uuid}]}]]]]))

(defn- dimensions-table []
  (let [dimensions (rf/subscribe [:pull-with-attr :dimension/name])
        on-select  #(rf/dispatch [:state/select :dimensions (:db/id %)])
        on-delete  #(when (js/confirm (str "Are you sure you want to delete the dimension " (:dimension/name %) "?"))
                      (rf/dispatch [:api/delete-entity %]))]
    [:div
     {:style {:height "400px" :overflow-y "scroll"}}
     [simple-table
      [:dimension/name]
      (sort-by :dimension/name @dimensions)
      {:on-select on-select
       :on-delete on-delete}]]))

(defn units-page [_]
  (let [*dimension (rf/subscribe [:selected :dimensions])
        *unit      (rf/subscribe [:selected :units])]
    [:div.container
     [:div.row
      [:h3 "Dimensions / Units"]
      [:div.row
       [:div.col-6
        [dimensions-table]]
       [:div.col-6
        [units-table @*dimension]]]]
     [:div.row
      [:div.col-6
       [dimension-editor @*dimension @*unit]]
      [:div.col-6
       (when @*dimension
         [unit-editor @*dimension @*unit])]]]))
