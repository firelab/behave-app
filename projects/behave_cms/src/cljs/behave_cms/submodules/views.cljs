(ns behave-cms.submodules.views
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [behave-cms.components.common       :refer [accordion simple-table window]]
            [behave-cms.components.entity-form  :refer [entity-form]]
            [behave-cms.components.help-editor  :refer [help-editor]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width]]
            [behave-cms.components.translations :refer [all-translations]]
            [behave-cms.submodules.subs]))

(defn submodule-form [module-id id]
  [entity-form {:entity        :submodules
                :parent-field  :module/submodules
                :parent-id     module-id
                :id            id
                :fields        [{:label     "Name"
                                 :required? true
                                 :field-key :submodule/name}
                                {:label     "I/O"
                                 :field-key :submodule/io
                                 :type      :radio
                                 :options   [{:label "Input" :value "input"}
                                             {:label "Output" :value "output"}]}]}])

(defn manage-submodule [{:keys [id]}]
  (let [submodule (rf/subscribe [:state :submodule])]
    [:div.col-6
     [:h5 (if (nil? id) "Add" "Edit") "Submodule"
      [submodule-form id @submodule]]]))

(defn submodules-table [label submodules]
  [:<>
   [:h5 label]
   [simple-table
    [:submodule/name]
    (->> submodules (sort-by :submodule/order))
    {:on-select   #(rf/dispatch [:state/set-state :submodule (:id %)])
     :on-delete   #(when (js/confirm (str "Are you sure you want to delete the submodule " (:submodule/name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))
     :on-increase #(rf/dispatch [:submodule/reorder % :up submodules])
     :on-decrease #(rf/dispatch [:submodule/reorder % :down submodules])}]])

(defn all-submodule-tables [{:keys [id]}]
  (let [submodules        (rf/subscribe [:submodules id])
        [inputs outputs]  (partition-by #(= :input (:submodule/io %)) @submodules)]
    [:div.col-6
     [:row
      [submodules-table "Output Submodules" outputs]
      [submodules-table "Input Submodules" inputs]]]))

(defn list-submodules-page [{:keys [id]}]
  (r/with-let [module     (rf/subscribe [:entity id])
               submodules (rf/subscribe [:sidebar/submodules id])
               submodule  (rf/subscribe [:state :submodule])]
    [:<>
     [sidebar
      "Submodules"
      @submodules
      "Modules"
      (str "/applications/" (:application/rid @module))]
     [window sidebar-width
      [:div.container
       [:div.row.mb-3.mt-4
        [:h2 (:module/name @module)]]
       [accordion
        "Submodules"
        [all-submodule-tables @module]
        [manage-submodule @module @submodule]]
       [:hr]
       [accordion
        "Translations"
        [:div.col-12
         [all-translations (:module/translation-key @module)]]]
       [:hr]
       [accordion
        "Help Page"
        [:div.col-12
         [help-editor (:module/help-key @module)]]]]]]))
