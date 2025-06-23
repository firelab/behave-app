(ns behave-cms.submodules.views
  (:require [behave-cms.components.common       :refer [accordion btn-sm simple-table window]]
            [behave-cms.components.entity-form  :refer [entity-form]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width]]
            [behave-cms.components.translations :refer [all-translations]]
            [behave-cms.help.views              :refer [help-editor]]
            [behave-cms.components.pivot-tables :refer [manage-pivot-table-column]]
            [behave-cms.components.group-variable-selector :refer [group-variable-selector]]
            [behave-cms.submodules.subs]
            [re-frame.core                      :as rf]
            [reagent.core                       :as r]
            [goog.string :as gstring]))

(defn- submodule-form [module-id id num-submodules]
  [entity-form {:entity       :submodule
                :parent-field :module/_submodules
                :parent-id    module-id
                :id           id
                :fields       [{:label     "Name"
                                :required? true
                                :field-key :submodule/name}
                               {:label     "I/O"
                                :field-key :submodule/io
                                :type      :radio
                                :options   [{:label "Input" :value :input}
                                            {:label "Output" :value :output}]}]
                :on-create    #(assoc % :submodule/order num-submodules)}])

(defn- manage-submodule [module-id *submodule num-submodules]
  [:div.col-6
   [:h5 (if (nil? module-id) "Add" "Edit") " Submodule"
    [submodule-form module-id *submodule num-submodules]]])

(defn- submodules-table [label submodules]
  [:<>
   [:h5 label]
   [simple-table
    [:submodule/name]
    (->> submodules (sort-by :submodule/order))
    {:on-select   #(rf/dispatch [:state/set-state :submodule (:db/id %)])
     :on-delete   #(when (js/confirm (str "Are you sure you want to delete the submodule "
                                          (:submodule/name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))
     :on-increase #(rf/dispatch [:api/reorder % submodules :submodule/order :inc])
     :on-decrease #(rf/dispatch [:api/reorder % submodules :submodule/order :dec])}]])

(defn- all-submodule-tables [module-id]
  (let [submodules (rf/subscribe [:submodules module-id])
        inputs     (filter #(= :input (:submodule/io %)) @submodules)
        outputs    (filter #(= :output (:submodule/io %)) @submodules)]
    [:div.col-6
     [:row
      [submodules-table "Output Submodules" outputs]
      [submodules-table "Input Submodules" inputs]]]))

(defn submodules-page
  "Display submodules page. Takes a map with:
   - id [int]: Submodule entity ID."
  [{:keys [nid]}]
  (r/with-let [show-add-pivot-table? (r/atom false)
               show-add-search-table? (r/atom false)]
    (let [module      (rf/subscribe [:entity [:bp/nid nid]
                                     '[* {:application/_modules [:db/id :application/name :bp/nid]}]])
          module-id   (:db/id @module)
          application (get-in @module [:application/_modules 0])
          submodules  (rf/subscribe [:sidebar/submodules module-id])
          submodule   (rf/subscribe [:state :submodule])]
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
          [all-submodule-tables module-id]
          [manage-submodule module-id @submodule (count @submodules)]]
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
                      {:caption   "Pivot Table Fields"
                       :on-delete #(rf/dispatch [:api/delete-entity (:db/id %)])
                       :on-select #(reset! pivot-column-id-atom (:db/id %))}]
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
          (doall
           (map
            (fn [search-table]
              (r/with-let [search-table-column-id-atom (r/atom nil)
                           search-table-column-gv-id-atom (r/atom nil)]
                (let [search-table-id                (:db/id search-table)
                      search-table-columns           @(rf/subscribe [:search-table/columns search-table-id])]
                 [:<>
                  [accordion
                   (:search-table/title search-table)
                   [:div.row.col-12
                    [entity-form {:entity       :search-table
                                  :id           search-table-id
                                  :parent-field :module/_search-tables
                                  :parent-id    (:db/id @module)
                                  :fields       [{:label     "Title"
                                                  :required? true
                                                  :field-key :search-table/title}
                                                 {:label     "Group Variable"
                                                  :app-id    @(rf/subscribe [:module/_app-module-id module-id])
                                                  :required? true
                                                  :field-key :search-table/group-variable
                                                  :type      :group-variable}
                                                 {:label     "Op"
                                                  :required? true
                                                  :field-key :search-table/op
                                                  :type      :radio
                                                  :options   [{:label "Minimum" :value :min}
                                                              {:label "Maximum" :value :max}]}]
                                  :on-create    #(do (swap! show-add-search-table? not) %)}]
                    [:div.row.col-6
                     [simple-table
                      [:variable/name]
                      search-table-columns
                      {:caption     "Search Table Columns"
                       :on-delete   #(rf/dispatch [:api/delete-entity (:db/id %)])
                       :on-select   #(do
                                       (reset! search-table-column-id-atom (:db/id %))
                                       (reset! search-table-column-gv-id-atom
                                               (:db/id (:search-table-column/group-variable %))))
                       :on-increase #(rf/dispatch [:api/reorder % search-table-columns :search-table-column/order :inc])
                       :on-decrease #(rf/dispatch [:api/reorder % search-table-columns :search-table-column/order :dec])}]
                     [:div.col-6
                      [btn-sm
                       :outline-danger
                       "Delete Search Table"
                       #(when (js/confirm (str "Are you sure you want to delete this search table?"))
                          (rf/dispatch [:api/delete-entity search-table-id]))]]]
                    [:div.row.col-6
                     [group-variable-selector
                      {:app-id     @(rf/subscribe [:module/_app-module-id module-id])
                       :state-path [:state :editors :search-table search-table-id :search-table/group-variable]
                       :gv-id      @search-table-column-gv-id-atom
                       :title      "Search Table Column"
                       :on-submit  #(do
                                      (if @search-table-column-id-atom
                                        ;; update group variable in existing search-table column
                                        (rf/dispatch [:api/upsert-entity
                                                      {:db/id                              @search-table-column-id-atom
                                                       :search-table-column/group-variable %}])
                                        ;; Add a new column to the search-table
                                        (let [next-order (count (:search-table/columns search-table))]
                                          (rf/dispatch [:api/upsert-entity
                                                        {:db/id search-table-id
                                                         :search-table/columns
                                                         [{:search-table-column/group-variable %
                                                           :search-table-column/order          next-order}]}])))
                                      (reset! search-table-column-id-atom nil)
                                      (reset! search-table-column-gv-id-atom nil))}]]]]
                  [:hr]])))
            (:module/search-tables @module)))
          (if @show-add-search-table?
            [entity-form {:entity       :new-search-table
                          :parent-field :module/_search-tables
                          :parent-id    (:db/id @module)
                          :fields       [{:label     "Title"
                                          :required? true
                                          :field-key :search-table/title}
                                         {:label     "Group Variable"
                                          :app-id    @(rf/subscribe [:module/_app-module-id module-id])
                                          :required? true
                                          :field-key :search-table/group-variable
                                          :type      :group-variable}
                                         {:label     "Op"
                                          :required? true
                                          :field-key :search-table/op
                                          :type      :radio
                                          :options   [{:label "Minimum" :value :min}
                                                      {:label "Maximum" :value :max}]}]
                          :on-create    #(do (swap! show-add-search-table? not) %)}]
            [btn-sm
             :primary
             "Add Search Table"
             #(swap! show-add-search-table? not)])]
         ]]])))
