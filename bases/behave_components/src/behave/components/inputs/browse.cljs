(ns behave.components.inputs.browse
  (:require [behave.components.core :refer [button]]))

(defn browse-input [{:keys [label button-label focus? error? disabled? on-click]}]
  [:div
   {:class ["input-browse"
            (when focus? "input-browse--focused")
            (when error? "input-browse--error")
            (when disabled? "input-browse--disabled")]}
   [button
    {:label     button-label
     :disabled? (or disabled? error?)
     :on-click  on-click
     :variant   "primary"
     :size      "small"}]
   [:div
    {:class "input-browse__label"}
    label]])
