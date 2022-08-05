(ns behave.components.card
  (:require [behave.components.icon :refer [icon]]))

(defn card [{:keys [icon-name title content disabled? error? selected? on-select] :as c}]
  [:div {:class (str "card"
                     (when selected? " card--selected")
                     (when disabled? " card--disabled")
                     (when error?    " card--error"))
         :on-click #(on-select c)}
   [:div {:class "card__icon"}
    [icon icon-name]]
   [:div {:class "card__header"}
    [:div {:class "card__header__circle"}
     [:div {:class "card__header__circle__dot"}]]
    [:div {:class "card__header__title"} title]]
   [:p {:class "card__content"} content]])

(defn card-group [{:keys [cards on-select]}]
  [:div {:class "card-group"}
   (for [{:keys [order] :as c} (sort-by :order cards)]
     [:div {:class "card-group__card" :key order}
      [card (merge c {:on-select on-select})]])])
