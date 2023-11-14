(ns behave.stories.card-stories
  (:require [behave.components.card :refer [card]]
            [behave.stories.utils   :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Cards/Card"
       :component (r/reactify-component card)})

(defn template [& [args]]
  (->default {:component card
              :args      (merge {:title     "Card Title"
                                 :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                 :size      "normal"
                                 :selected? false
                                 :error?    false
                                 :disabled? false
                                 :on-select #(js/console.log "Selected!")}
                                args)
              :argTypes  {:size          {:control "radio"
                                          :options ["normal" "large"]}
                          :icon-position {:control "radio"
                                          :options ["left" "top"]}}}))

(def ^:export Default           (template))
(def ^:export CardWithIconLeft  (template {:icons [{:icon-name "active-incident"}]
                                           :title "Card Title"}))
(def ^:export CardWithIconTop  (template {:icons         [{:icon-name "active-incident"}]
                                          :icon-position "top"
                                          :title         "Card Title"}))
(def ^:export CardWithMultiIcon (template {:icons [{:icon-name "surface"} {:icon-name "contain"}]
                                           :title "Card Title"
                                           :size  "large"}))
