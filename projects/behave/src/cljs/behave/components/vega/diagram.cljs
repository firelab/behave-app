(ns behave.components.vega.diagram
  (:require [cljsjs.vega-embed]
            [behave.components.vega.core :refer [vega-box]]
            [goog.string                    :as gstring]
            [clojure.string :as str]))

(defn- add-ellipse [config {:keys [id color] :as _params}]
  (let [a   (str/join "_" ["A" id])
        b   (str/join "_" ["B" id])
        cx  (str/join "_" ["CX" id])
        cy  (str/join "_" ["CY" id])
        phi (str/join "_" ["PHI" id])]
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
                          :encoding  {:color {:datum id}
                                      :x     {:field "x"
                                              :type  "quantitative"}
                                      :y     {:field "y"
                                              :type  "quantitative"}
                                      :order {:field "t"
                                              :type  "quantitative"}}}))
        (update :params #(into %  [{:name  a
                                    :value 50
                                    :bind  {:input "range" :min 0 :max 500 :step 1}}
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
        (update-in [:encoding :color :scale :domain] #(conj % id))
        (update-in [:encoding :color :scale :range] #(conj % color)))))

(defn- add-arrow [config {:keys [id color] :as _params}]
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
        (update :params #(into %  [{:name  r
                                    :value 50
                                    :bind  {:input "range" :min 0 :max 100 :step 1}}
                                   {:name  theta
                                    :value 0.0
                                    :bind  {:input "range" :min 0 :max 360 :step 1}}]))
        (update-in [:encoding :color :scale :domain] #(conj % id))
        (update-in [:encoding :color :scale :range] #(conj % color)))))

(defn demo-output-diagram [{:keys [width height x-axis y-axis ellipses arrows]}]
  (let [base-config {:$schema     "https://vega.github.io/schema/vega-lite/v5.1.1.json"
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
                                           :legend {:symbolSize        500
                                                    :symbolStrokeWidth 5.0
                                                    :labelFontSize     20}}}
                     :layer       []
                     :params      []}]
    [:div
     [vega-box
      (as-> base-config $
        (reduce (fn [acc ellipse] (add-ellipse acc ellipse)) $ ellipses)
        (reduce (fn [acc arrow] (add-arrow acc arrow)) $ arrows))]]))
