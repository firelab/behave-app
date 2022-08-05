(ns behave.components.button
  (:require [behave.components.icon :refer [icon]]))

(defn button [& [{:keys [variant size icon-name icon-position label on-click disabled? flat-edge]
                  :or   {icon-position "left"}}]]
  [:button {:class    ["button"
                       (when variant (str "button--" variant))
                       (when size (str "button--" size))
                       (when flat-edge (str "button--flat-edge-" flat-edge))]
            :disabled disabled?
            :on-click on-click}
   (when (and icon-name (= icon-position "left"))
     [:div {:class "button__icon"}
      [icon icon-name]])
   [:div {:class "button__label"} label]
   (when (and icon-name (= icon-position "right"))
     [:div {:class "button__icon"}
      [icon icon-name]])])
