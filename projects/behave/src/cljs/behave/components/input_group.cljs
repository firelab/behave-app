(ns behave.components.input-group
  (:require [reagent.core            :as r]
            [re-frame.core           :as rf]
            [behave.components.core  :as c]
            [dom-utils.interface     :refer [input-value]]
            [string-utils.interface  :refer [->kebab]]
            [behave.translate        :refer [<t bp]]
            [behave.utils            :refer [inclusive-range]]
            [behave.components.unit-selector :refer [unit-display]]))

;;; Helpers

(defn upsert-input [ws-uuid group-uuid repeat-id gv-uuid value & [units]]
  (rf/dispatch [:wizard/upsert-input-variable ws-uuid group-uuid repeat-id gv-uuid value units]))

;;; Components

(defmulti wizard-input (fn [variable _ _] (:variable/kind variable)))

(defmethod wizard-input nil [variable] (println [:NO-KIND-VAR variable]))

(defmethod wizard-input :continuous [{gv-uuid           :bp/uuid
                                      var-name          :variable/name
                                      var-max           :variable/maximum
                                      var-min           :variable/minimum
                                      dimension-uuid    :variable/dimension-uuid
                                      native-unit-uuid  :variable/native-unit-uuid
                                      english-unit-uuid :variable/english-unit-uuid
                                      metric-unit-uuid  :variable/metric-unit-uuid
                                      help-key          :group-variable/help-key}
                                     ws-uuid
                                     group-uuid
                                     repeat-id
                                     repeat-group?]
  (r/with-let [value                 (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id gv-uuid])
               *unit-uuid            (rf/subscribe [:worksheet/input-units ws-uuid group-uuid repeat-id gv-uuid])
               value-atom            (r/atom @value)
               warn-limit?           (true? @(rf/subscribe [:state :warn-multi-value-input-limit]))
               acceptable-char-codes (set (map #(.charCodeAt % 0) "0123456789., "))
               on-change-units       #(rf/dispatch [:wizard/update-input-units ws-uuid group-uuid repeat-id gv-uuid %])
               show-range-selector? (rf/subscribe [:wizard/show-range-selector? gv-uuid repeat-id])]

    [:div
     [:div.wizard-input
     [:div.wizard-input__input
      {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
      [c/text-input {:id           (str repeat-id "-" uuid)
                     :label        (if repeat-group? var-name "Values:")
                     :placeholder  (when repeat-group? "Values")
                     :value-atom   value-atom
                     :required?    true
                     :min          var-min
                     :max          var-max
                     :error?       warn-limit?
                     :on-change    #(reset! value-atom (input-value %))
                     :on-key-press (fn [event]
                                     (when-not (contains? acceptable-char-codes (.-charCode event))
                                       (.preventDefault event)))
                     :on-blur      #(upsert-input ws-uuid
                                                  group-uuid
                                                  repeat-id
                                                  gv-uuid
                                                  @value-atom)}]]
      [:div.wizard-input__range-selector-button
       [c/button {:variant  "secondary"
                  :label    @(<t (bp "range_selector"))
                  :on-click #(rf/dispatch [:wizard/toggle-show-range-selector gv-uuid repeat-id])}]]
     [unit-display
      @*unit-uuid
      dimension-uuid
      native-unit-uuid
      english-unit-uuid
      metric-unit-uuid
      on-change-units]]
     (when @show-range-selector?
       [:div.wizard-input__range-selector
        [c/compute {:compute-btn-label @(<t (bp "insert_range"))
                    :compute-fn        (fn [from to step]
                                         (inclusive-range from to step))
                    :compute-args      [{:name @(<t (bp "from"))}
                                        {:name @(<t (bp "to"))}
                                        {:name @(<t (bp "steps"))}]
                    :on-compute        #(do
                                          (reset! value-atom %)
                                          (rf/dispatch [:wizard/insert-range-input
                                                        ws-uuid
                                                        group-uuid
                                                        repeat-id
                                                        gv-uuid
                                                        @value-atom]))}]])]))

(defmethod wizard-input :discrete [variable ws-uuid group-uuid repeat-id repeat-group?]
  (r/with-let [{uuid     :bp/uuid
                var-name :variable/name
                help-key :group-variable/help-key
                list     :variable/list} variable
               selected                  (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id uuid])
               on-change                 #(upsert-input ws-uuid group-uuid repeat-id uuid (input-value %))
               options                   (sort-by :list-option/order (filter #(not (:list-option/hide? %)) (:list/options list)))
               num-options               (count options)
               ->option                  (fn [{value :list-option/value name :list-option/name default? :list-option/default}]
                                           {:value     value
                                            :label     name
                                            :on-change on-change
                                            :selected? (or (= @selected value) (and (nil? @selected) default?))
                                            :checked?  (= @selected value)})]
    [:div.wizard-input
     {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     (if (>= 4 num-options)
       [c/radio-group
        {:id      (str repeat-id "-" uuid)
         :label   (when repeat-group? var-name)
         :name    (->kebab var-name)
         :options (doall (map ->option options))}]
       [c/dropdown
        {:id        (str repeat-id "-" uuid)
         :label     (when repeat-group? var-name)
         :on-change on-change
         :name      (->kebab var-name)
         :options   (concat [{:label "Select..." :value "nil"}]
                            (map ->option options))}])]))

(defmethod wizard-input :text [{uuid     :bp/uuid
                                var-name :variable/name
                                help-key :group-variable/help-key}
                               ws-uuid
                               group-uuid
                               repeat-id
                               repeat-group?]
  (let [value      (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id uuid])
        value-atom (r/atom @value)]
    [:div.wizard-input
     {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [c/text-input {:id            (str repeat-id "-" uuid)
                    :label         (if repeat-group? var-name "Values:")
                    :placeholder   (when repeat-group? "Value")
                    :default-value (first @value)
                    :on-change     #(reset! value-atom (input-value %))
                    :on-blur       #(upsert-input ws-uuid group-uuid repeat-id uuid (input-value %))
                    :required?     true}]]))

(defn repeat-group [ws-uuid group variables]
  (let [{group-name :group/name
         group-uuid :bp/uuid} group
        *repeat-ids           (rf/subscribe [:worksheet/group-repeat-ids ws-uuid group-uuid])
        next-repeat-id        (or  (some->> @*repeat-ids seq (apply max) inc)
                                   0)]
    [:<>
     (map-indexed
       (fn [index repeat-id]
         ^{:key repeat-id}
         [:<>
          [:div.wizard-repeat-group
           [:div.wizard-repeat-group__header
            (str group-name " #" (inc index))]]
          [:div.wizard-group__inputs
           (for [variable variables]
             ^{:key (:db/id variable)}
             [wizard-input variable ws-uuid group-uuid repeat-id true])]])
       @*repeat-ids)
     [:div {:style {:display         "flex"
                    :padding         "20px"
                    :align-items     "center"
                    :justify-content "center"}}
      [c/button {:variant  "primary"
                 :label    "Add Resource"
                 :on-click #(rf/dispatch [:worksheet/add-input-group ws-uuid group-uuid next-repeat-id])}]]]))

(defn input-group [ws-uuid group variables level]
  [:div.wizard-group
   {:class (str "wizard-group--level-" level)}
   [:div.wizard-group__header (:group/name group)]
   (if (:group/repeat? group)
     [repeat-group ws-uuid group variables]
     [:div.wizard-group__inputs
      (for [variable variables]
        ^{:key (:db/id variable)}
        [wizard-input variable ws-uuid (:bp/uuid group) 0])])])
