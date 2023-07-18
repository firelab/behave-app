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
      (for [{id :db/id v-name :variable/name} results]
        ^{:key id}
        [:button
         {:class ["list-group-item" "list-group-item-action" (when (= id @selected) "active")]
          :on-click       #(on-click id)
          :on-blur        on-blur
          :on-mouse-enter #(reset! selected id)}
         v-name])]]))
