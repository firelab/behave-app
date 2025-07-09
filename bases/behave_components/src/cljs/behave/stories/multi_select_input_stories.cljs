(ns behave.stories.multi-select-input-stories
  (:require [behave.components.inputs :refer [multi-select-input]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Inputs/Multi Select Input"
       :component (r/reactify-component multi-select-input)})

(defn ^:export Default []
  (r/as-element [multi-select-input
                 {:prompt1                       "Please select from the following Inputs (you can select multiple)"
                  :prompt2                       "Your Input Selections"
                  :prompt3                       "View your Input selections"
                  :expand-options-button-label   "Select More"
                  :collapse-options-button-label "View"
                  :input-label                   "Input"
                  :options                       [{:label       "option1"
                                                   :value       1
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)
                                                   :selected?   true}

                                                  {:label       "option2"
                                                   :value       2
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)
                                                   :selected?   true}

                                                  {:label       "option3"
                                                   :value       3
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option4"
                                                   :value       4
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option5"
                                                   :value       5
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option6"
                                                   :value       6
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option7"
                                                   :value       7
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option8"
                                                   :value       8
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option9"
                                                   :value       9
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}]}]))


(defn ^:export Tags []
  (r/as-element [multi-select-input
                 {:input-label                   "Input"
                  :prompt1                       "Please select from the following Inputs (you can select multiple)"
                  :prompt2                       "Your Input Selections"
                  :prompt3                       "View your Input selections"
                  :expand-options-button-label   "Select More"
                  :collapse-options-button-label "View"
                  :filter-tags                   [{:id :odd :label "Odd" :order 2}
                                                  {:id :even :label "Even" :order 1}]
                  :options                       [{:label       "option1"
                                                   :value       1
                                                   :tags        #{:odd}
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)
                                                   :selected?   true}

                                                  {:label       "option2"
                                                   :tags        #{:even}
                                                   :value       2
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)
                                                   :selected?   true}

                                                  {:label       "option3"
                                                   :value       3
                                                   :tags        #{:odd}
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option4"
                                                   :value       4
                                                   :tags        #{:even}
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option5"
                                                   :value       5
                                                   :tags        #{:odd}
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option6"
                                                   :value       6
                                                   :tags        #{:even}
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option7"
                                                   :value       7
                                                   :tags        #{:odd}
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}]}]))


(defn ^:export Tags-and-color-tags []
  (r/as-element [multi-select-input
                 {:input-label                   "Input"
                  :prompt1                       "Please select from the following Inputs (you can select multiple)"
                  :prompt2                       "Your Input Selections"
                  :prompt3                       "View your Input selections"
                  :expand-options-button-label   "Select More"
                  :collapse-options-button-label "View"
                  :filter-tags                   [{:id :odd :label "Odd" :order 1}
                                                  {:id :even :label "Even" :order 2}]
                  :color-tags                    [{:label "Example Color 1"
                                                   :color "var(--green-2)"}
                                                  {:label "Example Color 2"
                                                   :color "var(--peach-2)"}
                                                  {:label "Example Color 3"
                                                   :color "var(--orange-2)"}]
                  :options                       [{:label       "option1"
                                                   :value       1
                                                   :tags        #{:odd}
                                                   :color-tag   {:color "var(--green-2)"}
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)
                                                   :selected?   true}

                                                  {:label       "option-2"
                                                   :tags        #{:even}
                                                   :color-tag   {:color "var(--green-2)"}
                                                   :value       2
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option3"
                                                   :value       3
                                                   :tags        #{:odd}
                                                   :color-tag   {:color "var(--peach-2)"}
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)
                                                   :selected?   true}

                                                  {:label       "option4"
                                                   :value       4
                                                   :tags        #{:even}
                                                   :color-tag   {:color "var(--peach-2)"}
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option5"
                                                   :value       5
                                                   :tags        #{:odd}
                                                   :color-tag   {:color "var(--orange-2)"}
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)
                                                   :selected?   true}

                                                  {:label       "option6"
                                                   :value       6
                                                   :tags        #{:even}
                                                   :color-tag   {:color "var(--orange-2)"}
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}]}]))

(defn ^:export SearchBox []
  (r/as-element [multi-select-input
                 {:input-label                   "Input"
                  :prompt1                       "Select an Input. To make additional selections, press Enter after searching."
                  :search                        true
                  :expand-options-button-label   "Show All Options"
                  :collapse-options-button-label "Hide Options"
                  :options                       [{:label       "option1"
                                                   :value       1
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)
                                                   :selected?   true}

                                                  {:label       "option2"
                                                   :value       2
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)
                                                   :selected?   true}

                                                  {:label       "option3"
                                                   :value       3
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option4"
                                                   :value       4
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option5"
                                                   :value       5
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option6"
                                                   :value       6
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option7"
                                                   :value       7
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option8"
                                                   :value       8
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}

                                                  {:label       "option9"
                                                   :value       9
                                                   :on-deselect #(js/console.log "on-deselect:" %)
                                                   :on-select   #(js/console.log "on-select:" %)}]}]))
