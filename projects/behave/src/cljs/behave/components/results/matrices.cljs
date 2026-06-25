(ns behave.components.results.matrices
  (:require [behave.components.core  :as c]
            [behave.translate        :refer [<t bp]]
            [behave.units-conversion :refer [to-map-units]]
            [clojure.string          :as str]
            [goog.string             :as gstring]
            [map-utils.interface     :as map-utils]
            [re-frame.core           :as rf :refer [dispatch dispatch-sync subscribe]]))

;;==============================================================================
;; Helpers
;;==============================================================================

(defn- find-discrete-outputs-with-colors [output-entities]
  (->> output-entities
       (filter (fn [{gv-uuid :bp/uuid}]
                 (and @(subscribe [:wizard/discrete-group-variable? gv-uuid])
                      (seq @(subscribe [:wizard/gv-list-options-with-colors gv-uuid])))))
       vec))

(defn- color-options->value-color-map [color-options]
  (into {} (map (fn [{:keys [value color]}] [value color]) color-options)))

(defn- compute-color-map-1d [cell-color-gv-uuid matrix-data-raw input-fmt-fn]
  (when cell-color-gv-uuid
    (let [color-options  @(subscribe [:wizard/gv-list-options-with-colors cell-color-gv-uuid])
          value->color   (color-options->value-color-map color-options)
          entries-by-row (group-by (fn [[[row _] _]] row) matrix-data-raw)]
      (reduce-kv
       (fn [acc row entries]
         (if-let [color (some (fn [[[_ col-uuid] v]]
                                (when (= col-uuid cell-color-gv-uuid)
                                  (get value->color v)))
                              entries)]
           (let [fmt-row (input-fmt-fn row)]
             (reduce (fn [a [[_ col-uuid] _]]
                       (assoc a [fmt-row col-uuid] color))
                     acc
                     entries))
           acc))
       {}
       entries-by-row))))

(defn- header-label [label units]
  (if (seq units)
    (gstring/format "%s (%s)" label units)
    (gstring/format "%s" label)))

(defn- fetch-map-units-settings [ws-uuid]
  (let [entity @(subscribe [:worksheet/map-units-settings-entity ws-uuid])]
    {:units        (:map-units-settings/units entity)
     :rep-fraction (:map-units-settings/map-rep-fraction entity)}))

(defn- format-matrix-cell [value formatter shaded?]
  [:div {:class (cond-> ["result-matrix-cell-value"]
                  (true? shaded?)  (conj "table-cell__shaded")
                  (false? shaded?) (conj "table-cell__in-range"))}
   (if (neg? value)
     "-"
     (formatter value))])

(defn- convert-to-map-units [value units map-units map-rep-frac]
  (to-map-units value units map-units map-rep-frac))

(defn- map-units-column-key [gv-uuid]
  (str gv-uuid "-map-units"))

(defn- fetch-matrix-data-2d
  [{:keys [ws-uuid row-gv-uuid row-values col-gv-uuid col-values output-gv-uuid submatrix-gv-uuid submatrix-value]}]
  (if (and submatrix-value submatrix-gv-uuid)
    @(subscribe [:print/matrix-table-three-multi-valued-inputs
                 {:ws-uuid           ws-uuid
                  :row-gv-uuid       row-gv-uuid
                  :row-values        row-values
                  :col-gv-uuid       col-gv-uuid
                  :col-values        col-values
                  :output-gv-uuid    output-gv-uuid
                  :submatrix-gv-uuid submatrix-gv-uuid
                  :submatrix-value   submatrix-value}])
    @(subscribe [:print/matrix-table-two-multi-valued-inputs
                 {:ws-uuid        ws-uuid
                  :row-gv-uuid    row-gv-uuid
                  :row-values     row-values
                  :col-gv-uuid    col-gv-uuid
                  :col-values     col-values
                  :output-gv-uuid output-gv-uuid}])))

(defn- build-matrix-data
  [matrix-data-raw row-fmt-fn col-fmt-fn output-fmt-fn shade-set any-filters-enabled?]
  (reduce-kv
   (fn [acc [row col] value]
     (let [shaded? (contains? shade-set [row col])]
       (assoc acc [(row-fmt-fn row) (col-fmt-fn col)]
              (format-matrix-cell value output-fmt-fn (when any-filters-enabled? shaded?)))))
   {}
   matrix-data-raw))

