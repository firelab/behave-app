(ns behave.stories.toggle-stories
  (:require [behave.components.inputs :refer [toggle]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Inputs/Toggle"
       :component (r/reactify-component toggle)})

(defn template [& [args]]
  (->default {:component toggle
              :args      (merge {:label       "Toggle"
                                 :left-label  "Off"
                                 :right-label "On"
                                 :checked?    false
                                 :disabled?   false
                                 :on-change   #(js/console.log "Changed!")}
                                args)}))

(def ^:export Default (template))
(def ^:export Checked (template {:checked? true}))
(def ^:export Disabled (template {:disabled? true}))
(def ^:export WithLabel (template {:label "Settings"}))
