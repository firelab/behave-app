(ns behave.components.input-group
  (:require [reagent.core           :as r]
            [re-frame.core          :as rf]
            [behave.components.core :as c]
            [behave.translate       :refer [<t]]
            [dom-utils.interface    :refer [input-float-value input-value]]
            [string-utils.interface :refer [->kebab]]))

(defmulti wizard-input (fn [variable _ _] (:variable/kind variable)))

(defmethod wizard-input :continuous [{id            :db/id
                                      var-name      :variable/name
                                      var-max       :variable/maximum
                                      var-min       :variable/minimum
                                      native-units  :variable/native-units
                                      english-units :variable/english-units
                                      metric-units  :variable/metric-units}
                                     group-id
                                     repeat-id
                                     repeat-group?]
  (let [value (rf/subscribe [:state [:worksheet :inputs group-id repeat-id id]])]
    [:div.wizard-input
     [:div.wizard-input__input
      [c/number-input {:label       (if repeat-group? var-name "Values:")
                       :placeholder (when repeat-group? "Values")
                       :id          (->kebab var-name)
                       :value       @value
                       :required?   true
                       :min         var-min
                       :max         var-max
                       :on-change   #(rf/dispatch [:state/set [:worksheet :inputs group-id repeat-id id] (input-float-value %)])}]]
     [:div.wizard-input__description
      (str "Units used: " native-units)
      [:div.wizard-input__description__units
       [:div (str "English Units: " english-units)]
       [:div (str "Metric Units: " metric-units)]]]]))

(defmethod wizard-input :discrete [{id :db/id var-name :variable/name}
                                   group-id
                                   repeat-id
                                   repeat-group?]
  (let [selected  (rf/subscribe [:state [:worksheet :inputs group-id repeat-id id]])
        on-change #(do (println %)
                       (rf/dispatch [:state/set [:worksheet :inputs group-id repeat-id id] (input-value %)]))]
    [:div.wizard-input
     [c/radio-group
      {:label   (when repeat-group? var-name)
       :name    (->kebab var-name)
       :options [{:value "HeadAttack" :label "Head Attack" :on-change on-change :checked? (= @selected "HeadAttack")}
                 {:value "RearAttack" :label "Rear Attack" :on-change on-change :checked? (= @selected "RearAttack")}]}]]))

;#_(map (fn [{id :db/id value :option/value t-key :option/translation-key}]
;         {:label     @(<t t-key)
;          :id        id
;          :selected  (= @selected value)
;          :name      var-name
;          :on-change })
;       options)

(defmethod wizard-input :text [{var-name :variable/name id :db/id} group-id repeat-id repeat-group?]
  (let [value (rf/subscribe [:state [:worksheet :inputs group-id repeat-id id]])]
    [:div.wizard-input
     [c/text-input {:label       (if repeat-group? var-name "Values:")
                    :placeholder (when repeat-group? "Value")
                    :id          (->kebab var-name)
                    :value       @value
                    :on-change   #(rf/dispatch [:state/set [:worksheet :inputs group-id repeat-id id] (input-value %)])
                    :required?   true}]]))

(defn repeat-group [group variables]
  (let [{group-name :group/name group-id :db/id} group
        repeats (rf/subscribe [:state [:worksheet :repeat-groups group-id]])]
    [:<>
     (for [repeat-id (range (or @repeats 0))]
       ^{:key repeat-id}
       [:<> 
        [:div.wizard-repeat-group
         [:div.wizard-repeat-group__header
          (str group-name " #" (inc repeat-id))]]
        [:div.wizard-group__inputs
         (for [variable variables]
           ^{:key (:db/id variable)}
           [wizard-input variable group-id repeat-id true])]])
     [:div {:style {:display         "flex"
                    :padding         "20px"
                    :align-items     "center"
                    :justify-content "center"}}
      [c/button {:variant  "primary"
                 :label    "Add Resource"
                 :on-click #(rf/dispatch [:state/update [:worksheet :repeat-groups group-id] inc])}]]]))

(defn input-group [group variables]
  (let [{group-name :group/name} group
        variables (sort-by :group-variable/variable-order variables)]
    [:div.wizard-group
     [:div.wizard-group__header group-name]
     (if (or (= 2849 (:db/id group)) (:group/repeat? group))
       [repeat-group group variables]
       [:div.wizard-group__inputs
        (for [variable variables]
          ^{:key (:db/id variable)}
          [wizard-input variable (:db/id group) 0])])]))
