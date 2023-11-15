(ns behave.components.card
  (:require [behave.components.icon.core :refer [icon]]
            [behave.components.a11y      :refer [on-enter]]))

(defn card [{:keys [title content size icons icon-position disabled? error? selected? on-select]
             :as   c
             :or   {icon-position "left"
                    size          "normal"}}]
  (let [icons-clj (js->clj icons :keywordize-keys true)]
    [:div {:class        ["card"
                          (str "card--" size)
                          (when selected? "card--selected")
                          (when disabled? "card--disabled")
                          (when error?    "card--error")]
           :tabindex     0
           :on-key-press (on-enter #(on-select c))
           :on-click     #(on-select c)}
     (when (= icon-position "top")
       [:div {:class "card__header__icons"}
        (for [icon-name icons-clj]
          [:div {:class "card__header__icon"}
           [icon (merge icon-name {:disabled? disabled?
                                   :error?    error?
                                   :selected? selected?})]])])
     [:div {:class "card__header"}
      (when (= icon-position "left")
        [:div {:class "card__header__icons"}
         (for [icon-name icons-clj]
           [:div {:class "card__header__icon"}
            [icon (merge icon-name {:disabled? disabled?
                                    :error?    error?
                                    :selected? selected?})]])])
      [:div {:class "card__header__circle"}
       [:div {:class "card__header__circle__dot"}]]
      [:div {:class "card__header__title"} title]]
     [:p {:class "card__content"} content]]))

(defn card-group [{:keys [cards card-size flex-direction icon-position on-select]
                   :or   {flex-direction "row"
                          card-size      "normal"
                          icon-position  "left"}}]
  [:div {:class ["card-group"
                 (str "card-group--flex-direction-" flex-direction)]}
   (for [{:keys [order] :as c} (sort-by :order cards)]
     [:div {:class "card-group__card" :key order}
      [card (merge c {:on-select     on-select
                      :size          card-size
                      :icon-position icon-position})]])])
