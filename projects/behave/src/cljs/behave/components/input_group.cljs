(ns behave.components.input-group
  (:require [reagent.core            :as r]
            [re-frame.core           :as rf]
            [behave.components.core  :as c]
            [behave.translate        :refer [<t]]
            [dom-utils.interface    :refer [input-float-values input-value]]
            [browser-utils.interface :refer [debounce]]
            [string-utils.interface  :refer [->kebab]]))

(defn upsert-input [ws-uuid group-uuid repeat-id gv-uuid value & [units]]
  (rf/dispatch-sync [:worksheet/add-input-group ws-uuid group-uuid repeat-id])
  (rf/dispatch [:worksheet/upsert-input-variable ws-uuid group-uuid repeat-id gv-uuid value units]))

(defmulti wizard-input (fn [variable _ _] (:variable/kind variable)))

(defmethod wizard-input :continuous [{uuid          :bp/uuid
                                      var-name      :variable/name
                                      var-max       :variable/maximum
                                      var-min       :variable/minimum
                                      native-units  :variable/native-units
                                      english-units :variable/english-units
                                      metric-units  :variable/metric-units
                                      help-key      :group-variable/help-key}
                                     ws-uuid
                                     group-uuid
                                     repeat-id
                                     repeat-group?]
  (let [value                 (rf/subscribe [:worksheet/input ws-uuid group-uuid repeat-id uuid])
        warn-limit?           (true? @(rf/subscribe [:state :warn-continuous-input-limit]))
        acceptable-char-codes (set (map #(.charCodeAt % 0) "0123456789., "))
        on-change             (debounce #'upsert-input 1000)]
    [:div.wizard-input
     [:div.wizard-input__input
      {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
      [c/text-input {:id          (str repeat-id "-" uuid)
                     :label       (if repeat-group? var-name "Values:")
                     :placeholder (when repeat-group? "Values")
                     :value       (first @value)
                     :required?   true
                     :min         var-min
                     :max         var-max

                     :error?       warn-limit?
                     :on-key-press (fn [event]
                                     (when-not (contains? acceptable-char-codes (.-charCode event))
                                       (.preventDefault event)))
                     :on-change    #(on-change ws-uuid
                                               group-uuid
                                               repeat-id
                                               uuid
                                               (input-value %))}]]
     [:div.wizard-input__description
      (str "Units used: " native-units)
      [:div.wizard-input__description__units
       [:div (str "English Units: " english-units)]
       [:div (str "Metric Units: " metric-units)]]]]))

(defmethod wizard-input :discrete [{uuid :bp/uuid
                                    var-name :variable/name
                                    help-key :group-variable/help-key}
                                   ws-uuid
                                   group-uuid
                                   repeat-id
                                   repeat-group?]
  (let [selected  (rf/subscribe [:worksheet/input ws-uuid group-uuid repeat-id uuid])
        on-change #(upsert-input ws-uuid group-uuid repeat-id uuid (input-value %))]
    [:div.wizard-input
     {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [c/radio-group
      {:id      (str repeat-id "-" uuid)
       :label   (when repeat-group? var-name)
       :name    (->kebab var-name)
       :options [{:value "HeadAttack" :label "Head Attack" :on-change on-change :checked? (= (first @selected) "HeadAttack")}
                 {:value "RearAttack" :label "Rear Attack" :on-change on-change :checked? (= (first @selected) "RearAttack")}]}]]))

;; #_(map (fn [{id :db/id value :option/value t-key :option/translation-key}]
;;         {:label     @(<t t-key)
;;          :id        id
;;          :selected  (= @selected value)
;;          :name      var-name
;;          :on-change })
;;       options)

(defmethod wizard-input :text [{uuid     :bp/uuid
                                var-name :variable/name
                                help-key :group-variable/help-key}
                               ws-uuid
                               group-uuid
                               repeat-id
                               repeat-group?]
  (let [value        (rf/subscribe [:worksheet/input ws-uuid group-uuid repeat-id uuid])
        upsert-input (debounce #'upsert-input 1000)]
    [:div.wizard-input
     {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [c/text-input {:id          (str repeat-id "-" uuid)
                    :label       (if repeat-group? var-name "Values:")
                    :placeholder (when repeat-group? "Value")
                    :value       (first @value)
                    :on-change   #(upsert-input ws-uuid group-uuid repeat-id uuid (input-value %))
                    :required?   true}]]))

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

(defn input-group [ws-uuid group variables]
  (r/with-let [variables (sort-by :group-variable/variable-order variables)]
    [:div.wizard-group
     [:div.wizard-group__header (:group/name group)]
     (if (:group/repeat? group)
       [repeat-group ws-uuid group variables]
       [:div.wizard-group__inputs
        (for [variable variables]
          ^{:key (:db/id variable)}
          [wizard-input variable ws-uuid (:bp/uuid group) 0])])]))
