(ns behave.components.graph-settings.views
  (:require [behave.components.core                :as c]
            [behave.components.graph-settings.events]
            [behave.components.graph-settings.subs]
            [behave.components.settings-form.views :refer [settings-form]]
            [behave.translate                      :refer [<t bp]]
            [clojure.string                        :as str]
            [re-frame.core                         :refer [dispatch subscribe]]))

(defn- radio-group [{:keys [label selected? variables on-change]}]
  [c/radio-group {:label   label
                  :options (mapv (fn [{group-var-uuid :bp/uuid}]
                                   (let [var-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name group-var-uuid])]
                                     {:value     var-name
                                      :label     var-name
                                      :on-change #(when on-change (on-change group-var-uuid))
                                      :checked?  (= selected? group-var-uuid)}))
                                 variables)}])

(defn graph-settings-modal [ws-uuid]
  (let [*multi-value-input-uuids (subscribe [:worksheet/multi-value-input-uuids ws-uuid])
        group-variables          (->> @*multi-value-input-uuids
                                      (map #(deref (subscribe [:wizard/group-variable %]))))
        multi-valued-input-uuids @(subscribe [:worksheet/multi-value-input-uuids ws-uuid])
        multi-valued-input-count (count multi-valued-input-uuids)
        x-axis-limits            (first @(subscribe [:worksheet/graph-settings-x-axis-limits ws-uuid]))
        x-axis-gv-uuid           (first x-axis-limits)
        [x-default-min
         x-default-max]          @(subscribe [:wizard/x-axis-limit-min+max-defaults ws-uuid x-axis-gv-uuid])
        y-default-min-values     @(subscribe [:worksheet/output-uuid->result-min-or-max-values ws-uuid :min])
        y-default-max-values     @(subscribe [:worksheet/output-uuid->result-min-or-max-values ws-uuid :max])
        enabled?                 @(subscribe [:wizard/enable-graph-settings? ws-uuid])
        x-axis-selected?         (first @(subscribe [:graph-settings/attr-values ws-uuid :graph-settings/x-axis-group-variable-uuid]))
        z-axis-selected?         (first @(subscribe [:graph-settings/attr-values ws-uuid :graph-settings/z-axis-group-variable-uuid]))
        z2-axis-selected?        (first @(subscribe [:graph-settings/attr-values ws-uuid :graph-settings/z2-axis-group-variable-uuid]))
        close-fn                 #(dispatch [:graph-settings/toggle])]
    [c/modal {:title          @(<t (bp "graph_settings"))
              :close-on-click close-fn
              :content        [:<>
                               (when enabled?
                                 (cond-> [:div.graph-settings]
                                   (>= multi-valued-input-count 1)
                                   (conj [radio-group {:label     (str @(<t (bp "select_x_axis_variable")) ":")
                                                       :selected? x-axis-selected?
                                                       :variables group-variables
                                                       :on-change #(do (dispatch [:graph-settings/update-attr
                                                                                  ws-uuid
                                                                                  :graph-settings/x-axis-group-variable-uuid
                                                                                  %])
                                                                       (dispatch [:worksheet/upsert-x-axis-limit ws-uuid %]))}])

                                   (>= multi-valued-input-count 2)
                                   (conj [radio-group {:label     (str @(<t (bp "select_z_axis_variable")) ":")
                                                       :selected? z-axis-selected?
                                                       :variables group-variables
                                                       :on-change #(dispatch [:graph-settings/update-attr
                                                                              ws-uuid
                                                                              :graph-settings/z-axis-group-variable-uuid
                                                                              %])}])

                                   (>= multi-valued-input-count 3)
                                   (conj [radio-group {:label     (str @(<t (bp "select_z2_axis_variable")) ":")
                                                       :selected? z2-axis-selected?
                                                       :variables group-variables
                                                       :on-change #(dispatch [:graph-settings/update-attr
                                                                              ws-uuid
                                                                              :graph-settings/z2-axis-group-variable-uuid
                                                                              %])}])

                                   (and (>= multi-valued-input-count 1) (not @(subscribe [:wizard/discrete-group-variable? x-axis-gv-uuid])))
                                   (conj [settings-form {:ws-uuid            ws-uuid
                                                         :title              @(<t (bp "x_graph_and_axis_limits"))
                                                         :headers            (->> [@(<t (bp "input_variable"))
                                                                                   @(<t (bp "range"))
                                                                                   @(<t (bp "minimum"))
                                                                                   @(<t (bp "maximum"))]
                                                                                  (mapv #(str/upper-case %)))
                                                         :rf-event-id        :worksheet/update-x-axis-limit-attr
                                                         :rf-sub-id          :worksheet/graph-settings-x-axis-limits
                                                         :min-attr-id        :x-axis-limit/min
                                                         :max-attr-id        :x-axis-limit/max
                                                         :default-min-values {x-axis-gv-uuid x-default-min}
                                                         :default-max-values {x-axis-gv-uuid x-default-max}
                                                         :enabled?           enabled?}])

                                   (>= multi-valued-input-count 1)
                                   (conj [settings-form {:ws-uuid            ws-uuid
                                                         :title              @(<t (bp "y_graph_and_axis_limits"))
                                                         :headers            (->> [@(<t (bp "output_variable"))
                                                                                   @(<t (bp "range"))
                                                                                   @(<t (bp "minimum"))
                                                                                   @(<t (bp "maximum"))]
                                                                                  (mapv #(str/upper-case %)))
                                                         :rf-event-id        :worksheet/update-y-axis-limit-attr
                                                         :rf-sub-id          :worksheet/graph-settings-y-axis-limits-filtered
                                                         :min-attr-id        :y-axis-limit/min
                                                         :max-attr-id        :y-axis-limit/max
                                                         :default-min-values y-default-min-values
                                                         :default-max-values y-default-max-values}])))]}]))
