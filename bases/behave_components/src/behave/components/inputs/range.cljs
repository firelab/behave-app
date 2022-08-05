(ns behave.components.inputs.range)

(defn range-input [{:keys [label id name on-change disabled? error? min max]}]
  [:div {:class ["input-range " (when error? "input-range--error")]}
   [:label
    {:class "input-range__label" :for id}
    label
    [:input
     {:type      "range"
      :class     "input-range__input"
      :disabled  disabled?
      :id        id
      :name      name
      :min       min
      :max       max
      :on-change on-change}]]])
