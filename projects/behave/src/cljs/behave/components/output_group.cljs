(ns behave.components.output-group
  (:require [re-frame.core :as rf]
            [behave.components.core :as c]))

(defn wizard-output [ws-uuid {uuid :bp/uuid var-name :variable/name help-key :group-variable/help-key}]
  (let [checked? (rf/subscribe [:worksheet/output-enabled? ws-uuid uuid])]
    [:div.wizard-output {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [c/checkbox {:label     var-name
                  :checked?  @checked?
                  :on-change #(rf/dispatch [:worksheet/upsert-output ws-uuid uuid (not @checked?)])}]]))

(defn output-group [ws-uuid group variables & [level]]
  (let [{group-name :group/name} group
        level                    (if (nil? level) 0 level)]
    [:<>
     [:div.wizard-group
      {:class (str "wizard-group--level-" level)}
      [:div.wizard-group__header group-name]
      [:div.wizard-group__outputs
       (for [variable variables]
         ^{:key (:db/id variable)}
         [wizard-output ws-uuid variable])]]
     (when (:group/children group)
       (let [subgroups @(rf/subscribe [:wizard/subgroups (:db/id group)])]
         (for [subgroup subgroups]
           [output-group ws-uuid subgroup (:group/group-variables subgroup) (if (nil? level) 1 (inc level))])))]))
