(ns behave.components.input-group
  (:require [behave.components.core :as c]))

(defmulti wizard-input :variable/kind)

(defmethod wizard-input :continuous [{var-name :variable/name
                                     var-max :variable/max
                                     var-min :variable/min
                                     units   :variable/native-units}]
  [:div.wizard-input
   [:label {:for var-name} (str var-name "(" units ")")
    [:input {:type "number" :min var-min :max var-max}]]])

(defmethod wizard-input :discrete [{var-name :variable/name
                                   options  :variable/options}]
  [:div.wizard-input
   [:label {:for var-name} var-name
    [:select
     [:option "Please select one..."]
     (for [{id :db/id value :option/value t-key :option/translation-key} options]
       ^{:key id}
       [:options {:value value} t-key])]]])

(defmethod wizard-input :text [{var-name :variable/name}]
  [:div.wizard-input
   [:label {:for var-name} var-name
    [:input {:type "text"}]]])

(defn input-group [props]
  (let [{group-name :group/name variables :group/variables} props]
    [:div.wizard-group
     [:div.wizard-group__header group-name]
     [:div.wizard-group__inputs
      (for [variable variables] (wizard-input variable))]]))
