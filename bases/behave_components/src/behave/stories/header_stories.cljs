(ns behave.stories.header-stories
  (:require [behave.components.core :refer [h1 h2 h3 h4 h5 h6]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Headers/Header Component"
       :component (r/reactify-component h1)})

(defn template [h-component h-level]
  (r/as-element [h-component (str "H" h-level " Header")]))

(defn ^:export H1Header []
  (template h1 1))

(defn ^:export H2Header []
  (template h2 2))

(defn ^:export H3Header []
  (template h3 3))

(defn ^:export H4Header []
  (template h4 4))

(defn ^:export H5Header []
  (template h5 5))

(defn ^:export H6Header []
  (template h6 6))
