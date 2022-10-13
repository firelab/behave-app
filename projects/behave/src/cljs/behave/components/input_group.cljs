(ns behave.components.input-group
  (:require [reagent.core            :as r]
            [re-frame.core           :as rf]
            [behave.components.core  :as c]
            [behave.translate        :refer [<t]]
            [dom-utils.interface     :refer [input-value]]
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
                                      metric-units  :variable/metric-units}
                                     ws-uuid
                                     group-uuid
                                     repeat-id
                                     repeat-group?]
  (let [value (rf/subscribe [:worksheet/input ws-uuid group-uuid repeat-id uuid])
        upsert-input (debounce #'upsert-input 1000)]
    [:div.wizard-input
     [:div.wizard-input__input
      [c/text-input {:label       (if repeat-group? var-name "Values:")
                     :placeholder (when repeat-group? "Values")
                     :id          (->kebab var-name)
                     :value       (first @value)
                     :required?   true
                     :min         var-min
                     :max         var-max
                     :on-change   #(upsert-input ws-uuid group-uuid repeat-id uuid (input-value %))}]]
     [:div.wizard-input__description
      (str "Units used: " native-units)
      [:div.wizard-input__description__units
       [:div (str "English Units: " english-units)]
       [:div (str "Metric Units: " metric-units)]]]]))

(defmethod wizard-input :discrete [{uuid :bp/uuid var-name :variable/name}
                                   ws-uuid
                                   group-uuid
                                   repeat-id
                                   repeat-group?]
  (let [selected  (rf/subscribe [:worksheet/input ws-uuid group-uuid repeat-id uuid])
        on-change #(upsert-input ws-uuid group-uuid repeat-id uuid (input-value %))]
    [:div.wizard-input
     [c/radio-group
      {:label   (when repeat-group? var-name)
       :name    (->kebab var-name)
       :options [{:value "HeadAttack" :label "Head Attack" :on-change on-change :checked? (= (first @selected) "HeadAttack")}
                 {:value "RearAttack" :label "Rear Attack" :on-change on-change :checked? (= (first @selected) "RearAttack")}]}]]))

;#_(map (fn [{id :db/id value :option/value t-key :option/translation-key}]
;         {:label     @(<t t-key)
;          :id        id
;          :selected  (= @selected value)
;          :name      var-name
;          :on-change })
;       options)

(defmethod wizard-input :text [{uuid :bp/uuid var-name :variable/name}
                               ws-uuid
                               group-uuid
                               repeat-id
                               repeat-group?]
  (let [value        (rf/subscribe [:worksheet/input ws-uuid group-uuid repeat-id uuid])
        upsert-input (debounce #'upsert-input 1000)]
    [:div.wizard-input
     [c/text-input {:label       (if repeat-group? var-name "Values:")
                    :placeholder (when repeat-group? "Value")
                    :id          (->kebab var-name)
                    :value       (first @value)
                    :on-change   #(upsert-input ws-uuid group-uuid repeat-id uuid (input-value %))
                    :required?   true}]]))

(defn repeat-group [ws-uuid group variables]
  (let [{group-name :group/name group-uuid :bp/uuid} group
        repeats (rf/subscribe [:worksheet/repeat-groups ws-uuid group-uuid])]
    [:<>
     (for [repeat-id (range (or (count @repeats) 0))]
       ^{:key repeat-id}
       [:<> 
        [:div.wizard-repeat-group
         [:div.wizard-repeat-group__header
          (str group-name " #" (inc repeat-id))]]
        [:div.wizard-group__inputs
         (for [variable variables]
           ^{:key (:db/id variable)}
           [wizard-input variable ws-uuid group-uuid repeat-id true])]])
     [:div {:style {:display         "flex"
                    :padding         "20px"
                    :align-items     "center"
                    :justify-content "center"}}
      [c/button {:variant  "primary"
                 :label    "Add Resource"
                 :on-click #(rf/dispatch [:worksheet/add-input-group ws-uuid group-uuid (count @repeats)])}]]]))

(defn input-group [ws-uuid group variables]
  (r/with-let [{group-name :group/name} group
               variables (sort-by :group-variable/variable-order variables)]
    [:div.wizard-group
     [:div.wizard-group__header group-name]
     (if (:group/repeat? group)
       [repeat-group ws-uuid group variables]
       [:div.wizard-group__inputs
        (for [variable variables]
          ^{:key (:db/id variable)}
          [wizard-input variable ws-uuid (:bp/uuid group) 0])])]))
