(ns behave.components.chart
  (:require [cljsjs.vega-embed]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.core.async :refer [go]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [reagent.dom  :as rd]))

;;TODO remove when subscription exists for pulling results data.
(defn- build-dummy-data [input1 input2 input3]
  (loop [[i1 & rest-i1 :as i1s] input1
         [i2 & rest-i2 :as i2s] input2
         [i3 & rest-i3]         input3
         index                  0
         result                 []]
    (cond
      (and i1 i2 i3)
      (recur i1s
             i2s
             rest-i3
             (inc index)
             (conj result {:input-1  i1
                           :input-2  i2
                           :input-3  i3
                           :output-1 (* (.random js/Math) index 2)}))
      (and i1 i2 (nil? i3))
      (recur i1s
             rest-i2
             input3
             (inc index)
             result)

      (and i1 (nil? i2))
      (recur rest-i1
             input2
             input3
             (inc index)
             result)

      :else
      result)))

(defn build-line-chart
  "Given a map of with the following entries:
   data: sequence of maps with shape {:key1 val1 :key2 val2 :key3 val3 ... etc}
   x: lookup key for the maps in data to use for the x axis
   y: lookup key for the maps in data to use for the y axis

   Optional
   z: lookup key for the maps in data to use for the z axis
   z2: lookup key for the maps in data to use for the z2 axis"
  [{:keys [data x y z z2]}]
  (when (and x y)
    (let [line-chart {:mark     {:type "line"}
                      :encoding (cond-> {:x     {:field x
                                                 :type "nominal"}
                                         :y     {:field     y
                                                 :type      "quantitative"
                                                 :aggregate "average"}}
                                  z
                                  (-> (assoc :shape {:field  z
                                                     :type   "nominal"
                                                     :legend nil})
                                      (assoc :color {:field  z
                                                     :type   "nominal"
                                                     :legend nil}))

                                  z2
                                  (assoc :facet {:field z2 :type "nominal" :legend nil :columns 2}))}
          z-legend {:mark     {:type "point"}
                    :title    z
                    :encoding {:shape {:field     z
                                       :type      "nominal"
                                       :aggregate "min"
                                       :legend    nil}
                               :color {:field     z
                                       :type      "nominal"
                                       :aggregate "min"
                                       :legend    nil}
                               :fill  {:field     z
                                       :type      "nominal"
                                       :aggregate "min"
                                       :legend    nil}
                               :y     {:field z
                                       :type  "nominal"
                                       :title nil}}} ]
      (cond->
          {:$schema     "https://vega.github.io/schema/vega-lite/v2.json"
           :description "test"
           :data        {:values data}}
        z        (assoc :hconcat [line-chart z-legend])
        (nil? z) (merge line-chart)))))


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

(defn wrapper []
  (let [data (build-dummy-data (range 10)
                               (range 5)
                               (range 4))]
    ;TODO remove examples
    [:div

     [:div "2 dimension example"]
     [vega-box (build-line-chart {:data data
                                  :x    "input-1"
                                  :y    "output-1"})
      100 100]

     [:div "3 dimensions example"]
     [vega-box (build-line-chart {:data data
                                  :x    "input-1"
                                  :y    "output-1"
                                  :z    "input-2"})
      100 100]

     [:div "4 dimensions example"]
     [vega-box (build-line-chart {:data data
                                  :x    "input-1"
                                  :y    "output-1"
                                  :z    "input-2"
                                  :z2   "input-3"})
      100 100]]))

;;; WORKSPACE
