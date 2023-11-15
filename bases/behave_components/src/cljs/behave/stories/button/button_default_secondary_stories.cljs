(ns behave.stories.button.button-default-secondary-stories
  (:require [behave.components.button            :refer [button]]
            [behave.stories.button.button-common :refer [common-args common-arg-types]]
            [behave.stories.utils                :refer [->default]]
            [reagent.core                        :as r]))

(def ^:export default
  #js {:title     "Buttons/Default/Secondary"
       :component (r/reactify-component button)})

(defn template [& [args]]
  (->default {:component button
              :args      (-> common-args
                             (merge {:label   "Secondary"
                                     :variant "secondary"})
                             (merge args))
              :argTypes  common-arg-types}))

(def ^:export Default  (template))
(def ^:export Icon     (template {:icon-name :save :label "Save"}))
(def ^:export Disabled (template {:disabled? true}))
