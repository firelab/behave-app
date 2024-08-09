(ns behave.stories.tab-stories
  (:require [behave.components.tab :refer [tab]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Tabs/Tab"
       :component (r/reactify-component tab)})

(def ^:private selected (atom -1))

(defn template [& [args]]
  (->default {:component tab
              :args      (merge {:disabled? false
                                 :order-id  0
                                 :selected? (= @selected 0)
                                 :on-click  #(do (println %)
                                                 (reset! selected (:order-id %))
                                                 (println "Selected tab: "@selected))}
                                args)
              :argTypes  {:flat-edge {:control "radio"
                                      :options ["top" "bottom"]}
                          :size      {:control "radio"
                                      :options ["small" "normal" "large"]}}}))

(def ^:export OutlinePrimaryTab (template {:variant "primary"
                                           :label   "General Units"}))

(def ^:export OutlineSecondaryTab (template {:variant   "secondary"
                                             :icon-name "help2"
                                             :label     "Help"}))

(def ^:export OutlineHighlightTab (template {:variant   "highlight"
                                             :icon-name "notes"
                                             :label     "Notes"}))
