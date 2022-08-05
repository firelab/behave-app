(ns behave.stories.inputs.checkbox-stories
  (:require [behave.components.core :refer [checkbox]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Input/Checkbox"
       :component (r/reactify-component checkbox)})

(defn template [& [args]]
  (->default {:component checkbox
              :args      (merge {:label "Checkbox"
                                 :checked? false
                                 :disabled? false
                                 :error? false
                                 :on-change #(js/console.log "Changed!")}
                                args)
              :argTypes  {:on-change {:action "Changed!"}}}))

(def ^:export Default  (template))
(def ^:export Checked  (template {:checked? true}))
(def ^:export Disabled (template {:disabled? true}))
(def ^:export Error    (template {:error? true}))
