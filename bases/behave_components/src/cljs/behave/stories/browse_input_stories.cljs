(ns behave.stories.browse-input-stories
  (:require [behave.components.inputs :refer [browse-input]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Inputs/Browse Input"
       :component (r/reactify-component browse-input)})

(defn template [& [args]]
  (->default {:component browse-input
              :args      (merge {:label        "Select a file."
                                 :button-label "Browse"
                                 :error?       false
                                 :disabled?    false
                                 :on-click     #(js/console.log "Clicked!")}
                                args)}))

(def ^:export Default  (template))
(def ^:export Focused  (template {:focus? true}))
(def ^:export Disabled (template {:disabled? true
                                  :label     "Not a valid file selected."}))
(def ^:export Error    (template {:error? true
                                  :label  "Not a valid file selected."}))