(defn- build-matrix-data-with-map-units
  [{:keys [matrix-data-raw row-fmt-fn col-fmt-fn output-fmt-fn output-units map-units map-rep-frac shade-set any-filters-enabled?]}]
  (reduce-kv
   (fn [acc [row col] value]
     (let [shaded?         (contains? shade-set [row col])
           converted-value (convert-to-map-units value output-units map-units map-rep-frac)]
       (assoc acc [(row-fmt-fn row) (col-fmt-fn col)]
              (format-matrix-cell converted-value output-fmt-fn (when any-filters-enabled? shaded?)))))
   {}
   matrix-data-raw))

(defn- compute-color-map-2d
  [{:keys [ws-uuid row-gv-uuid row-values col-gv-uuid col-values
           cell-color-gv-uuid submatrix-gv-uuid submatrix-value]}]
  (when cell-color-gv-uuid
    (let [color-options @(subscribe [:wizard/gv-list-options-with-colors cell-color-gv-uuid])
          value->color  (color-options->value-color-map color-options)
          matrix-data   (fetch-matrix-data-2d {:ws-uuid           ws-uuid
                                               :row-gv-uuid       row-gv-uuid
                                               :row-values        row-values
                                               :col-gv-uuid       col-gv-uuid
                                               :col-values        col-values
                                               :output-gv-uuid    cell-color-gv-uuid
                                               :submatrix-gv-uuid submatrix-gv-uuid
                                               :submatrix-value   submatrix-value})]
      (reduce-kv (fn [acc [row col] value]
                   (if-let [color (get value->color value)]
                     (assoc acc [row col] color)
                     acc))
                 {}
                 matrix-data))))

;;==============================================================================
;; construct-result-matrices
;;==============================================================================

(defmulti construct-result-matrices
  "Constructs Result matrices based on how many multi-valued inputs there are"
  (fn [{:keys [multi-valued-inputs]}]
    (let [multi-valued-inputs-count (count multi-valued-inputs)]
      (if (<= 0 multi-valued-inputs-count 3)
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
        regular-rows              (reduce (fn [acc {output-gv-uuid :bp/uuid
                                                    units          :units}]
                                            (let [value    @(subscribe [:worksheet/first-row-results-gv-uuid->value ws-uuid output-gv-uuid])
                                                  fmt-fn   (get formatters output-gv-uuid identity)
                                                  var-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name output-gv-uuid])]
                                              (conj acc {:output var-name
                                                         :value  (if (neg? value)
                                                                   "-"
                                                                   (fmt-fn value))
                                                         :units  units})))
                                          []
                                          output-entities)
        map-units-rows            (reduce (fn [acc {output-gv-uuid :bp/uuid
                                                    units          :units}]
                                            (if (process-map-units? output-gv-uuid)
                                              (let [value    @(subscribe [:worksheet/first-row-results-gv-uuid->value ws-uuid output-gv-uuid])
                                                    fmt-fn   (get formatters output-gv-uuid identity)
                                                    var-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name output-gv-uuid])]
                                                (conj acc {:output (gstring/format @(<t (bp "s_map_units")) var-name)
                                                           :value  (if (pos? value)
                                                                     (-> value
                                                                         (to-map-units units map-units map-rep-frac)
                                                                         fmt-fn)
                                                                     "-")
                                                           :units  map-units}))
                                              acc))
                                          []
                                          output-entities)]
    [:div.print__result-table
     (c/table {:title   @(<t (bp "results"))
               :headers [@(<t (bp "output_variable"))
                         @(<t (bp "value"))
                         @(<t (bp "units"))]
               :columns [:output :value :units]
               :rows    regular-rows})
     (when (seq map-units-rows)
       (c/table {:title   (gstring/format @(<t (bp "s_map_units_(s)")) @(<t (bp "results")) map-units)
                 :headers [@(<t (bp "output_variable"))
                           @(<t (bp "value"))
                           @(<t (bp "units"))]
                 :columns [:output :value :units]
                 :rows    map-units-rows}))]))

