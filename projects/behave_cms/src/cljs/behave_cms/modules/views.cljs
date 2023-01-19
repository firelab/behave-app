(ns behave-cms.modules.views
  (:require [reagent.core                       :as r]
            [re-frame.core                      :as rf]
            [data-utils.core                    :refer [parse-int]]
            [behave-cms.components.common       :refer [accordion simple-table window]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width]]
            [behave-cms.components.translations :refer [app-translations]]
            [behave-cms.help.views              :refer [help-editor]]
            [behave-cms.components.entity-form  :refer [entity-form]]
            [behave-cms.subs]))

(defn module-form [application-id module-id]
  [entity-form {:entity       :modules
                :parent-field :application/_modules
                :parent-id    application-id
                :id           module-id
                :fields       [{:label     "Name"
                                :required? true
                                :field-key :module/name}]}])

(defn manage-module [application-id *module]
  [:div.col-6
   [:h4 (str (if *module "Edit" "Add") " Module")
    [module-form application-id *module]]])

(defn modules-table [application-id]
  (let [modules (rf/subscribe [:modules application-id])]
    [:div.col-6
     [simple-table
      [:module/name]
      (sort-by :module/order @modules)
      {:on-select   #(rf/dispatch [:state/set-state :module (:db/id %)])
       :on-delete   #(when (js/confirm (str "Are you sure you want to delete the module " (:module/name %) "?"))
                       (rf/dispatch [:api/delete-entity %]))
       :on-increase #(rf/dispatch [:api/reorder % @modules :module/order :inc])
       :on-decrease #(rf/dispatch [:api/reorder % @modules :module/order :dec])}]]))

(defn list-modules-page [{:keys [id]}]
  (let [loaded? (rf/subscribe [:state :loaded?])
        id      (parse-int id)]
    (if @loaded?
      (let [application (rf/subscribe [:application id])
            modules     (rf/subscribe [:sidebar/modules id])
            *module     (rf/subscribe [:state :module])]
        [:<>
         [sidebar
          "Modules"
          @modules
          "Applications"
          "/applications"]
         [window sidebar-width
          [:div.container
           [:div.row.mb-3.mt-4
            [:h2 (:application/name @application)]]
           [accordion
            "Modules"
            [modules-table id]
            [manage-module id @*module]]
           [:hr]
           [accordion
            "Help Page"
            [:div.col-12
             [help-editor (:application/help-key @application)]]]
           [:hr]
           [accordion
            "Application Translations"
            [app-translations (:application/translation-key @application)]]]]])
      [:div "Loading ..."])))

(comment

  (def id 78)
  (rf/subscribe [:sidebar/modules id])
  (rf/subscribe [:sidebar/modules id])
  (rf/subscribe [:entity id '[*]])

  )
