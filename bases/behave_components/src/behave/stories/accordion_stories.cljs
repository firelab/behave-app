(ns behave.stories.accordion-stories
  (:require [behave.components.core :refer [accordion]]
            [behave.stories.utils   :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Accordions/Accordion"
       :component (r/reactify-component accordion)})

(defn template [& [args]]
  (->default {:component accordion
              :args      (merge {:title     "Card Title"
                                 :content   "Lorem ipsum dolor accordion content."
                                 :opened?   true
                                 :on-select #(js/console.log "Selected!")}
                                args)
              :argTypes  {:variant  {:control "radio"
                                     :options ["information" "research" "settings" "tools"]}}}))

(def ^:export Default     (template))
(def ^:export Information (template {:title "Information" :variant "information"}))
(def ^:export Research    (template {:title "Research" :variant "research"}))
(def ^:export Settings    (template {:icon-name "settings" :title "settings" :variant "settings"}))
(def ^:export Tools       (template {:icon-name "tools" :title "Tools" :variant "tools"}))
