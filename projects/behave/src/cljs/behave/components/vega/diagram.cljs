(ns behave.components.vega.diagram
  (:require [cljsjs.vega-embed]
            [behave.components.vega.core :refer [vega-box]]
            [goog.string                    :as gstring]))

(defn- add-ellipse [config {:keys [id color a b phi x-offset stroke-dash] :as _params}]
  (let [a-name   (str "A_" id)
        b-name   (str "B_" id)
        phi-name (str "PHI_" id)
        cx-name  (str "CX_" id)
        cy-name  (str "CY_" id)]
    (-> config
        (update :layer
                #(conj % {:mark      {:type       "line"
                                      :strokeDash (or stroke-dash [1 0])
                                      :stroke     color}
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
                          :encoding  {:color {:datum id}
                                      :x     {:field "x"
                                              :type  "quantitative"}
                                      :y     {:field "y"
                                              :type  "quantitative"}
                                      :order {:field "t"
                                              :type  "quantitative"}}}))
        (update :params #(into %  [{:name a-name :value a}
                                   {:name b-name :value b}
                                   {:name phi-name :value phi}
                                   {:name cx-name :expr (gstring/format "(%s * -sin(%s * (PI / 180) - PI) + %s)" a phi (or x-offset 0))}
                                   {:name cy-name :expr (gstring/format "(%s * -cos(%s * (PI / 180) - PI))" a phi)}]))
        (update-in [:encoding :color :scale :domain] #(conj % id))
        (update-in [:encoding :color :scale :range] #(conj % color)))))

(defn- add-arrow [config {:keys [id color r theta] :as _params}]
  (let [r-name     (str "R_" id)
        theta-name (str "THETA_" id)]
    (-> config
        (update :layer #(conj % {:data      {:values [{r-name 0.0 "origin" true}
                                                      {r-name 0.5}]}
                                 :transform [{:calculate (gstring/format "isDefined(datum.origin) ? 0 : %s * -sin(%s * (PI/180) - PI)"
                                                                         r-name theta-name)
                                              :as        "x"}
                                             {:calculate (gstring/format "isDefined(datum.origin)? 0 : %s * -cos(%s * (PI/180) - PI)"
                                                                         r-name theta-name)
                                              :as        "y"}]
                                 :mark      {:type  "line"
                                             :color id
                                             :point {:shape  "arrow"
                                                     :filled true
                                                     :color  id
                                                     :angle  {:expr theta}
                                                     :size   {:expr "isDefined(datum.origin) ? 0 : 200"}}}
                                 :encoding  {:color {:datum id}
                                             :x     {:field "x"
                                                     :type  "quantitative"}
                                             :y     {:field "y"
                                                     :type  "quantitative"}}}))
        (update :params #(into %  [{:name r-name :value r}
                                   {:name theta-name :value theta}]))
        (update-in [:encoding :color :scale :domain] #(conj % id))
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

(defn output-diagram [{:keys [title width height x-axis y-axis ellipses arrows]}]
  (let [base-config {:$schema     "https://vega.github.io/schema/vega-lite/v5.1.1.json"
                     :title       title
                     :description "diagram"
                     :width       width
                     :height      height
                     :encoding    {:x     {:axis  {:title  "x"
                                                   :offset (compute-axis-offset (:domain y-axis)
                                                                                height)}
                                           :scale {:domain (:domain x-axis)}}
                                   :y     {:axis  {:title  "y"
                                                   :offset (compute-axis-offset (:domain x-axis)
                                                                                width)}
                                           :scale {:domain (:domain y-axis)}}
                                   :color {:type   "nominal"
                                           :scale  {:domain []
                                                    :range  []}
                                           :legend {:symbolSize        500
                                                    :symbolStrokeWidth 5.0
                                                    :labelFontSize     15}}}
                     :layer       []
                     :params      []}]
    [:div
     [vega-box
      (as-> base-config $
        (reduce (fn [acc ellipse] (add-ellipse acc ellipse)) $ ellipses)
        (reduce (fn [acc arrow] (add-arrow acc arrow)) $ arrows))]]))
