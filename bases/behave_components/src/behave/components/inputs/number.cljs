(ns behave.components.inputs.number)

(defn number-input [{:keys [label id name on-change disabled? error? min max]}]
  [:div {:class ["input-number " (when error? "input-number--error")]}
   [:label
    {:class "input-number__label" :for id}
    label
    [:input
     {:type      "number"
      :class     "input-number__input"
      :disabled  disabled?
      :id        id
      :name      name
      :on-change on-change
      :min       min
      :max       max}]]])
