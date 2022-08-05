(ns behave.stories.inputs.text-input-stories
  (:require [behave.components.core :refer [text-input]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Input/Text Input"
       :component (r/reactify-component text-input)})

(defn template [& [args]]
  (->default {:component text-input
              :args      (merge {:label "Text Input"
                                 :placeholder "Placeholder"
                                 :error? false
                                 :disabled? false
                                 :focused? false
                                 :id "text"
                                 :on-change #(js/console.log "Changed!")}
                                args)}))

(def ^:export Default  (template))
(def ^:export Focused  (template {:focused? true}))
(def ^:export Disabled (template {:disabled? true}))
(def ^:export Error    (template {:error? true}))
