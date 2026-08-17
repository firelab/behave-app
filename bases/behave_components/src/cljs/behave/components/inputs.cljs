(ns behave.components.inputs
  (:require
   [behave.components.a11y      :refer [on-space on-enter]]
   [behave.components.button    :refer [button]]
   [behave.components.icon.core :refer [icon]]
   [clojure.string              :as str]
   [goog.string                 :as gstring]
   [reagent.core                :as r]))

;;==============================================================================
;; Checkbox
;;==============================================================================

#_{:clj-kondo/ignore [:shadowed-var]}
(defn checkbox [{:keys [label id name on-change checked? disabled? error?]}]
  [:label {:class ["input-checkbox"
                   (when checked? "input-checkbox--checked")
                   (when disabled? "input-checkbox--disabled")
                   (when error? "input-checkbox--error")]
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

#_{:clj-kondo/ignore [:shadowed-var]}
(defn number-input [{:keys [label id name on-change on-blur disabled? error? error-msg min max value value-atom default-value step placeholder]}]
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
      step          (assoc :step step)
      placeholder   (assoc :placeholder placeholder)
      value         (assoc :value value)
      value-atom    (assoc :value @value-atom)
      default-value (assoc :default-value default-value))]
   (when error?
     [:div.input-number__error error-msg])])

;;==============================================================================
;; Range
;;==============================================================================

#_{:clj-kondo/ignore [:shadowed-var]}
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

#_{:clj-kondo/ignore [:shadowed-var]}
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

(defn toggle [{:keys [label left-label right-label checked? on-change disabled?]}]
  (let [measure-text     (fn [text]
                           (when text
                             (let [span (.createElement js/document "span")]
                               (set! (.-className span) "input-toggle__slider__text")
                               (set! (.-style.-position span) "absolute")
                               (set! (.-style.-visibility span) "hidden")
                               (set! (.-textContent span) (str text))
                               (.appendChild (.-body js/document) span)
                               (let [width (.-offsetWidth span)]
                                 (.removeChild (.-body js/document) span)
                                 width))))
        max-text-width   (max (or (measure-text left-label) 0)
                              (or (measure-text right-label) 0))
        base-width       32
        padding          16
        dynamic-width    (+ base-width max-text-width padding)
        slider-translate (- dynamic-width 32)]
    [:div {:class ["input-toggle"
                   (when disabled? "input-toggle--disabled")]}
     (when label
       [:label {:class "input-toggle__label"} label])
     [:div {:class "input-toggle__container"}
      [:label {:class "input-toggle__switch"
               :style {:width               (str dynamic-width "px")
                       "--slider-translate" (str slider-translate "px")}}
       [:input {:type      "checkbox"
                :checked   checked?
                :disabled  disabled?
                :on-change on-change
                :class     "input-toggle__input"}]
       [:span {:class "input-toggle__slider"}
        [:span {:class "input-toggle__slider__text"}
         (if checked? right-label left-label)]]]]]))

;;==============================================================================
;; Dropdown
;;==============================================================================

(defn- option [{:keys [label value selected? disabled?]}]
  [:option
   {:class    "input-dropdown__option"
    :disabled disabled?
    :selected selected?
    :value    value} label])

(defn- option-group [label]
  [:optgroup {:key label :class "input-dropdown__option-group" :label label}])

#_{:clj-kondo/ignore [:shadowed-var]}
(defn dropdown [{:keys [label id name value on-change disabled? error? options]}]
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
      :value     value
      :on-change on-change}
     (for [{:keys [group] :as opt} options]
       (if (some? group)
         ^{:key group}
         [option-group group (for [o (:options opt)] [option o])]
         ^{:key (:value opt)}
         [option opt]))]]])

;;==============================================================================
;; Text
;;==============================================================================

#_{:clj-kondo/ignore [:shadowed-var]}
(defn text-input
  [{:keys [disabled? error? error-msg focused? id label name on-blur on-change on-focus
           placeholder value value-atom default-value on-key-press background font-color]}]
  [:div {:class ["input-text"
                 (when error? "input-text--error")
                 (when disabled? "input-text--disabled")
                 (when focused? "input-text--focused")]}
   [:label {:class "input-text__label" :for id} label]
   [:input (cond-> {:class        "input-text__input"
                    :disabled     disabled?
                    :id           id
                    :name         name
                    :on-blur      on-blur
                    :on-key-press on-key-press
                    :on-focus     on-focus
                    :placeholder  placeholder
                    :type         "text"}
             background    (assoc :style {:background background})
             font-color    (assoc-in [:style :color] font-color)
             on-change     (assoc :on-change on-change)
             default-value (assoc :default-value default-value)
             value         (assoc :value value)
             value-atom    (assoc :value @value-atom))]
   (when error?
     [:div.input-text__error error-msg])])

;;==============================================================================
;; Multi Select
;;==============================================================================

(defn on-matching-keys [keycodes f]
  (fn [e]
    (when (keycodes (.-charCode e))
      (f))))

