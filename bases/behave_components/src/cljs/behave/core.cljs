(ns behave.core
  (:require [behave.components.header :refer [h1]]
            [reagent.dom :refer [render]]))

(defn init []
  (render [h1 "Hello, World!"]
          (js/document.getElementById "app")))
