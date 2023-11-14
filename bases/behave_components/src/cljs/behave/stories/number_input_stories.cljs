(ns behave.stories.number-input-stories
  (:require [behave.components.inputs :refer [number-input]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Inputs/Number Input"
       :component (r/reactify-component number-input)})

(defn template [& [args]]
  (->default {:component number-input
              :args      (merge {:label "Hello"
                                 :error? false
                                 :disabled? false
                                 :max 100
                                 :min 0
                                 :on-change #(js/console.log "Changed!")}
                                args)}))

(def ^:export Default  (template))
(def ^:export Disabled (template {:disabled? true}))
(def ^:export Error    (template {:error? true}))