(defn on-space-enter [f]
  (on-matching-keys #{13 32} f))

(defn input-value
  "Returns the value property of the target property of an event."
  [event]
  (-> event .-target .-value))

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

(def ^:private empty-selections
  "Empty set of selected options, ordered by label."
  (sorted-set-by #(compare (:label %1) (:label %2))))

(defn- multi-select-on-select
  "Toggles `opt` in `selections-atom`, invoking its `on-select`/`on-deselect`.
  With `disable-multi-valued-input?`, selecting deselects all other options."
  [selections-atom {:keys [value on-select on-deselect] :as opt} disable-multi-valued-input?]
  (if (contains? @selections-atom opt)
    (do
      (swap! selections-atom disj opt)
      (when on-deselect (on-deselect value)))
    (do
      (when disable-multi-valued-input?
        (doseq [{:keys [value on-deselect]} @selections-atom]
          (when on-deselect (on-deselect value)))
        (reset! selections-atom empty-selections))
      (when on-select (on-select value))
      (swap! selections-atom conj opt))))

(defn- filter-tag-buttons
  "Tag buttons that narrow the options list. A clicked tag stays active
  until another tag is clicked or the filter is cleared."
  [filter-tags selected-tag]
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
               :on-click  #(reset! selected-tag id)}]])
   (when @selected-tag
     [:div.multi-select__tags__tag
      [button {:label    "Clear"
               :variant  "transparent-secondary"
               :size     "small"
               :on-click #(reset! selected-tag nil)}]])])

(defn- matches-search?
  "Whether an option's label contains `search`, case-insensitively."
  [search {:keys [label]}]
  (str/includes? (str/lower-case label) (str/lower-case search)))

(defn- no-results-message []
  [:div.multi-select__no-results "No Results"])

(defn- selected-option-chip
  "Chip for a selected option; clicking it deselects the option."
  [{:keys [label color-tag on-click]}]
  [:div (cond-> {:class    ["multi-select__option--selected"
                            (when color-tag "multi-select__option__color-tag")]
                 :style    {}
                 :on-click on-click}
          color-tag
          (assoc-in [:style :border-color] (:color color-tag)))
   [:div.multi-select__option__icon--minus
    [icon "minus"]]
   label])

(defn multi-select-input
  "Creates a multi-select input component with the following options:
  - `input-label`: text to display as the label for the input.
  - `prompt1`: text to display instructions
  - `expand-options-button-label`: text to display on the button to expand options
  - `collapse-options-button-label`: text to display on the button that collapses options
  - `options`: vector of maps with keys:
     - `:selected?` - whether the option has been selected
     - `:label`     - label of the option
     - `:value`     - value of the option
     - `:tags`      - a collection of keywords or numbers
     - `:color-tag` - a map with at least the key `:color` representing a hex color.
  - [Optional] `prompt2`: description shown while options are collapsed
  - [Optional] `prompt3`: description shown while options are expanded
  - [Optional] `filter-tags`: vector of maps with keys:
     - `:label` - label of the tag
     - `:id`    - id of the tag (keyword or number)
  - [Optional] `color-tags`:
     - `:label` - label of the option
     - `:color` - color of the tag"
  [{:keys [input-label prompt1 prompt2 prompt3 expand-options-button-label
           collapse-options-button-label options filter-tags color-tags
           disable-multi-valued-input?]}]
  (r/with-let [selections    (r/atom (into empty-selections
                                           (filter #(true? (:selected? %)) options)))
               show-options? (r/atom false)
               selected-tag  (r/atom nil)
               search        (r/atom "")]
    (let [visible-options (cond->> options
                            (and filter-tags @selected-tag)
                            (filter #(contains? (:tags %) @selected-tag))

                            (seq @search)
                            (filter (partial matches-search? @search)))
          select-option!  (fn [opt]
                            (multi-select-on-select selections opt disable-multi-valued-input?)
                            (reset! search ""))]
      [:div.multi-select
       [:div.multi-select__prompt prompt1]
       (when (seq color-tags)
         [:div.multi-select__color-tags
          (for [{:keys [label color]} color-tags]
            ^{:key color}
            [:div {:class "multi-select__color-tags__tag"
                   :style {:border-color color}}
             label])])
       [:div.multi-select__selections
        [:div.multi-select__selections__header
         [:div (gstring/format "Selected %s" input-label)]
         [:div.multi-select__selections__header__button
          (if @show-options?
            [button {:label     collapse-options-button-label
                     :variant   "secondary"
                     :icon-name "minus"
                     :size      "small"
                     :on-click  #(do (reset! show-options? false)
                                     (reset! search ""))}]
            [button {:label     expand-options-button-label
                     :variant   "primary"
                     :icon-name "plus"
                     :size      "small"
                     :on-click  #(reset! show-options? true)}])]]
        (when-let [description (if @show-options? prompt3 prompt2)]
          [:div.multi-select__selections__description description])
        [:div.multi-select__selections__body
         (doall
          (for [{:keys [label color-tag] :as opt} @selections]
            ^{:key label}
            [selected-option-chip {:label     label
                                   :color-tag color-tag
                                   :on-click  #(select-option! opt)}]))]
        [text-input {:label        "Search"
                     :on-key-press (on-enter #(when-let [opt (first visible-options)]
                                                (select-option! opt)))
                     :on-change    #(reset! search (input-value %))
                     :value-atom   search}]]
       (when (and @show-options? filter-tags)
         [filter-tag-buttons filter-tags selected-tag])
       (when (and (seq @search) (empty? visible-options))
         [no-results-message])
       (when (or (seq @search) @show-options?)
         ;; --searching enables the first-option highlight that previews
         ;; what pressing Enter will select
         [:div {:class ["multi-select__options"
                        (when (seq @search) "multi-select__options--searching")]}
          (doall
           (for [{:keys [label color-tag] :as opt} visible-options]
             ^{:key label}
             [multi-select-option {:selected? (contains? @selections opt)
                                   :color-tag color-tag
                                   :label     label
                                   :on-click  #(select-option! opt)}]))])])))