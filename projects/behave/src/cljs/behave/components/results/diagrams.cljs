(ns behave.components.results.diagrams
  (:require [behave.components.vega.diagram :refer [output-diagram]]
            [behave.translate               :refer [<t]]
            [clojure.set                    :refer [rename-keys]]
            [clojure.string                 :as str]
            [goog.string                    :as gstring]
            [re-frame.core                  :refer [subscribe]]
            [version-utils.interface        :as vu]))

(defn- construct-summary-table [ws-uuid group-variable-uuid row-id]
  (let [gv-order          @(subscribe [:vms/group-variable-order])
        outputs-to-filter (set @(subscribe [:wizard/diagram-output-gv-uuids group-variable-uuid]))
        output-formatters @(subscribe [:worksheet/result-table-formatters outputs-to-filter])
        outputs           (->> (subscribe [:worksheet/output-gv-uuid+value+units ws-uuid row-id])
                               deref
                               (filter (fn [[gv-uuid]] (contains? outputs-to-filter gv-uuid)))
                               (sort-by (fn [[gv-uuid]] (.indexOf gv-order gv-uuid)))
                               (map (fn resolve-gv-uuid->name [[gv-uuid value units]]
                                      (let [fmt-fn (get output-formatters gv-uuid identity)]
                                        [@(subscribe [:wizard/gv-uuid->resolve-result-variable-name gv-uuid])
                                         (fmt-fn value)
                                         units]))))
        inputs-to-filter  (set @(subscribe [:wizard/diagram-input-gv-uuids group-variable-uuid]))
        input-formatters  @(subscribe [:worksheet/result-table-formatters inputs-to-filter])
        inputs            (->> (subscribe [:worksheet/input-gv-uuid+value+units ws-uuid row-id])
                               deref
                               (filter (fn [[gv-uuid]] (contains? inputs-to-filter gv-uuid)))
                               (sort-by (fn [[gv-uuid]] (.indexOf gv-order gv-uuid)))
                               (map (fn resolve-gv-uuid->name [[gv-uuid value units]]
                                      (let [fmt-fn (get input-formatters gv-uuid identity)]
                                        [@(subscribe [:wizard/gv-uuid->resolve-result-variable-name gv-uuid])
                                         (fmt-fn value)
                                         units]))))]
    [:div
     [:table.diagram__table
      (map (fn [[variable-name value units]]
             [:tr
              [:td (str variable-name ":")]
              [:td (if (seq units)
                     (str value " (" units ")")
                     value)]])
           inputs)]
     [:table.diagram__table
      (map (fn [[variable-name value units]]
             [:tr
              [:td (str variable-name ":")]
              [:td (if (seq units)
                     (str value " (" units ")")
                     value)]])
           outputs)]]))

(defn- build-title [ws-uuid diagram-title row-id]
  (let [multi-valued-input-uuids  (set @(subscribe [:worksheet/multi-value-input-uuids ws-uuid]))
        *gv-order                 (subscribe [:vms/group-variable-order ws-uuid])
        input-gv-uuid+value+units (sort-by #(.indexOf @*gv-order (first %))
                                           @(subscribe [:worksheet/input-gv-uuid+value+units ws-uuid row-id]))
        result                    (->> input-gv-uuid+value+units
                                       (filter (fn [[gv-uuid _ _]]
                                                 (contains? multi-valued-input-uuids gv-uuid)))
                                       (map (fn [[gv-uuid value unit]]
                                              (gstring/format
                                               "%s=%s (%s) "
                                               @(subscribe [:wizard/gv-uuid->default-variable-name gv-uuid])
                                               value
                                               unit)))
                                       (apply str))]
    (if (str/blank? result)
      diagram-title
      (gstring/format "%s for: %s" diagram-title result))))

