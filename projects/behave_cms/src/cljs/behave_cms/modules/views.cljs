(ns behave-cms.modules.views
  (:require [reagent.core   :as r]
            [re-frame.core  :as rf]
            [behave-cms.components.common       :refer [accordion simple-table window]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width]]
            [behave-cms.components.translations :refer [app-translations]]
            [behave-cms.components.help-editor  :refer [help-editor]]
            [behave-cms.components.entity-form  :refer [entity-form]]
            [behave-cms.modules.subs]))

(defn module-form [application-id module-id]
  [entity-form {:parent-field  :application/modules
                :parent-id     application-id
                :id            module-id
                :fields        [{:label     "Name"
                                 :required? true
                                 :field-key :module/name}]}])

(defn manage-module [{id :db/id}]
  (let [module (rf/subscribe [:state :module])]
    [:div.col-6
     [:h4 (str (if @module "Edit" "Add") " Module")
      [module-form id @module]]]))

(defn application-translations [_]
  [app-translations "behaveplus"])

(defn modules-table [{modules :modules}]
  [:div.col-6
   [simple-table
    [:module/name]
    (sort-by :module/order @modules)
    {:on-select   #(rf/dispatch [:state/set-state :module (:db/id %)])
     :on-delete   #(when (js/confirm (str "Are you sure you want to delete the module " (:module/name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))
     :on-increase #(rf/dispatch [:module/reorder % :up modules])
     :on-decrease #(rf/dispatch [:module/reorder % :down modules])}]])

(defn list-modules-page [{:keys [id]}]
  (r/with-let [application (rf/subscribe [:entity id '[* {:application/modules [*]}]])
               module      (rf/subscribe [:state :module])]
    [:<>
     [sidebar
      "Modules"
      @(rf/subscribe [:sidebar/modules id])
      "Applications"
      "/applications"]
     [window sidebar-width
      [:div.container
       [:div.row.mb-3.mt-4
        [:h2 (:application/name @application)]]
       [accordion
        "Modules"
        [modules-table @application]
        [manage-module @application @module]]
       [:hr]
       [accordion
        "Help Page"
        [:div.col-12
         [help-editor (:application/help-key @application)]]]
       [:hr]
       [accordion
        "Application Translations"
        [application-translations @application]]]]]))
