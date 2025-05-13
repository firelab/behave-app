(ns behave.components.inputs
  (:require [behave.components.button :refer [button]]
            [behave.components.a11y   :refer [on-space on-enter]]
            [behave.components.icon.core :refer [icon]]
            [reagent.core :as r]
            [goog.string              :as gstring]))

;;==============================================================================
;; Checkbox
;;==============================================================================

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

;;==============================================================================
;; Browse
;;==============================================================================

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

;;==============================================================================
;; Number
;;==============================================================================

(defn number-input [{:keys [label id name on-change on-blur disabled? error? error-msg min max value value-atom step]}]
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
      step       (assoc :step step)
      value      (assoc :value value)
      value-atom (assoc :value @value-atom))]
   (when error?
     [:div.input-number__error error-msg])])

;;==============================================================================
;; Range
;;==============================================================================

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

;;==============================================================================
;; Radio Group
;;==============================================================================

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

;;==============================================================================
;; Dropdown
;;==============================================================================

(defn- option [{:keys [label value selected? disabled?]}]
  [:option
   {:key      value
    :class    "input-dropdown__option"
    :disabled disabled?
    :selected selected?
    :value    value} label])

(defn- option-group [label]
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

;;==============================================================================
;; Multi Select
;;==============================================================================

(defn- multi-select-option [{:keys [selected? label on-click color-tag]}]
  [:div (cond-> {:class    ["multi-select__option"
                            (when selected? "multi-select__option--selected")
                            (when color-tag "multi-select__option__color-tag")]
                 :style    {}
                 :on-click on-click}
          color-tag
          (assoc-in [:style :border-color] (:color color-tag)))
   [:div {:class [(if selected? "multi-select__option__icon--minus" "multi-select__option__icon--plus")]}
    [icon (if selected? "minus" "plus")]]
   label])

(defn multi-select-input
  "Creates a multi-select input component with the following options:
  - `input-label`: text to display as the label for the input.
  - `options`: vector of maps with keys:
     - `:selected?` - whether the option has been selected
     - `:label`     - label of the option
     - `:value`     - value of the option
     - `:tags`      - a collection of keywords or numbers
     - `:color-tag` - a map with at least the key `:color` representing a hex color.
  - [Optional] `filter-tags`: vector of maps with keys:
     - `:label` - label of the tag
     - `:id`    - id of the tag (keyword or number)
  - [Optional] `color-tags`:
     - `:label` - label of the option
     - `:color` - color of the tag"
  [{:keys [input-label options filter-tags color-tags]}]
  (r/with-let [selections (r/atom (->> options
                                       (filter #(true? (:selected? %)))
                                       (map (fn [{:keys [label value on-deselect]}]
                                              [label value on-deselect]))
                                       (into (sorted-set))))
               show-options? (r/atom false)
               selected-tag (r/atom nil)]
    [:div.multi-select
     (when @show-options?
       [:<>
        [:div.multi-select__prompt
         (gstring/format "Please select from the following %s (you can select multiple)" input-label)]
        (when filter-tags
          [:div.multi-select__tags
           (for [{:keys [id label]} (if (-> filter-tags (first) (:order))
                                      (sort-by :order filter-tags)
                                      (sort-by :label filter-tags))]
             ^{:key id}
             [:div.multi-select__tags__tag
              [button {:label     label
                       :variant   "outline-secondary"
                       :size      "small"
                       :selected? (= @selected-tag id)
                       :on-click  #(if (= @selected-tag id)
                                     (reset! selected-tag nil)
                                     (reset! selected-tag id))}]])])
        (when (seq color-tags)
          [:div.multi-select__color-tags
           (for [{:keys [label color]} color-tags]
             ^{:key color}
             [:div {:class "multi-select__color-tags__tag"
                    :style {:border-color color}}
              label])])
        [:div.multi-select__options
         (doall
          (for [{:keys [label
                        value
                        on-select
                        on-deselect
                        color-tag]} (cond->> options
                                      (and filter-tags @selected-tag)
                                      (filter (fn [id] (contains? (:tags id) @selected-tag))))]
            ^{:key label}
            (let [selection [label value on-deselect]]
              [multi-select-option {:selected? (contains? @selections selection)
                                    :color-tag color-tag
                                    :label     label
                                    :on-click  #(do (if (contains? @selections selection)
                                                      (do
                                                        (reset! selections (disj @selections selection))
                                                        (when on-deselect (on-deselect value)))
                                                      (do
                                                        (reset! selections (conj @selections selection))
                                                        (when on-select
                                                          (on-select value)))))}])))]])
     [:div.multi-select__selections
      [:div.multi-select__selections__header
       [:div (gstring/format "Selected %s" input-label)]
       [:div.multi-select__selections__header__button (if (false? @show-options?)
                                                        [button {:label     "Select More"
                                                                 :variant   "primary"
                                                                 :icon-name "plus"
                                                                 :size      "small"
                                                                 :on-click  #(reset! show-options? (not @show-options?))}]
                                                        [button {:label     "View"
                                                                 :variant   "highlight"
                                                                 :icon-name "arrow"
                                                                 :size      "small"
                                                                 :on-click  #(reset! show-options? (not @show-options?))}])]]
      [:div.multi-select__selections__description (if (false? @show-options?)
                                                    (gstring/format "Your %s selections" input-label)
                                                    (gstring/format "View your %s selections" input-label))]
      (when (false? @show-options?)
        [:div.multi-select__selections__body (for [[label value on-deselect color-tag :as selection] @selections]
                                               [:div
                                                (cond-> {:class    ["multi-select__option--selected"
                                                                    (when color-tag "multi-select__option__color-tag")]
                                                         :style    {}
                                                         :on-click #(do
                                                                      (reset! selections (disj @selections selection))
                                                                      (when on-deselect (on-deselect value)))}
                                                  color-tag
                                                  (assoc-in [:style :border-color] (:color color-tag)))
                                                [:div.multi-select__option__icon--minus
                                                 [icon "minus"]]
                                                label])])]]))

;;==============================================================================
;; Text
;;==============================================================================

(defn text-input
  [{:keys [disabled? error? error-msg focused? id label name on-blur on-change on-focus
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
             value-atom    (assoc :value @value-atom))]
   (when error?
     [:div.input-text__error error-msg])])
