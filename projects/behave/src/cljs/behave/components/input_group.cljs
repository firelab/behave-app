(ns behave.components.input-group
  (:require [reagent.core            :as r]
            [re-frame.core           :as rf]
            [behave.components.core  :as c]
            [dom-utils.interface     :refer [input-value]]
            [string-utils.interface  :refer [->kebab]]
            [map-utils.interface     :refer [index-by]]
            [behave.translate        :refer [<t bp]]))

(defn upsert-input [ws-uuid group-uuid repeat-id gv-uuid value & [units]]
  (rf/dispatch [:worksheet/upsert-input-variable ws-uuid group-uuid repeat-id gv-uuid value units]))

(defn unit-selector [prev-unit-uuid units on-click]
  (r/with-let [*unit-uuid (r/atom prev-unit-uuid)]
    [:div.wizard-input__unit-selector
     [c/dropdown
      {:id            "unit-selector"
       :default-value @*unit-uuid
       :on-change     #(on-click (input-value %))
       :name          "unit-selector"
       :options       (concat [{:label "Select..." :value nil}]
                              (->> units
                                   (map (fn [unit]
                                          {:label (:unit/name unit)
                                           :value (:bp/uuid unit)}))
                                   (sort-by :label)))}]]))

(defn unit-display [*unit-uuid dimension-uuid native-unit-uuid english-unit-uuid metric-unit-uuid on-change-units]
  (r/with-let [dimension      (rf/subscribe [:wizard/dimension+units dimension-uuid])
               units          (:dimension/units @dimension)
               units-by-uuid  (index-by :bp/uuid units)
               native-unit    (get units-by-uuid native-unit-uuid)
               english-unit   (get units-by-uuid english-unit-uuid)
               metric-unit    (get units-by-uuid metric-unit-uuid)
               default-unit   (or native-unit english-unit metric-unit) ;; FIXME: Get from Worksheet settings
               show-selector? (r/atom false)
               on-click       #(do
                                 (on-change-units %)
                                 (reset! show-selector? false))]
    [:div.wizard-input__description
     (str @(<t (bp "units_used")) " " (:unit/short-code (or (get units-by-uuid *unit-uuid) default-unit)))
     [:div.wizard-input__description__units
      (when english-unit
        [:div (str @(<t (bp "english_units")) " " (:unit/short-code english-unit))])
      (when metric-unit
        [:div (str @(<t (bp "metric_units")) " " (:unit/short-code metric-unit))])]

     (when (< 1 (count units))
       [:div.button.button--secondary.button--xsmall
        {:class    (concat ["button" "button--secondary" "button--xsmall"]
                           (when @show-selector? ["button--disabled"]))
         :style    {:display        "inline-block"
                    :margin-top     "5px"
                    :padding-bottom "0px"}
         :on-click #(swap! show-selector? not)
         :disabled @show-selector?}
        @(<t (bp "change_units"))])
     (when @show-selector?
       [unit-selector *unit-uuid units on-click])]))

(defn range-selector
  [*unit-uuid
   dimension-uuid
   native-unit-uuid
   english-unit-uuid
   metric-unit-uuid
   var-min
   var-max
   on-compute]
  (r/with-let [dimension      (rf/subscribe [:wizard/dimension+units dimension-uuid])
               units          (:dimension/units @dimension)
               units-by-uuid  (index-by :bp/uuid units)
               native-unit    (get units-by-uuid native-unit-uuid)
               english-unit  (get units-by-uuid english-unit-uuid)
               metric-unit   (get units-by-uuid metric-unit-uuid)
               default-unit  (or native-unit english-unit metric-unit) ; FIXME: Get from Worksheet settings
               units-used    (:unit/short-code (or (get units-by-uuid *unit-uuid) default-unit))]
    [c/compute {:compute-btn-label "Insert Range"
                :compute-fn        (fn [from to step]
                                     (range from to step))
                :compute-args      [{:name  @(<t (bp "from"))
                                     ;; :units units-used
                                     ;; :range [var-min var-max]
                                     }
                                    {:name  @(<t (bp "to"))
                                     :units units-used
                                     :range [var-min var-max]}
                                    {:name  @(<t (bp "steps"))
                                     :units units-used
                                     :range [var-min var-max]}]
                :on-compute        #(on-compute %)}]))

(defmulti wizard-input (fn [variable _ _] (:variable/kind variable)))

(defmethod wizard-input nil [variable] (println [:NO-KIND-VAR variable]))

(defmethod wizard-input :continuous [{uuid               :bp/uuid
                                      var-name           :variable/name
                                      var-max            :variable/maximum
                                      var-min            :variable/minimum
                                      dimension-uuid     :variable/dimension-uuid
                                      native-unit-uuid  :variable/native-unit-uuid
                                      english-unit-uuid :variable/english-unit-uuid
                                      metric-unit-uuid  :variable/metric-unit-uuid
                                      help-key           :group-variable/help-key}
                                     ws-uuid
                                     group-uuid
                                     repeat-id
                                     repeat-group?]
  (r/with-let [value                 (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id uuid])
               *unit-uuid            (rf/subscribe [:worksheet/input-units ws-uuid group-uuid repeat-id uuid])
               value-atom            (r/atom @value)
               warn-limit?           (true? @(rf/subscribe [:state :warn-multi-value-input-limit]))
               acceptable-char-codes (set (map #(.charCodeAt % 0) "0123456789., "))
               on-change-units       #(rf/dispatch [:worksheet/update-input-units ws-uuid group-uuid repeat-id uuid %])]

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
                                                  uuid
                                                  @value-atom)}]]
     [unit-display
      @*unit-uuid
      dimension-uuid
      native-unit-uuid
      english-unit-uuid
      metric-unit-uuid
      on-change-units]
     [range-selector
      unit-display
      dimension-uuid
      native-unit-uuid
      english-unit-uuid
      metric-unit-uuid
      var-min
      var-max
      #(do
         (reset! value-atom %)
         (upsert-input ws-uuid
                       group-uuid
                       repeat-id
                       uuid
                       @value-atom))]]))

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
