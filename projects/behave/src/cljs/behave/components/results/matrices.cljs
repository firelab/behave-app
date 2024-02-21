(ns behave.components.results.matrices
  (:require [behave.components.core  :as c]
            [behave.units-conversion :refer [to-map-units]]
            [behave.translate        :refer [<t bp]]
            [goog.string             :as gstring]
            [re-frame.core           :refer [subscribe]]))

(defn- shade-cell-value? [table-setting-filters col-uuid value]
  (let [[_ mmin mmax enabled?] (first (filter
                                       (fn [[gv-uuid]]
                                         (= gv-uuid col-uuid))
                                       table-setting-filters))]
    (and enabled? mmin mmax (not (<= mmin value mmax)))))

(defn- header-label  [label units]
  (if (seq units)
    (gstring/format "%s (%s)" label units)
    (gstring/format "%s" label)))

(defmulti construct-result-matrices
  (fn [{:keys [multi-valued-inputs]}]
    (let [multi-valued-inputs-count (count multi-valued-inputs)]
      (if (<= 0 multi-valued-inputs-count 2)
        multi-valued-inputs-count
        :not-supported))))

(defmethod construct-result-matrices :not-supported
  [{:keys [multi-valued-inputs]}]
  [:div (gstring/format @(<t (bp "tables_for_d_multi_valued_inputs_are_not_supported"))
                        (count multi-valued-inputs))])

(defmethod construct-result-matrices 0
  [{:keys [ws-uuid process-map-units? output-entities formatters]}]
  (let [map-units-settings-entity @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units                 (:map-units-settings/units map-units-settings-entity)
        map-rep-frac              (:map-units-settings/map-rep-fraction map-units-settings-entity)
        rows                      (reduce (fn [acc {output-gv-uuid :bp/uuid
                                                    units          :units}]
                                            (let [value    @(subscribe [:worksheet/first-row-results-gv-uuid->value
                                                                        ws-uuid
                                                                        output-gv-uuid])
                                                  fmt-fn   (get formatters output-gv-uuid identity)
                                                  var-name @(subscribe [:wizard/gv-uuid->variable-name-1 output-gv-uuid])]
                                              (cond-> acc
                                                :always (conj {:output var-name
                                                               :value  (fmt-fn value)
                                                               :units  units})

                                                (process-map-units? output-gv-uuid)
                                                (conj {:output (gstring/format @(<t (bp "s_map_units")) var-name)
                                                       :value  (-> value
                                                                   (to-map-units
                                                                    units
                                                                    map-units
                                                                    map-rep-frac)
                                                                   fmt-fn)
                                                       :units  map-units}))))
                                          []
                                          output-entities)]
    [:div.print__result-table
     (c/table {:title   @(<t (bp "results"))
               :headers [@(<t (bp "output_variable"))
                         @(<t (bp "value"))
                         @(<t (bp "units"))]
               :columns [:output :value :units]
               :rows    rows})]))

(defmethod construct-result-matrices 1
  [{:keys [ws-uuid process-map-units? multi-valued-inputs formatters output-entities units-lookup table-setting-filters]}]
  (let [[multi-var-name
         multi-var-units
         multi-var-gv-uuid
         multi-var-values]        (first multi-valued-inputs)
        input-fmt-fn              (get @(subscribe [:worksheet/result-table-formatters [multi-var-gv-uuid]]) multi-var-gv-uuid)
        matrix-data-raw           @(subscribe [:worksheet/matrix-table-data-single-multi-valued-input
                                               ws-uuid
                                               multi-var-gv-uuid
                                               multi-var-values
                                               (map :bp/uuid output-entities)])
        matrix-data-formatted     (reduce-kv (fn [acc [row col-uuid] v]
                                               (let [fmt-fn (get formatters col-uuid identity)]
                                                 (assoc acc [(input-fmt-fn row) col-uuid]
                                                        (let [shade-value? (shade-cell-value? table-setting-filters col-uuid v)]
                                                          [:div.result-matrix-cell-value
                                                           [:div (fmt-fn v)]
                                                           (when shade-value?
                                                             [:div "(X)"])]))))
                                             {}
                                             matrix-data-raw)
        map-units-settings-entity @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units                 (:map-units-settings/units map-units-settings-entity)
        map-rep-frac              (:map-units-settings/map-rep-fraction map-units-settings-entity)
        column-headers            (reduce (fn insert-map-units-columns [acc {output-gv-uuid :bp/uuid
                                                                             output-units   :units}]
                                            (let [output-name @(subscribe [:wizard/gv-uuid->variable-name-1 output-gv-uuid])]
                                              (cond-> acc
                                                :always (conj {:name (header-label output-name output-units)
                                                               :key  output-gv-uuid})

                                                (process-map-units? output-gv-uuid)
                                                (conj {:name (gstring/format @(<t (bp "s_map_units_(s)"))
                                                                             output-name
                                                                             map-units)
                                                       :key  (str output-gv-uuid "-map-units")}))))
                                          []
                                          output-entities)
        row-headers (map (fn [value] {:name (input-fmt-fn value) :key (input-fmt-fn value)}) multi-var-values)
        final-data  (reduce (fn insert-map-units-values [acc [[i j] value]]
                              (let [fmt-fn (get formatters j identity)]
                                (cond-> acc
                                  (process-map-units? j)
                                  (assoc [i (str j "-map-units")]
                                         (-> value
                                             (to-map-units
                                              (get units-lookup j)
                                              map-units
                                              map-rep-frac)
                                             fmt-fn)))))
                            matrix-data-formatted
                            matrix-data-formatted)]
    [:div.print__result-table
     (c/matrix-table {:title          @(<t (bp "results"))
                      :rows-label     (header-label multi-var-name multi-var-units)
                      :cols-label     @(<t (bp "outputs"))
                      :column-headers column-headers
                      :row-headers    row-headers
                      :data           final-data})]))

