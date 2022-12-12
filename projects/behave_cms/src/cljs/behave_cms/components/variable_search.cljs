(ns behave-cms.components.variable-search
  (:require [reagent.core :as r]
            [behave-cms.utils :as u]))

(defn variable-search [results on-change on-click on-blur]
  (let [selected (r/atom nil)]
    [:<>
     [:input.form-control
      {:type        "text"
       :placeholder "Search variables..."
       :on-change   #(-> % (u/input-value) (on-change))}]
     [:div.list-group
      {:on-mouse-leave (reset! selected nil)}
      (for [{:keys [uuid variable_name]} results]
        ^{:key uuid}
        [:button
         {:class ["list-group-item" "list-group-item-action" (when (= uuid @selected) "active")]
          :on-click       #(on-click (str uuid))
          :on-blur        on-blur
          :on-mouse-enter #(reset! selected uuid)}
         variable_name])]]))
