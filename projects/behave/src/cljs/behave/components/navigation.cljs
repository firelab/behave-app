(ns behave.components.navigation
  (:require [behave.components.core :as c]))

(defn wizard-navigation [{:keys [next-label back-label on-back on-next]}]
  [:div.wizard-navigation
   (when back-label
     [c/button {:label    back-label
                :variant  "secondary"
                :on-click on-back}])
   [c/button {:label         next-label
              :variant       "highlight"
              :icon-name     "arrow2"
              :icon-position "right"
              :on-click      on-next}]])
