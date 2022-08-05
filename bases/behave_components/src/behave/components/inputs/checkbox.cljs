(ns behave.components.inputs.checkbox)

(defn checkbox [{:keys [label id name on-change checked? disabled? error?]}]
  [:label {:class ["input-checkbox"
                   (when checked?  "input-checkbox--checked")
                   (when disabled? "input-checkbox--disabled")
                   (when error?    "input-checkbox--error")]
           :for id}
   [:div {:class "input-checkbox__box"}
    [:div {:class "input-checkbox__box__check"}]]
   [:input
    {:type      "checkbox"
     :class     "input-checkbox__input"
     :on-change on-change
     :checked   checked?
     :disabled  disabled?
     :id        id
     :name      name}]
   [:span {:class "input-checkbox__label"} label]])
