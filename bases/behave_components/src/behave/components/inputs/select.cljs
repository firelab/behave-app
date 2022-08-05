(ns behave.components.inputs.select)

(defn option [{:keys [label value]}]
  [:option {:key value :class "input-dropdown__option" :value value} label])

(defn option-group [label]
  [:optgroup {:key label :class "input-dropdown__option-group" :label label}])

(defn select-input [{:keys [label id name on-change disabled? error? options]}]
  [:div {:class ["input-dropdown"
                     (when error? "input-dropdown--error")
                     (when disabled? "input-dropdown--disabled")]}
   [:label {:class "input-dropdown__label" :for id} label]
   [:div {:class "input-dropdown__select-wrapper"}
    [:select
     {:type      "select"
      :class     "input-dropdown__select-wrapper__select"
      :disabled  disabled?
      :id        id
      :name      name
      :on-change on-change}
     (for [{:keys [group] :as opt} options]
       (if (some? group)
         [option-group group (for [o (:options opt)] [option o])]
         [option opt]))]]])
