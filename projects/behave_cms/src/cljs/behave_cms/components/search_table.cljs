(ns behave-cms.components.search-table
  (:require [behave-cms.components.conditionals      :refer [conditionals-graph manage-conditionals]]
            [behave-cms.components.common            :refer [accordion btn-sm]]
            [behave-cms.components.entity-form       :refer [entity-form]]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.submodules.subs]
            [reagent.core                            :as r]
            [re-frame.core                           :as rf]))

(defn search-tables
  "Component for displaying all existing search tables and a form for entering new ones"
  [module]
  (r/with-let [show-add-search-table? (r/atom false)]
    (let [module-id (:db/id @module)]
      [:div
       (doall
        (map
         (fn [search-table]
           (let [search-table-id (:db/id search-table)]
             [:div
              [accordion
               (:search-table/name search-table)
               [:div.row.col-12
                [entity-form {:entity       :search-table
                              :id           search-table-id
                              :parent-field :module/_search-tables
                              :parent-id    module-id
                              :fields       [{:label     "Title"
                                              :required? true
                                              :field-key :search-table/name}
                                             {:label     "Group Variable"
                                              :app-id    @(rf/subscribe [:module/_app-module-id module-id])
                                              :required? true
                                              :field-key :search-table/group-variable
                                              :type      :group-variable}
                                             {:label     "Operator"
                                              :required? true
                                              :field-key :search-table/operator
                                              :type      :radio
                                              :options   [{:label "Minimum" :value :min}
                                                          {:label "Maximum" :value :max}]}]
                              :on-create    #(do (swap! show-add-search-table? not) %)}]
                [table-entity-form {:title              "Search Table Filters"
                                    :entity             :search-table-filter
                                    :entities           @(rf/subscribe [:search-table/filters search-table-id])
                                    :table-header-attrs [:variable/name :search-table-filter/operator :search-table-filter/value]
                                    :entity-form-fields [{:label     "Group Variable"
                                                          :field-key :search-table-filter/group-variable
                                                          :app-id    @(rf/subscribe [:module/_app-module-id module-id])
                                                          :required? true
                                                          :type      :group-variable}
                                                         {:label     "Operator"
                                                          :field-key :search-table-filter/operator
                                                          :required? true
                                                          :type      :radio
                                                          :options   [{:value :equal :label "="}
                                                                      {:value :not-equal :label "!="}]}
                                                         {:label                    "Value"
                                                          :field-key                :search-table-filter/value
                                                          :required?                true
                                                          :group-variable-field-key :search-table-filter/group-variable
                                                          :type                     :group-variable-value}]
                                    :parent-id          search-table-id
                                    :parent-field       :search-table/_filters}]
                [table-entity-form {:title              "Search Table Columns"
                                    :entity             :search-table-column
                                    :entities           @(rf/subscribe [:search-table/columns search-table-id])
                                    :table-header-attrs [:search-table-column/name :search-table-column/translation-key]
                                    :entity-form-fields [{:label     "Name"
                                                          :required? true
                                                          :field-key :search-table-column/name
                                                          :type      :default}
                                                         {:label     "Group Variable"
                                                          :app-id    @(rf/subscribe [:module/_app-module-id module-id])
                                                          :required? true
                                                          :field-key :search-table-column/group-variable
                                                          :type      :group-variable}
                                                         {:label     "Translation Key (Optional: Auto Generated)"
                                                          :app-id    @(rf/subscribe [:module/_app-module-id module-id])
                                                          :required? false
                                                          :field-key :search-table-column/translation-key
                                                          :type      :translation-key}]
                                    :parent-id          search-table-id
                                    :parent-field       :search-table/_columns
                                    :order-attr         :search-table-column/order}]
                [:div
                 [:div {:style {:color "#6c757d"}}
                  "Show Table Conditionals"]
                 [:hr]
                 [:div.row
                  [:div.col-9
                   [conditionals-graph
                    search-table-id
                    search-table-id
                    @(rf/subscribe [:group-variable/conditionals search-table-id :search-table/show-conditionals])
                    :search-table/show-conditionals
                    :search-table/show-conditionals-operator]]
                  [:div.col-3
                   [manage-conditionals search-table-id :search-table/show-conditionals]]]]
                [:div.row
                 {:style {:padding "5px"}}
                 [btn-sm
                  :outline-danger
                  "Delete Search Table"
                  #(when (js/confirm (str "Are you sure you want to delete this search table?"))
                     (rf/dispatch [:api/delete-entity search-table-id]))]]]]
              [:hr]]))
         (:module/search-tables @module)))
       (if @show-add-search-table?
         [entity-form {:entity       :search-table
                       :parent-field :module/_search-tables
                       :parent-id    (:db/id @module)
                       :fields       [{:label     "Title"
                                       :required? true
                                       :field-key :search-table/name}
                                      {:label     "Group Variable"
                                       :app-id    @(rf/subscribe [:module/_app-module-id module-id])
                                       :required? true
                                       :field-key :search-table/group-variable
                                       :type      :group-variable}
                                      {:label     "Operator"
                                       :required? true
                                       :field-key :search-table/operator
                                       :type      :radio
                                       :options   [{:label "Minimum" :value :min}
                                                   {:label "Maximum" :value :max}]}]
                       :on-create    #(do (swap! show-add-search-table? not) %)}]
         [btn-sm
          :primary
          "Add Search Table"
          #(swap! show-add-search-table? not)])])))
