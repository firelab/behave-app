(ns behave.components.tab)

(defn tab [{:keys [label variant selected? on-select] :as t}]
  [:div {:class ["tab"
                 (when variant (str "tab--" variant))
                 (when selected? "tab--selected")]
         :on-click #(on-select t)}
   [:div {:class "tab__label"} label]])

(defn tab-group [{:keys [tabs variant on-select]}]
  [:div {:class "tab-group"}
   (for [t (sort-by :order tabs)]
     [tab (merge t {:variant variant :on-select on-select})])])
