(ns behave.components.output-group
  (:require [re-frame.core :as rf]
            [behave.components.core :as c]))

(defn output-variable [{id :db/id var-name :variable/name}]
  (let [checked? (rf/subscribe [:state [:worksheet :outputs id]])]
    [:div.wizard-output
     [c/checkbox {:label var-name
                  :checked? @checked?
                  :on-change #(rf/dispatch [:state/set [:worksheet :outputs id] (not @checked?)])}]]))

(defn output-group [props]
  (let [{group-name :group/name variables :group/variables} props]
    [:div.wizard-group
     [:div.wizard-group__header group-name]
     [:div.wizard-group__outputs
      (for [variable variables]
        ^{:key (:db/id variable)}
        [output-variable variable])]]))
