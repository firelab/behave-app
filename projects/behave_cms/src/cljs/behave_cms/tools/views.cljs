(ns behave-cms.tools.views
  (:require [re-frame.core :as rf]
            [behave-cms.components.common       :refer [accordion simple-table window]]
            [behave-cms.components.entity-form  :refer [entity-form]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width ->sidebar-links]]
            [behave-cms.components.translations :refer [all-translations]]
            [behave-cms.help.views              :refer [help-editor]]
            [behave-cms.tools.subs]))


(defn- subtool-form [tool-id id num-subtools]
  [entity-form {:entity        :subtool
                :parent-field  :tool/_subtools
                :parent-id     tool-id
                :id            id
                :fields        [{:label     "Name"
                                 :required? true
                                 :field-key :subtool/name}]
                :on-create     #(assoc % :subtool/order num-subtools)}])

(defn- manage-subtool [tool-id *subtool num-subtools]
  [:div.col-6
   [:h5 (if (nil? tool-id) "Add" "Edit") " Subtool"
    [subtool-form tool-id *subtool num-subtools]]])

(defn- subtools-table [tool-id]
  (let [subtools (rf/subscribe [:tool/subtools tool-id])]
    [:<>
     [:h5 "Subtools"]
     [simple-table
      [:subtool/name]
      (sort-by :subtool/order @subtools)
      {:on-select   #(rf/dispatch [:state/set-state :subtool (:db/id %)])
       :on-delete   #(when (js/confirm (str "Are you sure you want to delete the subtool "
                                            (:subtool/name %) "?"))
                       (rf/dispatch [:api/delete-entity %]))
       :on-increase #(rf/dispatch [:api/reorder % subtools :subtool/order :inc])
       :on-decrease #(rf/dispatch [:api/reorder % subtools :subtool/order :dec])}]]))

(defn tools-page
  "Displays Tools page. Takes a map with:
  - :id [int] - Tool Entity ID"
  [{tool-eid :id}]
  (let [tool           (rf/subscribe [:entity tool-eid '[* {:application/_tools [*]}]])
        application-id (get-in @tool [:application/_tools 0 :db/id])
        subtools       (rf/subscribe [:tool/subtools tool-eid])
        *subtool       (rf/subscribe [:state :subtool])]
    [:<>
     [sidebar
      "Subtools"
      (->sidebar-links @subtools :subtool/name :get-subtool)
      "Tools"
      (str "/applications/" application-id)]
     [window sidebar-width
      [:div.container
       [:div.row.mb-3.mt-4
        [:h2 (:tool/name @tool)]]
       [accordion
        "Subtools"
        [:div.col-6
         [subtools-table tool-eid]]
        [:div.col-6
         [manage-subtool tool-eid @*subtool (count @subtools)]]]
       [:hr]
       [accordion
        "Translations"
        [:div.col-12
         [all-translations (:tool/translation-key @tool)]]]
       [:hr]
       [accordion
        "Help Page"
        [:div.col-12
         [help-editor (:tool/help-key @tool)]]]]]]))
