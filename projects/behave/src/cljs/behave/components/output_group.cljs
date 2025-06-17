(ns behave.components.output-group
  (:require [behave.components.core :as c]
            [behave.translate       :refer [<t]]
            [re-frame.core          :as rf]))

(defn wizard-output [ws-uuid {gv-uuid  :bp/uuid
                              help-key :group-variable/help-key}]
  (when @(rf/subscribe [:wizard/default-option ws-uuid gv-uuid])
    (rf/dispatch [:worksheet/upsert-output ws-uuid gv-uuid true]))
  (let [checked?       (rf/subscribe [:worksheet/output-enabled? ws-uuid gv-uuid])
        on-focus-click #(rf/dispatch [:help/highlight-section help-key])
        disabled?      (rf/subscribe [:wizard/disabled-output-group-variable? ws-uuid gv-uuid])]
    [:div.wizard-output
     {:on-click on-focus-click
      :on-focus on-focus-click}
     [c/checkbox {:label     @(rf/subscribe [:wizard/gv-uuid->default-variable-name gv-uuid])
                  :checked?  @checked?
                  :disabled? @disabled?
                  :on-change #(rf/dispatch [:worksheet/upsert-output ws-uuid gv-uuid (not @checked?)])}]]))

(defn wizard-single-select-outupt [ws-uuid group all-group-variables]
  (let [selected-options? @(rf/subscribe [:wizard/selected-group-variables ws-uuid (:db/id group)])
        on-focus-click    #(rf/dispatch [:help/highlight-section (:group/help-key group)])
        ->option          (fn [{gv-uuid :bp/uuid}]
                            (when @(rf/subscribe [:wizard/default-option ws-uuid gv-uuid])
                              (rf/dispatch [:worksheet/upsert-output ws-uuid gv-uuid true]))
                            {:value     gv-uuid
                             :label     @(rf/subscribe [:wizard/gv-uuid->default-variable-name gv-uuid])
                             :on-change #(rf/dispatch [:worksheet/select-single-select-output
                                                       ws-uuid
                                                       (:db/id group)
                                                       gv-uuid])
                             :disabled? @(rf/subscribe [:wizard/disabled-output-group-variable? ws-uuid gv-uuid])
                             :selected? (contains? selected-options? gv-uuid)
                             :checked?  (contains? selected-options? gv-uuid)})]
    [:div.wizard-output
     {:on-click on-focus-click
      :on-focus on-focus-click}
     [c/radio-group
      {:id      uuid
       :options (doall (map ->option all-group-variables))}]]))

(defn output-group [{:keys [ws-uuid workflow]} group variables level]
  [:div.wizard-group
   {:class (str "wizard-group--level-" level)}
   [:div {:class [(if (= workflow :standard)
                    "wizard-group__header--standard"
                    "wizard-group__header")]}
    @(<t (:group/translation-key group))]
   [:div.wizard-group__outputs
    (if (:group/single-select? group)
      [wizard-single-select-outupt ws-uuid group variables]
      (for [variable variables]
        ^{:key (:db/id variable)}
        [wizard-output ws-uuid variable]))]])
