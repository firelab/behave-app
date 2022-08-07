(ns behave.components.card
  (:require [behave.components.icon :refer [icon]]))

(defn card [{:keys [icon-name title content variant disabled? error? selected? on-select selected-fn] :as c}]
  (let [selected? (if (fn? selected-fn) (selected-fn c) selected?)]
    [:div {:class ["card"
                   (when variant   (str "card--" variant))
                   (when selected? "card--selected")
                   (when disabled? "card--disabled")
                   (when error?    "card--error")]
           :on-click #(on-select c)}
     [:div {:class "card__icon"}
      [icon icon-name]]
     [:div {:class "card__header"}
      [:div {:class "card__header__circle"}
       [:div {:class "card__header__circle__dot"}]]
      [:div {:class "card__header__title"} title]]
     [:p {:class "card__content"} content]]))

(defn card-group [{:keys [cards on-select variant selected-fn]}]
  [:div {:class "card-group"}
   (for [{:keys [order] :as c} (sort-by :order cards)]
     ^{:key order}
     [:div {:class "card-group__card" :key order}
      [card (merge c {:on-select on-select
                      :variant variant
                      :selected-fn selected-fn})]])])
