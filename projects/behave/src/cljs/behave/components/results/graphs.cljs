(ns behave.components.results.graphs
  (:require [behave.components.vega.result-chart :refer [result-chart]]
            [re-frame.core                       :refer [subscribe]]))

(defn result-graphs [ws-uuid cell-data]
  (let [worksheet      @(subscribe [:worksheet ws-uuid])
        graph-enabled? (get-in worksheet [:worksheet/graph-settings :graph-settings/enabled?])
        graph-settings @(subscribe [:worksheet/graph-settings ws-uuid])]
    (when (and graph-enabled? graph-settings)
      (let [*output-uuids (subscribe [:worksheet/all-output-uuids ws-uuid])
            graph-data    (->> cell-data
                               (group-by first)
                               (reduce (fn [acc [_row-id cell-data]]
                                         (conj acc
                                               (reduce (fn [acc [_row-id col-uuid _repeat-id value]]
                                                         (let [fmt-fn (-> @(subscribe [:worksheet/result-table-formatters [col-uuid]])
                                                                          (get col-uuid identity))]
                                                           (assoc acc
                                                                  @(subscribe [:wizard/gv-uuid->variable-name-2 col-uuid])
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
             @(subscribe [:wizard/gv-uuid->variable-name-2 output-uuid])]
            [:div.wizard-results__graph
             (let [x-axis-group-variable-uuid  (:graph-settings/x-axis-group-variable-uuid graph-settings)
                   z-axis-group-variable-uuid  (:graph-settings/z-axis-group-variable-uuid graph-settings)
                   z2-axis-group-variable-uuid (:graph-settings/z2-axis-group-variable-uuid graph-settings)]
               (result-chart {:data   graph-data
                              :x      {:name  @(subscribe [:wizard/gv-uuid->variable-name-2
                                                           x-axis-group-variable-uuid])
                                       :scale [x-min x-max]}
                              :y      {:name  @(subscribe [:wizard/gv-uuid->variable-name-2 output-uuid])
                                       :scale [y-min y-max]}
                              :z      {:name @(subscribe [:wizard/gv-uuid->variable-name-2 z-axis-group-variable-uuid])}
                              :z2     {:name    @(subscribe [:wizard/gv-uuid->variable-name-2
                                                             z2-axis-group-variable-uuid])
                                       :columns 2}
                              :width  250
                              :height 250}))]])]))))
