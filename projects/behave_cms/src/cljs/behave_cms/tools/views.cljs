(ns behave-cms.tools.views
  (:require
   [behave-cms.components.common            :refer [accordion window]]
   [behave-cms.components.sidebar           :refer [->sidebar-links sidebar sidebar-width]]
   [behave-cms.components.table-entity-form :refer [table-entity-form table-entity-form-on-select]]
   [behave-cms.components.translations      :refer [all-translations]]
   [behave-cms.help.views                   :refer [help-editor]]
   [re-frame.core                           :as rf]))

(defn- subtools-table [tool-id]
  (let [selected-state-path [:selected :subtool]
        editor-state-path   [:editors :subtool]
        entities            (rf/subscribe [:tool/subtools tool-id])]
    [table-entity-form
     {:entity             :module
      :form-state-path    editor-state-path
      :entities           (sort-by :subtool/order @entities)
      :on-select          (table-entity-form-on-select selected-state-path)
      :parent-id          tool-id
      :parent-field       :tool/_subtools
      :table-header-attrs [:subtool/name
                           :subtool/auto-compute?]
      :order-attr         :subtool/order
      :entity-form-fields [{:label     "Name"
                            :required? true
                            :field-key :subtool/name}
                           {:label     "Auto Compute?"
                            :required? true
                            :field-key :subtool/auto-compute?
                            :type      :checkbox
                            :options   [{:value true}]}]}]))

(defn tools-page
  "Displays Tools page. Takes a map with:
  - :id [int] - Tool Entity ID"
  [{nid :nid}]
  (let [tool            (rf/subscribe [:entity [:bp/nid nid] '[* {:application/_tools [*]}]])
        tool-eid        (:db/id @tool)
        application-nid (get-in @tool [:application/_tools 0 :bp/nid])
        subtools        (rf/subscribe [:tool/subtools tool-eid])]
    [:<>
     [sidebar
      "Subtools"
      (->sidebar-links @subtools :subtool/name :get-subtool)
      "Tools"
      (str "/applications/" application-nid)]
     [window sidebar-width
      [:div.container
       [:div.row.mb-3.mt-4
        [:h2 (:tool/name @tool)]]
       [accordion
        "Subtools"
        [:div.col-12
         [subtools-table tool-eid]]]
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
