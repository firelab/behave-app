(ns behave.components.results.diagrams
  (:require [behave.components.vega.diagram :refer [output-diagram]]
            [behave.translate               :refer [<t]]
            [clojure.set                    :refer [rename-keys]]
            [clojure.string                 :as str]
            [goog.string                    :as gstring]
            [re-frame.core                  :refer [subscribe]]))

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

(defn- construct-diagram [ws-uuid
                          {row-id              :worksheet.diagram/row-id
                           ellipses            :worksheet.diagram/ellipses
                           arrows              :worksheet.diagram/arrows
                           scatter-plots       :worksheet.diagram/scatter-plots
                           group-variable-uuid :worksheet.diagram/group-variable-uuid}]

  (let [cms-diagram        @(subscribe [:wizard/diagram-by-gv-uuid group-variable-uuid])
        title              (if-let [tk (:diagram/title-translation-key cms-diagram)]
                             @(<t tk)
                             (:diagram/title cms-diagram))
        x-units-uuid       (:diagram/x-units-uuid cms-diagram)
        y-units-uuid       (:diagram/y-units-uuid cms-diagram)
        x-axis-title       (if-let [tk (:diagram/x-axis-title-translation-key cms-diagram)]
                             @(<t tk)
                             (:diagram/x-axis-title cms-diagram))
        y-axis-title       (if-let [tk (:diagram/y-axis-title-translation-key cms-diagram)]
                             @(<t tk)
                             (:diagram/y-axis-title cms-diagram))
        symmetric-axes?    (:diagram/symmetric-axes? cms-diagram)
        mirror-y?          (:diagram/mirror-y? cms-diagram)
        connect-points?    (:diagram/connect-points? cms-diagram)
        symmetric?         (if (some? symmetric-axes?) symmetric-axes? true)
        do-mirror?         (if (some? mirror-y?) mirror-y? true)
        x-units-short-code (when x-units-uuid @(subscribe [:vms/units-uuid->short-code x-units-uuid]))
        y-units-short-code (when y-units-uuid @(subscribe [:vms/units-uuid->short-code y-units-uuid]))
        x-vals             (concat (map #(Math/abs (* 2 (:ellipse/semi-minor-axis %))) ellipses)
                                   (map #(Math/abs (:arrow/length %)) arrows)
                                   (->> scatter-plots
                                        (mapcat #(str/split (:scatter-plot/x-coordinates %) ","))
                                        (map #(Math/abs (double %)))))
        y-vals             (concat (map #(Math/abs (* 2 (:ellipse/semi-major-axis %))) ellipses)
                                   (map #(Math/abs (:arrow/length %)) arrows)
                                   (->> scatter-plots
                                        (mapcat #(str/split (:scatter-plot/y-coordinates %) ","))
                                        (map #(Math/abs (double %)))))
        domain             (apply max (concat x-vals y-vals))
        x-domain           (if symmetric? [(* -1 domain) domain] [0 (apply max x-vals)])
        y-max              (* 1.1 (apply max y-vals))
        y-domain           (if symmetric? [(* -1 domain) domain] [(* -0.1 y-max) y-max])]
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
                      :ellipses      (mapv #(rename-keys (into {} %)
                                                         {:ellipse/legend-id       :legend-id
                                                          :ellipse/semi-major-axis :a
                                                          :ellipse/semi-minor-axis :b
                                                          :ellipse/rotation        :phi
                                                          :ellipse/color           :color})
                                           ellipses)
                      :arrows        (mapv #(rename-keys (into {} %)
                                                         {:arrow/legend-id :legend-id
                                                          :arrow/length    :r
                                                          :arrow/rotation  :theta
                                                          :arrow/color     :color
                                                          :arrow/dashed?   :dashed?})
                                           arrows)
                      :scatter-plots (mapv (fn [{legend-id     :scatter-plot/legend-id
                                                 x-coordinates :scatter-plot/x-coordinates
                                                 y-coordinates :scatter-plot/y-coordinates
                                                 color         :scatter-plot/color}]
                                             (let [x-doubles (map double (str/split x-coordinates ","))
                                                   y-doubles (map double (str/split y-coordinates ","))]
                                               {:legend-id legend-id
                                                :color     color
                                                :connect?  connect-points?
                                                :data      (cond-> (mapv (fn [x y] {"x" x "y" y})
                                                                         x-doubles
                                                                         y-doubles)
                                                             do-mirror? (concat (mapv (fn [x y] {"x" x "y" (* -1 y)})
                                                                                      x-doubles
                                                                                      y-doubles)))}))
                                           scatter-plots)}]
     (construct-summary-table ws-uuid group-variable-uuid row-id)]))

(defn result-diagrams [ws-uuid]
  (let [*ws (subscribe [:worksheet-entity ws-uuid])]
    (when (seq (:worksheet/diagrams @*ws))
      [:div.wizard-results__diagrams {:id "diagram"}
       [:div.wizard-notes__header "Diagram"]
       (map #(construct-diagram ws-uuid %)
            (sort-by :worksheet.diagram/row-id
                     (:worksheet/diagrams @*ws)))])))
