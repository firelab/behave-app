(ns behave-cms.submodules.views
  (:require [re-frame.core                      :as rf]
            [behave-cms.components.common       :refer [accordion btn-sm simple-table window]]
            [behave-cms.components.entity-form  :refer [entity-form]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width]]
            [behave-cms.components.translations :refer [all-translations]]
            [behave-cms.help.views              :refer [help-editor]]
            [behave-cms.components.pivot-tables :refer [manage-pivot-table]]
            [behave-cms.submodules.subs]))

(defn- submodule-form [module-id id num-submodules]
  [entity-form {:entity        :submodule
                :parent-field  :module/_submodules
                :parent-id     module-id
                :id            id
                :fields        [{:label     "Name"
                                 :required? true
                                 :field-key :submodule/name}
                                {:label     "I/O"
                                 :field-key :submodule/io
                                 :type      :radio
                                 :options   [{:label "Input" :value :input}
                                             {:label "Output" :value :output}]}]
                :on-create     #(assoc % :submodule/order num-submodules)}])

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
  (let [module      (rf/subscribe [:entity [:bp/nid nid] '[* {:application/_modules [:db/id :application/name :bp/nid]}]])
        id          (:db/id @module)
        application (get-in @module [:application/_modules 0])
        submodules  (rf/subscribe [:sidebar/submodules id])
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
        [all-submodule-tables id]
        [manage-submodule id @submodule (count @submodules)]]
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
        [:div.col-12
         [entity-form {:entity       :pivot-table
                       :parent-field :module/_pivot-tables
                       :parent-id    (:db/id @module)
                       :fields       [{:label     "Tittle"
                                       :required? true
                                       :field-key :pivot-table/tittle}]}]
         (doall
          (map
           (fn [pivot-table]
             (let [pivot-table-id     (:db/id pivot-table)
                   pivot-table-rows   @(rf/subscribe [:pivot-table/rows pivot-table-id])
                   pivot-table-values @(rf/subscribe [:pivot-table/values pivot-table-id])]
               [accordion
                (:pivot-table/tittle pivot-table)
                [:div.col-6
                 [simple-table
                  [:variable/name]
                  pivot-table-rows
                  {:on-delete #(when (js/confirm (str "Are you sure you want to delete the pivot table row "
                                                      (:variable/name %) "?"))
                                 (rf/dispatch [:api/delete-entity (:db/id %)]))}]
                 [simple-table
                  [:variable/name :pivot-value/function]
                  pivot-table-values
                  {:on-delete #(when (js/confirm (str "Are you sure you want to delete the pivot table value "
                                                      (:variable/name %) "?"))
                                 (rf/dispatch [:api/delete-entity (:db/id %)]))}]
                 [btn-sm
                  :outline-danger
                  "Delete Pivot Table"
                  #(rf/dispatch [:api/delete-entity pivot-table-id])]]
                [:div.col-6
                 [manage-pivot-table id pivot-table-id]]]))
           (:module/pivot-tables @module)))]]]]]))
