(ns behave.components.graph
  (:require [re-frame.core    :refer [subscribe]]
            [behave.print.subs]
            [behave.components.chart         :refer [chart]]))

(defn result-graph [ws-uuid cell-data]
  (letfn [(uuid->variable-name [uuid]
            (:variable/name @(subscribe [:wizard/group-variable uuid])))]
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
                                                           (assoc acc
                                                                  (-> (subscribe [:wizard/group-variable col-uuid])
                                                                      deref
                                                                      :variable/name)
                                                                  (js/parseFloat value)))
                                                         {}
                                                         cell-data)))
                                         []))]
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
               (:variable/name @(subscribe [:wizard/group-variable output-uuid]))]
              [:div.wizard-results__graph
               (chart {:data   graph-data
                       :x      {:name (-> (:graph-settings/x-axis-group-variable-uuid graph-settings)
                                          (uuid->variable-name))}
                       :y      {:name  (:variable/name @(subscribe [:wizard/group-variable output-uuid]))
                                :scale [y-min y-max]}
                       :z      {:name (-> (:graph-settings/z-axis-group-variable-uuid graph-settings)
                                          (uuid->variable-name))}
                       :z2     {:name    (-> (:graph-settings/z2-axis-group-variable-uuid graph-settings)
                                             (uuid->variable-name))
                                :columns 2}
                       :width  250
                       :height 250})]])])))))
