(ns behave.stories.accordion-stories
  (:require [behave.components.accordion :refer [accordion]]
            [behave.stories.utils   :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Accordions/Accordion"
       :component (r/reactify-component accordion)})

(defn template [& [args]]
  (->default {:component accordion
              :args      (merge {:accordion-items [{:label   "Accordion Item 1"
                                                    :content "Lorem ipsum dolor accordion content."}
                                                   {:label   "Accordion Item 2"
                                                    :content "Lorem ipsum dolor accordion content."}
                                                   {:label   "Accordion Item 3"
                                                    :content "Lorem ipsum dolor accordion content."}]}
                                args)}))

(def ^:export Default     (template))
