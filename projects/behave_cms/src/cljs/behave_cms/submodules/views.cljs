(ns behave-cms.submodules.views
  (:require [behave-cms.components.common       :refer [accordion btn-sm simple-table window]]
            [behave-cms.components.entity-form  :refer [entity-form]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width]]
            [behave-cms.help.views              :refer [help-editor]]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.components.translations :refer [all-translations]]
            [behave-cms.components.search-table :refer [search-tables]]
            [behave-cms.submodules.subs]
            [re-frame.core                      :as rf]
            [reagent.core                       :as r]))

(defn submodules-page
  "Display submodules page. Takes a map with:
   - id [int]: Submodule entity ID."
  [{:keys [nid]}]
  (r/with-let [show-add-pivot-table? (r/atom false)]
    (let [module      (rf/subscribe [:entity [:bp/nid nid]
                                     '[* {:application/_modules [:db/id :application/name :bp/nid]}]])
          module-id   (:db/id @module)
          application (get-in @module [:application/_modules 0])
          submodules  (rf/subscribe [:sidebar/submodules module-id])]
      [:<>
       [sidebar
        "Submodules"
        @submodules
        (str (:application/name application) " Modules")
        (str "/applications/" (:bp/nid application))]
       [window sidebar-width
        [:div.container
         [:div.row.mb-3.mt-4
          [:h2 (:module/name @module)]]
         [accordion
          "Submodules"
          [table-entity-form {:entity             :submodule
                              :entities           @(rf/subscribe [:submodules module-id])
                              :table-header-attrs [:submodule/name :submodule/io]
                              :entity-form-fields [{:label     "Name"
                                                    :required? true
                                                    :field-key :submodule/name}
                                                   {:label     "I/O"
                                                    :field-key :submodule/io
                                                    :type      :radio
                                                    :options   [{:label "Input" :value :input}
                                                                {:label "Output" :value :output}]}]
                              :parent-field       :module/_submodules
                              :parent-id          module-id
                              :order-attr         :submodule/order
                              }]]
         [:hr]
         [accordion
          "Translations"
          [:div.col-12
           [all-translations (:module/translation-key @module)]]]
         [:hr]
         [accordion
          "Help Page"
          [:div.col-12
           [help-editor (:module/help-key @module)]]]
         [:hr]
         [accordion
          "Pivot Tables"
          [:<>
           [:hr]
           [:div.col-12
            (doall
             (map
              (fn [pivot-table]
                (let [pivot-table-id (:db/id pivot-table)]
                  [:<>
                   [accordion
                    (:pivot-table/title pivot-table)
                    (let [pivot-table-column-state-path  [:selected pivot-table-id :pivot-table-field]
                          pivot-table-column-editor-path [:editors pivot-table-id :pivot-table-field]
                          pivot-table-columns            (rf/subscribe [:pivot-table/columns pivot-table-id])
                          pivot-table-column             (rf/subscribe [:state pivot-table-column-state-path])]
                      [table-entity-form
                       {:title              "Pivot Table Columns"
                        :form-state-path    pivot-table-column-editor-path
                        :entity             :pivot-table/columns
                        :entities           (sort-by :pivot-column/order @pivot-table-columns)
                        :parent-id          pivot-table-id
                        :parent-field       :pivot-table/_columns
                        :on-select          #(if (= (:db/id %) (:db/id @pivot-table-column))
                                               (do (rf/dispatch [:state/set-state pivot-table-column-state-path nil])
                                                   (rf/dispatch [:state/set-state pivot-table-column-state-path nil]))
                                               (rf/dispatch [:state/set-state pivot-table-column-state-path
                                                             @(rf/subscribe [:re-entity (:db/id %)])]))
                        :order-attr         :pivot-column/order
                        :table-header-attrs [:variable/name :pivot-column/type :pivot-column/function]
                        :entity-form-fields (cond-> [{:label     "Column Type"
                                                      :type      :radio
                                                      :required? true
                                                      :field-key :pivot-column/type
                                                      :options   [{:label "Field" :value :field}
                                                                  {:label "Value" :value :value}]}
                                                     {:label          "Group Variable"
                                                      :field-key      :pivot-column/group-variable-uuid
                                                      :field-key-type :db.type/string
                                                      :app-id         (:db/id application)
                                                      :required?      true
                                                      :type           :group-variable}]
                                              (or (= (:pivot-column/type @pivot-table-column) :value)
                                                  (= (:pivot-column/type @(rf/subscribe [:state pivot-table-column-editor-path])) :value))
                                              (conj {:label     "Function"
                                                     :type      :keyword-select
                                                     :field-key :pivot-column/function
                                                     :required  true
                                                     :options   [{:value "sum" :label "sum"}
                                                                 {:value "min" :label "min"}
                                                                 {:value "max" :label "max"}
                                                                 {:value "count" :label "count"}]}))}])]
                   [:hr]]))
              (:module/pivot-tables @module)))
            (if @show-add-pivot-table?
              [entity-form {:entity       :pivot-table
                            :parent-field :module/_pivot-tables
                            :parent-id    (:db/id @module)
                            :fields       [{:label     "Title"
                                            :required? true
                                            :field-key :pivot-table/title}]
                            :on-create    #(do (swap! show-add-pivot-table? not) %)}]
              [btn-sm
               :primary
               "Add Pivot Table"
               #(swap! show-add-pivot-table? not)])]]]
         [:hr]
         [accordion
          "Search Tables"
          [:hr]
          [search-tables module]]]]])))
