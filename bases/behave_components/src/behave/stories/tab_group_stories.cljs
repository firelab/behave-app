(ns behave.stories.tab-group-stories
  (:require [behave.components.core :refer [tab-group]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title "Tabs/Tab Group"
       :component (r/reactify-component tab-group)})

(def ^:private selected (atom 0))

(defn ^:export TabGroup []
 (r/as-element [tab-group {:variant "selected"
                           :tabs [{:label "First"
                                   :selected? true
                                   :disabled? false
                                   :error? false
                                   :order 0}
                                  {:label "Second"
                                   :selected? (= @selected 1)
                                   :disabled? false
                                   :error? false
                                   :order 1}]}]))
