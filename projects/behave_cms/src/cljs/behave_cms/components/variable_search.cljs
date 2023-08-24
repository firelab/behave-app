(ns behave-cms.components.variable-search
  (:require [reagent.core :as r]
            [behave-cms.utils :as u]))

(defn variable-search
  "Displays a variable search component. Takes a map with:
   - results   [<seq<map>]: Collection of results to display matching query.
   - disabled? [bool]: Whether element is disabled.
   - on-change [fn]: Function that takes the latest query string.
   - on-select  [fn]: Function that takes the result when clicked.
   - on-blur   [fn]: Function called when the user de-selects the element."
  [{:keys [results disabled? on-change on-select on-blur]}]
  (let [selected (r/atom nil)]
    [:<>
     [:input.form-control
      {:type        "text"
       :placeholder "Search variables..."
       :disabled    disabled?
       :on-change   #(-> % (u/input-value) (on-change))}]
     [:div.list-group
      {:on-mouse-leave (reset! selected nil)}
      (doall
       (for [{id :db/id v-name :variable/name} results]
         ^{:key id}
         [:button
          {:class          ["list-group-item" "list-group-item-action" (when (= id @selected) "active")]
           :on-click       #(on-select id)
           :on-blur        on-blur
           :on-mouse-enter #(reset! selected id)}
          v-name]))]]))
