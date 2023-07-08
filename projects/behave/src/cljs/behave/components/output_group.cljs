(ns behave.components.output-group
  (:require [re-frame.core :as rf]
            [behave.components.core :as c]))

(defn wizard-output [ws-uuid {uuid :bp/uuid var-name :variable/name help-key :group-variable/help-key}]
  (let [checked? (rf/subscribe [:worksheet/output-enabled? ws-uuid uuid])]
    ;; (when checked?
    ;;   (println "Variable checked for: worksheet" ws-uuid)
    ;;   (println "uuid:" uuid))
    [:div.wizard-output {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [c/checkbox {:label     var-name
                  :checked?  @checked?
                  :on-change #(rf/dispatch [:worksheet/upsert-output ws-uuid uuid (not @checked?)])}]]))

(defn output-group [ws-uuid group variables level]
  [:div.wizard-group
   {:class (str "wizard-group--level-" level)}
   [:div.wizard-group__header (:group/name group)]
   [:div.wizard-group__outputs
    (for [variable variables]
      ^{:key (:db/id variable)}
      [wizard-output ws-uuid variable])]])
