(ns behave-cms.lists.views
  (:require [reagent.core                      :as r]
            [re-frame.core                     :as rf]
            [behave-cms.components.common      :refer [simple-table
                                                       labeled-text-input
                                                       labeled-integer-input]]
            [behave-cms.components.entity-form :refer [entity-form]]
            [behave-cms.utils                  :as u]
            [behave-cms.events]
            [behave-cms.subs]))

(def columns [:list/name])

(defn lists-table []
  (let [lists     (rf/subscribe [:pull-with-attr :list/name '[* {:list/options [*]}]])
        on-select #(do
                     (rf/dispatch [:state/set-state :list %])
                     (rf/dispatch [:state/set-state :list-option nil]))
        on-delete #(when (js/confirm (str "Are you sure you want to delete the list " (:list/name %) "?"))
                     (rf/dispatch [:api/delete-entity (:db/id %)]))]
    [simple-table
     columns
     (sort-by :list/name @lists)
     {:on-select on-select
      :on-delete on-delete}]))

(defn list-options-table [list]
  (let [list-options (rf/subscribe [:pull-children :list/options (:db/id list)])
        on-select #(rf/dispatch [:state/set-state :list-option %])
        on-delete #(when (js/confirm (str "Are you sure you want to delete the list " (:list-option/name %) "?"))
                     (rf/dispatch [:api/delete-entity (:db/id %)]))]
    [simple-table
     [:list-option/name :list-option/value :list-option/order :list-option/default]
     (sort-by :list-option/order @list-options)
     {:on-select on-select
      :on-delete on-delete
      :on-increase #(rf/dispatch [:api/reorder % @list-options :list-option/order :inc])
      :on-decrease #(rf/dispatch [:api/reorder % @list-options :list-option/order :dec])}]))

(defn list-option-form [list]
  (let [list-options (rf/subscribe [:pull-children :list/options (:db/id list)])
        *list-option (rf/subscribe [:state :list-option])]
    (println list)
    [:<>
     [:h3 (if @*list-option "Edit Option" "Add Option")]
     [entity-form {:entity        :list-options
                   :parent-field  :list/_options
                   :parent-id     (:db/id list)
                   :id            (:db/id @*list-option)
                   :on-create     #(assoc % :list-option/order (count @list-options))
                   :fields        [{:label     "Name"
                                    :required? true
                                    :field-key :list-option/name}
                                   {:label     "Index"
                                    :required? true
                                    :field-key :list-option/value}
                                   {:label     "Hide Option?"
                                    :type      :checkbox
                                    :field-key :list-option/hide?}
                                   {:label     "Default"
                                    :type      :radio
                                    :field-key :list-option/default
                                    :options   [{:label "False" :value false}
                                                {:label "True" :value true}]}]}]]))

(defn list-form [list]
  [:<>
   [:div.row
    [:h3 (if list (str "Edit " (:list/name list)) "Add List")]]
   [:div.row
    [:div.col-3
     [entity-form {:entity :lists
                   :id     (when list (:db/id list))
                   :fields [{:label     "Name"
                             :required? true
                             :field-key :list/name}]}]]
    [:div.col-6
     [:h4 "All Options"]
     [list-options-table list]]

    [:div.col-3
     [list-option-form list]]]])

(defn list-lists-page [_]
(let [loaded? (rf/subscribe [:state :loaded?])
      *list   (rf/subscribe [:state :list])]
    (if @loaded?
      [:div.container
       [:div.row.my-3
        [:div.col-12
         [:h3 "Lists"]
         [:div
          {:style {:height "400px" :overflow-y "scroll"}}
          [lists-table]]]]
       [list-form @*list]]
      [:div "Loading..."])))
