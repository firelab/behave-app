(ns behave-cms.components.common
  (:require [clojure.string         :as str]
            [herb.core              :refer [<class]]
            [reagent.core           :as r]
            [re-frame.core          :as rf]
            [string-utils.interface :refer [->str ->kebab]]
            [behave-cms.styles      :as $]
            [behave-cms.utils       :as u]
            [nano-id.core           :refer [nano-id]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UI Styles
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- $accordion [expanded?]
  (merge
   ^{:combinators {[:> :.accordion__content] {:max-height (if expanded? "auto" "0px")
                                              :visibility (if expanded? "visible" "hidden")
                                              :transition "max-height 300ms ease-in-out"}}}
   {:transition "max-height 300ms ease-in-out"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UI Components
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn accordion
  "A component for an accordion."
  [title & content]
  (r/with-let [expanded? (r/atom false)]
    ^{:key title}
    [:div.row.accordion {:class (<class $accordion @expanded?)}
     [:div.accordion__title  {:style {:display "flex" :flex-direction "row" :width "100%" :justify-content "space-between"}}
      [:h4 title]
      [:button.btn.btn-sm.btn-outline-secondary {:on-click #(swap! expanded? not)} (if @expanded? "Collapse" "Expand")]]
     (when @expanded?
       [:div.row.accordion__content content])]))

(defn dropdown
  "A component for dropdowns."
  [{:keys [label on-select options disabled? multiple? selected]}]
  (let [selected  (if (u/atom? selected) @selected selected)
        selected? (if (or multiple? (coll? selected))
                    #(not (nil? ((set selected) %)))
                    #(= % (if (keyword? selected) (name selected) selected)))]
    [:div.mb-3
     [:label.form-label label]
     [:select.form-select
      {:on-change on-select
       :disabled  disabled?
       :multiple  multiple?}
      (when-not multiple?
        [:option
         {:key "none" :value nil :selected (when-not selected true)}
         (str "Select " label "...")])
      (for [{:keys [value label]} options]
        [:option {:key value :value value :selected (selected? value)} label])]]))

(defn radio-buttons
  "A component for radio button."
  [group-label state options & [on-change]]
  (let [on-change (if (fn? on-change) on-change #(reset! state (u/input-value %)))
        state     (if (u/atom? state) @state state)
        id        (nano-id)
        options   (map #(assoc % :opt-id (str "-" (nano-id))) options)]
    [:fieldset.mb-3
     {:id id}
     [:label.form-label group-label]
     (for [{:keys [label value opt-id]} options]
       ^{:key value}
       [:div.form-check
        [:input.form-check-input
         {:type      "radio"
          :name      id
          :checked   (= state value)
          :id        opt-id
          :value     value
          :on-change on-change}]
        [:label.form-check-label {:for opt-id} label]])]))

(defn checkbox
  "A component for check box."
  [label-text checked? on-change]
  (let [id (->kebab label-text)]
    [:span {:style {:margin-bottom ".5rem"}}
     [:input.form-check-input
      {:style     {:margin-right ".25rem"}
       :id        id
       :type      "checkbox"
       :checked   checked?
       :on-change on-change}]
     [:label.form-check-label
      {:for id}
      label-text]]))

(defn checkboxes
  "Multiple check boxes."
  [options state on-change]
  (let [state (set (if (u/atom? state) @state state))
        id    (nano-id)]
    [:fieldset
     {:id id}
     (for [{:keys [label value]} options]
       ^{:key value}
       [:div.form-check
        [:input.form-check-input
         {:type      "checkbox"
          :name      id
          :id        value
          :value     value
          :checked   (some? (state value))
          :on-change on-change}]
        [:label.form-check-label {:for value} label]])]))

;; checkbox label (= value @state) on-change]))

(defn labeled-input
  "Input and label pair component. Takes as `opts`:
   - `:type`         - Input type (e.g. `\"text\"`)
   - `:on-change`    - On change event handler.
   - `:disabled?`    - Disable input.
   - `:autofocus?`   - Autofocus input.
   - `:required?`    - Whether input is required.
   - `:zero-margin?` - Enable zero margin."
  [label state & {:keys [type autocomplete disabled? on-change autofocus? required? placeholder zero-margin?]
                  :as   opts
                  :or   {type "text" disabled? false on-change #(reset! state (u/input-value %)) required? false}}]
  (let [state (if (u/atom? state) @state state)
        id    (nano-id)]
    [:div
     {:class [(if zero-margin? "" "my-3")]}
     [:label.form-label {:for id} label]
     [:input.form-control
      {:auto-complete autocomplete
       :auto-focus    autofocus?
       :disabled      disabled?
       :required      required?
       :placeholder   placeholder
       :id            id
       :type          type
       :value         state
       :on-change     on-change}]]))

(defn labeled-float-input
  "Float input."
  [label state on-change & [opts]]
  (labeled-input
   label
   state
   (merge opts {:text "number" :on-change #(on-change (u/input-float-value %))})))

(defn labeled-integer-input
  "Integer input."
  [label state on-change & [opts]]
  (labeled-input
   label
   state
   (merge opts {:text "number" :on-change #(on-change (u/input-int-value %))})))

(defn labeled-text-input
  "Text input."
  [label state on-change & [opts]]
  (labeled-input
   label
   state
   (merge opts {:type "text" :on-change #(on-change (u/input-value %))})))

(defn labeled-file-input
  "File input."
  [label state on-change & [opts]]
  (labeled-input
   label
   state
   (merge opts {:type "file" :on-change #(on-change (u/input-file %))})))

(defn limited-date-picker
  "Creates a date input with limited dates."
  [label id value days-before days-after]
  (let [today-ms (u/current-date-ms)
        day-ms   86400000]
    [:div {:style {:display "flex" :flex-direction "column"}}
     [:label {:for   id
              :style {:font-size "0.9rem" :font-weight "bold"}}
      label]
     [:select {:id        id
               :on-change #(reset! value (u/input-int-value %))
               :value     @value}
      (for [day (range (* -1 days-before) (+ 1 days-after))]
        (let [date-ms (+ today-ms (* day day-ms))
              date    (u/format-date (js/Date. date-ms))]
          [:option {:key   date
                    :value date-ms}
           date]))]]))

(defn input-hour
  "Simple 24-hour input component. Shows the hour with local timezone (e.g. 13:00 PDT)"
  [label id value]
  (let [timezone (u/current-timezone-shortcode)]
    [:div {:style {:display "flex" :flex-direction "column"}}
     [:label {:for   id
              :style {:font-size "0.9rem" :font-weight "bold"}}
      label]
     [:select {:id        id
               :on-change #(reset! value (u/input-int-value %))
               :value     @value}
      (for [hour (range 0 24)]
        [:option {:key   hour
                  :value hour}
         (str hour ":00 " timezone)])]]))

(defn simple-form
  "Simple form component. Adds input fields, an input button, and optionally a footer."
  ([title button-text fields on-click]
   (simple-form title button-text fields on-click nil))
  ([title button-text fields on-click footer]
   [:form {:style     {:height "fit-content"}
           :on-submit #(do (.preventDefault %) (.stopPropagation %) (on-click %))}
    [:div {:style ($/action-box)}
     [:div {:style ($/action-header)}
      [:label {:style ($/padding "1px" :l)} title]]
     [:div {:style ($/combine {:overflow "auto"})}
      [:div
       [:div {:style ($/combine [$/margin "1.5rem"])}
        (doall (map-indexed (fn [i [label state type autocomplete]]
                              ^{:key i} [labeled-input label state {:autocomplete autocomplete
                                                                    :type         type
                                                                    :autofocus?   (= 0 i)
                                                                    :required?    true}])
                            fields))
        [:input {:class (<class $/p-form-button)
                 :style ($/combine ($/align :block :right) {:margin-top ".5rem"})
                 :type  "submit"
                 :value button-text}]
        (when footer (footer))]]]]]))

(defn btn
  "Bootstrap button. Can be passed:
   - `style`  - Button style (e.g. `\"primary\"`)
   - `label`  - Button label
   - `action` - Fn to call when button is pressed.
   - `opts`   - Optional hashmap.

  `opts` can take:
  - `size` - Button size (e.g. `:sm`/`:md`/`:lg`)
  - `icon` - Font Awesome icon name to display to the left of the label."
  [style label action & [{:keys [icon size]}]]
  [:button {:class    ["btn"
                       (when style (str "btn-" (name style)))
                       (when size (str "btn-" (name size)))
                       "mx-1"]
            :on-click action}
   (when icon
     [:span {:class ["fa-solid" (str "fa-" icon)]}])
   label])

(defn btn-sm
  "Small Bootstrap button. See ``behave-cms.components.common/btn``."
  [style label action & [opts]]
  [btn style label action (merge opts {:size :sm})])

(defn simple-table
  "Produces a simple table with:
  - `columns` - A vector of attribute keywords (e.g. `:entity/name`)
  - `rows`    - A vector of hash-maps with matching attribute keywords
  - `opt-fns` - An optional hash-map of functions

  `opt-fns` can contain the following K/V pairs:
  - `:on-select`   - Fn called a table row is selected.
  - `:on-delete`   - Fn called a table row is deleted.
  - `:on-increase` - Fn called a table row position is increased.
  - `:on-decrease` - Fn called a table row position is decreased."
  [columns rows & [{:keys [on-select on-delete on-increase on-decrease caption add-entity-fn]}]]
  [:div {:style {:width   "100%"
                 :height  "100%"}}
   [:div.table-header {:style {:display         "flex"
                               :flex-direction  "row"
                               :align-items     "center"
                               :justify-content "space-between"
                               :border-bottom   "3px solid black"
                               :padding "5px"}}
    [:div caption]
    [:div [btn-sm
           :primary
           "Add Entry"
           add-entity-fn]]]
   [:div.table-wrapper {:style {:height           "100%"
                                :overflow-y       "auto"
                                :scroll-snap-type "y mandatory"}}
    [:table.table.table-hover
     {:style {:border-collapse "collapse"
              :width           "100%"}}
     [:thead
      {:style {:background-color "white"
               :position         "sticky"
               :top              0}}
      [:tr
       {:style {:scroll-snap-align "start"
                :width             "100%"}}
       (for [column (map #(-> %
                              (name)
                              (str/replace #"[_-]" " ")
                              (str/replace #"uuid" "")
                              (str/capitalize))
                         columns)]
         [:th {:key column}
          column])
       (when (or on-select on-delete) [:th {:style {:whitespace "nowrap"}} "Modify"])
       (when (or on-increase on-decrease) [:th {:style {:whitespace "nowrap"}} "Reorder"])]]

     [:tbody
      {:style {
               ;; :display      "table"
               :overflow-y   "auto"
               :width        "100%"
               :table-layout "fixed"}}
      (for [row rows]
        ^{:key (:db/id row)}
        [:tr {:style {:scroll-snap-align "start"}}
         (for [column columns
               :let   [value (get row column "")]]
           [:td {:key   column
                 :style {:word-break "break-all"}}
            (if-let [nname @(rf/subscribe [:entity-uuid->name value])]
              nname
              (->str value))])
         (when (or on-select on-delete)
           [:td {:key "modify" :class "td" :style {:white-space "nowrap"}}
            (when on-select [btn-sm :outline-secondary "Edit"   #(on-select row)])
            (when on-delete [btn-sm :outline-danger    "Delete" #(on-delete row)])])
         (when (and on-decrease on-increase)
           [:td
            {:key "order"}
            [btn-sm :outline-secondary nil #(on-decrease row) {:icon "arrow-up"}]
            [btn-sm :outline-secondary nil #(on-increase row) {:icon "arrow-down"}]])])]]]])

(defn window
  "Window container to ensure a fixed window."
  [sidebar-width & children]
  [:div.window {:style {:position "fixed"
                        :top      "50px"
                        :left     sidebar-width
                        :right    "0px"
                        :bottom   "0px"
                        :overflow "auto"}}
   children])
