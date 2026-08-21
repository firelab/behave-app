(ns behave.components.button
  (:require [behave.components.icon.core :refer [icon]]))

(defn button
  "A button. `:title` is a tooltip, and doubles as the accessible name for
  icon-only buttons (those without a `:label`)."
  [& [{:keys [variant size icon-name icon-position label title on-click disabled? flat-edge selected?]
       :or   {icon-position "left"}}]]
  [:button (cond-> {:class    ["button"
                               (when variant (str "button--" variant))
                               (when size (str "button--" size))
                               (when flat-edge (str "button--flat-edge-" flat-edge))
                               (when selected? (str "button--selected"))]
                    :disabled disabled?
                    :on-click on-click}
             title                    (assoc :title title)
             (and title (empty? label)) (assoc :aria-label title))
   (when (and icon-name (= icon-position "left"))
     [:div {:class "button__icon"}
      [icon icon-name]])
   (when (seq label)
     [:div {:class "button__label"} label])
   (when (and icon-name (= icon-position "right"))
     [:div {:class "button__icon"}
      [icon icon-name]])])
