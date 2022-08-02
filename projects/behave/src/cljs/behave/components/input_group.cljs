(ns behave.components.input-group)

(defmulti kind->input :variable/kind)

(defmethod kind->input :continuous [{var-name :variable/name
                                     var-max :variable/max
                                     var-min :variable/min}]
  [:li
   [:label {:for var-name} var-name
    [:input {:type "number" :min var-min :max var-max}]]])

(defmethod kind->input :discrete [{var-name :variable/name
                                   options  :variable/options}]
  [:li
   [:label {:for var-name} var-name
    [:select
     [:option "Please select one..."]
     (for [{id :db/id value :option/value t-key :option/translation-key} options]
       ^{:key id}
       [:options {:value value} t-key])]]])

(defmethod kind->input :text [{var-name :variable/name}]
  [:li
   [:label {:for var-name} var-name
    [:input {:type "text"}]]])

(defn input-group [props]
  (let [{group-name :group/name variables :group/variables} props]
    [:div.output-group
     [:strong group-name]
     [:ul
      (for [variable variables] (kind->input variable))]]))
