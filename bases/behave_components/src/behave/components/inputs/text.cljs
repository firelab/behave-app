(ns behave.components.inputs.text)

(defn text-input [{:keys [label placeholder id name value on-change disabled? error? focused?]}]
  [:div {:class ["input-text"
                 (when error? "input-text--error")
                 (when focused? "input-text--focused")
                 (when disabled? "input-text--disabled")]}
   [:label
    {:class "input-text__label" :for id}
    label]
   [:input
    {:type          "text"
     :class         "input-text__input"
     :disabled      disabled?
     :placeholder   placeholder
     :id            id
     :name          name
     :default-value value
     :on-change     on-change}]])
