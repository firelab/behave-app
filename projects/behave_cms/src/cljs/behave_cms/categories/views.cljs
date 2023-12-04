(ns behave-cms.categories.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.common      :refer [simple-table]]
            [behave-cms.components.entity-form :refer [entity-form]]
            [behave-cms.events]
            [behave-cms.subs]))

(def columns [:category/name])

(defn categories-table []
  (let [categories (rf/subscribe [:pull-with-attr :category/name '[*]])
        on-select  #(rf/dispatch [:state/set-state :category %])
        on-delete  #(when (js/confirm (str "Are you sure you want to delete the category " (:category/name %) "?"))
                      (rf/dispatch [:api/delete-entity %]))]
    [simple-table
     columns
     (sort-by :category/name @categories)
     {:on-select on-select
      :on-delete on-delete}]))

(defn category-form [category]
  [:<>
   [:div.row
    [:h3 (if category (str "Edit " (:category/name category)) "Add Category")]]
   [:div.row
    [:div.col-3
     [entity-form {:entity :categories
                   :id     (when category (:db/id category))
                   :fields [{:label     "Name"
                             :required? true
                             :field-key :category/name}]}]]]])

(defn list-categories-page [_]
  (let [loaded?   (rf/subscribe [:state :loaded?])
        *category (rf/subscribe [:state :category])]
    (if @loaded?
      [:div.container
       [:div.row.my-3
        [:div.col-12
         [:h3 "Categories"]
         [:div
          {:style {:height "400px" :overflow-y "scroll"}}
          [categories-table]]]]
       [category-form @*category]]
      [:div "Loading..."])))
