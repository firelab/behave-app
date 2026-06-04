(ns behave.components.table-settings.views
  (:require [behave.components.core :as c]
            [behave.components.table-settings.events]
            [behave.components.table-settings.subs]
            [behave.translate       :refer [<t bp]]
            [re-frame.core          :refer [dispatch subscribe]]))

(defn- multi-mvi-radio-group
  [{:keys [label selected on-select group-vars]}]
  [c/radio-group
   {:label   label
    :options (mapv (fn [{:keys [gv-uuid var-name]}]
                     {:value     var-name
                      :label     var-name
                      :on-change #(on-select gv-uuid)
                      :checked?  (= selected gv-uuid)})
                   group-vars)}])

(defn- single-mvi-radio-group
  [{:keys [label selected mvi-uuid mvi-name on-select]}]
  [c/radio-group
   {:label   label
    :options [{:value     mvi-name
               :label     mvi-name
               :on-change #(on-select mvi-uuid)
               :checked?  (= selected mvi-uuid)}
              {:value     @(<t (bp "outputs"))
               :label     @(<t (bp "outputs"))
               :on-change #(on-select "outputs")
               :checked?  (= selected "outputs")}]}])

(defn table-settings-modal
  "A modal component for displaying table settings"
  [ws-uuid]
  (let [multi-valued-input-uuids @(subscribe [:worksheet/multi-value-input-uuids ws-uuid])
        multi-valued-input-count (count multi-valued-input-uuids)
        group-variables          (map #(deref (subscribe [:wizard/group-variable %])) multi-valued-input-uuids)
        group-vars               (mapv (fn [{gv-uuid :bp/uuid}]
                                         {:gv-uuid  gv-uuid
                                          :var-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name gv-uuid])})
                                       group-variables)
        row-selected             (or (first @(subscribe [:table-settings/attr-values ws-uuid :table-settings/row-group-variable-uuid]))
                                     (first @(subscribe [:graph-settings/attr-values ws-uuid :graph-settings/z-axis-group-variable-uuid])))
        col-selected             (or (first @(subscribe [:table-settings/attr-values ws-uuid :table-settings/col-group-variable-uuid]))
                                     (first @(subscribe [:graph-settings/attr-values ws-uuid :graph-settings/x-axis-group-variable-uuid])))
        sub-table-selected       (or (first @(subscribe [:table-settings/attr-values ws-uuid :table-settings/submatrix-group-variable-uuid]))
                                     (first @(subscribe [:graph-settings/attr-values ws-uuid :graph-settings/z2-axis-group-variable-uuid])))
        row-attr-val             (first @(subscribe [:table-settings/attr-values ws-uuid :table-settings/row-group-variable-uuid]))
        col-attr-val             (first @(subscribe [:table-settings/attr-values ws-uuid :table-settings/col-group-variable-uuid]))
        close-fn                 #(dispatch [:table-settings/close])]
    [:<>
     [:div.modal__background {:on-click close-fn}]
     [c/modal
      {:title          @(<t (bp "table-settings"))
       :close-on-click close-fn
       :content        [:<>
                        (when (= multi-valued-input-count 1)
                          (let [{mvi-uuid :gv-uuid mvi-name :var-name} (first group-vars)]
                            [:<>
                             [single-mvi-radio-group {:label     (str @(<t (bp "row-variable")) ":")
                                                      :selected  row-attr-val
                                                      :mvi-uuid  mvi-uuid
                                                      :mvi-name  mvi-name
                                                      :on-select #(dispatch [:table-settings/update-attr ws-uuid :table-settings/row-group-variable-uuid %])}]
                             [single-mvi-radio-group {:label     (str @(<t (bp "column-variable")) ":")
                                                      :selected  col-attr-val
                                                      :mvi-uuid  mvi-uuid
                                                      :mvi-name  mvi-name
                                                      :on-select #(dispatch [:table-settings/update-attr ws-uuid :table-settings/col-group-variable-uuid %])}]]))
                        (when (>= multi-valued-input-count 2)
                          [multi-mvi-radio-group {:label      (str @(<t (bp "row-variable")) ":")
                                                  :selected   row-selected
                                                  :on-select  #(dispatch [:table-settings/update-attr ws-uuid :table-settings/row-group-variable-uuid %])
                                                  :group-vars group-vars}])
                        (when (>= multi-valued-input-count 2)
                          [multi-mvi-radio-group {:label      (str @(<t (bp "column-variable")) ":")
                                                  :selected   col-selected
                                                  :on-select  #(dispatch [:table-settings/update-attr ws-uuid :table-settings/col-group-variable-uuid %])
                                                  :group-vars group-vars}])
                        (when (>= multi-valued-input-count 3)
                          [multi-mvi-radio-group {:label      (str @(<t (bp "sub-table-variable")) ":")
                                                  :selected   sub-table-selected
                                                  :on-select  #(dispatch [:table-settings/update-attr ws-uuid :table-settings/submatrix-group-variable-uuid %])
                                                  :group-vars group-vars}])]}]]))
