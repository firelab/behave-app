(ns behave.components.output-group
  (:require [re-frame.core :as rf]
            [behave.components.core :as c]))

(defn wizard-output [{id :db/id var-name :variable/name help-key :group-variable/help-key}]
  (let [checked? (rf/subscribe [:state [:worksheet :outputs id]])]
    [:div.wizard-output {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])} ;should set the alignToTop=true but not working atm.
     [c/checkbox {:label     var-name
                  :checked?  @checked?
                  :on-change #(rf/dispatch [:state/set [:worksheet :outputs id] (not @checked?)])}]]))

(defn output-group [group variables]
  (let [{group-name :group/name} group]
    [:div.wizard-group
     [:div.wizard-group__header group-name]
     [:div.wizard-group__outputs
      (for [variable variables]
        ^{:key (:db/id variable)}
        [wizard-output variable])]]))
