(ns behave.components.input-group
  (:require [reagent.core           :as r]
            [re-frame.core          :as rf]
            [behave.components.core :as c]
            [behave.translate       :refer [<t]]
            [dom-utils.interface    :refer [input-float-value]]
            [string-utils.interface :refer [->kebab]]))

(defmulti wizard-input :variable/kind)

(defmethod wizard-input :continuous [{id            :db/id
                                      var-name      :variable/name
                                      var-max       :variable/maximum
                                      var-min       :variable/minimum
                                      native-units  :variable/native-units
                                      english-units :variable/english-units
                                      metric-units  :variable/metric-units}]
  [:div.wizard-input
   [:div.wizard-input__input
    [c/number-input {:label       "Values:"
                     :placeholder "Values"
                     :id          (->kebab var-name)
                     :required?   true
                     :min         var-min
                     :max         var-max
                     :on-change   #(rf/dispatch [:state/set [:worksheet :inputs id] (input-float-value %)])}]]
   [:div.wizard-input__description
    (str "Units used: " native-units)
    [:div.wizard-input__description__units
     [:div (str "English Units: " english-units)]
     [:div (str "Metric Units: " metric-units)]]]])

(defmethod wizard-input :discrete [{var-name :variable/name
                                    options  :variable/options}]
  (let [selected (r/atom nil)]
    [:div.wizard-input
     [c/radio-group
      {:name    (->kebab var-name)
       :options (map (fn [{id :db/id value :option/value t-key :option/translation-key}]
                       {:label     @(<t t-key)
                        :id        id
                        :selected  (= @selected value)
                        :name      var-name
                        :on-change #(rf/dispatch [:state/set [:worksheet :inputs id] %])})
                     options)}]]))

(defmethod wizard-input :text [{var-name :variable/name}]
  [:div.wizard-input
   [c/text-input {:label       "Value:"
                  :placeholder "Value"
                  :id          (->kebab var-name)
                  :required?   true}]])

(defn input-group [group variables on-change]
  (let [{group-name :group/name} group]
    [:div.wizard-group
     [:div.wizard-group__header group-name]
     [:div.wizard-group__inputs
      (for [variable variables]
        ^{:key (:db/id variable)}
        [wizard-input variable on-change])]]))
