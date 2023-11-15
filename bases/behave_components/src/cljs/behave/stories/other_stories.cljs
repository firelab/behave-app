(ns behave.stories.other-stories
  (:require [behave.components.button :refer [button]]
            [behave.stories.utils :refer [->default ->params]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Button Component"
       :component (r/reactify-component button)})

(defn ^:export Button []
  (r/as-element [button {:label "Hello"}]))

(def ^:export DefaultButton
  (->default {:title     "Atoms/Button"
              :component button
              :args      {:label "Hello"
                          :disabled? false
                          :on-click  #(js/console.log "Clicked!")}
              :argTypes  {:variant  {:control {:type "primary"}
                                     :options ["primary"
                                               "secondary"]}
                          :on-click {:action "clicked!"}}}))

