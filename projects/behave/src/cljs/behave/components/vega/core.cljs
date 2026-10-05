(ns behave.components.vega.core
  (:require [cljsjs.vega-embed]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.core.async :refer [go]]
            [reagent.core :as r]
            [reagent.dom  :as rd]))

(defn- finalize-view!
  "Release the Vega embed attached to `elem`, if any.

  `vegaEmbed` returns a View that owns a dataflow graph, a canvas context and
  window-level event listeners, and (with the actions menu) registers a
  document-level `click` listener that closes over the View. Dropping the
  reference releases none of these, and `view.finalize()` leaves the document
  listener in place — only the embed result's own `finalize()` removes it and
  finalizes the view. Without this a re-render or unmount orphans the whole
  graph for the life of the page."
  [elem]
  (when-let [embed (.-behaveVegaEmbed elem)]
    (try
      (.finalize embed)
      (catch :default e (js/console.log e)))
    (set! (.-behaveVegaEmbed elem) nil)))

(defn- render-vega [spec elem]
  ;; Free the previous view before replacing it — `component-did-update` renders
  ;; into the same node on every prop change.
  (finalize-view! elem)
  (go
    (try
      (let [result (<p! (js/vegaEmbed elem
                                      (clj->js spec)
                                      (clj->js {:renderer "canvas"
                                                :mode     "vega-lite"})))]
        (set! (.-behaveVegaEmbed elem) result))
      (catch ExceptionInfo e (js/console.log (ex-cause e))))))

(defn- vega-canvas []
  (r/create-class
   {:component-did-mount
    (fn [this]
      (let [{:keys [spec]} (r/props this)]
        (render-vega spec (rd/dom-node this))))

    :component-did-update
    (fn [this _]
      (let [{:keys [spec]} (r/props this)]
        (render-vega spec (rd/dom-node this))))

    :component-will-unmount
    (fn [this]
      (finalize-view! (rd/dom-node this)))

    :render
    (fn [this]
      [:div.vega-canvas
       {:style {:height (:box-height (r/props this))
                :width  (:box-width  (r/props this))}}])}))

(defn vega-box
  "A function to create a Vega plot."
  [spec box-height box-width]
  [vega-canvas {:spec       spec
                :box-height box-height
                :box-width  box-width}])
