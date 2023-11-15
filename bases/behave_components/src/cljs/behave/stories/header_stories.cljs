(ns behave.stories.header-stories
  (:require [behave.components.header :refer [h1 h2 h3 h4 h5 h6]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Headers/Header Component"
       :component (r/reactify-component h1)})

(defn ^:export H1Header []
  (r/as-element [h1 "First"]))

(defn ^:export H2Header []
  (r/as-element [h2 "First"]))

(defn ^:export H3Header []
  (r/as-element [h3 "First"]))
