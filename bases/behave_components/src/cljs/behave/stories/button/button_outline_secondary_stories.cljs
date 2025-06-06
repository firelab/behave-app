(ns behave.stories.button.button-outline-secondary-stories
  (:require [behave.components.button            :refer [button]]
            [behave.stories.button.button-common :refer [common-args common-arg-types]]
            [behave.stories.utils                :refer [->default]]
            [reagent.core                        :as r]))

(def ^:export default
  #js {:title     "Buttons/Outline/Secondary"
       :component (r/reactify-component button)})

(defn template [& [args]]
  (->default {:component button
              :args      (-> common-args
                             (merge {:label   "Secondary"
                                     :variant "outline-secondary"})
                             (merge args))
              :argTypes  common-arg-types}))

(def ^:export Default  (template))
(def ^:export Icon     (template {:icon-name :save :label "Save"}))
(def ^:export Disabled (template {:disabled? true}))
