(ns behave.stories.card-group-stories
  (:require [behave.components.core :refer [card-group]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Cards/Card Groups"
       :component (r/reactify-component card-group)})

(defn ^:export WorkflowCards []
  (r/as-element [card-group {:cards     [{:title     "First Card"
                                          :icon-name "guided"
                                          :content   "Card content."
                                          :error?    false
                                          :disabled? false}
                                         {:title     "Second Card"
                                          :content   "Card content."
                                          :error?    false
                                          :disabled? false}]
                             :on-select #(js/console.log "Selected!")}]))

(defn ^:export ObjectiveCards []
  (r/as-element [card-group {:cards     [{:title     "First Card"
                                          :content   "Card content."
                                          :error?    false
                                          :disabled? false}
                                         {:title     "Second Card"
                                          :content   "Card content."
                                          :error?    false
                                          :disabled? false}]
                             :on-select #(js/console.log "Selected!")}]))

(defn ^:export ModuleCards []
  (r/as-element [card-group {:cards      [{:title     "Surface & Crown"
                                          :content   "<Card content>"
                                          :error?    false
                                          :disabled? false}
                                         {:title     "Surface Only"
                                          :content   "<Card content>"
                                          :error?    false
                                          :disabled? false}
                                         {:title     "Surface & Contain"
                                          :content   "<Card content>"
                                          :error?    false
                                          :disabled? false}
                                         {:title     "Surface & Mortality"
                                          :content   "<Card content>"
                                          :error?    false
                                          :disabled? false}
                                         {:title     "Mortality Only"
                                          :content   "<Card content>"
                                          :error?    false
                                          :disabled? false}]
                             :variant   "module"
                             :on-select #(js/console.log "Selected!")}]))
