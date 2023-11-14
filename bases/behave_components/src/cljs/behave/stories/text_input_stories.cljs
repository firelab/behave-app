(ns behave.stories.text-input-stories
  (:require [behave.components.inputs :refer [text-input]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Inputs/Text Input"
       :component (r/reactify-component text-input)})

(defn template [& [args]]
  (->default {:component text-input
              :args      (merge {:disabled?   false
                                 :error?      false
                                 :focused?    false
                                 :label       "Text Input"
                                 :on-blur   #(js/console.log "Blurred!")
                                 :on-focus  #(js/console.log "Focused!")
                                 :placeholder "Enter Something Here"}
                                 args)}))

(def ^:export Default   (template))
(def ^:export Disabled  (template {:disabled? true :placeholder "Disabled Text"}))
(def ^:export Error     (template {:error? true :placeholder "Erred Text"}))
(def ^:export Focused   (template {:focused? true :placeholder "Focused Text"}))
(def ^:export WithValue (template {:value "Value Text"}))
