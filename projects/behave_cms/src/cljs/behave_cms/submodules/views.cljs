(ns behave-cms.submodules.views
  (:require [behave-cms.components.common       :refer [accordion btn-sm simple-table window]]
            [behave-cms.components.entity-form  :refer [entity-form]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width]]
            [behave-cms.help.views              :refer [help-editor]]
            [behave-cms.components.pivot-tables :refer [manage-pivot-table-column]]
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
                (let [pivot-table-id       (:db/id pivot-table)
                      pivot-table-fields   @(rf/subscribe [:pivot-table/fields pivot-table-id])
                      pivot-table-values   @(rf/subscribe [:pivot-table/values pivot-table-id])
                      pivot-column-id-atom (r/atom nil)]
                  [:<>
                   [accordion
                    (:pivot-table/title pivot-table)
                    [:div.row.col-6
                     [simple-table
                      [:variable/name]
                      pivot-table-fields
                      {:caption     "Pivot Table Fields"
                       :on-increase #(rf/dispatch [:api/reorder % pivot-table-fields :pivot-column/order :inc])
                       :on-decrease #(rf/dispatch [:api/reorder % pivot-table-fields :pivot-column/order :dec])
                       :on-delete   #(rf/dispatch [:api/delete-entity (:db/id %)])
                       :on-select   #(reset! pivot-column-id-atom (:db/id %))}]
                     [simple-table
                      [:variable/name :pivot-column/function]
                      pivot-table-values
                      {:caption   "Pivot Table Values"
                       :on-delete #(rf/dispatch [:api/delete-entity (:db/id %)])
                       :on-select #(reset! pivot-column-id-atom (:db/id %))}]
                     [btn-sm
                      :outline-danger
                      "Delete Pivot Table"
                      #(when (js/confirm (str "Are you sure you want to delete this pivot table?"))
                         (rf/dispatch [:api/delete-entity pivot-table-id]))]]
                    [:div.row.col-6
                     [manage-pivot-table-column module-id pivot-table-id pivot-column-id-atom]]]
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
