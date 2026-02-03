(ns behave-cms.submodules.views
  (:require [behave-cms.components.common       :refer [accordion btn-sm window]]
            [behave-cms.components.entity-form  :refer [entity-form]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width]]
            [behave-cms.help.views              :refer [help-editor]]
            [behave-cms.components.table-entity-form :refer [table-entity-form on-select]]
            [behave-cms.components.translations :refer [all-translations]]
            [behave-cms.components.search-table :refer [search-tables]]
            [behave-cms.submodules.subs]
            [re-frame.core                      :as rf]
            [reagent.core                       :as r]))

(defn- pivot-table [pivot-table-id application-id]
  (let [selected-state-path [:selected :pivot-table pivot-table-id :pivot-column]
        editor-path         [:editors :pivot-table pivot-table-id :pivot-column]
        selected-entity     (rf/subscribe [:state selected-state-path])
        entities            (rf/subscribe [:pivot-table/columns pivot-table-id])]
    [table-entity-form
     {:title              "Pivot Table Columns"
      :form-state-path    selected-state-path
      :entity             :pivot-table/columns
      :entities           (sort-by :pivot-column/order @entities)
      :parent-id          pivot-table-id
      :parent-field       :pivot-table/_columns
      :on-select          (on-select selected-state-path)
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
                                    :app-id         application-id
                                    :required?      true
                                    :type           :group-variable}]
                            (or (= (:pivot-column/type @selected-entity) :value)
                                (= (:pivot-column/type @(rf/subscribe [:state editor-path])) :value))
                            (conj {:label     "Function"
                                   :type      :keyword-select
                                   :field-key :pivot-column/function
                                   :required  true
                                   :options   [{:value "sum" :label "sum"}
                                               {:value "min" :label "min"}
                                               {:value "max" :label "max"}
                                               {:value "count" :label "count"}]}))}]))

(defn- submodules-table [module-id app-id]
  (let [selected-state-path [:selected :submodule]
        editor-state-path   [:editors :submodule]
        submodule           (rf/subscribe [:submodules module-id])]
    [table-entity-form
     {:entity             :submodule
      :form-state-path    editor-state-path
      :entities           (sort-by :submodule/order @submodule)
      :on-select          (on-select selected-state-path)
      :parent-id          app-id
      :parent-field       :application/_submodules
      :table-header-attrs [:submodule/name :submodule/io]
      :order-attr         :submodule/order
      :entity-form-fields [{:label     "Name"
                            :required? true
                            :field-key :submodule/name}
                           {:label     "I/O"
                            :field-key :submodule/io
                            :type      :radio
                            :options   [{:label "Input" :value :input}
                                        {:label "Output" :value :output}]}]}]))

(defn- submodules-results-order-table [module-id app-id]
  (let [submodule (rf/subscribe [:submodules module-id])]
    [table-entity-form
     {:entity             :submodule
      :entities           (sort-by :submodule/results-order @submodule)
      :modify?            false
      :parent-id          app-id
      :parent-field       :application/_submodules
      :table-header-attrs [:submodule/name :submodule/io]
      :order-attr         :submodule/order
      :entity-form-fields [{:label     "Name"
                            :required? true
                            :field-key :submodule/name}]}]))

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
          [submodules-table (:db/id @module) (:db/id application)]]
         [:hr]
         [accordion
          "Submodules Results Order"
          [submodules-results-order-table (:db/id @module) (:db/id application)]]
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
              (fn [pivot-table-entity]
                [:<>
                 [accordion
                  (:pivot-table/title pivot-table-entity)
                  [pivot-table (:db/id pivot-table-entity) (:db/id application)]]
                 [:hr]])
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
