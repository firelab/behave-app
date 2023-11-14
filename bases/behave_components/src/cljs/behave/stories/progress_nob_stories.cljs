(ns behave.stories.progress-nob-stories
  (:require [behave.components.progress :refer [progress-nob]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title "Progress/Progress Nob"
       :component (r/reactify-component progress-nob)})

(defn template [& [args]]
  (->default {:component progress-nob
              :args      (merge {:label      "First"
                                 :selected?  false
                                 :on-select  #(js/console.log "Selected!")}
                                args)}))

(def ^:export Default   (template))
(def ^:export Selected  (template {:selected? true}))
