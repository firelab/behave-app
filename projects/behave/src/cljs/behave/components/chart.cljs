(ns behave.components.chart
  (:require [cljsjs.vega-embed]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.core.async :refer [go]]
            [goog.string                    :as gstring]
            [reagent.core :as r]
            [reagent.dom  :as rd]))

(defn build-line-chart
  "Given a map of with the following entries:
   data: sequence of maps with shape {:key1 val1 :key2 val2 :key3 val3 ... etc}
   x: a map with the key :name who's value is used to look up values in data for the x-axis
   y: a map with the key :name who's value is used to look up values in data for the x-axis
      - Optionally set y-axis-limits using :scale -> tuple (i.e. [0 100])

   Optional
   y: a map with the key :name who's value is used to look up values in data for the z-axis
   z2: a map with the key :name who's value is used to look up values in data for the z2-axis
      - Optionally set the number of columns of subplots to display usin :columns -> long.
        (defaults to 1)."
  [{:keys [data x y z z2 width height]}]
  (when (and x y)
    (let [line-chart {:mark     (cond-> {:type "line"}
                                  (:scale y)
                                  (assoc :clip true))
                      :encoding (cond-> {:x     {:field (:name x)
                                                 :type "nominal"}
                                         :y     (cond-> {:field     (:name y)
                                                         :type      "quantitative"
                                                         :aggregate "average"}
                                                  (:scale y)
                                                  (assoc :scale {:domain (:scale y)}))}
                                  z
                                  (-> (assoc :shape {:field  (:name z)
                                                     :type   "nominal"
                                                     :legend nil})
                                      (assoc :color {:field  (:name z)
                                                     :type   "nominal"
                                                     :legend nil}))

                                  z2
                                  (assoc :facet {:field   (:name z2)
                                                 :type    "nominal"
                                                 :legend  nil
                                                 :columns (or (:columns z2) 1)}))
                      :resolve {:axis {:x "independent" :y "independent"}}
                      :width   width
                      :height  height}
          z-name   (:name z)
          z-legend {:mark     {:type "point"}
                    :title    z-name
                    :encoding {:shape {:field     z-name
                                       :type      "nominal"
                                       :aggregate "min"
                                       :legend    nil}
                               :color {:field     z-name
                                       :type      "nominal"
                                       :aggregate "min"
                                       :legend    nil}
                               :fill  {:field     z-name
                                       :type      "nominal"
                                       :aggregate "min"
                                       :legend    nil}
                               :y     {:field z-name
                                       :type  "nominal"
                                       :title nil}}} ]
      (cond-> {:$schema     "https://vega.github.io/schema/vega-lite/v2.json"
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

(defn chart [{:keys [width height] :as params}]
  [:div
   [vega-box
    (build-line-chart params)
    height
    width]])

(comment
  {:width    300
   :height   300
   :ellipses [{:name       "fl-constructed"
               :major-axis 6
               :minor-axis 4
               :rotation   45}
              {:name       "perim-at-init-attack"
               :major-axis 6
               :minor-axis 4
               :rotation   45}]})

(defn add-ellipse [config {:keys [id color color-encoding] :as _params}]
  (let [a   (str "A_" id)
        b   (str "B_" id)
        cx  (str "CX_" id)
        cy  (str "CY_" id)
        phi (str "PHI_" id)]
    (-> config
        (update :layer
                #(conj % {:mark      "line"
                          :data      {:sequence {:start -1.00
                                                 :stop  1.01
                                                 :step  0.01
                                                 :as    "t"}}
                          :transform [{:calculate (gstring/format  "(%s * cos(datum.t * PI) * sin(%s * (PI / 180))) + (%s * sin(datum.t * PI) * cos(%s * (PI / 180))) + %s"
                                                                   a phi b phi cx)
                                       :as        "x"}
                                      {:calculate (gstring/format "(%s * cos(datum.t * PI) * cos(%s * (PI / 180))) - (%s * sin(datum.t * PI) * sin(%s * (PI / 180))) + %s"
                                                                  a phi b phi cy)
                                       :as        "y"}]
                          :encoding  {:color {:datum color-encoding}
                                      :x     {:field "x"
                                              :type  "quantitative"}
                                      :y     {:field "y"
                                              :type  "quantitative"}
                                      :order {:field "t"
                                              :type  "quantitative"}}}))
        (update :params #(into %  [{:name  a
                                    :value 50
                                    :bind  {:input "range" :min 0 :max 1000 :step 1}}
                                   {:name  b
                                    :value 25
                                    :bind  {:input "range" :min 0 :max 100 :step 1}}
                                   {:name cx
                                    :expr (gstring/format "(%s * -sin(%s * (PI / 180) - PI))" a phi)}
                                   {:name cy
                                    :expr (gstring/format "(%s * -cos(%s * (PI / 180) - PI))" a phi)}
                                   {:name  phi
                                    :value 0
                                    :bind  {:input "range" :min 0 :max 360 :step 1}}]))
        (update-in [:encoding :color :scale :domain] #(conj % color-encoding))
        (update-in [:encoding :color :scale :range] #(conj % color)))))

(defn add-arrow [config {:keys [id color-encoding color] :as _params}]
  (let [r     (str "R_" id)
        theta (str "THETA_" id)]
    (-> config
        (update :layer #(conj % {:data      {:values [{r 0.0 "origin" true}
                                                      {r 0.5}]}
                                 :transform [{:calculate (gstring/format "isDefined(datum.origin) ? 0 : %s * -sin(%s * (PI/180) - PI)"
                                                                         r theta)
                                              :as        "x"}
                                             {:calculate (gstring/format "isDefined(datum.origin)? 0 : %s * -cos(%s * (PI/180) - PI)"
                                                                         r theta)
                                              :as        "y"}]
                                 :mark      {:type  "line"
                                             :color color-encoding
                                             :point {:shape  "arrow"
                                                     :filled true
                                                     :color  color-encoding
                                                     :angle  {:expr theta}
                                                     :size   {:expr "isDefined(datum.origin) ? 0 : 200"}}}
                                 :encoding  {:color {:datum color-encoding}
                                             :x     {:field "x"
                                                     :type  "quantitative"}
                                             :y     {:field "y"
                                                     :type  "quantitative"}}}))
        (update :params #(into %  [{:name  r
                                    :value 50
                                    :bind  {:input "range" :min 0 :max 100 :step 1}}
                                   {:name  theta
                                    :value 0.0
                                    :bind  {:input "range" :min 0 :max 360 :step 1}}]))
        (update-in [:encoding :color :scale :domain] #(conj % color-encoding))
        (update-in [:encoding :color :scale :range] #(conj % color)))))

(defn demo-output-diagram [{:keys [width height x-axis y-axis]}]
  [:div
   [vega-box
    (-> {:$schema     "https://vega.github.io/schema/vega-lite/v5.1.1.json"
         :description "diagram"
         :width       width
         :height      height
         :encoding    {:x     {:axis  {:title  "x"
                                       :offset (:offset x-axis)}
                               :scale {:domain (:scale x-axis)}}
                       :y     {:axis  {:title  "y"
                                       :offset (:offset y-axis)}
                               :scale {:domain (:scale y-axis)}}
                       :color {:type   "nominal"
                               :scale  {:domain []
                                        :range  []}
                               :legend {:labelFontSize 20
                                        :symbolSize 1000}}}
         :layer       []
         :params      []}
        (add-ellipse {:id "1" :color-encoding "series1" :color "blue"})
        (add-ellipse {:id "2" :color-encoding "series2" :color "red"})
        (add-ellipse {:id "3" :color-encoding "series3" :color "purple"})
        (add-arrow {:id "1" :color-encoding "series3" :color "black"})
        (add-arrow {:id "2" :color-encoding "series4 " :color "orange"}))]])

;;; WORKSPACE

(comment
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

  (def dummy-data (build-dummy-data (range 10)
                                    (range 5)
                                    (range 4)))

  ;; Example charts using dummy data. Add this to the wrapper to visualize
  (let [data (build-dummy-data (range 10)
                               (range 5)
                               (range 4))]

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
      100 100]])
  )
