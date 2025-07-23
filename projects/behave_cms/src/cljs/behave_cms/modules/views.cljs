(ns behave-cms.modules.views
  (:require
   [re-frame.core                      :as rf]
   [behave-cms.components.common       :refer [accordion window]]
   [behave-cms.components.sidebar      :refer [sidebar sidebar-width ->sidebar-links]]
   [behave-cms.components.translations :refer [app-translations]]
   [behave-cms.help.views              :refer [help-editor]]
   [behave-cms.components.table-entity-form :refer [table-entity-form]]
   [reagent.core :as r]))

;;; Modules

(defn- modules-table [app-id]
  (r/with-let [selected-module-atom (r/atom nil)]
    (let [modules @(rf/subscribe [:application/modules app-id])]
      [:div.col-12
       [table-entity-form
        {:entity             :module
         :entities           (sort-by :module/order modules)
         :on-select          #(reset! selected-module-atom @(rf/subscribe [:re-entity (:db/id %)]))
         :parent-id          app-id
         :parent-field       :application/_modules
         :table-header-attrs [:module/name :module/order]
         :order-attr         :module/order
         :entity-form-fields [{:label     "Name"
                               :required? true
                               :field-key :module/name}]}]])))

;;; Tools

(defn- tools-table [app-id]
  (r/with-let [selected-tool-atom (r/atom nil)]
    (let [tools @(rf/subscribe [:application/tools app-id])]
      [:div.col-12
       [table-entity-form
        {:entity             :tool
         :entities           (sort-by :tool/order tools)
         :on-select          #(reset! selected-tool-atom @(rf/subscribe [:re-entity (:db/id %)]))
         :parent-id          app-id
         :parent-field       :application/_tools
         :table-header-attrs [:tool/name :tool/order]
         :order-attr         :tool/order
         :entity-form-fields [{:label     "Name"
                               :required? true
                               :field-key :tool/name}
                              {:label     "Library Namespace"
                               :required? true
                               :field-key :tool/lib-ns}]}]])))

;; Priortzed Results Table
(defn- prioritized-results-table
  [app-id]
  (r/with-let [selected-prioritized-results-atom (r/atom nil)]
    (let [prioritized-results @(rf/subscribe [:application/prioritized-results app-id])]
      [:div.col-12
       [table-entity-form
        {:entity             :tool
         :entities           (sort-by :prioritized-results/order prioritized-results)
         :on-select          #(reset! selected-prioritized-results-atom @(rf/subscribe [:re-entity (:db/id %)]))
         :parent-id          app-id
         :parent-field       :application/_prioritized-results
         :table-header-attrs [:variable/name :prioritized-results/order]
         :order-attr         :prioritized-results/order
         :entity-form-fields [{:label     "Group Variable"
                               :app-id    app-id
                               :required? true
                               :field-key :prioritized-results/group-variable
                               :type      :group-variable}]}]])))

;;; Public

(defn list-modules-page
  "Displays page for modules. Takes a single map with:
  - id [int] - Application Entity ID"
  [{nid :nid}]
  (let [application (rf/subscribe [:application [:bp/nid nid]])
        app-id      (:db/id @application)
        modules     (rf/subscribe [:application/modules app-id])
        tools       (rf/subscribe [:application/tools app-id])]
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
        [modules-table app-id]]
       [:hr]
       [accordion
        "Tools"
        [tools-table app-id]]
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
        "Application's Prioritized Results"
        [:div.col-12
         [:div.row
          [prioritized-results-table app-id]]]]]]]))
