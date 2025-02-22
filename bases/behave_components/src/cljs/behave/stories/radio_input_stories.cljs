(ns behave.stories.radio-input-stories
  (:require [behave.components.inputs :refer [radio-input]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title "Inputs/Radio Inputs"
       :component (r/reactify-component radio-input)})

(defn template [& [args]]
  (->default {:component radio-input
              :args      (merge {:label "Hello"
                                 :checked? false
                                 :disabled? false
                                 :error? false
                                 :on-change #(js/console.log "Changed!")}
                                args)}))

(def ^:export Default  (template))
(def ^:export Checked  (template {:checked? true}))
(def ^:export Disabled (template {:disabled? true}))
(def ^:export Error    (template {:error? true}))
