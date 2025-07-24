(ns behave-cms.modules.views
  (:require
   [re-frame.core                      :as rf]
   [behave-cms.components.common       :refer [accordion window]]
   [behave-cms.components.sidebar      :refer [sidebar sidebar-width ->sidebar-links]]
   [behave-cms.components.translations :refer [app-translations]]
   [behave-cms.help.views              :refer [help-editor]]
   [behave-cms.components.table-entity-form :refer [table-entity-form]]))

;;; Modules

(defn- modules-table [app-id]
  (let [selected-module-state-path [:selected :module]
        module-editor-path         [:editors :module]
        selected-module            (rf/subscribe [:state selected-module-state-path])
        modules                    (rf/subscribe [:application/modules app-id])]
    [:div.col-12
     [table-entity-form
      {:entity             :module
       :form-state-path    module-editor-path
       :entities           (sort-by :module/order @modules)
       :on-select          #(if (= (:db/id %) (:db/id @selected-module))
                              (do (rf/dispatch [:state/set-state selected-module-state-path nil])
                                  (rf/dispatch [:state/set-state selected-module-state-path nil]))
                              (rf/dispatch [:state/set-state selected-module-state-path
                                            @(rf/subscribe [:re-entity (:db/id %)])]))
       :parent-id          app-id
       :parent-field       :application/_modules
       :table-header-attrs [:module/name]
       :order-attr         :module/order
       :entity-form-fields [{:label     "Name"
                             :required? true
                             :field-key :module/name}]}]]))

;;; Tools

(defn- tools-table [app-id]
  (let [selected-tool-state-path [:selected :tool]
        tool-editor-path         [:editors :tool]
        selected-tool            (rf/subscribe [:state selected-tool-state-path])
        tools                    (rf/subscribe [:application/tools app-id])]
    [:div.col-12
     [table-entity-form
      {:entity             :tool
       :form-state-path    tool-editor-path
       :entities           (sort-by :tool/order @tools)
       :on-select          #(if (= (:db/id %) (:db/id @selected-tool))
                              (do (rf/dispatch [:state/set-state selected-tool-state-path nil])
                                  (rf/dispatch [:state/set-state selected-tool-state-path nil]))
                              (rf/dispatch [:state/set-state selected-tool-state-path
                                            @(rf/subscribe [:re-entity (:db/id %)])]))
       :parent-id          app-id
       :parent-field       :application/_tools
       :table-header-attrs [:tool/name]
       :order-attr         :tool/order
       :entity-form-fields [{:label     "Name"
                             :required? true
                             :field-key :tool/name}
                            {:label     "Library Namespace"
                             :required? true
                             :field-key :tool/lib-ns}]}]]))

;; Priortzed Results Table
(defn- prioritized-results-table
  [app-id]
  (let [prioritized-results-state-path  [:selected :prioritized-results]
        prioritized-results-editor-path [:editors :prioritized-results]
        selected-prioritized-results    (rf/subscribe [:state prioritized-results-state-path])
        prioritized-results             (rf/subscribe [:application/prioritized-results app-id])]
    [:div.col-12
     [table-entity-form
      {:entity             :prioritized-results
       :form-state-path    prioritized-results-editor-path
       :entities           (sort-by :prioritized-results/order @prioritized-results)
       :on-select          #(if (= (:db/id %) (:db/id @selected-prioritized-results))
                              (do (rf/dispatch [:state/set-state prioritized-results-state-path nil])
                                  (rf/dispatch [:state/set-state prioritized-results-state-path nil]))
                              (rf/dispatch [:state/set-state prioritized-results-state-path
                                            @(rf/subscribe [:re-entity (:db/id %)])]))
       :parent-id          app-id
       :parent-field       :application/_prioritized-results
       :table-header-attrs [:variable/name]
       :order-attr         :prioritized-results/order
       :entity-form-fields [{:label     "Group Variable"
                             :app-id    app-id
                             :required? true
                             :field-key :prioritized-results/group-variable
                             :type      :group-variable}]}]]))

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
