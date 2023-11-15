(ns behave.stories.compute-stories
  (:require [behave.components.compute :refer [compute]]
            [behave.stories.utils   :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Compute/Compute"
       :component (r/reactify-component compute)})

(defn template [& [args]]
  (->default {:component compute
              :args      (merge {} args)}))

(def ^:export ComputeRange (template {:compute-btn-label "Select Range"
                                      :compute-fn        (fn [from to step]
                                                           (range from to step))
                                      :compute-args      [{:name  "From"
                                                           :units "ac"
                                                           :range [0 100]}
                                                          {:name  "To"
                                                           :units "ac"
                                                           :range [0 100]}
                                                          {:name  "Steps"
                                                           :units "ac"
                                                           :range [0 100]}]
                                      :on-compute        #(js/console.log "computed:" %)}))

(def ^:export ComputeSum (template {:compute-fn        (fn [a b c]
                                                         (+ a b c))
                                    :compute-btn-label "Select Sum"
                                    :compute-args      [{:name  "A"
                                                         :units "ft"
                                                         :range [0 100]}
                                                        {:name  "B"
                                                         :units "ft"
                                                         :range [0 100]}
                                                        {:name  "C"
                                                         :units "ft"
                                                         :range [0 100]}]
                                    :on-compute        #(js/console.log "computed:" %)}))
