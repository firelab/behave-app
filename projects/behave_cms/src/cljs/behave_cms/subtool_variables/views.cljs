(ns behave-cms.subtool-variables.views
  (:require
   [behave-cms.components.common        :refer [accordion
                                               checkbox
                                               window]]
   [behave-cms.components.cpp-editor    :refer [cpp-editor-form]]
   [behave-cms.components.sidebar.views :refer [sidebar-width]]
   [behave-cms.components.translations  :refer [all-translations]]
   [behave-cms.help.views               :refer [help-editor]]
   [re-frame.core                       :as rf]))

;;; Constants

(def ^:private cpp-attrs {:cpp-class :subtool-variable/cpp-class-uuid
                          :cpp-fn    :subtool-variable/cpp-function-uuid
                          :cpp-ns    :subtool-variable/cpp-namespace-uuid
                          :cpp-param :subtool-variable/cpp-parameter-uuid})

(defn- bool-setting [label attr entity]
  (let [{id :db/id} entity
        *value?     (atom (get entity attr))
        update!     #(rf/dispatch [:api/update-entity {:db/id id attr @*value?}])]
    [:div.mt-1
     [checkbox
      label
      @*value?
      #(do (swap! *value? not)
           (update!))]]))

(defn- settings [subtool-variable]
  [:div.row.mt-2
   [bool-setting "Dynamic Units?" :subtool-variable/dynamic-units? subtool-variable]])

;;; Public Views

(defn subtool-variable-page
  "Renders the subtool-variable page. Takes in a map with:
   - :id [int]: Subtool variable's entity ID."
  [{nid :nid}]
  (let [subtool-variable (rf/subscribe [:entity [:bp/nid nid] '[* {:variable/_subtool-variables [*]
                                                                   :subtool/_variables          [:bp/nid :subtool/name]}]])
        variable         (get-in @subtool-variable [:variable/_subtool-variables 0])]
    [window
     sidebar-width
     [:div.container
      [:div.row.mb-3.mt-4
       [:h2 (:variable/name variable)]]
      [accordion
       "Translations"
       [all-translations (:subtool-variable/translation-key @subtool-variable)]]
      [:hr]
      [accordion
       "Help Page"
       [:div.col-12
        [help-editor (:subtool-variable/help-key @subtool-variable)]]]
      [:hr]
      [accordion
       "CPP Functions"
       [:div.col-6
        [cpp-editor-form
         (merge cpp-attrs {:id [:bp/nid nid] :editor-key :subtool-variables})]]]
      [:hr]
      [accordion
       "Settings"
       [settings @subtool-variable]]]]))
