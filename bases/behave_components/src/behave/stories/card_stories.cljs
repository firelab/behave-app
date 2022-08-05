(ns behave.stories.card-stories
  (:require [behave.components.core :refer [card]]
            [behave.stories.utils   :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Cards/Card"
       :component (r/reactify-component card)})

(defn template [& [args]]
  (->default {:component card
              :args      (merge {:title     "Card Title"
                                 :content   "Lorem ipsum dolor card content."
                                 :icon-name "worksheet"
                                 :selected? false
                                 :error?    false
                                 :disabled? false
                                 :on-select #(js/console.log "Selected!")}
                                args)}))

(def ^:export Default  (template))
(def ^:export Selected (template {:selected? true}))
(def ^:export Disabled (template {:disabled? true}))
(def ^:export Error    (template {:error? true}))
