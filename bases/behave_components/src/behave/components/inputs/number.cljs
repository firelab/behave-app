(ns behave.components.inputs.number)

(defn number-input [{:keys [label id name value on-change disabled? error? required? min max]}]
  [:div {:class ["input-number"
                 (when disabled? "input-number--disabled")
                 (when error? "input-number--error")]}
   [:label {:class "input-number__label" :for id} label]
   [:input
    {:type          "number"
     :class         "input-number__input"
     :disabled      disabled?
     :id            id
     :name          name
     :default-value value
     :required      required?
     :on-change     on-change
     :min           min
     :max           max}]])
