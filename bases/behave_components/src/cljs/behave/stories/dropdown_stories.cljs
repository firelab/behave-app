(ns behave.stories.dropdown-stories
  (:require [behave.components.inputs :refer [dropdown]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Inputs/Dropdown"
       :component (r/reactify-component dropdown)})

(def options [{:label "First" :value 1}
              {:label "Second" :value 2 :selected? true}
              {:label "Third" :value 3}
              {:label "Fourth" :value 4}
              {:label "Fifth" :value 5}])

(defn ^:export Default []
  (r/as-element [dropdown {:label "Dropdown"
                           :error? false
                           :disabled? false
                           :on-change #(js/console.log "Changed!")
                           :options options}]))

(defn ^:export Disabled []
  (r/as-element [dropdown {:label "Dropdown"
                           :error? false
                           :disabled? true
                           :on-change #(js/console.log "Changed!")
                           :options options}]))

(defn ^:export Error []
  (r/as-element [dropdown {:label "Dropdown"
                           :error? true
                           :disabled? false
                           :on-change #(js/console.log "Changed!")
                           :options options}]))
