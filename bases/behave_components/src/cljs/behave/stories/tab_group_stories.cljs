(ns behave.stories.tab-group-stories
  (:require [behave.components.tab :refer [tab-group]]
            [behave.stories.utils  :refer [->default]]
            [reagent.core          :as r]))

(def ^:export default
  #js {:title     "Tabs/Tab Group"
       :component (r/reactify-component tab-group)})

(def ^:private selected (atom 0))

(defn template [& [args]]
  (->default {:component tab-group
              :args      (merge {:flat-edge "bottom"
                                 :align     "left"
                                 :on-click  #(do (println %)
                                                 (reset! selected (:order-id %))
                                                 (println "Selected tab: "@selected))}
                                args)
              :argTypes  {:flat-edge {:control "radio"
                                      :options ["top" "bottom"]}
                          :align     {:control "radio"
                                      :options ["left" "right"]}}}))

(def ^:export Primary
  (template {:tabs [{:label     "Help"
                     :icon-name "help2"
                     :selected? (= @selected 0)
                     :order-id  0}
                    {:label     "Help Manual"
                     :icon-name "help-manual"
                     :selected? (= @selected 1)
                     :order-id  1}]}))

(def ^:export Secondary
  (template {:variant   "secondary"
             :flat-edge "top"
             :align     "right"
             :tabs      [{:label     "inputs"
                          :selected? (= @selected 0)
                          :order-id  0}
                         {:label     "outputs"
                          :selected? (= @selected 1)
                          :order-id  1}]}))

(def ^:export Highlight
  (template {:variant "highlight"
             :tabs    [{:label     "Notes"
                        :icon-name "notes"
                        :selected? (= @selected 0)
                        :order-id  0}
                       {:label     "Tables"
                        :icon-name "tables"
                        :selected? (= @selected 1)
                        :order-id  1}
                       {:label     "Graphs"
                        :icon-name "graphs"
                        :selected? (= @selected 2)
                        :order-id  2}]}))

(def ^:export Themed
  (template {:variant "themed"
             :tabs    [{:label     "Notes"
                        :icon-name "notes"
                        :selected? (= @selected 0)
                        :order-id  0}
                       {:label     "Tables"
                        :icon-name "tables"
                        :selected? (= @selected 1)
                        :order-id  1}
                       {:label     "Graphs"
                        :icon-name "graphs"
                        :selected? (= @selected 2)
                        :order-id  2}]}))
