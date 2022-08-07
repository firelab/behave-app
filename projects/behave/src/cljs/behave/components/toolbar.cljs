(ns behave.components.toolbar
  (:require [behave.components.core :as c]
            [behave.translate       :refer [<t]]))

(defn toolbar-tool [{icon-name :icon translation-key :label}]
  (let [translation (<t translation-key)]
    [:div.toolbar__tool
     [:div.toolbar__tool__icon [c/icon icon-name]]
     [:div.toolbar__tool__label @translation]]))

(defn toolbar []
  (let [tools [{:icon :help  :label "behaveplus:help"}
               {:icon :save  :label "behaveplus:save"}
               {:icon :print :label "behaveplus:print"}
               {:icon :share :label "behaveplus:share"}]]
    [:div
     [:div.toolbar
      (for [tool tools]
        ^{:key (:label tool)}
        [toolbar-tool tool])]
     [:div.progress-indicator
      [c/progress {:steps [{:label     "Work Style"
                            :selected? true
                            :order     0}]}]]]))
