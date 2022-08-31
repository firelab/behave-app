(ns behave.components.accordion
  (:require [behave.components.icon :refer [icon]]))

(defn accordion [{:keys [title icon-name variant opened? on-toggle] :or {opened? false}} & [content]]
  [:div {:class (str "accordion"
                     (when variant (str " accordion--" variant))
                     (when opened? " accordion--opened"))}
   [:div {:class "accordion__header" :on-click on-toggle}
    (when icon-name
      [:div {:class "accordion__header__icon"}
       [icon icon-name]])
    [:div {:class "accordion__header__title"} title]
    [:div {:class "accordion__header__toggle"}
     (if (= variant "research")
       [:div {:class "accordion__header__toggle__plusminus"}]
       [icon "arrow"])]]
   [:div {:class "accordion__body"} content]])