(defmethod construct-result-matrices 1
  [{:keys [ws-uuid process-map-units? multi-valued-inputs formatters output-entities units-lookup shade-set any-filters-enabled? title header-color cell-color-gv-uuid]}]
  (let [[multi-var-name
         multi-var-units
         multi-var-gv-uuid
         multi-var-values]           (first multi-valued-inputs)
        table-settings               @(subscribe [:worksheet/table-settings ws-uuid])
        outputs-on-rows?             (= (:table-settings/col-group-variable-uuid table-settings) multi-var-gv-uuid)
        input-fmt-fn                 (get @(subscribe [:worksheet/result-table-formatters [multi-var-gv-uuid]]) multi-var-gv-uuid)
        matrix-data-raw              @(subscribe [:worksheet/matrix-table-data-single-multi-valued-input
                                                  ws-uuid
                                                  multi-var-gv-uuid
                                                  multi-var-values
                                                  (map :bp/uuid output-entities)])
        color-map                    (compute-color-map-1d cell-color-gv-uuid matrix-data-raw input-fmt-fn)
        {:keys [units rep-fraction]} (fetch-map-units-settings ws-uuid)
        [regular-column-headers
         map-units-column-headers
         matrix-data-formatted
         map-units-data
         row-headers
         rows-label
         cols-label]                 (if outputs-on-rows?
                                       ;; MVI on columns, outputs on rows
                                       (let [col-headers     (mapv (fn [v] {:name (input-fmt-fn v) :key (input-fmt-fn v)}) multi-var-values)
                                             flipped-data    (reduce-kv (fn [acc [mvi-val output-uuid] v]
                                                                          (let [fmt-fn  (get formatters output-uuid identity)
                                                                                shaded? (contains? shade-set mvi-val)]
                                                                            (assoc acc [output-uuid (input-fmt-fn mvi-val)]
                                                                                   (format-matrix-cell v fmt-fn (when any-filters-enabled? shaded?)))))
                                                                        {}
                                                                        matrix-data-raw)
                                             output-row-hdrs (mapv (fn [{output-gv-uuid :bp/uuid output-units :units}]
                                                                     (let [output-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name output-gv-uuid])]
                                                                       {:name (header-label output-name output-units)
                                                                        :key  output-gv-uuid}))
                                                                   output-entities)]
                                         [col-headers [] flipped-data {} output-row-hdrs
                                          @(<t (bp "outputs"))
                                          (header-label multi-var-name multi-var-units)])
                                       ;; MVI on rows, outputs on columns (default)
                                       (let [[reg-cols mu-cols] (reduce (fn [[reg mu] {output-gv-uuid :bp/uuid output-units :units}]
                                                                          (let [output-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name output-gv-uuid])]
                                                                            [(conj reg {:name (header-label output-name output-units)
                                                                                        :key  output-gv-uuid})
                                                                             (if (process-map-units? output-gv-uuid)
                                                                               (conj mu {:name (header-label output-name units)
                                                                                         :key  (map-units-column-key output-gv-uuid)})
                                                                               mu)]))
                                                                        [[] []]
                                                                        output-entities)
                                             [reg-data mu-data] (reduce-kv (fn [[reg mu] [row col-uuid] v]
                                                                             (let [fmt-fn  (get formatters col-uuid identity)
                                                                                   shaded? (contains? shade-set row)]
                                                                               [(assoc reg [(input-fmt-fn row) col-uuid]
                                                                                       (format-matrix-cell v fmt-fn (when any-filters-enabled? shaded?)))
                                                                                (if (process-map-units? col-uuid)
                                                                                  (assoc mu [(input-fmt-fn row) (map-units-column-key col-uuid)]
                                                                                         (format-matrix-cell
                                                                                          (convert-to-map-units v (get units-lookup col-uuid) units rep-fraction)
                                                                                          fmt-fn
                                                                                          (when any-filters-enabled? shaded?)))
                                                                                  mu)]))
                                                                           [{} {}]
                                                                           matrix-data-raw)
                                             mvi-row-hdrs       (map (fn [v] {:name (input-fmt-fn v) :key (input-fmt-fn v)}) multi-var-values)]
                                         [reg-cols mu-cols reg-data mu-data mvi-row-hdrs
                                          (header-label multi-var-name multi-var-units)
                                          @(<t (bp "outputs"))]))
        flipped-color-map            (when (and outputs-on-rows? color-map)
                                       (into {} (map (fn [[[fmt-mvi output-uuid] color]] [[output-uuid fmt-mvi] color]) color-map)))
        common-matrix-props          {:rows-label  rows-label
                                      :cols-label  cols-label
                                      :row-headers row-headers}]
    [:div.print__result-table
     (c/matrix-table (merge common-matrix-props
                            {:title          title
                             :header-color   header-color
                             :column-headers regular-column-headers
                             :data           matrix-data-formatted
                             :cell-colors    (if outputs-on-rows? flipped-color-map color-map)}))
     (when (seq map-units-column-headers)
       [:div.result-matrix__map-units-table
        (c/matrix-table (merge common-matrix-props
                               {:title          (gstring/format @(<t (bp "s_map_units_(s)")) title units)
                                :header-color   header-color
                                :column-headers map-units-column-headers
                                :data           map-units-data}))])]))

