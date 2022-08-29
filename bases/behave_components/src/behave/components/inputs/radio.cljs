(ns behave.components.inputs.radio)

(defn radio-input [{:keys [label id name on-change checked? disabled? error?]}]
  [:label {:class ["input-radio"
                       (when checked? "input-radio--checked")
                       (when error? "input-radio--error")
                       (when disabled? "input-radio--disabled")]}
   [:div {:class "input-radio__circle"}
    [:div {:class "input-radio__circle__dot"}]]
   [:input
    {:type "radio"
     :class "input-radio__input"
     :checked checked?
     :disabled disabled?
     :id id
     :name name
     :on-change on-change}]
   [:span {:class "input-radio__label"} label]])

(defn radio-group [{:keys [label options name]}]
  [:div {:class "input-radio-group"}
   [:label {:class "input-radio-group__label"} label]
   (for [{:keys [id] :as option} options]
     ^{:keys id}
     [radio-input (merge option {:name name})])])
