(ns behave.stories.tab-stories
  (:require [behave.components.core :refer [tab]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title "Tabs/Tab"
       :component (r/reactify-component tab)})

(defn template [& [args]]
  (->default {:component tab
              :args      (merge {:label     "Hello"
                                 :selected? false
                                 :disabled? false
                                 :error?    false
                                 :on-select #(js/console.log "Selected!")}
                                args)}))

(def ^:export Default (template))