(defmethod construct-result-matrices 2
  [{:keys [ws-uuid process-map-units? multi-valued-inputs formatters output-entities shade-set any-filters-enabled?
           sub-title submatrix-value submatrix-gv-uuid header-color cell-color-gv-uuid]}]
  (let [graph-settings                              @(subscribe [:worksheet/graph-settings ws-uuid])
        table-settings                              @(subscribe [:worksheet/table-settings ws-uuid])
        row-axis-gv-uuid                            (or (:table-settings/row-group-variable-uuid table-settings)
                                                        (:graph-settings/z-axis-group-variable-uuid graph-settings))
        col-axis-gv-uuid                            (or (:table-settings/col-group-variable-uuid table-settings)
                                                        (:graph-settings/x-axis-group-variable-uuid graph-settings))
        [row-name row-units row-gv-uuid row-values] (->> multi-valued-inputs
                                                         (filter (fn [[_ _ gv-uuid]] (= gv-uuid row-axis-gv-uuid)))
                                                         first)
        [col-name col-units col-gv-uuid col-values] (->> multi-valued-inputs
                                                         (filter (fn [[_ _ gv-uuid]] (= gv-uuid col-axis-gv-uuid)))
                                                         first)
        {:keys [units rep-fraction]}                (fetch-map-units-settings ws-uuid)
        input-formatters                            @(subscribe [:worksheet/result-table-formatters [row-gv-uuid col-gv-uuid]])
        row-fmt-fn                                  (get input-formatters row-gv-uuid identity)
        col-fmt-fn                                  (get input-formatters col-gv-uuid identity)
        color-map                                   (compute-color-map-2d {:ws-uuid            ws-uuid
                                                                           :row-gv-uuid        row-gv-uuid
                                                                           :row-values         row-values
                                                                           :col-gv-uuid        col-gv-uuid
                                                                           :col-values         col-values
                                                                           :cell-color-gv-uuid cell-color-gv-uuid
                                                                           :submatrix-gv-uuid  submatrix-gv-uuid
                                                                           :submatrix-value    submatrix-value})
        cell-colors                                 (when color-map
                                                      (map-utils/update-map color-map
                                                                            identity
                                                                            (fn [[row col]]
                                                                              [(row-fmt-fn row) (col-fmt-fn col)])))
        row-headers                                 (map (fn [value] {:name (row-fmt-fn value) :key (row-fmt-fn value)}) row-values)
        column-headers                              (map (fn [value] {:name (col-fmt-fn value) :key (col-fmt-fn value)}) col-values)
        row-headers-sorted                          (sort-by :name row-headers)
        column-headers-sorted                       (sort-by :name column-headers)]
    [:div.print__construct-result-matrices
     (for [{output-gv-uuid :bp/uuid output-units :units} output-entities]
       (let [output-name     @(subscribe [:wizard/gv-uuid->resolve-result-variable-name output-gv-uuid])
             output-fmt-fn   (get formatters output-gv-uuid identity)
             matrix-data-raw (fetch-matrix-data-2d {:ws-uuid           ws-uuid
                                                    :row-gv-uuid       row-gv-uuid
                                                    :row-values        row-values
                                                    :col-gv-uuid       col-gv-uuid
                                                    :col-values        col-values
                                                    :output-gv-uuid    output-gv-uuid
                                                    :submatrix-gv-uuid submatrix-gv-uuid
                                                    :submatrix-value   submatrix-value})
             output-values   (map second matrix-data-raw)]
         (when-not (every? #(= "-1" %) output-values)
           (let [matrix-data-formatted (build-matrix-data matrix-data-raw row-fmt-fn col-fmt-fn output-fmt-fn shade-set any-filters-enabled?)]
             [:<>
              (when (process-map-units? output-gv-uuid)
                [:div.print__result-table
                 (let [data (build-matrix-data-with-map-units {:matrix-data-raw      matrix-data-raw
                                                               :row-fmt-fn           row-fmt-fn
                                                               :col-fmt-fn           col-fmt-fn
                                                               :output-fmt-fn        output-fmt-fn
                                                               :output-units         output-units
                                                               :map-units            units
                                                               :map-rep-frac         rep-fraction
                                                               :shade-set            shade-set
                                                               :any-filters-enabled? any-filters-enabled?
                                                               :color-map            color-map})]
                   (c/matrix-table {:title          (gstring/format @(<t (bp "s_map_units_(s)")) output-name units)
                                    :header-color   header-color
                                    :sub-title      sub-title
                                    :rows-label     (header-label row-name row-units)
                                    :cols-label     (header-label col-name col-units)
                                    :row-headers    row-headers-sorted
                                    :column-headers column-headers-sorted
                                    :data           data
                                    :cell-colors    cell-colors}))])
              [:div.print__result-table
               (c/matrix-table {:title          (header-label output-name output-units)
                                :header-color   header-color
                                :sub-title      sub-title
                                :rows-label     (header-label row-name row-units)
                                :cols-label     (header-label col-name col-units)
                                :row-headers    row-headers-sorted
                                :column-headers column-headers-sorted
                                :data           matrix-data-formatted
                                :cell-colors    cell-colors})]]))))]))

(defmethod construct-result-matrices 3
  [{:keys [ws-uuid process-map-units? multi-valued-inputs formatters output-entities shade-set any-filters-enabled? units-lookup header-color cell-color-gv-uuid]}]
  (let [graph-settings                             @(subscribe [:worksheet/graph-settings ws-uuid])
        table-settings                             @(subscribe [:worksheet/table-settings ws-uuid])
        z2-axis-group-variable-uuid                (or (:table-settings/submatrix-group-variable-uuid table-settings)
                                                       (:graph-settings/z2-axis-group-variable-uuid graph-settings))
        [var-name units-short-code gv-uuid values] (->> multi-valued-inputs
                                                        (filter (fn [[_ _ gv-uuid]] (= gv-uuid z2-axis-group-variable-uuid)))
                                                        first)
        rest-multi-valued-inputs                   (filter (fn [[_ _ gv-uuid]] (not= gv-uuid z2-axis-group-variable-uuid)) multi-valued-inputs)]
    [:div.print__result-table
     (for [value values]
       [:div.print__result-table
        [construct-result-matrices
         {:ws-uuid              ws-uuid
          :title                @(<t (bp "results"))
          :sub-title            (if (not-empty units-short-code)
                                  (gstring/format "%s: %s (%s)"
                                                  var-name
                                                  @(subscribe [:vms/resolve-enum-translation gv-uuid value])
                                                  units-short-code)
                                  (gstring/format "%s: %s"
                                                  var-name
                                                  @(subscribe [:vms/resolve-enum-translation gv-uuid value])))
          :submatrix-value      value
          :submatrix-gv-uuid    gv-uuid
          :process-map-units?   process-map-units?
          :multi-valued-inputs  rest-multi-valued-inputs
          :output-entities      output-entities
          :units-lookup         units-lookup
          :formatters           formatters
          :header-color         header-color
          :cell-color-gv-uuid   cell-color-gv-uuid
          :shade-set            (get shade-set value)
          :any-filters-enabled? any-filters-enabled?}]])]))

;;==============================================================================
;; Discrete Color Selector & Legend
;;==============================================================================

(defn- color-legend [cell-color-gv-uuid]
  (let [color-options                  @(subscribe [:wizard/gv-list-options-with-colors cell-color-gv-uuid])
        color-options-grouped-by-color (group-by :color color-options)]
    (when (seq color-options-grouped-by-color)
      [:div.result-matrices__color-legend
       (for [[color entries] color-options-grouped-by-color
             :let            [entry (first entries)
                              t-key (:t-key entry)]]
         ^{:key t-key}
         [:div.result-matrices__color-legend__item
          [:span.result-matrices__color-legend__swatch {:style {:background-color color}}]
          [:span.result-matrices__color-legend__text @(<t t-key)]])])))

(defn- discrete-color-selector [discrete-outputs-with-colors]
  (let [selected-uuid      @(subscribe [:wizard/selected-output-cell-coloring])
        cell-color-gv-uuid (when-not (= :none selected-uuid) selected-uuid)
        output-count       (count discrete-outputs-with-colors)]
    (when (pos? output-count)
      [:div.result-matrices__color-selector
       (if (= output-count 1)
         (let [{gv-uuid :bp/uuid} (first discrete-outputs-with-colors)
               output-name        @(subscribe [:wizard/gv-uuid->resolve-result-variable-name gv-uuid])]
           [c/toggle {:label     (gstring/format "%s : %s" @(<t (bp "color_by")) output-name)
                      :checked?  (= selected-uuid gv-uuid)
                      :on-change #(dispatch [:wizard/set-discrete-color-output
                                             (if (= selected-uuid gv-uuid) :none gv-uuid)])}])
         [c/radio-group
          {:label   @(<t (bp "color_by"))
           :options (conj
                     (mapv (fn [{gv-uuid :bp/uuid}]
                             (let [output-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name gv-uuid])]
                               {:id        gv-uuid
                                :name      "discrete-color-output"
                                :value     gv-uuid
                                :label     output-name
                                :checked?  (= selected-uuid gv-uuid)
                                :on-change #(dispatch [:wizard/set-discrete-color-output gv-uuid])}))
                           discrete-outputs-with-colors)
                     {:id        "none"
                      :name      "discrete-color-output"
                      :value     :none
                      :label     "None"
                      :checked?  (= selected-uuid :none)
                      :on-change #(dispatch [:wizard/set-discrete-color-output :none])})}])
       (when cell-color-gv-uuid
         [color-legend cell-color-gv-uuid])])))

;;==============================================================================
;; View for Result Matrices
;;==============================================================================

(defn result-matrices
  "Construct all result matrices for a give worksheet
  - ws-uuid : worksheet uuid"
  [ws-uuid]
  (let [directions                      @(subscribe [:worksheet/output-directions ws-uuid])
        map-units-settings-entity       @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units-enabled?              (:map-units-settings/enabled? map-units-settings-entity)
        map-unit-convertible-variables  @(subscribe [:wizard/map-unit-convertible-variables])
        units-lookup                    @(subscribe [:worksheet/result-table-units ws-uuid])
        table-setting-filters           @(subscribe [:worksheet/table-settings-filters ws-uuid])
        gv-order                        @(subscribe [:vms/group-variable-order ws-uuid])
        pivot-tables                    @(subscribe [:worksheet/pivot-tables ws-uuid])
        directional-uuids               (set @(subscribe [:vms/directional-group-variable-uuids]))
        pivot-table-uuids               (->> pivot-tables
                                             (mapcat (fn [pivot-table] @(subscribe [:worksheet/pivot-table-fields (:db/id pivot-table)])))
                                             set)
        all-output-gv-uuids             (->> @(subscribe [:worksheet/output-uuids-conditionally-filtered ws-uuid])
                                             (remove #(contains? pivot-table-uuids %))
                                             (sort-by #(.indexOf gv-order %)))
        non-directional-output-gv-uuids (remove #(contains? directional-uuids %) all-output-gv-uuids)
        directional-gv-uuids            (filter #(contains? directional-uuids %) all-output-gv-uuids)
        multi-valued-inputs             @(subscribe [:print/matrix-table-multi-valued-inputs ws-uuid])
        all-group-variables             (map (fn [gv-uuid] @(subscribe [:wizard/group-variable gv-uuid])) all-output-gv-uuids)
        all-output-entities             (map (fn [gv] (merge gv {:units (get units-lookup (:bp/uuid gv))})) all-group-variables)
        discrete-outputs-with-colors    (find-discrete-outputs-with-colors all-output-entities)
        color-output-state              @(subscribe [:wizard/selected-output-cell-coloring])
        cell-color-gv-uuid              (when-not (= :none color-output-state) color-output-state)
        ;; Non-directional outputs rely on the Heading direction for shading.
        heading-direction               (some (fn [d] (when (= "heading" (str/lower-case (name d))) d)) directions)
        heading-gv-uuids                (when heading-direction
                                          (filter #(deref (subscribe [:vms/group-variable-is-directional? % heading-direction]))
                                                  directional-gv-uuids))
        any-filters-enabled?            (boolean (some (fn [[_ _ _ enabled?]] enabled?) table-setting-filters))]
    (when (and (seq discrete-outputs-with-colors) (nil? color-output-state))
      (dispatch-sync [:wizard/set-discrete-color-output (:bp/uuid (first discrete-outputs-with-colors))]))
    (when (seq all-output-gv-uuids)
      [:div.wizard-results
       (when (seq discrete-outputs-with-colors)
         [discrete-color-selector discrete-outputs-with-colors])
       (when (seq directional-gv-uuids)
         (for [direction directions]
           (let [output-gv-uuids (filter #(deref (subscribe [:vms/group-variable-is-directional? % direction])) directional-gv-uuids)
                 group-variables (map (fn [gv-uuid] @(subscribe [:wizard/group-variable gv-uuid])) output-gv-uuids)
                 output-entities (map (fn [gv] (merge gv {:units (get units-lookup (:bp/uuid gv))})) group-variables)
                 formatters      @(subscribe [:worksheet/result-table-formatters output-gv-uuids])
                 direction-t-key @(subscribe [:vms/direction-translation-key direction])
                 direction-color @(subscribe [:vms/direction-color direction])
                 title           (or (when direction-t-key @(<t direction-t-key))
                                     (str/capitalize direction))]
             [construct-result-matrices
              {:title                title
               :header-color         direction-color
               :ws-uuid              ws-uuid
               :process-map-units?   (fn [v-uuid] (and map-units-enabled? (map-unit-convertible-variables v-uuid)))
               :multi-valued-inputs  multi-valued-inputs
               :output-gv-uuids      output-gv-uuids
               :output-entities      output-entities
               :units-lookup         units-lookup
               :formatters           formatters
               :cell-color-gv-uuid   cell-color-gv-uuid
               :shade-set            @(subscribe [:worksheet/shade-set ws-uuid output-gv-uuids])
               :any-filters-enabled? any-filters-enabled?}])))
       (when (seq non-directional-output-gv-uuids)
         (let [group-variables (map (fn [gv-uuid] @(subscribe [:wizard/group-variable gv-uuid])) non-directional-output-gv-uuids)
               output-entities (map (fn [gv] (merge gv {:units (get units-lookup (:bp/uuid gv))})) group-variables)
               formatters      @(subscribe [:worksheet/result-table-formatters non-directional-output-gv-uuids])
               ;; Shade non-directional results by their own outputs plus the
               ;; Heading direction's, so rows align with the Heading table.
               shade-scope     (concat non-directional-output-gv-uuids heading-gv-uuids)]
           [construct-result-matrices
            {:ws-uuid              ws-uuid
             :title                @(<t (bp "results"))
             :process-map-units?   (fn [v-uuid] (and map-units-enabled? (map-unit-convertible-variables v-uuid)))
             :multi-valued-inputs  multi-valued-inputs
             :output-gv-uuids      non-directional-output-gv-uuids
             :output-entities      output-entities
             :units-lookup         units-lookup
             :formatters           formatters
             :cell-color-gv-uuid   cell-color-gv-uuid
             :shade-set            @(subscribe [:worksheet/shade-set ws-uuid shade-scope])
             :any-filters-enabled? any-filters-enabled?}]))])))
