(ns behave.stories.progress-stories
  (:require [behave.components.core :refer [progress]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title "Progress/Progress Steps"
       :component (r/reactify-component progress)})

(defn ^:export ProgressSteps []
 (r/as-element [progress {:on-select #(js/console.log "Selected!")
                          :steps     [{:label      "First"
                                       :selected?  false
                                       :completed? true
                                       :step       1}
                                      {:label      "Second"
                                       :selected?  true
                                       :completed? false
                                       :step       2}
                                      {:label      "Third"
                                       :selected?  false
                                       :completed? false
                                       :step       3}]}]))
