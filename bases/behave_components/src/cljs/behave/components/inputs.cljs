(ns behave.components.inputs
  (:require [behave.components.button :refer [button]]
            [behave.components.a11y   :refer [on-space on-enter]]))

(defn checkbox [{:keys [label id name on-change checked? disabled? error?]}]
  [:label {:class ["input-checkbox"
                   (when checked?  "input-checkbox--checked")
                   (when disabled? "input-checkbox--disabled")
                   (when error?    "input-checkbox--error")]
           :for   id}
   [:div
    {:class        "input-checkbox__box"
     :tabindex     0
     :on-key-press (on-space on-change)}
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

(defn browse-input [{:keys [accept label button-label focus? error? disabled? on-change]}]
  [:div
   {:class ["input-browse"
            (when focus? "input-browse--focused")
            (when error? "input-browse--error")
            (when disabled? "input-browse--disabled")]}
   [:input
    {:class     "input-browse__input"
     :type      "file"
     :accept    accept
     :on-change on-change}]
   [button
    {:label     button-label
     :disabled? (or disabled? error?)
     :variant   "primary"
     :size      "small"}]
   [:div
    {:class "input-browse__label"}
    label]])

(defn number-input [{:keys [label id name on-change on-blur disabled? error? min max value value-atom]}]
  [:div {:class ["input-number " (when error? "input-number--error")]}
   [:label
    {:class "input-number__label" :for id}
    label]
   [:input
    (cond-> {:type      "number"
             :class     "input-number__input"
             :disabled  disabled?
             :id        id
             :name      name
             :on-change on-change
             :on-blur   on-blur
             :min       min
             :max       max}
      value      (assoc :value value)
      value-atom (assoc :value @value-atom))]])

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

(defn radio-input [{:keys [label id name value on-change checked? disabled? error?]}]
  [:label {:class ["input-radio"
                   (when checked? "input-radio--checked")
                   (when error? "input-radio--error")
                   (when disabled? "input-radio--disabled")]}
   [:div
    {:class        "input-radio__circle"
     :tabindex     0
     :on-key-press (on-space on-change)}
    [:div {:class "input-radio__circle__dot"}]]
   [:input
    {:type      "radio"
     :class     "input-radio__input"
     :checked   checked?
     :disabled  disabled?
     :id        id
     :name      name
     :value     value
     :on-change on-change}]
   [:span {:class "input-radio__label"} label]])

(defn radio-group [{:keys [label options disabled?]}]
  [:div {:class ["input-radio-group"
                 (when disabled? "input-radio-group--disabled")]}
   [:label {:class "input-radio-group__label"} label]
   [:div.input-radio-group__options
    (for [option options]
     ^{:key (:label option)}
     [radio-input (cond-> option
                    disabled? (assoc :disabled? true))])]])

(defn option [{:keys [label value selected?]}]
  [:option
   {:key      value
    :class    "input-dropdown__option"
    :selected selected?
    :value    value} label])

(defn option-group [label]
  [:optgroup {:key label :class "input-dropdown__option-group" :label label}])

(defn dropdown [{:keys [label id name on-change disabled? error? options]}]
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

(defn text-input
  [{:keys [disabled? error? focused? id label name on-blur on-change on-focus
           placeholder value value-atom default-value on-key-press]}]
  [:div {:class ["input-text"
                 (when error?    "input-text--error")
                 (when disabled? "input-text--disabled")
                 (when focused?  "input-text--focused")]}
   [:label {:class "input-text__label" :for id} label]
   [:input (cond-> {:class         "input-text__input"
                    :disabled      disabled?
                    :id            id
                    :name          name
                    :on-blur       on-blur
                    :on-key-press  on-key-press
                    :on-focus      on-focus
                    :placeholder   placeholder
                    :type          "text"}
             on-change     (assoc :on-change on-change)
             default-value (assoc :default-value default-value)
             value         (assoc :value value)
             value-atom    (assoc :value @value-atom))]])
