(ns behave.components.button
  (:require [behave.components.icon.default-icon :refer [icon]]))

(defn button [& [{:keys [variant size icon-name icon-position label on-click disabled? flat-edge selected?]
                  :or   {icon-position "left"}}]]
  [:button {:class    ["button"
                       (when variant (str "button--" variant))
                       (when size (str "button--" size))
                       (when flat-edge (str "button--flat-edge-" flat-edge))
                       (when selected? (str "button--selected"))]
            :disabled disabled?
            :on-click on-click}
   (when (and icon-name (= icon-position "left"))
     [:div {:class "button__icon"}
      [icon icon-name]])
   (when (seq label)
     [:div {:class "button__label"} label])
   (when (and icon-name (= icon-position "right"))
     [:div {:class "button__icon"}
      [icon icon-name]])])
