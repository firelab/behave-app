(ns behave.components.table-settings.views
  (:require [behave.components.core :as c]
            [behave.components.table-settings.events]
            [behave.components.table-settings.subs]
            [re-frame.core          :refer [dispatch subscribe]]))

(defn table-settings-modal [ws-uuid]
  (let [multi-valued-input-uuids @(subscribe [:worksheet/multi-value-input-uuids ws-uuid])
        multi-valued-input-count (count multi-valued-input-uuids)
        group-variables          (map #(deref (subscribe [:wizard/group-variable %])) multi-valued-input-uuids)]
    (letfn [(radio-group [{:keys [label attr fallback-attr]}]
              (let [selected? (or (first @(subscribe [:table-settings/attr-values ws-uuid attr]))
                                  (first @(subscribe [:graph-settings/attr-values ws-uuid fallback-attr])))]
                [c/radio-group
                 {:label   label
                  :options (mapv (fn [{group-var-uuid :bp/uuid}]
                                   (let [var-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name group-var-uuid])]
                                     {:value     var-name
                                      :label     var-name
                                      :on-change #(dispatch [:table-settings/update-attr
                                                             ws-uuid
                                                             attr
                                                             group-var-uuid])
                                      :checked?  (= selected? group-var-uuid)}))
                                 group-variables)}]))
            (single-mvi-radio-group [{:keys [label this-attr opposite-attr mvi-gv-uuid]}]
              (let [this-attr-val     (first @(subscribe [:table-settings/attr-values ws-uuid this-attr]))
                    opposite-attr-val (first @(subscribe [:table-settings/attr-values ws-uuid opposite-attr]))
                    mvi-name          @(subscribe [:wizard/gv-uuid->resolve-result-variable-name mvi-gv-uuid])
                    neither-set?      (and (nil? this-attr-val) (nil? opposite-attr-val))]
                [c/radio-group
                 {:label   label
                  :options [{:value     mvi-name
                             :label     mvi-name
                             :on-change #(dispatch [:table-settings/update-attr ws-uuid this-attr mvi-gv-uuid])
                             :checked?  (or (= this-attr-val mvi-gv-uuid)
                                            (and neither-set? (= this-attr :table-settings/row-group-variable-uuid)))}
                            {:value     "Outputs"
                             :label     "Outputs"
                             :on-change #(dispatch [:table-settings/update-attr ws-uuid opposite-attr mvi-gv-uuid])
                             :checked?  (or (= opposite-attr-val mvi-gv-uuid)
                                            (and neither-set? (= this-attr :table-settings/col-group-variable-uuid)))}]}]))]
      [c/modal {:title          "Table Settings"
                :close-on-click #(dispatch [:table-settings/toggle])
                :content        [:<>
                                 (when (= multi-valued-input-count 1)
                                   (let [mvi-gv-uuid (:bp/uuid (first group-variables))]
                                     [:<>
                                      [single-mvi-radio-group {:label         "Row variable:"
                                                               :this-attr     :table-settings/row-group-variable-uuid
                                                               :opposite-attr :table-settings/col-group-variable-uuid
                                                               :mvi-gv-uuid   mvi-gv-uuid}]
                                      [single-mvi-radio-group {:label         "Column variable:"
                                                               :this-attr     :table-settings/col-group-variable-uuid
                                                               :opposite-attr :table-settings/row-group-variable-uuid
                                                               :mvi-gv-uuid   mvi-gv-uuid}]]))
                                 (when (>= multi-valued-input-count 2)
                                   [radio-group {:label         "Row variable:"
                                                 :attr          :table-settings/row-group-variable-uuid
                                                 :fallback-attr :graph-settings/z-axis-group-variable-uuid}])
                                 (when (>= multi-valued-input-count 2)
                                   [radio-group {:label         "Column variable:"
                                                 :attr          :table-settings/col-group-variable-uuid
                                                 :fallback-attr :graph-settings/x-axis-group-variable-uuid}])
                                 (when (>= multi-valued-input-count 3)
                                   [radio-group {:label         "Sub-table variable:"
                                                 :attr          :table-settings/submatrix-group-variable-uuid
                                                 :fallback-attr :graph-settings/z2-axis-group-variable-uuid}])]}])))
