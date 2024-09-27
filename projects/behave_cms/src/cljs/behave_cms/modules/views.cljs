(ns behave-cms.modules.views
  (:require
   [re-frame.core                      :as rf]
   [behave-cms.components.common       :refer [accordion simple-table window]]
   [behave-cms.components.sidebar      :refer [sidebar sidebar-width ->sidebar-links]]
   [behave-cms.components.translations :refer [app-translations]]
   [behave-cms.help.views              :refer [help-editor]]
   [behave-cms.components.entity-form  :refer [entity-form]]
   [behave-cms.components.group-variable-selector :refer [group-variable-selector]]))

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
                                :field-key :tool/name}
                               {:label     "Library Namespace"
                                :required? true
                                :field-key :tool/lib-ns}]
                :on-create    #(assoc % :tool/order num-tools)}])

(defn- manage-tool [application-id *tool num-tools]
  [:div.col-6
   [:h4 (str (if *tool "Edit" "Add") " Tool")
    [tool-form application-id *tool num-tools]]])

;; Group Variable Order Override Table
(defn group-variable-order-override-table
  ""
  [app-id]
  (let [group-variable-order-overrides (rf/subscribe [:application/group-variable-order-overrides app-id])]
    [:div.col-6
     [:div
      [:h4 "Group Variables"]
      [:p "Use this list to sort group variables ahead of the normal sort order accross all modules in this application"]
      [simple-table
       [:variable/name]
       (sort-by :group-variable-order-override/order @group-variable-order-overrides)
       {:on-select   #(do (prn "on-select:" %)
                          (rf/dispatch [:state/set-state :group-variable-order-override %]))
        :on-delete   #(rf/dispatch [:api/delete-entity %])
        :on-increase #(rf/dispatch [:api/reorder % @group-variable-order-overrides
                                    :group-variable-order-override/order :inc])
        :on-decrease #(rf/dispatch [:api/reorder % @group-variable-order-overrides
                                    :group-variable-order-override/order :dec])}]]]))

;;; Public

(defn list-modules-page
  "Displays page for modules. Takes a single map with:
  - id [int] - Application Entity ID"
  [{nid :nid}]
  (let [application       (rf/subscribe [:application [:bp/nid nid]])
        id                (:db/id @application)
        modules           (rf/subscribe [:application/modules id])
        *module           (rf/subscribe [:state :module])
        tools             (rf/subscribe [:application/tools id])
        *tool             (rf/subscribe [:state :tool])
        gv-order-override (rf/subscribe [:state :group-variable-order-override])
        gv-id             (:db/id (:group-variable-order-override/group-variable @gv-order-override))]
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
        [app-translations (:application/translation-key @application)]]
       [:hr]
       [accordion
        "Application Group Varible Order Overrides"
        [:div.col-12
         [:div.row
          [group-variable-order-override-table (:db/id @application)]
          [group-variable-selector
           {:state-path          [:editors :application/group-variable-order-override :variable-lookup]
            :app-id              (:db/id @application)
            :gv-id               gv-id
            :on-submit           #(let [gv-count @(rf/subscribe [:application/group-variable-order-overrides-count (:db/id @application)])]
                                    (rf/dispatch [:api/upsert-entity
                                                  (if (:db/id @gv-order-override)
                                                    {:db/id                                        (:db/id @gv-order-override)
                                                     :group-variable-order-override/group-variable %}
                                                    {:application/_group-variable-order-overrides  (:db/id @application)
                                                     :group-variable-order-override/group-variable %
                                                     :group-variable-order-override/order          gv-count})])
                                    (rf/dispatch [:state/set-state :group-variable-order-override nil]))}]]]]]]]))
