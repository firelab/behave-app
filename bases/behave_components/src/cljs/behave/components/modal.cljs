(ns behave.components.modal
  (:require [behave.components.button :refer [button]]
            [behave.components.icon.core :refer [icon]]))

(defn- header [{:keys [icon-name]} close-on-click title]
  [:div {:class "modal__header"}
   (when icon-name
     [:div {:class "modal__icon"}
      [icon icon-name]])
   [:div {:class "modal__title"} title
    [:div {:class "modal__close"}
     [button {:icon-name "close"
              :on-click  close-on-click
              :shape     "round"
              :size      "small"
              :variant   "primary"}]]]])

(defn modal [{:keys [title icon close-on-click buttons content]}]
  (let [buttons (js->clj buttons :keywordize-keys true)
        icon    (js->clj icon :keywordize-keys true)]
    [:div {:class "modal"}
     [header icon close-on-click title]
     [:div {:class "modal__body"} content]
     (when (seq buttons)
       [:div {:class "modal__buttons"}
        (for [btn buttons]
          [button btn])])]))
