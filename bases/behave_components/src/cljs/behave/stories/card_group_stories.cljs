(ns behave.stories.card-group-stories
  (:require [behave.components.card :refer [card-group]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Cards/Card Groups"
       :component (r/reactify-component card-group)})

(defn ^:export VerticalCards []
  (r/as-element [card-group {:flex-direction "column"
                             :cards          [{:title     "First Card"
                                               :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                               :icons     [{:icon-name "guided-work"}]
                                               :error?    false
                                               :disabled? false}
                                              {:title     "Second Card"
                                               :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                               :icons     [{:icon-name "independent-work"}]
                                               :error?    false
                                               :disabled? false}
                                              {:title     "Second Card"
                                               :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                               :icons     [{:icon-name "import-files"}]
                                               :error?    false
                                               :disabled? false}]
                             :card-size      "large"
                             :on-select      #(js/console.log "Selected!")}]))

(defn ^:export HorizontalCards []
  (r/as-element [card-group {:flex-direction "row"
                             :cards          [{:title     "First Card"
                                               :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                               :icons     [{:icon-name "worksheet"}]
                                               :error?    false
                                               :disabled? false}
                                              {:title     "Second Card"
                                               :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                               :icons     [{:icon-name "active-incident"}]
                                               :error?    false
                                               :disabled? false}
                                              {:title     "Third Card"
                                               :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                               :icons     [{:icon-name "existing-run"}]
                                               :error?    false
                                               :disabled? false}
                                              {:title     "Fourth Card"
                                               :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                               :icons     [{:icon-name "fuel-model"}]
                                               :error?    false
                                               :disabled? false}]
                             :card-size      "normal"
                             :icon-position  "top"
                             :on-select      #(js/console.log "Selected!")}]))
