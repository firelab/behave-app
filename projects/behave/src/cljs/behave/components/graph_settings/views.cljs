(ns behave.components.graph-settings.views
  (:require [behave.components.core :as c]
            [behave.components.graph-settings.events]
            [behave.components.graph-settings.subs]
            [behave.translate       :refer [<t bp]]
            [clojure.string         :as str]
            [dom-utils.interface    :refer [input-float-value input-int-value]]
            [goog.string            :as gstring]
            [goog.string.format]
            [re-frame.core          :refer [dispatch subscribe]]
            [reagent.core           :as r]))

(defn- number-inputs
  [{:keys [saved-entries on-change default-values enabled?]}]
  (map (fn [[gv-uuid saved-value row-enabled?]]
         (let [value-atom            (r/atom saved-value)
               show-tenth-precision? (< (get default-values gv-uuid) 1)
               disabled?             (if (some? enabled?)
                                       (not enabled?)
                                       (if (some? row-enabled?)
                                         (not row-enabled?)
                                         false))]
           [c/number-input (cond-> {:disabled?  disabled?
                                    :on-change  #(let [v (if show-tenth-precision?
                                                           (input-float-value %)
                                                           (input-int-value %))]
                                                   (reset! value-atom v))
                                    :on-blur    #(on-change gv-uuid @value-atom)
                                    :value-atom value-atom}
                             show-tenth-precision?
                             (assoc :step "0.1"))]))
       saved-entries))

(defn- settings-form
  [{:keys [ws-uuid title headers rf-event-id rf-sub-id min-attr-id max-attr-id
           default-min-values default-max-values enabled?]}]
  (let [*gv-uuid+min+max-entries       (subscribe [rf-sub-id ws-uuid])
        *gv-order                      (subscribe [:vms/group-variable-order ws-uuid])
        gv-uuid+min+max-entries-sorted (->> @*gv-uuid+min+max-entries
                                            (sort-by #(.indexOf @*gv-order (first %))))
        units-lookup                   @(subscribe [:worksheet/result-table-units ws-uuid])
        maximums                       (number-inputs {:saved-entries  (map (fn [[gv-uuid _min-val max-val row-enabled?]]
                                                                              [gv-uuid max-val row-enabled?])
                                                                            gv-uuid+min+max-entries-sorted)
                                                       :on-change      #(dispatch [rf-event-id ws-uuid %1 max-attr-id %2])
                                                       :default-values default-max-values
                                                       :enabled?       enabled?})
        minimums                       (number-inputs {:saved-entries  (map (fn [[gv-uuid min-val _max-val row-enabled?]]
                                                                              [gv-uuid min-val row-enabled?])
                                                                            gv-uuid+min+max-entries-sorted)
                                                       :on-change      #(dispatch [rf-event-id ws-uuid %1 min-attr-id %2])
                                                       :default-values default-min-values
                                                       :enabled?       enabled?})
        output-ranges                  (map (fn [[gv-uuid & _rest]]
                                              (let [min-val     (get default-min-values gv-uuid)
                                                    min-val-fmt (if (< min-val 1) "%.1f" "%d")
                                                    max-val     (get default-max-values gv-uuid)
                                                    max-val-fmt (if (< max-val 1) "%.1f" "%d")
                                                    fmt         (gstring/format "%s - %s" min-val-fmt max-val-fmt)]
                                                (gstring/format fmt min-val max-val)))
                                            gv-uuid+min+max-entries-sorted)
        names                          (map (fn [[gv-uuid _min _max]]
                                              (gstring/format "%s (%s)"
                                                              @(subscribe [:wizard/gv-uuid->resolve-result-variable-name gv-uuid])
                                                              (get units-lookup
                                                                   (if-let [directional-children (seq @(subscribe [:vms/directional-children gv-uuid]))]
                                                                     (:bp/uuid (first directional-children))
                                                                     gv-uuid))))
                                            gv-uuid+min+max-entries-sorted)
        column-keys                    (mapv (fn [idx] (keyword (str "col" idx))) (range (count headers)))
        row-data                       (map (fn [& args]
                                              (into {}
                                                    (map (fn [x y] [x y])
                                                         column-keys args)))
                                            names
                                            output-ranges
                                            minimums
                                            maximums)]
    [:div.settings-form
     (c/table {:title   title
               :headers headers
               :columns column-keys
               :rows    row-data})]))

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
        enabled?                 @(subscribe [:wizard/enable-graph-settings? ws-uuid])]
    (letfn [(radio-group [{:keys [label attr variables on-change]}]
              (let [*values   (subscribe [:graph-settings/attr-values ws-uuid attr])
                    selected? (first @*values)]
                [c/radio-group {:label   label
                                :options (mapv (fn [{group-var-uuid :bp/uuid}]
                                                 (let [var-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name group-var-uuid])]
                                                   {:value     var-name
                                                    :label     var-name
                                                    :on-change #(do (dispatch [:graph-settings/update-attr
                                                                               ws-uuid
                                                                               attr
                                                                               group-var-uuid])
                                                                    (when on-change (on-change group-var-uuid)))
                                                    :checked?  (= selected? group-var-uuid)}))
                                               variables)}]))]
      (let [close-fn #(dispatch [:graph-settings/toggle])]
        [c/modal {:title          "Graph Settings"
                  :close-on-click close-fn
                  :content        [:<>
                                   (when enabled?
                                     (cond-> [:div.graph-settings]
                                       (>= multi-valued-input-count 1)
                                       (conj [radio-group {:label     (str @(<t (bp "select_x_axis_variable")) ":")
                                                           :attr      :graph-settings/x-axis-group-variable-uuid
                                                           :variables group-variables
                                                           :on-change #(dispatch [:worksheet/upsert-x-axis-limit ws-uuid %])}])

                                       (>= multi-valued-input-count 2)
                                       (conj [radio-group {:label     (str @(<t (bp "select_z_axis_variable")) ":")
                                                           :attr      :graph-settings/z-axis-group-variable-uuid
                                                           :variables group-variables}])

                                       (>= multi-valued-input-count 3)
                                       (conj [radio-group {:label     (str @(<t (bp "select_z2_axis_variable")) ":")
                                                           :attr      :graph-settings/z2-axis-group-variable-uuid
                                                           :variables group-variables}])

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
                                                             :default-max-values y-default-max-values}])))]}]))))
