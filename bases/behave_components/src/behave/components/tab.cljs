(ns behave.components.tab)

(defn tab [{:keys [label variant selected? selected-fn on-select] :or {selected? false} :as t}]
  (let [selected? (if (fn? selected-fn) (selected-fn t) selected?)]
    [:div {:class ["tab"
                   (when variant (str "tab--" variant))
                   (when selected? "tab--selected")]
           :on-click #(when (fn? on-select) (on-select t))}
     [:div {:class "tab__label"} label]]))

(defn tab-group [{:keys [tabs variant on-select selected-fn]}]
  [:div {:class "tab-group"}
   (for [t (sort-by :order tabs)]
     ^{:key (:label t)}
     [tab (merge t {:variant variant :on-select on-select :selected-fn selected-fn})])])
