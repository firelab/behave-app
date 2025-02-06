(ns behave.components.input-group
  (:require [behave.components.core          :as c]
            [behave.components.unit-selector :refer [unit-display]]
            [behave.translate                :refer [<t bp]]
            [behave.utils                    :refer [inclusive-range]]
            [clojure.string                  :as str]
            [data-utils.core                 :refer-macros [vmap]]
            [dom-utils.interface             :refer [input-value]]
            [re-frame.core                   :as rf]
            [reagent.core                    :as r]
            [string-utils.interface          :refer [->kebab]]))

;;; Helpers

(defn- upsert-input [ws-uuid group-uuid repeat-id gv-uuid value & [units]]
  (rf/dispatch [:wizard/upsert-input-variable ws-uuid group-uuid repeat-id gv-uuid value units]))

(defn- highlight-help-section [help-key]
  (rf/dispatch [:help/highlight-section help-key]))


;;; Components

(defmulti wizard-input (fn [variable _ _]
                         (if (:group-variable/discrete-multiple? variable)
                           :multi-discrete
                           (:variable/kind variable))))

(defmethod wizard-input nil [variable] (println [:NO-KIND-VAR variable]))

(defmethod wizard-input :continuous [{gv-uuid           :bp/uuid
                                      var-min           :variable/minimum
                                      var-max           :variable/maximum
                                      dimension-uuid    :variable/dimension-uuid
                                      domain-uuid       :variable/domain-uuid
                                      native-unit-uuid  :variable/native-unit-uuid
                                      english-unit-uuid :variable/english-unit-uuid
                                      metric-unit-uuid  :variable/metric-unit-uuid
                                      help-key          :group-variable/help-key}
                                     ws-uuid
                                     group-uuid
                                     repeat-id
                                     repeat-group?]
  (r/with-let [*domain               (rf/subscribe [:vms/entity-from-uuid domain-uuid])
               value                 (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id gv-uuid])
               native-unit-uuid      (or (:domain/native-unit-uuid @*domain) native-unit-uuid)
               *unit-uuid            (rf/subscribe [:worksheet/input-units ws-uuid group-uuid repeat-id gv-uuid])
               warn-limit?           (true? @(rf/subscribe [:state :warn-multi-value-input-limit]))
               acceptable-char-codes (set (map #(.charCodeAt % 0) "0123456789., "))
               on-focus-click        (partial highlight-help-section help-key)
               on-change-units       #(let [new-units-uuid %
                                            old-units-uuid (or @*unit-uuid native-unit-uuid)
                                            value          @value]
                                        (rf/dispatch [:wizard/update-input-units
                                                      (vmap ws-uuid group-uuid repeat-id gv-uuid value new-units-uuid old-units-uuid)]))
               show-range-selector? (rf/subscribe [:wizard/show-range-selector? gv-uuid repeat-id])]
    (let [value-atom         (r/atom @value)
          *outside-range?    (rf/subscribe [:wizard/outside-range? native-unit-uuid @*unit-uuid var-min var-max @value])
          *outside-range-msg (rf/subscribe [:wizard/outside-range-error-msg native-unit-uuid @*unit-uuid var-min var-max])]
      [:div
       [:div.wizard-input
        [:div.wizard-input__input
         {:on-click on-focus-click
          :on-focus on-focus-click}
         [c/text-input {:id           (str repeat-id "-" gv-uuid)
                        :label        (if repeat-group?
                                        @(rf/subscribe [:wizard/gv-uuid->default-variable-name gv-uuid])
                                        "Values:")
                        :placeholder  (when repeat-group? "Values")
                        :value-atom   value-atom
                        :required?    true
                        :error?       (or warn-limit? @*outside-range?)
                        :error-msg    @*outside-range-msg
                        :on-change    #(reset! value-atom (input-value %))
                        :on-key-press (fn [event]
                                        (when-not (contains? acceptable-char-codes (.-charCode event))
                                          (.preventDefault event)))
                        :on-blur      #(upsert-input ws-uuid
                                                     group-uuid
                                                     repeat-id
                                                     gv-uuid
                                                     @value-atom)}]]
        [:div
         {:class [(if @show-range-selector?
                    "wizard-input__range-selector-button--selected"
                    "wizard-input__range-selector-button")]}
         [c/button {:variant  "secondary"
                    :label    @(<t (bp "range_selector"))
                    :on-click #(rf/dispatch [:wizard/toggle-show-range-selector gv-uuid repeat-id])}]]
        [unit-display
         domain-uuid
         @*unit-uuid
         (or (:domain/dimension-uuid @*domain) dimension-uuid)
         (or (:domain/native-unit-uuid @*domain) native-unit-uuid)
         (or (:domain/english-unit-uuid @*domain) english-unit-uuid)
         (or (:domain/metric-unit-uuid @*domain) metric-unit-uuid)
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
                                                          @value-atom]))}]])])))

