(ns behave.stories.note-stories
  (:require
   [behave.components.note :refer [note]]
   [reagent.core :as r]
   [behave.stories.utils :refer [->default]]))

(def ^:export default
  #js {:title     "Note/Note"
       :component (r/reactify-component note)})

(defn template [& [args]]
  (->default {:component note
              :args      (merge {:title-label       "Note's Name / Category"
                                 :title-placeholder "Enter note's name or category"
                                 :body-placeholder  "Add notes"
                                 :on-save           #(js/console.log %)
                                 :limit             10}

                                args)}))

(def ^:export Default  (template))
