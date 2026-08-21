(ns behave.components.results.graphs
  (:require [behave.components.core              :as c]
            [behave.components.vega.result-chart :refer [result-chart]]
            [behave.modal.core                   :refer [modal-content]]
            [number-utils.core                   :refer [parse-float]]
            [re-frame.core                       :refer [dispatch subscribe]]))

(def ^:private inline-chart-size
  "Width/height of a graph as it sits on the Results page."
  250)

(defn- cell-data->graph-data
  "Turns result-table cell data into the row maps Vega plots, keyed by result
  variable name."
  [cell-data]
  (->> cell-data
       (group-by first)
       (reduce (fn [acc [_row-id cells]]
                 (conj acc
                       (->> (reduce (fn [acc [_row-id col-uuid _repeat-id value]]
                                      (assoc acc
                                             @(subscribe [:wizard/gv-uuid->resolve-result-variable-name col-uuid])
                                             (parse-float value)))
                                    {}
                                    cells)
                            (remove (fn [[_ value]] (= value -1)))
                            (into {}))))
               [])))

(defn- chart-spec
  "Builds the `result-chart` params for one output variable at `width`×`height`."
  [{:keys [graph-settings data output-uuid width height]}]
  (let [x-axis-limit                (:graph-settings/x-axis-limits graph-settings)
        x-min                       (:x-axis-limit/min x-axis-limit)
        x-max                       (:x-axis-limit/max x-axis-limit)
        y-axis-limit                (->> (:graph-settings/y-axis-limits graph-settings)
                                         (filter #(= output-uuid (:y-axis-limit/group-variable-uuid %)))
                                         (first))
        y-min                       (:y-axis-limit/min y-axis-limit)
        y-max                       (:y-axis-limit/max y-axis-limit)
        x-axis-group-variable-uuid  (:graph-settings/x-axis-group-variable-uuid graph-settings)
        z-axis-group-variable-uuid  (:graph-settings/z-axis-group-variable-uuid graph-settings)
        z2-axis-group-variable-uuid (:graph-settings/z2-axis-group-variable-uuid graph-settings)]
    {:data   data
     :x      {:name      @(subscribe [:wizard/gv-uuid->resolve-result-variable-name
                                      x-axis-group-variable-uuid])
              :scale     (when (and x-min x-max) [x-min x-max])
              :discrete? @(subscribe [:wizard/discrete-group-variable? x-axis-group-variable-uuid])}
     :y      {:name  @(subscribe [:wizard/gv-uuid->resolve-result-variable-name output-uuid])
              :scale (when (and y-min y-max) [y-min y-max])}
     :z      (when z-axis-group-variable-uuid
               {:name      @(subscribe [:wizard/gv-uuid->resolve-result-variable-name
                                        z-axis-group-variable-uuid])
                :discrete? true})
     :z2     (when z2-axis-group-variable-uuid
               {:name    @(subscribe [:wizard/gv-uuid->resolve-result-variable-name
                                      z2-axis-group-variable-uuid])
                :columns 2})
     :width  width
     :height height}))

(defn- pop-out-size
  "Chart size for the pop-out modal, bounded by the viewport so the whole graph
  (and Vega's export menu) stays inside the modal."
  []
  (let [width  (min 1000 (max 300 (- (.-innerWidth js/window) 240)))
        height (min 700 (max 250 (- (.-innerHeight js/window) 280)))]
    [width height]))

(defmethod modal-content :graph
  [{:keys [ws-uuid output-uuid]}]
  (let [graph-settings @(subscribe [:worksheet/graph-settings ws-uuid])
        cell-data      @(subscribe [:worksheet/result-table-cell-data ws-uuid])
        [width height] (pop-out-size)]
    [:div.wizard-results__graph-pop-out
     (result-chart (chart-spec {:graph-settings graph-settings
                                :data           (cell-data->graph-data cell-data)
                                :output-uuid    output-uuid
                                :width          width
                                :height         height}))]))

(defn- pop-out-button
  "Opens `output-uuid`'s graph in a modal. Kept out of the chart itself so that
  Vega's own export menu stays clickable."
  [ws-uuid output-uuid output-name]
  [:div.wizard-graph__pop-out-button
   [c/button {:icon-name "pop-out"
              :variant   "secondary"
              :size      "small"
              :title     "Enlarge graph"
              :on-click  #(dispatch [:modal/open :graph {:ws-uuid     ws-uuid
                                                         :output-uuid output-uuid
                                                         :modal/title output-name
                                                         :modal/size  :large}])}]])

(defn result-graphs
  "Renders the Results graphs for the worksheet `ws-uuid`.

  Options:
  - `:hide-controls?` omits the on-screen-only controls (Graph Settings and the
    pop-out button), which have nothing to act on outside the app — e.g. on the
    printed page."
  [ws-uuid cell-data & [{:keys [hide-controls?]}]]
  (let [graph-enabled? @(subscribe [:wizard/enable-graph-settings? ws-uuid])
        graph-settings @(subscribe [:worksheet/graph-settings ws-uuid])]
    (when (and graph-enabled? graph-settings)
      (let [*output-uuids (subscribe [:worksheet/graphed-output-uuids ws-uuid])
            graph-data    (cell-data->graph-data cell-data)]
        [:div.wizard-results__graphs {:id "graph"}
         [:div.wizard-graph__header "Graphs"]
         (when-not hide-controls?
           [:div.wizard-results__graph-settings-button
            [c/button {:label     "Graph Settings"
                       :variant   "secondary"
                       :icon-name "settings"
                       :on-click  #(dispatch [:graph-settings/toggle])}]])
         (for [output-uuid @*output-uuids
               :when       (not @(subscribe [:wizard/discrete-group-variable? output-uuid]))
               :let        [output-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name
                                                     output-uuid])]]
           ^{:key output-uuid}
           [:div.wizard-results__graph
            [:div.wizard-graph__output-header
             output-name
             (when-not hide-controls?
               [pop-out-button ws-uuid output-uuid output-name])]
            [:div.wizard-results__graph
             (result-chart (chart-spec {:graph-settings graph-settings
                                        :data           graph-data
                                        :output-uuid    output-uuid
                                        :width          inline-chart-size
                                        :height         inline-chart-size}))]])]))))
