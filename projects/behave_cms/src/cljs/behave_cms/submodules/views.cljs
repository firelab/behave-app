(ns behave-cms.submodules.views
  (:require [reagent.core                       :as r]
            [re-frame.core                      :as rf]
            [data-utils.core                    :refer [parse-int]]
            [behave-cms.components.common       :refer [accordion simple-table window]]
            [behave-cms.components.entity-form  :refer [entity-form]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width]]
            [behave-cms.components.translations :refer [all-translations]]
            [behave-cms.help.views              :refer [help-editor]]
            [behave-cms.submodules.subs]))

(defn submodule-form [module-id id num-submodules]
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

(defn manage-submodule [module-id *submodule num-submodules]
  [:div.col-6
   [:h5 (if (nil? module-id) "Add" "Edit") " Submodule"
    [submodule-form module-id *submodule num-submodules]]])

(defn submodules-table [label submodules]
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

(defn all-submodule-tables [module-id]
  (let [submodules (rf/subscribe [:submodules module-id])
        inputs     (filter #(= :input (:submodule/io %)) @submodules)
        outputs    (filter #(= :output (:submodule/io %)) @submodules)]
    [:div.col-6
     [:row
      [submodules-table "Output Submodules" outputs]
      [submodules-table "Input Submodules" inputs]]]))

(defn list-submodules-page [{:keys [id]}]
  (let [module         (rf/subscribe [:entity id '[* {:application/_modules [:db/id]}]])
        application-id (get-in @module [:application/_modules 0 :db/id])
        submodules     (rf/subscribe [:sidebar/submodules id])
        submodule      (rf/subscribe [:state :submodule])]
    [:<>
     [sidebar
      "Submodules"
      @submodules
      "Modules"
      (str "/applications/" application-id)]
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
         [help-editor (:module/help-key @module)]]]]]]))
