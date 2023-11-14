(ns behave.stories.modal-stories
  (:require [behave.components.modal :refer [modal]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Modal/Modal"
       :component (r/reactify-component modal)})

(defn template [& [args]]
  (->default {:component modal
              :args      (merge {:title          "Title"
                                 :close-on-click #(js/console.log "Clicked!")
                                 :content        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."}
                                args)}))

(def ^:export Default (template))

(def ^:export WithIcon (template {:icon {:icon-name "help2"}}))

(def ^:export WithIconsAndButtons (template {:icon    {:icon-name "help2"}
                                             :buttons [{:variant  "primary"
                                                        :label    "button-1"
                                                        :on-click #(js/console.log "Clicked!")}
                                                       {:variant  "secondary"
                                                        :label    "button-2"
                                                        :on-click #(js/console.log "Clicked!")}]}))


[modal]
