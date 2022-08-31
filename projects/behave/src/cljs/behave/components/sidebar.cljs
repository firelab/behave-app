(ns behave.components.sidebar
  (:require [behave.components.core :refer [icon]]
            [behave.translate       :refer [<t]]))

(defn sidebar-module [{icon-name :icon translation-key :label}]
  (let [translation (<t translation-key)]
    [:div.sidebar-group__module
     [:div.sidebar-group__module__icon [icon icon-name]]
     [:div.sidebar-group__module__label @translation]]))

(defn sidebar-group [{:keys [modules]}]
  [:div.sidebar-group
   [:div.sidebar-group__header "Modules"]
   (for [module modules]
     ^{:key (:label module)}
     [sidebar-module module])])

(defn sidebar []
  [:div.sidebar-container
   [sidebar-group {:modules [{:label "behaveplus:surface"   :icon :surface}
                             {:label "behaveplus:crown"     :icon :crown}
                             {:label "behaveplus:mortality" :icon :mortality}
                             {:label "behaveplus:contain"   :icon :contain}]}]
   [sidebar-group {:title   ""
                   :modules [{:label "behaveplus:tools"     :icon :tools}
                             {:label "behaveplus:settings"  :icon :settings}]}]])
