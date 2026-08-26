(ns behave.components.results.pop-out
  "Shared bits for opening a Results chart in the pop-out modal."
  (:require [behave.components.core :as c]
            [re-frame.core          :refer [dispatch]]))

(defn pop-out-size
  "Chart size for the pop-out modal, bounded by the viewport."
  []
  (let [width  (min 1000 (max 300 (- (.-innerWidth js/window) 240)))
        height (min 700 (max 250 (- (.-innerHeight js/window) 280)))]
    [width height]))

(defn pop-out-button
  "Opens the `id` modal with `ctx`. Sits over a chart's top-right corner."
  [id ctx]
  [:div.pop-out-button
   [c/button {:icon-name "pop-out"
              :variant   "secondary"
              :size      "small"
              :title     "Enlarge"
              :on-click  #(dispatch [:modal/open id ctx])}]])
