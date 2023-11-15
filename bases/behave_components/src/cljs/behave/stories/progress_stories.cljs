(ns behave.stories.progress-stories
  (:require [behave.components.progress :refer [progress]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Progress/Progress Steps"
       :component (r/reactify-component progress)})

(defn ^:export ProgressSteps []
  (r/as-element [progress {:on-select              #(js/console.log %)
                           :completed-last-step-id 2
                           :steps                  [{:label      "First"
                                                     :selected?  false
                                                     :completed? true
                                                     :step-id    1}
                                                    {:label      "Second"
                                                     :selected?  true
                                                     :completed? true
                                                     :step-id    2}
                                                    {:label      "Third"
                                                     :selected?  false
                                                     :completed? false
                                                     :step-id    3}]}]))
