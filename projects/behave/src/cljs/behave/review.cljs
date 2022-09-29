(ns behave.review
  (:require [re-frame.core :as rf]
            [behave.components.core :as c]
            [behave.translate :refer [<t bp]]))

(defn root-component [params]
  (let [title @(<t (bp "working_area"))]
    [:<>
     [:div.workflow-select
      [:div.workflow-select__header
       [:div.wizard-header__banner
        [:div.wizard-header__banner__icon
         [c/icon :modules]]
        [:div.wizard-header__banner__title (str "Contain Module: Review")]]]
      [:div.wizard-navigation
       [c/button {:label   "Back"
                  :variant "secondary"}]
       [c/button {:label         "Run"
                  :variant       "highlight"
                  :icon-name     "arrow2"
                  :icon-position "right"
                  :on-click      #(rf/dispatch [:wizard/solve params])}]]]]))
