(ns behave-cms.components.common
  (:require [clojure.string         :as str]
            [herb.core              :refer [<class]]
            [reagent.core           :as r]
            [re-frame.core          :as rf]
            [string-utils.interface :refer [->str ->kebab]]
            [behave-cms.styles      :as $]
            [behave-cms.utils       :as u]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UI Styles
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- $labeled-input []
  {:display        "flex"
   :flex           1
   :flex-direction "column"
   :padding-bottom ".5rem"
   :width          "100%"})

(defn- $radio [checked?]
  (merge
   (when checked? {:background-color ($/color-picker :border-color 0.6)})
   {:border        "2px solid"
    :border-color  ($/color-picker :border-color)
    :border-radius "100%"
    :height        "1rem"
    :margin-right  ".4rem"
    :width         "1rem"}))

(defn- $accordion [expanded?]
  (merge
    ^{:combinators {[:> :.accordion__content] {:max-height (if expanded? "auto" "0px")
                                          :visibility (if expanded? "visible" "hidden")
                                          :transition "max-height 300ms ease-in-out"}}}
    {:transition "max-height 300ms ease-in-out"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UI Components
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn accordion [title & content]
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
      [:option {:key value :value value :selected (= value selected)} label])]])

(defn radio-buttons
  "A component for radio button."
  [group-label state options]
  [:div.mb-3
   [:label.form-label group-label]
   (for [{:keys [label value]} options]
     ^{:key value}
     [:div.form-check
      [:input.form-check-input
       {:type      "radio"
        :name      (->kebab group-label)
        :id        value
        :value     value
        :on-change #(reset! state (u/input-value %))}]
      [:label.form-check-label {:for value} label]])])

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
  [:<>
   (for [{:keys [label value]} options]
     ^{:key value}
     [:div.form-check
      [:input.form-check-input
       {:type      "checkbox"
        :id        value
        :value     value
        :on-change on-change}]
      [:label.form-check-label {:for value} label]])])
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

(defn btn [style label action & [{:keys [icon]}]]
  [:button {:class    ["btn" (when style (str "btn-" (name style))) "mx-1"]
            :on-click action}
   (when icon
     [:span {:class ["fa-solid" (str "fa-" icon)]}])
   label])

(defn btn-sm [style label action & [{:keys [icon]}]]
  [:button {:class    ["btn" "btn-sm" (when style (str "btn-" (name style))) "mx-1"]
            :on-click action}
   (when icon
     [:span {:class ["fa-solid" (str "fa-" icon)]}])
   (when label
     [:span {:class [(when icon "ms-2")]} label])])

(defn simple-table [columns rows & [{:keys [on-select on-delete on-increase on-decrease]}]]
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

(defn window [sidebar-width & children]
  [:div.window {:style {:position "fixed"
                        :top      "50px"
                        :left     sidebar-width
                        :right    "0px"
                        :bottom   "0px"
                        :overflow "auto"}}
   children])
