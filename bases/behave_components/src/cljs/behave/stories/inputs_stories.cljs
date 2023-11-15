(ns behave.stories.inputs-stories
  (:require [behave.components.inputs :refer [checkbox
                                              number-input
                                              range-input
                                              radio-input
                                              text-input]]
            [behave.stories.utils     :refer [->params ->default]]
            [reagent.core :as r]))

(def ^:export Checkbox
  #js {:title     "Inputs/Checkbox"
       :component checkbox
       :args      {:label "Checkbox:"
                   :disabled? false
                   :on-change #(js/console.log "Changed!")}})

(def ^:export NumberInput
  #js {:title     "Inputs/Number Input"
       :component number-input
       :args      {:label "Number Input:"
                   :disabled? false
                   :on-change #(js/console.log "Changed!")}})

(def ^:export RangeInput
  #js {:title     "Inputs/Range Input"
       :component range-input
       :args      {:label "Range Input:"
                   :disabled? false
                   :on-change #(js/console.log "Changed!")}})

(def ^:export RadioInput
  #js {:title     "Inputs/Radio Input"
       :component radio-input
       :args      {:label "Radio Input:"
                   :disabled? false
                   :on-change #(js/console.log "Changed!")}})

 (def ^:export TextInput
   #js {:title     "Inputs/Text Input"
        :component text-input
        :args      {:label "Text Input:"
                    :disabled? false
                    :on-change #(js/console.log "Changed!")}})


