(ns behave.components.results.diagrams
  (:require [behave.components.vega.diagram :refer [output-diagram]]
            [clojure.set                    :refer [rename-keys]]
            [clojure.string                 :as str]
            [re-frame.core                  :refer [subscribe]]))

(defn- construct-summary-table [ws-uuid group-variable-uuid row-id]
  (let [gv-order @(subscribe [:vms/group-variable-order])
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

(defn- construct-diagram [ws-uuid
                          {row-id              :worksheet.diagram/row-id
                           ellipses            :worksheet.diagram/ellipses
                           arrows              :worksheet.diagram/arrows
                           scatter-plots       :worksheet.diagram/scatter-plots
                           title               :worksheet.diagram/title
                           group-variable-uuid :worksheet.diagram/group-variable-uuid}]

  (let [domain (apply max (concat (map #(Math/abs (* 2 (:ellipse/semi-minor-axis %))) ellipses)
                                  (map #(Math/abs (* 2 (:ellipse/semi-major-axis %))) ellipses)
                                  (map #(Math/abs (:arrow/length %)) arrows)
                                  (->> scatter-plots
                                       (mapcat #(str/split (:scatter-plot/x-coordinates %) ","))
                                       (map #(Math/abs (double %))))
                                  (->> scatter-plots
                                       (mapcat #(str/split (:scatter-plot/y-coordinates %) ","))
                                       (map #(Math/abs (double %))))))]
    [:div.diagram
     [output-diagram {:title         (str title " for result row: " (inc row-id))
                      :width         500
                      :height        500
                      :x-axis        {:domain        [(* -1 domain) domain]
                                      :title         "x"
                                      :tick-min-step 5}
                      :y-axis        {:domain        [(* -1 domain) domain]
                                      :title         "y"
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
                                                :data      (concat
                                                            (mapv (fn [x y]
                                                                    {"x" x
                                                                     "y" y})
                                                                  x-doubles
                                                                  y-doubles)
                                                            (mapv (fn [x y]
                                                                    {"x" x
                                                                     "y" (* -1 y)})
                                                                  x-doubles
                                                                  y-doubles))}))
                                           scatter-plots)}]
     (construct-summary-table ws-uuid group-variable-uuid row-id)]))

(defn result-diagrams [ws-uuid]
  (let [*ws (subscribe [:worksheet-entity ws-uuid])]
    (when (seq (:worksheet/diagrams @*ws))
      [:div.wizard-results__diagrams {:id "diagram"}
       [:div.wizard-notes__header "Diagram"]
       (map #(construct-diagram ws-uuid % )
            (sort-by :worksheet.diagram/row-id
                     (:worksheet/diagrams @*ws)))])))
