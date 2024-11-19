(ns behave.components.results.graphs
  (:require [behave.components.vega.result-chart :refer [result-chart]]
            [re-frame.core                       :refer [subscribe]]))

(defn result-graphs [ws-uuid cell-data]
  (let [worksheet      @(subscribe [:worksheet ws-uuid])
        graph-enabled? (get-in worksheet [:worksheet/graph-settings :graph-settings/enabled?])
        graph-settings @(subscribe [:worksheet/graph-settings ws-uuid])]
    (when (and graph-enabled? graph-settings)
      (let [*output-uuids (subscribe [:worksheet/output-uuids-filtered ws-uuid])
            graph-data    (->> cell-data
                               (group-by first)
                               (reduce (fn [acc [_row-id cell-data]]
                                         (conj acc
                                               (reduce (fn [acc [_row-id col-uuid _repeat-id value]]
                                                         (let [fmt-fn (-> @(subscribe [:worksheet/result-table-formatters [col-uuid]])
                                                                          (get col-uuid identity))]
                                                           (assoc acc
                                                                  @(subscribe [:wizard/gv-uuid->resolve-result-variable-name col-uuid])
                                                                  (fmt-fn value))))
                                                       {}
                                                       cell-data)))
                                       []))
            x-axis-limit  (:graph-settings/x-axis-limits graph-settings)
            x-min         (:x-axis-limit/min x-axis-limit)
            x-max         (:x-axis-limit/max x-axis-limit)]
        [:div.wizard-results__graphs {:id "graph"}
         [:div.wizard-graph__header "Graphs"]
         (for [output-uuid @*output-uuids
               :let        [y-axis-limit (->> (:graph-settings/y-axis-limits graph-settings)
                                              (filter #(= output-uuid (:y-axis-limit/group-variable-uuid %)))
                                              (first))
                            y-min (:y-axis-limit/min y-axis-limit)
                            y-max (:y-axis-limit/max y-axis-limit)]]
           [:<>
            [:div.wizard-graph__output-header
             @(subscribe [:wizard/gv-uuid->resolve-result-variable-name output-uuid])]
            [:div.wizard-results__graph
             (let [x-axis-group-variable-uuid  (:graph-settings/x-axis-group-variable-uuid graph-settings)
                   z-axis-group-variable-uuid  (:graph-settings/z-axis-group-variable-uuid graph-settings)
                   z2-axis-group-variable-uuid (:graph-settings/z2-axis-group-variable-uuid graph-settings)]
               (result-chart
                {:data   graph-data
                 :x      {:name      @(subscribe [:wizard/gv-uuid->resolve-result-variable-name
                                                  x-axis-group-variable-uuid])
                          :scale     (when (and x-min x-max) [x-min x-max])
                          :discrete? @(subscribe [:wizard/discrete-group-variable? x-axis-group-variable-uuid])}
                 :y      {:name  @(subscribe [:wizard/gv-uuid->resolve-result-variable-name output-uuid])
                          :scale (when (and y-min y-max) [y-min y-max])}
                 :z      {:name      @(subscribe [:wizard/gv-uuid->resolve-result-variable-name
                                                  z-axis-group-variable-uuid])
                          :discrete? true}
                 :z2     {:name    @(subscribe [:wizard/gv-uuid->resolve-result-variable-name
                                                z2-axis-group-variable-uuid])
                          :columns 2}
                 :width  250
                 :height 250}))]])]))))
