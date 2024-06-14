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
     [:div.row.accordion__content content]]))

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
        id        (nano-id)]
    [:fieldset.mb-3
     {:id id}
     [:label.form-label group-label]
     (for [{:keys [label value]} options]
       ^{:key value}
       [:div.form-check
        [:input.form-check-input
         {:type      "radio"
          :name      id
          :checked   (= state value)
          :id        value
          :value     value
          :on-change on-change}]
        [:label.form-check-label {:for value} label]])]))

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
  "Input and label pair component. Takes as `opts`
  - type
  - call-back
  - disabled?
  - autofocus?
  - required?"
  [label state & [{:keys [type autocomplete disabled? call-back autofocus? required? placeholder]
                   :or {type "text" disabled? false call-back #(reset! state (u/input-value %)) required? false}}]]
  [:div.my-3
   [:label.form-label {:for (->kebab label)} label]
   [:input.form-control
    {:auto-complete autocomplete
     :auto-focus    autofocus?
     :disabled      disabled?
     :required      required?
     :placeholder   placeholder
     :id            (->kebab label)
     :type          type
     :value         @state
     :on-change     call-back}]])

(defn labeled-float-input
  [label state call-back & [opts]]
  (apply labeled-input
         label
         state
         (merge opts {:text "number" :call-back #(call-back (u/input-float-value %))})))

(defn labeled-integer-input
  [label state call-back & [opts]]
  (apply labeled-input
         label
         state
         (merge opts {:text "number" :call-back #(call-back (u/input-int-value %))})))

(defn labeled-text-input
  [label state call-back & [opts]]
  (apply labeled-input
         label
         state
         (merge opts {:type "text" :call-back #(call-back (u/input-value %))})))

(defn labeled-file-input
  [label state call-back & [opts]]
  (apply labeled-input
         label
         state
         (merge opts {:type "file" :call-back #(call-back (u/input-file %))})))

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
  [style label action & opts]
  [btn style label action (merge opts {:size :sm})])

(defn simple-table
  "Produces a simple table with:
  - `columns` - A vector of attribute keywords (e.g. `:entity/name`)
  - `rows`    - A vector of hash-maps with matching attribute keywords
  - `opt-fns` - An optional hash-map of functions

  `opt-fns` can contain the following K/V pairs:
  - `:on-select`   - Fn callled a table row is selected.
  - `:on-delete`   - Fn callled a table row is deleted.
  - `:on-decrease` - Fn callled a table row position is increased.
  - `:on-decrease` - Fn callled a table row position is decreased."
  [columns rows & [{:keys [on-select on-delete on-increase on-decrease]}]]
  [:table.table.table-hover
   [:thead
    [:tr
     (for [column (map #(-> %
                            (name)
                            (str/replace #"[_-]" " ")
                            (str/replace #"uuid" "")
                            (str/capitalize))
                       columns)]
       [:th {:key column} column])
     (when (or on-select on-delete) [:th {:style {:whitespace "nowrap"}} "Modify"])
     (when (or on-increase on-decrease) [:th "Reorder"])]]

   [:tbody
    (for [row rows]
      ^{:key (:db/id row)}
      [:tr
       (for [column columns
             :let [value (get row column "")]]
         [:td {:key column} (if-let [nname @(rf/subscribe [:entity-uuid->name value])]
                              nname
                              (->str value))])
       (when (or on-select on-delete)
         [:td {:key "modify" :class "td" :style {:white-space "nowrap"}}
          (when on-select [btn-sm :outline-secondary "Edit"   #(on-select row)])
          (when on-delete [btn-sm :outline-danger    "Delete" #(on-delete row)])])
       [:td
        {:key "order"}
        (when on-decrease [btn-sm :outline-secondary nil #(on-decrease row) {:icon "arrow-up"}])
        (when on-increase [btn-sm :outline-secondary nil #(on-increase row) {:icon "arrow-down"}])]])]])

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