(defmethod construct-result-matrices 2
  [{:keys [ws-uuid process-map-units? multi-valued-inputs formatters output-entities table-setting-filters]}]
  (let [[row-name row-units row-gv-uuid row-values] (first multi-valued-inputs)
        [col-name col-units col-gv-uuid col-values] (second multi-valued-inputs)
        map-units-settings-entity                   @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units                                   (:map-units-settings/units map-units-settings-entity)
        map-rep-frac                                (:map-units-settings/map-rep-fraction map-units-settings-entity)
        input-formatters                            @(subscribe [:worksheet/result-table-formatters [row-gv-uuid col-gv-uuid]])]
    [:div.print__construct-result-matrices
     (for [{output-gv-uuid :bp/uuid
            output-units   :units} output-entities]
       (let [output-name           @(subscribe [:wizard/gv-uuid->variable-name-1 output-gv-uuid])
             output-fmt-fn         (get formatters output-gv-uuid identity)
             row-fmt-fn            (get input-formatters row-gv-uuid identity)
             col-fmt-fn            (get input-formatters col-gv-uuid identity)
             matrix-data-raw       @(subscribe [:print/matrix-table-two-multi-valued-inputs ws-uuid
                                                row-gv-uuid
                                                row-values
                                                col-gv-uuid
                                                col-values
                                                output-gv-uuid])
             matrix-data-formatted (reduce-kv (fn [acc [x y] v]
                                                (assoc acc
                                                       [(row-fmt-fn x) (col-fmt-fn y)]
                                                       (let [shade-value? (shade-cell-value? table-setting-filters output-gv-uuid x)]
                                                         [:div.result-matrix-cell-value
                                                          [:div (output-fmt-fn v)]
                                                          (when shade-value?
                                                            [:div "(X)"])])))
                                              {}
                                              matrix-data-raw)
             row-headers           (map (fn [value] {:name (row-fmt-fn value) :key (row-fmt-fn value)}) row-values)
             column-headers        (map (fn [value] {:name (col-fmt-fn value) :key (col-fmt-fn value)}) col-values)]
         [:<>
          [:div.print__result-table
           (c/matrix-table {:title          (header-label output-name output-units)
                            :rows-label     (header-label row-name row-units)
                            :cols-label     (header-label col-name col-units)
                            :row-headers    row-headers
                            :column-headers column-headers
                            :data           matrix-data-formatted})]
          (when (process-map-units? output-gv-uuid)
            [:div.print__result-table
             (let [data (reduce-kv (fn [acc [i j] value]
                                     (assoc acc [i j] (-> value
                                                          (to-map-units output-units map-units map-rep-frac)
                                                          output-fmt-fn)))
                                   matrix-data-formatted
                                   matrix-data-formatted)]
               (c/matrix-table {:title          (gstring/format @(<t (bp "s_map_units_(s)")) output-name map-units)
                                :rows-label     (header-label row-name row-units)
                                :cols-label     (header-label col-name col-units)
                                :row-headers    row-headers
                                :column-headers column-headers
                                :data           data}))])]))]))

(defn result-matrices [ws-uuid]
  (let [map-units-settings-entity      @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units-enabled?             (:map-units-settings/enabled? map-units-settings-entity)
        map-unit-convertible-variables @(subscribe [:wizard/map-unit-convertible-variables])
        units-lookup                   @(subscribe [:worksheet/result-table-units ws-uuid])
        output-gv-uuids                @(subscribe [:worksheet/all-output-uuids ws-uuid])
        table-setting-filters          @(subscribe [:worksheet/table-settings-filters ws-uuid])]
    [construct-result-matrices
     {:ws-uuid               ws-uuid
      :process-map-units?    (fn [v-uuid]
                               (and map-units-enabled?
                                    (map-unit-convertible-variables v-uuid)))
      :multi-valued-inputs   @(subscribe [:print/matrix-table-multi-valued-inputs ws-uuid])
      :output-gv-uuids       output-gv-uuids
      :output-entities       (map (fn [gv-uuid]
                                    (-> @(subscribe [:wizard/group-variable gv-uuid])
                                        (merge {:units (get units-lookup gv-uuid)}))) output-gv-uuids)
      :units-lookup          units-lookup
      :formatters            @(subscribe [:worksheet/result-table-formatters output-gv-uuids])
      :table-setting-filters table-setting-filters}]))
