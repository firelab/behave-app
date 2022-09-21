(ns behave.components.toolbar
  (:require [behave.components.core :as c]
            [behave.translate       :refer [<t]]))

(defn toolbar-tool [{icon-name :icon translation-key :label}]
  [:div.toolbar__tool
   [:div.toolbar__tool__icon
    [c/button {:variant   "transparent-primary"
               :title     @(<t translation-key)
               :icon-name icon-name}]]])

(defn toolbar []
  (let [tools [{:icon :home  :label "behaveplus:home"}
               {:icon :save  :label "behaveplus:save"}
               {:icon :print :label "behaveplus:print"}
               {:icon :share :label "behaveplus:share"}]]
    [:div
     [:div.toolbar
      (for [tool tools]
        ^{:key (:label tool)}
        [toolbar-tool tool])]
     #_[:div.progress-indicator
      [c/progress {:steps [{:label     "Work Style"
                            :selected? true
                            :order     0}]}]]]))
