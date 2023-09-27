(ns behave.components.vega.diagram
  (:require [cljsjs.vega-embed]
            [behave.components.vega.core :refer [vega-box]]
            [goog.string                    :as gstring]))

(defn- add-scatter-plot [schema {:keys [legend-id data color]}]
  (-> schema
      (update :layer
              #(conj % {:mark     {:type "circle"
                                   :clip true}
                        :data     {:values data}
                        :encoding {:color {:datum legend-id}
                                   :x     {:field "x"
                                           :type  "quantitative"}
                                   :y     {:field "y"
                                           :type  "quantitative"}}}))
      (update-in [:encoding :color :scale :domain] #(conj % legend-id))
      (update-in [:encoding :color :scale :range] #(conj % color))))

(defn- add-ellipse
  [schema {:keys [legend-id color a b phi x-offset stroke-dash]
           :or   {a           0
                  b           0
                  phi         0
                  x-offset    0
                  stroke-dash [1 0]}}]
  (let [a-name   (str "A_" legend-id)
        b-name   (str "B_" legend-id)
        phi-name (str "PHI_" legend-id)
        cx-name  (str "CX_" legend-id)
        cy-name  (str "CY_" legend-id)]
    (-> schema
        (update :layer
                #(conj % {:mark      {:type       "line"
                                      :strokeDash stroke-dash
                                      :clip       true}
                          :data      {:sequence {:start -1.00
                                                 :stop  1.01
                                                 :step  0.01
                                                 :as    "t"}}
                          :transform [{:calculate (gstring/format  "(%s * cos(datum.t * PI) * sin(%s * (PI / 180))) + (%s * sin(datum.t * PI) * cos(%s * (PI / 180))) + %s"
                                                                   a-name phi-name b-name phi-name cx-name)
                                       :as        "x"}
                                      {:calculate (gstring/format "(%s * cos(datum.t * PI) * cos(%s * (PI / 180))) - (%s * sin(datum.t * PI) * sin(%s * (PI / 180))) + %s"
                                                                  a-name phi-name b-name phi-name cy-name)
                                       :as        "y"}]
                          :encoding  {:color {:datum legend-id}
                                      :x     {:field "x"
                                              :type  "quantitative"}
                                      :y     {:field "y"
                                              :type  "quantitative"}
                                      :order {:field "t"
                                              :type  "quantitative"}}}))
        (update :params #(into %  [{:name a-name :value a}
                                   {:name b-name :value b}
                                   {:name phi-name :value phi}
                                   {:name cx-name :expr (gstring/format "(%s * -sin(%s * (PI / 180) - PI) + %s)" a phi x-offset)}
                                   {:name cy-name :expr (gstring/format "(%s * -cos(%s * (PI / 180) - PI))" a phi)}]))
        (update-in [:encoding :color :scale :domain] #(conj % legend-id))
        (update-in [:encoding :color :scale :range] #(conj % color)))))

(defn- add-arrow [schema {:keys [legend-id color r theta dashed?]
                          :or   {r       0
                                 theta   0
                                 dashed? false}}]
  (let [r-name          (str "R_" legend-id)
        theta-name      (str "THETA_" legend-id)
        stroke-width    5
        arrow-head-size (* stroke-width 200)]
    (-> schema
        (update :layer #(conj % {:data      {:values [{r-name 0.0 "origin" true}
                                                      {r-name 0.5}]}
                                 :transform [{:calculate (gstring/format "isDefined(datum.origin) ? 0 : %s * -sin(%s * (PI/180) - PI)"
                                                                         r-name theta-name)
                                              :as        "x"}
                                             {:calculate (gstring/format "isDefined(datum.origin)? 0 : %s * -cos(%s * (PI/180) - PI)"
                                                                         r-name theta-name)
                                              :as        "y"}]
                                 :mark      {:type        "line"
                                             :clip        true
                                             :strokeDash  (if dashed? [4,4] [1,0])
                                             :strokeWidth stroke-width
                                             :color       legend-id
                                             :point       {:shape  "arrow"
                                                           :filled true
                                                           :color  legend-id
                                                           :angle  {:expr theta}
                                                           :size   {:expr (str "isDefined(datum.origin) ? 0 : " arrow-head-size)}}}
                                 :encoding  {:color {:datum legend-id}
                                             :x     {:field "x"
                                                     :type  "quantitative"}
                                             :y     {:field "y"
                                                     :type  "quantitative"}}}))
        (update :params #(into %  [{:name r-name :value r}
                                   {:name theta-name :value theta}]))
        (update-in [:encoding :color :scale :domain] #(conj % legend-id))
        (update-in [:encoding :color :scale :range] #(conj % color)))))

(defn- compute-axis-offset
  "Given the domain and the pixel-width of the axis orthognal to the axis of interest,
  Compute the offset required to move the axis of interest to the origin."
  [domain pixel-width]
  (let [[domain-min domain-max] domain
        domain-abs-width        (+ (Math/abs domain-min)
                                   (Math/abs domain-max))]
    (* -1
       (/ pixel-width domain-abs-width)
       (Math/abs domain-min))))

(defn output-diagram
  "Takes a map of parameters:
   :title         - Title of the Diagram
   :width         - Width in pixels
   :height        - Height in pixels
   :x-axis        - A map of axis parameters
   :y-axis        - A map of axis parameters
   :ellipses      - A sequence of ellipse parameters
   :arrows        - A sequence of arrow parameters
   :scatter-plots - A sequence of scatter-plot parameters

   Axis Parameters:
   :domain - tuple determining the min and max values of the axis
   :title  - title displayed on the axis

   Ellipse Parameters:
   :legend-id - Unique identifier string for the ellipse (for legend)
   :color     - Color for ellipse
   :a         - The ellipses semi major axis
   :b         - The ellipses semi minor axis
   :phi       - The degrees to rotate clockwise starting from the positive y axis
   :x-offset  - The offset in the x axis

   Arrow Parameters:
   :legend-id - Unique identifier string for the arrow (for legend)
   :color     - Color for the arrow
   :r         - The length of the arrow
   :theta     - The degrees to rotate clockwise starting from the positive y axis
   :dashed?    - Whether the arrow should be dashed

   Scatter-plot Paramters:
   :legend-id - Unique identifier string for the scatter-plot (for legend)
   :color     - Color for the scatter plot
   :data      - sequence of maps of coordinates [{x 0 y 0}, {x 1 y 1}, ...]. x and y key are strings
  "
  [{:keys [title width height x-axis y-axis ellipses arrows scatter-plots]}]
  (let [base-schema {:$schema     "https://vega.github.io/schema/vega-lite/v5.1.1.json"
                     :title       {:text     title
                                   :fontSize 20}
                     :description "diagram"
                     :width       width
                     :height      height
                     :encoding    {:x     {:axis  {:title       "x"
                                                   :offset      (compute-axis-offset (:domain y-axis)
                                                                                     height)
                                                   :tickMinStep (or (:tick-min-step x-axis) 1)}
                                           :scale {:domain (:domain x-axis)}}
                                   :y     {:axis  {:title       "y"
                                                   :offset      (compute-axis-offset (:domain x-axis)
                                                                                     width)
                                                   :tickMinStep (or (:tick-min-step y-axis) 1)}
                                           :scale {:domain (:domain y-axis)}}
                                   :color {:type   "nominal"
                                           :scale  {:domain []
                                                    :range  []}
                                           :legend {:symbolType        "stroke"
                                                    :symbolSize        500
                                                    :symbolStrokeWidth 5.0
                                                    :labelFontSize     15}}}
                     :layer       []
                     :params      []}]
    [:div
     [vega-box
      (as-> base-schema $
        (reduce (fn [acc ellipse] (add-ellipse acc ellipse)) $ ellipses)
        (reduce (fn [acc arrow] (add-arrow acc arrow)) $ arrows)
        (reduce (fn [acc scatter-plot] (add-scatter-plot acc scatter-plot)) $ scatter-plots))]]))
