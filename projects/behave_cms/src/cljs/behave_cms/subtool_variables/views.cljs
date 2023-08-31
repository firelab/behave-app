(ns behave-cms.subtool-variables.views
  (:require [re-frame.core                      :as rf]
            [behave-cms.components.common       :refer [accordion
                                                        window]]
            [behave-cms.components.cpp-editor   :refer [cpp-editor-form]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width ->sidebar-links]]
            [behave-cms.components.translations :refer [all-translations]]
            [behave-cms.help.views              :refer [help-editor]]))

;;; Constants

(def ^:private cpp-attrs {:cpp-class :subtool-variable/cpp-class-uuid
                          :cpp-fn    :subtool-variable/cpp-function-uuid
                          :cpp-ns    :subtool-variable/cpp-namespace-uuid
                          :cpp-param :subtool-variable/cpp-parameter-uuid})

;;; Public Views

(defn subtool-variable-page
  "Renders the subtool-variable page. Takes in a map with:
   - :id [int]: Subtool variable's entity ID."
  [{eid :id}]
  (let [subtool-variable (rf/subscribe [:entity eid '[* {:variable/_subtool-variables [*]
                                                         :subtool/_variables           [*]}]])

        subtool          (get-in @subtool-variable [:subtool/_variables 0])
        variable         (get-in @subtool-variable [:variable/_subtool-variables 0])
        input-variables  (rf/subscribe [:subtool/input-variables (:db/id subtool)])
        output-variables (rf/subscribe [:subtool/output-variables (:db/id subtool)])]
    (prn "input-variables:" input-variables)
    (prn "output-variables:" output-variables)
    [:<>
     [sidebar
      "Input Variables"
      (->sidebar-links @input-variables :variable/name :get-subtool-variable)
      "Subtools"
      (str "/subtools/" (:db/id subtool))
      "Output Variables"
      (->sidebar-links @output-variables :variable/name :get-subtool-variable)]
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
          (merge cpp-attrs {:id eid :editor-key :subtool-variables})]]]]]]))
