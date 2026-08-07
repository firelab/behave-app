(ns behave.components.vega.diagram
  (:require [behave.components.vega.core :refer [vega-box]]
            [cljsjs.vega-embed]
            [clojure.string              :as str]
            [goog.string                 :as gstring]))

(defn- ->str-literal
  "A double-quoted Vega expression string literal for `legend-id`, used in a
  `:calculate` transform to tag every row of a layer with a `series` field."
  [legend-id]
  (str \"
       (-> legend-id
           (str/replace "\\" "\\\\") ;; safeguard against quotes
           (str/replace "\"" "\\\""))
       \"))

(defn- add-scatter-plot [schema {:keys [legend-id data color connect?]}]
  (-> schema
      (update :layer
              #(conj % {:mark      (if connect?
                                     {:type "line" :clip true}
                                     {:type "circle" :clip true})
                        :data      {:values data}
                        :transform [{:calculate (->str-literal legend-id)
                                     :as        "series"}]
                        :encoding  {:color {:field "series"
                                            :type  "nominal"}
                                    :x     {:field "x"
                                            :type  "quantitative"}
                                    :y     {:field "y"
                                            :type  "quantitative"}}}))
      (update-in [:encoding :color :scale :domain] #(conj % legend-id))
      (update-in [:encoding :color :scale :range] #(conj % color))))

(defn- add-ellipse
  [schema {:keys [legend-id color a b phi x-offset stroke-dash center-offset-distance dashed?]
           :or   {a           0
                  b           0
                  phi         0
                  x-offset    0
                  dashed?     false
                  stroke-dash [1 0]}}]
  (let [legend-id-cleaned (str/replace legend-id " " "_")
        a-name            (str "A_" legend-id-cleaned)
        b-name            (str "B_" legend-id-cleaned)
        phi-name          (str "PHI_" legend-id-cleaned)
        cx-name           (str "CX_" legend-id-cleaned)
        cy-name           (str "CY_" legend-id-cleaned)
        ;; Distance from the plot origin to the ellipse center, along phi. When a
        ;; focal distance is supplied the origin sits at the ellipse focus;
        ;; otherwise fall back to the semi-major axis (origin at the tail vertex).
        center-offset     (or center-offset-distance a)]
    (-> schema
        (update :layer
                #(conj % {:mark      {:type       "line"
                                      :strokeDash (if dashed? [4 4] stroke-dash)
                                      :clip       true}
                          :data      {:sequence {:start -1.00
                                                 :stop  1.01
                                                 :step  0.01
                                                 :as    "t"}}
                          :transform [{:calculate (gstring/format "(%s * cos(datum.t * PI) * sin(%s * (PI / 180))) + (%s * sin(datum.t * PI) * cos(%s * (PI / 180))) + %s"
                                                                  a-name phi-name b-name phi-name cx-name)
                                       :as        "x"}
                                      {:calculate (gstring/format "(%s * cos(datum.t * PI) * cos(%s * (PI / 180))) - (%s * sin(datum.t * PI) * sin(%s * (PI / 180))) + %s"
                                                                  a-name phi-name b-name phi-name cy-name)
                                       :as        "y"}
                                      {:calculate (->str-literal legend-id)
                                       :as        "series"}]
                          :encoding  {:color {:field "series"
                                              :type  "nominal"}
                                      :x     {:field "x"
                                              :type  "quantitative"}
                                      :y     {:field "y"
                                              :type  "quantitative"}
                                      :order {:field "t"
                                              :type  "quantitative"}}}))
        (update :params #(into % [{:name a-name :value a}
                                  {:name b-name :value b}
                                  {:name phi-name :value phi}
                                  {:name cx-name :expr (gstring/format "(%s * -sin(%s * (PI / 180) - PI) + %s)" center-offset phi x-offset)}
                                  {:name cy-name :expr (gstring/format "(%s * -cos(%s * (PI / 180) - PI))" center-offset phi)}]))
        (update-in [:encoding :color :scale :domain] #(conj % legend-id))
        (update-in [:encoding :color :scale :range] #(conj % color)))))

(def ^:private arrow-stroke-width 5)
(def ^:private arrow-head-size (* arrow-stroke-width 200))   ; point-mark area in px²

(defn- add-arrow [schema {:keys [legend-id color r theta dashed? offset-distance offset-rotation]
                          :or   {r               0
                                 theta           0
                                 dashed?         false
                                 offset-distance 0
                                 offset-rotation 0}}]
  (let [legend-id-cleaned (str/replace legend-id " " "_")
        r-name            (str "R_" legend-id-cleaned)
        theta-name        (str "THETA_" legend-id-cleaned)
        ;; Base point of the arrow, offset from the plot origin by
        ;; offset-distance along offset-rotation (same sin/cos convention as the
        ;; tip and the ellipse center). Both the base and tip shift by (ox, oy).
        offset-rad        (* offset-rotation (/ js/Math.PI 180))
        ox                (* offset-distance (Math/sin offset-rad))
        oy                (* offset-distance (Math/cos offset-rad))]
    (-> schema
        (update :layer #(conj % {:data      {:values [{r-name 0.0 "origin" true}
                                                      {r-name 0.5}]}
                                 :transform [{:calculate (gstring/format "isDefined(datum.origin) ? %s : %s * -sin(%s * (PI/180) - PI) + %s"
                                                                         ox r-name theta-name ox)
                                              :as        "x"}
                                             {:calculate (gstring/format "isDefined(datum.origin) ? %s : %s * -cos(%s * (PI/180) - PI) + %s"
                                                                         oy r-name theta-name oy)
                                              :as        "y"}
                                             {:calculate (->str-literal legend-id)
                                              :as        "series"}]
                                 :mark      {:type        "line"
                                             :clip        true
                                             :strokeDash  (if dashed? [4,4] [1,0])
                                             :strokeWidth arrow-stroke-width
                                             :color       legend-id
                                             :point       {:shape  "arrow"
                                                           :filled true
                                                           :color  legend-id
                                                           :angle  theta
                                                           :size   {:expr (str "isDefined(datum.origin) ? 0 : " arrow-head-size)}}}
                                 :encoding  {:color {:field "series"
                                                     :type  "nominal"}
                                             :x     {:field "x"
                                                     :type  "quantitative"}
                                             :y     {:field "y"
                                                     :type  "quantitative"}}}))
        (update :params #(into % [{:name r-name :value r}
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

(defn- axis-end-label
  "A text-mark layer that always draws `text` at (x 0, y `y`). `:color`/`:opacity`
  are given as values (not fields) so the label ignores the legend's nominal
  `series` color and the `visible_series` opacity condition that the top-level
  encoding otherwise imposes on every layer."
  [text y baseline dy]
  {:data     {:values [{}]}
   :mark     {:type "text" :text text :fontWeight "bold" :fontSize 13 :baseline baseline :dy dy}
   :encoding {:x       {:datum 0 :type "quantitative"}
              :y       {:datum y :type "quantitative"}
              :color   {:value "#333"}
              :opacity {:value 1}}})

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
   :title  - title displayed on the axis (defaults to \"x\" or \"y\")
   :units  - units label appended to the axis title in parentheses (e.g. \"ft\" displays as \"x (ft)\")

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
  [{:keys [title width height x-axis y-axis ellipses arrows scatter-plots hidden-by-default-series hide-axis-numbers?]}]
  (let [base-schema   {:$schema     "https://vega.github.io/schema/vega-lite/v5.1.1.json"
                       :title       {:text     title
                                     :fontSize 20}
                       :description "diagram"
                       :width       width
                       :height      height
                       :encoding    {:x       {:axis  {:title       (if (:units x-axis)
                                                                      (str (:title x-axis "x") " (" (:units x-axis) ")")
                                                                      (:title x-axis "x"))
                                                       :titleAnchor "start"
                                                       :titleAlign  "left"
                                                       :labels      (not hide-axis-numbers?)
                                                       :grid        (not hide-axis-numbers?)
                                                       :offset      (compute-axis-offset (:domain y-axis)
                                                                                         height)
                                                       :tickMinStep (or (:tick-min-step x-axis) 1)}
                                               :scale {:domain (:domain x-axis)}}
                                     :y       {:axis  {:title       (when-not (:pos-label y-axis)
                                                                      (if (:units y-axis)
                                                                        (str (:title y-axis "y") " (" (:units y-axis) ")")
                                                                        (:title y-axis "y")))
                                                       :titleAnchor "end"
                                                       :titleAngle  0
                                                       :titleAlign  "left"
                                                       :labels      (not hide-axis-numbers?)
                                                       :grid        (not hide-axis-numbers?)
                                                       :offset      (compute-axis-offset (:domain x-axis)
                                                                                         width)
                                                       :tickMinStep (or (:tick-min-step y-axis) 1)}
                                               :scale {:domain (:domain y-axis)}}
                                     :color   {:type   "nominal"
                                               :scale  {:domain []
                                                        :range  []}
                                               :legend {:title             "Legend"
                                                        :symbolType        "stroke"
                                                        :symbolSize        500
                                                        :symbolStrokeWidth 5.0
                                                      ;; gray out the legend entry of any series
                                                      ;; that has been toggled off (unselected)
                                                        :unselectedOpacity 0.35
                                                        :labelFontSize     15}}
                                   ;; Click a legend entry to toggle its series. The selection
                                   ;; holds the VISIBLE series (pre-populated below), so a series
                                   ;; is shown when in the selection and hidden otherwise;
                                   ;; `:empty false` keeps the all-toggled-off state fully hidden.
                                     :opacity {:condition {:param "visible_series"
                                                           :empty false
                                                           :value 1}
                                               :value     0}}
                       :layer       []
                       :params      []}
        ;; The arrowhead is a fixed-pixel point mark CENTERED on the arrow tip, so only
        ;; half its length pokes past the line end. Pull each arrow back by that half —
        ;; converting the arrowhead's pixel length to data units via the (square) plot
        ;; scale — so the head lands at the tip instead of overshooting the ellipse.
        [x-min x-max] (:domain x-axis)
        head-len-data (if (and x-min x-max (pos? width) (> x-max x-min))
                        (* 0.5
                           (Math/sqrt arrow-head-size)      ; ≈ arrowhead length (px)
                           (/ (- x-max x-min) width))       ; data units per px
                        0)
        arrows        (mapv (fn [a] (update a :r (fn [r] (max 0 (- (or r 0) head-len-data)))))
                            arrows)
        ;; Marks are reduced in legend order (color-scale :domain is built by
        ;; appending each series). Arrows first, then ellipses, then scatter-plots —
        ;; so the Fire Shape ellipse (Fire Perimeter) lands last. Diagrams without
        ;; arrows (contain) keep ellipses-before-scatter, unchanged.
        spec          (as-> base-schema $
                        (reduce (fn [acc arrow] (add-arrow acc arrow)) $ arrows)
                        ;; Reverse only the arrow layers so an arrow that appears earlier in the
                        ;; legend draws on top of a later one (e.g. Surface Fire Spread above
                        ;; Heading Fire). The color-scale :domain — and thus the legend order —
                        ;; is left untouched. This was done specificially for the Fire Shape Diagram
                        ;; If other diagrams where layers need specific order rules, this will need to be reworked.
                        (update $ :layer (comp vec reverse))
                        (reduce (fn [acc ellipse] (add-ellipse acc ellipse)) $ ellipses)
                        (reduce (fn [acc scatter-plot] (add-scatter-plot acc scatter-plot)) $ scatter-plots))
        ;; The legend-bound selection must live on a SINGLE layer: a top-level param in a
        ;; layered spec is instantiated once per layer, raising "Duplicate signal name".
        ;; Pre-populate it with every series so all start visible/ungrayed; each plain click
        ;; (`:toggle "true"`) then adds/removes one series, driving both its mark opacity and
        ;; its legend `:unselectedOpacity` graying.
        spec          (cond-> spec
                        (seq (:layer spec))
                        (update-in [:layer 0 :params] (fnil conj [])
                                   {:name   "visible_series"
                                    ;; Pre-select every series EXCEPT those flagged to start hidden
                                    ;; (arrows with :arrow/default-visible? false); those begin grayed
                                    ;; and are revealed by clicking the legend.
                                    :value  (->> (get-in spec [:encoding :color :scale :domain])
                                                 (remove (or hidden-by-default-series #{}))
                                                 (mapv (fn [id] {:series id})))
                                    :select {:type "point" :fields ["series"] :toggle "true"}
                                    :bind   "legend"}))
        ;; Optional directional end-labels on the y-axis (e.g. Upslope/Downslope on
        ;; the Fire Shape diagram). Appended last so layer 0 keeps `visible_series`.
        [y-min y-max] (:domain y-axis)
        spec          (cond-> spec
                        (:pos-label y-axis)
                        (update :layer conj (axis-end-label (:pos-label y-axis) y-max "top" 2))
                        (:neg-label y-axis)
                        (update :layer conj (axis-end-label (:neg-label y-axis) y-min "bottom" -2)))]
    [:div
     [vega-box spec]]))
