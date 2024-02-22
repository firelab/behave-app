(ns behave.components.output-group
  (:require [behave.components.core :as c]
            [re-frame.core          :as rf]))

(defn wizard-output [ws-uuid {gv-uuid  :bp/uuid
                              help-key :group-variable/help-key}]
  (let [checked? (rf/subscribe [:worksheet/output-enabled? ws-uuid gv-uuid])]
    [:div.wizard-output {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [c/checkbox {:label     @(rf/subscribe [:wizard/gv-uuid->variable-name-1 gv-uuid])
                  :checked?  @checked?
                  :on-change #(rf/dispatch [:worksheet/upsert-output ws-uuid gv-uuid (not @checked?)])}]]))

(defn output-group [ws-uuid group variables level]
  [:div.wizard-group
   {:class (str "wizard-group--level-" level)}
   [:div.wizard-group__header (:group/name group)]
   [:div.wizard-group__outputs
    (for [variable variables]
      ^{:key (:db/id variable)}
      [wizard-output ws-uuid variable])]])