(defmethod wizard-input :discrete [variable ws-uuid group-uuid repeat-id repeat-group?]
  (r/with-let [{gv-uuid  :bp/uuid
                help-key :group-variable/help-key
                list     :variable/list} variable
               selected                  (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id gv-uuid])
               default-option            (rf/subscribe [:wizard/default-option ws-uuid gv-uuid])
               disabled-options          (rf/subscribe [:wizard/disabled-options ws-uuid gv-uuid])
               on-focus-click            (partial highlight-help-section help-key)
               on-change                 #(upsert-input ws-uuid group-uuid repeat-id gv-uuid (input-value %))
               _                         (when (and (nil? @selected) @default-option)
                                           (upsert-input ws-uuid group-uuid repeat-id gv-uuid @default-option))
               options                   (sort-by :list-option/order (filter #(not (:list-option/hide? %)) (:list/options list)))
               num-options               (count options)
               ->option                  (fn [{value :list-option/value t-key :list-option/translation-key default? :list-option/default}]
                                           {:value     value
                                            :label     @(<t t-key)
                                            :on-change on-change
                                            :selected? (or (= @selected value) (and (nil? @selected) default?))
                                            :disabled? (if @disabled-options
                                                         (@disabled-options value)
                                                         false)
                                            :checked?  (= @selected value)})
               var-name                  @(rf/subscribe [:wizard/gv-uuid->default-variable-name gv-uuid])]
    [:div.wizard-input
     {:on-click on-focus-click
      :on-focus on-focus-click}
     (if (>= 4 num-options)
       [c/radio-group
        {:id      (str repeat-id "-" gv-uuid)
         :label   (when repeat-group? var-name)
         :name    (->kebab var-name)
         :options (doall (map ->option options))}]
       [c/dropdown
        {:id        (str repeat-id "-" gv-uuid)
         :label     (when repeat-group? var-name)
         :on-change on-change
         :name      (->kebab var-name)
         :options   (concat [{:label "Select..." :value "nil"}]
                            (map ->option options))}])]))

(defmethod wizard-input :multi-discrete [variable ws-uuid group-uuid repeat-id _repeat-group?]
  (r/with-let [{gv-uuid  :bp/uuid
                llist    :variable/list
                help-key :group-variable/help-key} variable
               on-focus-click            (partial highlight-help-section help-key)
               options                   (sort-by :list-option/order
                                                  (filter #(not (:list-option/hide? %))
                                                          (:list/options llist)))
               tags-enabled?             (seq? (mapcat :list-option/tags options))
               on-select                 #(rf/dispatch [:worksheet/upsert-multi-select-input
                                                        ws-uuid group-uuid repeat-id gv-uuid %])
               on-deselect               #(rf/dispatch [:worksheet/remove-multi-select-input
                                                        ws-uuid group-uuid repeat-id gv-uuid %])
               ws-input-values           (-> @(rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id gv-uuid])
                                             (str/split ",")
                                             (set))
               ->option                  (fn [{value :list-option/value
                                               tags  :list-option/tags
                                               t-key :list-option/translation-key}]
                                           {:value       value
                                            :label       @(<t t-key)
                                            :tags        (set tags)
                                            :on-select   on-select
                                            :on-deselect on-deselect
                                            :selected?   (contains? ws-input-values value)})]
    [:div.wizard-input
     {:on-click on-focus-click
      :on-focus on-focus-click}
     [c/multi-select-input
      {:input-label   @(rf/subscribe [:wizard/gv-uuid->default-variable-name gv-uuid])
       :tags-enabled? tags-enabled?
       :options       (doall (map ->option options))}]]))

(defmethod wizard-input :text [{gv-uuid  :bp/uuid
                                help-key :group-variable/help-key}
                               ws-uuid
                               group-uuid
                               repeat-id
                               repeat-group?]
  (let [value          (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id gv-uuid])
        on-focus-click (partial highlight-help-section help-key)
        value-atom     (r/atom @value)]
    [:div.wizard-input
     {:on-click on-focus-click
      :on-focus on-focus-click}
     [c/text-input {:id            (str repeat-id "-" gv-uuid)
                    :label         (if repeat-group?
                                     @(rf/subscribe [:wizard/gv-uuid->default-variable-name gv-uuid])
                                     "Values:")
                    :placeholder   (when repeat-group? "Value")
                    :default-value @value
                    :on-change     #(reset! value-atom (input-value %))
                    :on-blur       #(upsert-input ws-uuid group-uuid repeat-id gv-uuid (input-value %))
                    :required?     true}]]))

(defn repeat-group [ws-uuid group variables]
  (let [{group-translation-key :group/translation-key
         group-uuid            :bp/uuid} group
        *repeat-ids                      (rf/subscribe [:worksheet/group-repeat-ids ws-uuid group-uuid])
        next-repeat-id                   (or  (some->> @*repeat-ids seq (apply max) inc)
                                              0)]
    [:<>
     (map-indexed
      (fn [index repeat-id]
        ^{:key repeat-id}
        [:<>
         [:div.wizard-repeat-group
          [:div.wizard-repeat-group__header
           (str @(<t group-translation-key) " #" (inc index))]]
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
  (let [variables (sort-by :group-variable/order variables)]
    [:div.wizard-group
     {:class (str "wizard-group--level-" level)}
     [:div.wizard-group__header
      @(<t (:group/translation-key group))]
     (if (:group/repeat? group)
       [repeat-group ws-uuid group variables]
       [:div.wizard-group__inputs
        (for [variable variables]
          ^{:key (:db/id variable)}
          [wizard-input variable ws-uuid (:bp/uuid group) 0])])]))
