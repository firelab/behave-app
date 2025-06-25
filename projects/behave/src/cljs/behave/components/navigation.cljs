(ns behave.components.navigation
  (:require [behave.components.core :as c]))

(defn wizard-navigation [{:keys [next-label next-disabled? back-label on-back on-next]}]
  [:div {:class ["wizard-navigation"
                 (when (nil? back-label)
                   "wizard-navigation--next-only")]}
   (when back-label
     [:div.wizard-navigation__back
      [c/button {:label    back-label
                 :variant  "secondary"
                 :on-click on-back}]])
   [:div.wizard-navigation__next
    [c/button {:label         next-label
               :disabled?     next-disabled?
               :variant       "highlight"
               :icon-name     "arrow2"
               :icon-position "right"
               :on-click      on-next}]]])
