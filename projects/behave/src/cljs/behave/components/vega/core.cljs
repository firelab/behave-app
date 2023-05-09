(ns behave.components.vega.core
  (:require [cljsjs.vega-embed]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.core.async :refer [go]]
            [reagent.core :as r]
            [reagent.dom  :as rd]))

(defn- render-vega [spec elem]
  (go
    (try
      (<p! (js/vegaEmbed elem
                         (clj->js spec)
                         (clj->js {:renderer "canvas"
                                   :mode     "vega-lite"})))
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

    :render
    (fn [this]
      [:div#vega-canvas
       {:style {:height (:box-height (r/props this))
                :width  (:box-width  (r/props this))}}])}))

(defn vega-box
  "A function to create a Vega plot."
  [spec box-height box-width]
  [vega-canvas {:spec       spec
                :box-height box-height
                :box-width  box-width}])
