(ns behave-cms.modules.views
  (:require
   [re-frame.core                      :as rf]
   [behave-cms.components.common       :refer [accordion simple-table window]]
   [behave-cms.components.sidebar      :refer [sidebar sidebar-width ->sidebar-links]]
   [behave-cms.components.translations :refer [app-translations]]
   [behave-cms.help.views              :refer [help-editor]]
   [behave-cms.components.entity-form  :refer [entity-form]]))

;;; Modules

(defn- module-form [application-id module-id num-modules]
  [entity-form {:entity       :module
                :parent-field :application/_modules
                :parent-id    application-id
                :id           module-id
                :fields       [{:label     "Name"
                                :required? true
                                :field-key :module/name}]
                :on-create    #(assoc % :module/order num-modules)}])

(defn- manage-module [application-id *module num-modules]
  [:div.col-6
   [:h4 (str (if *module "Edit" "Add") " Module")
    [module-form application-id *module num-modules]]])

(defn- modules-table [application-id]
  (let [modules (rf/subscribe [:application/modules application-id])]
    [:div.col-6
     [simple-table
      [:module/name]
      (sort-by :module/order @modules)
      {:on-select   #(rf/dispatch [:state/set-state :module (:db/id %)])
       :on-delete   #(when (js/confirm (str "Are you sure you want to delete the module " (:module/name %) "?"))
                       (rf/dispatch [:api/delete-entity %]))
       :on-increase #(rf/dispatch [:api/reorder % @modules :module/order :inc])
       :on-decrease #(rf/dispatch [:api/reorder % @modules :module/order :dec])}]]))

;;; Tools

(defn- tools-table [application-id]
  (let [tools     (rf/subscribe [:application/tools application-id])
        on-select #(rf/dispatch [:state/set-state :tool (:db/id %)])
        on-delete #(when (js/confirm (str "Are you sure you want to delete the tool " (:tool/name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))]

    [simple-table
     [:tool/name]
     (sort-by :tool/order @tools)
     {:on-select on-select
      :on-delete on-delete}]))

(defn- tool-form [application-id tool-id num-tools]
  [entity-form {:entity       :tool
                :parent-field :application/_tools
                :parent-id    application-id
                :id           tool-id
                :fields       [{:label     "Name"
                                :required? true
                                :field-key :tool/name}]
                :on-create    #(assoc % :tool/order num-tools)}])

(defn- manage-tool [application-id *tool num-tools]
  [:div.col-6
   [:h4 (str (if *tool "Edit" "Add") " Tool")
    [tool-form application-id *tool num-tools]]])

;;; Public

(defn list-modules-page
  "Displays page for modules. Takes a single map with:
  - id [int] - Application Entity ID"
  [{id :id}]
  (let [application (rf/subscribe [:application id])
        modules     (rf/subscribe [:application/modules id])
        *module     (rf/subscribe [:state :module])
        tools       (rf/subscribe [:application/tools id])
        *tool       (rf/subscribe [:state :tool])]
    [:<>
     [sidebar
      "Modules"
      (->sidebar-links @modules :module/name :get-module)
      "Applications"
      "/applications"
      "Tools"
      (->sidebar-links @tools :tool/name :get-tool)]
     [window sidebar-width
      [:div.container
       [:div.row.mb-3.mt-4
        [:h2 (:application/name @application)]]
       [accordion
        "Modules"
        [modules-table id]
        [manage-module id @*module (count @modules)]]
       [:hr]
       [accordion
        "Tools"
        [:div.col-6
         [tools-table id]]
        [:div.col-6
         [manage-tool id @*tool (count @tools)]]]
       [:hr]
       [accordion
        "Help Page"
        [:div.col-12
         [help-editor (:application/help-key @application)]]]
       [:hr]
       [accordion
        "Application Translations"
        [app-translations (:application/translation-key @application)]]]]]))
