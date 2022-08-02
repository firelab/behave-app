(ns behave.components.output-group)

(defn output-group [props]
  (let [{group-name :group/name variables :group/variables} props]
    [:div.output-group
     [:strong group-name]
     [:ul
      (for [{var-name :variable/name} variables]
        [:li
         [:input {:type "checkbox"} var-name]])]]))