(defn- compute-domains [{:keys [ellipses arrows scatter-plots
                                show-q1? show-q2? show-q3? show-q4?]}]
  (let [buffer     0.10
        ellipse-x  (map #(Math/abs (* 2 (:ellipse/semi-minor-axis %))) ellipses)
        ellipse-y  (map #(Math/abs (* 2 (:ellipse/semi-major-axis %))) ellipses)
        arrow-mag  (map #(Math/abs (:arrow/length %)) arrows)
        scatter-x  (->> scatter-plots
                        (mapcat #(str/split (:scatter-plot/x-coordinates %) ","))
                        (mapv js/parseFloat))
        scatter-y  (->> scatter-plots
                        (mapcat #(str/split (:scatter-plot/y-coordinates %) ","))
                        (mapv js/parseFloat))
        shape-max  (apply max 0 (concat ellipse-x ellipse-y arrow-mag))
        scale      (fn [side-max] (* (+ 1 buffer) (max shape-max side-max)))
        x-pos-max  (scale (apply max 0 (filter pos? scatter-x)))
        x-neg-max  (scale (apply max 0 (->> scatter-x (filter neg?) (map -))))
        y-pos-max  (scale (apply max 0 (filter pos? scatter-y)))
        y-neg-max  (scale (apply max 0 (->> scatter-y (filter neg?) (map -))))
        x-pos?     (or show-q1? show-q4?)
        x-neg?     (or show-q2? show-q3?)
        y-pos?     (or show-q1? show-q2?)
        y-neg?     (or show-q3? show-q4?)
        all-quads? (and show-q1? show-q2? show-q3? show-q4?)
        square-max (max x-pos-max x-neg-max y-pos-max y-neg-max)
        x-right    (if all-quads? square-max x-pos-max)
        x-left     (if all-quads? square-max x-neg-max)
        y-top      (if all-quads? square-max y-pos-max)
        y-bot      (if all-quads? square-max y-neg-max)]
    {:x-domain [(if x-neg? (* -1 x-left) 0) (if x-pos? x-right 0)]
     :y-domain [(if y-neg? (* -1 y-bot)  0) (if y-pos? y-top   0)]}))

(defn- construct-diagram [ws-uuid
                          {row-id              :worksheet.diagram/row-id
                           ellipses            :worksheet.diagram/ellipses
                           arrows              :worksheet.diagram/arrows
                           scatter-plots       :worksheet.diagram/scatter-plots
                           group-variable-uuid :worksheet.diagram/group-variable-uuid}]

  (let [scatter-plot-legacy? (neg? (vu/compare-versions @(subscribe [:worksheet/version ws-uuid]) "7.1.5"))
        cms-diagram          @(subscribe [:wizard/diagram-by-gv-uuid group-variable-uuid])
        title                (if-let [tk (:diagram/title-translation-key cms-diagram)]
                               @(<t tk)
                               (:diagram/title cms-diagram))
        x-units-uuid         (:diagram/x-units-uuid cms-diagram)
        y-units-uuid         (:diagram/y-units-uuid cms-diagram)
        x-axis-title         (if-let [tk (:diagram/x-axis-title-translation-key cms-diagram)]
                               @(<t tk)
                               (:diagram/x-axis-title cms-diagram))
        y-axis-title         (if-let [tk (:diagram/y-axis-title-translation-key cms-diagram)]
                               @(<t tk)
                               (:diagram/y-axis-title cms-diagram))
        show-q1?             (get cms-diagram :diagram/show-quadrant-1? true)
        show-q2?             (get cms-diagram :diagram/show-quadrant-2? true)
        show-q3?             (get cms-diagram :diagram/show-quadrant-3? true)
        show-q4?             (get cms-diagram :diagram/show-quadrant-4? true)
        connect-points?      (:diagram/connect-points? cms-diagram)
        x-units-short-code   (when x-units-uuid @(subscribe [:vms/units-uuid->short-code x-units-uuid]))
        y-units-short-code   (when y-units-uuid @(subscribe [:vms/units-uuid->short-code y-units-uuid]))
        {:keys [x-domain
                y-domain]}   (compute-domains {:ellipses      ellipses
                                               :arrows        arrows
                                               :scatter-plots scatter-plots
                                               :show-q1?      show-q1?
                                               :show-q2?      show-q2?
                                               :show-q3?      show-q3?
                                               :show-q4?      show-q4?})]
    [:div.diagram
     [output-diagram {:title         (build-title ws-uuid title row-id)
                      :width         500
                      :height        500
                      :x-axis        {:domain        x-domain
                                      :units         x-units-short-code
                                      :title         (or x-axis-title "x")
                                      :tick-min-step 5}
                      :y-axis        {:domain        y-domain
                                      :title         (or y-axis-title "y")
                                      :units         y-units-short-code
                                      :tick-min-step 5}
                      :ellipses      (mapv #(-> (rename-keys (into {} %)
                                                             {:ellipse/legend-id       :legend-id
                                                              :ellipse/semi-major-axis :a
                                                              :ellipse/semi-minor-axis :b
                                                              :ellipse/rotation        :phi
                                                              :ellipse/color           :color})
                                                (update :legend-id (fn [k] @(<t k))))
                                           ellipses)
                      :arrows        (mapv #(-> (rename-keys (into {} %)
                                                             {:arrow/legend-id :legend-id
                                                              :arrow/length    :r
                                                              :arrow/rotation  :theta
                                                              :arrow/color     :color
                                                              :arrow/dashed?   :dashed?})
                                                (update :legen-id (fn [k] @(<t k))))
                                           arrows)
                      :scatter-plots (mapv (fn [{legend-id     :scatter-plot/legend-id
                                                 x-coordinates :scatter-plot/x-coordinates
                                                 y-coordinates :scatter-plot/y-coordinates
                                                 color         :scatter-plot/color}]
                                             (let [x-doubles (map double (str/split x-coordinates ","))
                                                   y-doubles (map double (str/split y-coordinates ","))]
                                               {:legend-id @(<t legend-id)
                                                :color     color
                                                :connect?  connect-points?
                                                :data      (let [pts (mapv (fn [x y] {"x" x "y" y})
                                                                           x-doubles
                                                                           y-doubles)]
                                                             (if scatter-plot-legacy?
                                                               (into pts (mapv (fn [x y] {"x" x "y" (* -1 y)})
                                                                               x-doubles
                                                                               y-doubles))
                                                               pts))}))
                                           scatter-plots)}]
     (construct-summary-table ws-uuid group-variable-uuid row-id)]))

(defn result-diagrams
  "Reagent component rendering all diagrams for a worksheet."
  [ws-uuid]
  (let [*ws (subscribe [:worksheet-entity ws-uuid])]
    (when (seq (:worksheet/diagrams @*ws))
      [:div.wizard-results__diagrams {:id "diagram"}
       [:div.wizard-notes__header "Diagram"]
       (map #(construct-diagram ws-uuid %)
            (sort-by :worksheet.diagram/row-id
                     (:worksheet/diagrams @*ws)))])))
