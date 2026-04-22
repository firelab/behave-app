(ns behave.components.results.matrices
  (:require [behave.components.core  :as c]
            [behave.translate        :refer [<t bp]]
            [behave.units-conversion :refer [to-map-units]]
            [cljs.math               :refer [round]]
            [clojure.string          :as str]
            [goog.string             :as gstring]
            [re-frame.core           :refer [subscribe]]))

;;==============================================================================
;; Helpers
;;==============================================================================

(defn- shade-cell-value? [table-setting-filters output-gv-uuid value]
  (let [[_ mmin mmax enabled?] (first (filter
                                       (fn [[gv-uuid]]
                                         (= gv-uuid output-gv-uuid))
                                       table-setting-filters))]
    (and enabled? mmin mmax (not (<= mmin (round value) mmax)))))

(defn- header-label [label units]
  (if (seq units)
    (gstring/format "%s (%s)" label units)
    (gstring/format "%s" label)))

(defn- fetch-map-units-settings [ws-uuid]
  (let [entity @(subscribe [:worksheet/map-units-settings-entity ws-uuid])]
    {:units        (:map-units-settings/units entity)
     :rep-fraction (:map-units-settings/map-rep-fraction entity)}))

(defn- format-matrix-cell [value formatter shaded? in-range?]
  [:div {:class (cond-> ["result-matrix-cell-value"]
                  shaded?   (conj "table-cell__shaded")
                  in-range? (conj "table-cell__in-range"))}
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

(defn- compute-shade-set-2d
  [{:keys [ws-uuid row-gv-uuid row-values col-gv-uuid col-values output-entities table-setting-filters submatrix-gv-uuid submatrix-value]}]
  (reduce (fn [acc {output-gv-uuid :bp/uuid}]
            (let [matrix-data (fetch-matrix-data-2d {:ws-uuid           ws-uuid
                                                     :row-gv-uuid       row-gv-uuid
                                                     :row-values        row-values
                                                     :col-gv-uuid       col-gv-uuid
                                                     :col-values        col-values
                                                     :output-gv-uuid    output-gv-uuid
                                                     :submatrix-gv-uuid submatrix-gv-uuid
                                                     :submatrix-value   submatrix-value})]
              (into acc
                    (reduce-kv
                     (fn [acc [row col] value]
                       (cond-> acc
                         (shade-cell-value? table-setting-filters output-gv-uuid value)
                         (conj [row col])))
                     #{}
                     matrix-data))))
          #{}
          output-entities))

(defn- build-matrix-data
  [matrix-data-raw row-fmt-fn col-fmt-fn output-fmt-fn shade-set]
  (reduce-kv
   (fn [acc [row col] value]
     (let [shaded? (contains? shade-set [row col])]
       (assoc acc [(row-fmt-fn row) (col-fmt-fn col)]
              (format-matrix-cell value output-fmt-fn shaded? (not shaded?)))))
   {}
   matrix-data-raw))

(defn- build-matrix-data-with-map-units
  [{:keys [matrix-data-raw row-fmt-fn col-fmt-fn output-fmt-fn output-units map-units map-rep-frac shade-set]}]
  (reduce-kv
   (fn [acc [row col] value]
     (let [shaded?         (contains? shade-set [row col])
           converted-value (convert-to-map-units value output-units map-units map-rep-frac)]
       (assoc acc [(row-fmt-fn row) (col-fmt-fn col)]
              (format-matrix-cell converted-value output-fmt-fn shaded? (not shaded?)))))
   {}
   matrix-data-raw))

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
  [{:keys [ws-uuid process-map-units? multi-valued-inputs formatters output-entities units-lookup table-setting-filters title]}]
  (let [[multi-var-name
         multi-var-units
         multi-var-gv-uuid
         multi-var-values]           (first multi-valued-inputs)
        input-fmt-fn                 (get @(subscribe [:worksheet/result-table-formatters [multi-var-gv-uuid]]) multi-var-gv-uuid)
        matrix-data-raw              @(subscribe [:worksheet/matrix-table-data-single-multi-valued-input
                                                  ws-uuid
                                                  multi-var-gv-uuid
                                                  multi-var-values
                                                  (map :bp/uuid output-entities)])
        rows-to-shade-set            (reduce-kv (fn [acc [row col-uuid] v]
                                                  (cond-> acc
                                                    (shade-cell-value? table-setting-filters col-uuid v)
                                                    (conj row)))
                                                #{}
                                                matrix-data-raw)
        {:keys [units rep-fraction]} (fetch-map-units-settings ws-uuid)
        [regular-column-headers
         map-units-column-headers]   (reduce (fn [[reg mu] {output-gv-uuid :bp/uuid output-units :units}]
                                               (let [output-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name output-gv-uuid])]
                                                 [(conj reg {:name (header-label output-name output-units)
                                                             :key  output-gv-uuid})
                                                  (if (process-map-units? output-gv-uuid)
                                                    (conj mu {:name (header-label output-name units)
                                                              :key  (map-units-column-key output-gv-uuid)})
                                                    mu)]))
                                             [[] []]
                                             output-entities)
        [matrix-data-formatted
         map-units-data]             (reduce-kv (fn [[reg mu] [row col-uuid] v]
                                                  (let [fmt-fn  (get formatters col-uuid identity)
                                                        shaded? (contains? rows-to-shade-set row)]
                                                    [(assoc reg [(input-fmt-fn row) col-uuid]
                                                            (format-matrix-cell v fmt-fn shaded? (not shaded?)))
                                                     (if (process-map-units? col-uuid)
                                                       (assoc mu [(input-fmt-fn row) (map-units-column-key col-uuid)]
                                                              (format-matrix-cell
                                                               (convert-to-map-units v (get units-lookup col-uuid) units rep-fraction)
                                                               fmt-fn
                                                               shaded?
                                                               (not shaded?)))
                                                       mu)]))
                                                [{} {}]
                                                matrix-data-raw)
        row-headers                  (map (fn [value] {:name (input-fmt-fn value) :key (input-fmt-fn value)}) multi-var-values)
        common-matrix-props          {:rows-label  (header-label multi-var-name multi-var-units)
                                      :cols-label  @(<t (bp "outputs"))
                                      :row-headers row-headers}]
    [:div.print__result-table
     (c/matrix-table (merge common-matrix-props
                            {:title          title
                             :column-headers regular-column-headers
                             :data           matrix-data-formatted}))
     (when (seq map-units-column-headers)
       [:div.result-matrix__map-units-table
        (c/matrix-table (merge common-matrix-props
                               {:title          (gstring/format @(<t (bp "s_map_units_(s)")) title units)
                                :column-headers map-units-column-headers
                                :data           map-units-data}))])]))

(defmethod construct-result-matrices 2
  [{:keys [ws-uuid process-map-units? multi-valued-inputs formatters output-entities table-setting-filters
           sub-title submatrix-value submatrix-gv-uuid]}]
  (let [graph-settings                              @(subscribe [:worksheet/graph-settings ws-uuid])
        x-axis-gv-uuid                              (:graph-settings/x-axis-group-variable-uuid graph-settings)
        z-axis-gv-uuid                              (:graph-settings/z-axis-group-variable-uuid graph-settings)
        [row-name row-units row-gv-uuid row-values] (->> multi-valued-inputs
                                                         (filter (fn [[_ _ gv-uuid]] (= gv-uuid z-axis-gv-uuid)))
                                                         first)
        [col-name col-units col-gv-uuid col-values] (->> multi-valued-inputs
                                                         (filter (fn [[_ _ gv-uuid]] (= gv-uuid x-axis-gv-uuid)))
                                                         first)
        {:keys [units rep-fraction]}                (fetch-map-units-settings ws-uuid)
        input-formatters                            @(subscribe [:worksheet/result-table-formatters [row-gv-uuid col-gv-uuid]])
        row-fmt-fn                                  (get input-formatters row-gv-uuid identity)
        col-fmt-fn                                  (get input-formatters col-gv-uuid identity)
        shade-set                                   (compute-shade-set-2d {:ws-uuid               ws-uuid
                                                                           :row-gv-uuid           row-gv-uuid
                                                                           :row-values            row-values
                                                                           :col-gv-uuid           col-gv-uuid
                                                                           :col-values            col-values
                                                                           :output-entities       output-entities
                                                                           :table-setting-filters table-setting-filters
                                                                           :submatrix-gv-uuid     submatrix-gv-uuid
                                                                           :submatrix-value       submatrix-value})
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
           (let [matrix-data-formatted (build-matrix-data matrix-data-raw row-fmt-fn col-fmt-fn output-fmt-fn shade-set)]
             [:<>
              (when (process-map-units? output-gv-uuid)
                [:div.print__result-table
                 (let [data (build-matrix-data-with-map-units {:matrix-data-raw matrix-data-raw
                                                               :row-fmt-fn      row-fmt-fn
                                                               :col-fmt-fn      col-fmt-fn
                                                               :output-fmt-fn   output-fmt-fn
                                                               :output-units    output-units
                                                               :map-units       units
                                                               :map-rep-frac    rep-fraction
                                                               :shade-set       shade-set})]
                   (c/matrix-table {:title          (gstring/format @(<t (bp "s_map_units_(s)")) output-name units)
                                    :sub-title      sub-title
                                    :rows-label     (header-label row-name row-units)
                                    :cols-label     (header-label col-name col-units)
                                    :row-headers    row-headers-sorted
                                    :column-headers column-headers-sorted
                                    :data           data}))])
              [:div.print__result-table
               (c/matrix-table {:title          (header-label output-name output-units)
                                :sub-title      sub-title
                                :rows-label     (header-label row-name row-units)
                                :cols-label     (header-label col-name col-units)
                                :row-headers    row-headers-sorted
                                :column-headers column-headers-sorted
                                :data           matrix-data-formatted})]]))))]))

(defmethod construct-result-matrices 3
  [{:keys [ws-uuid process-map-units? multi-valued-inputs formatters output-entities table-setting-filters units-lookup]}]
  (let [graph-settings                             @(subscribe [:worksheet/graph-settings ws-uuid])
        z2-axis-group-variable-uuid                (:graph-settings/z2-axis-group-variable-uuid graph-settings)
        [var-name units-short-code gv-uuid values] (->> multi-valued-inputs
                                                        (filter (fn [[_ _ gv-uuid]] (= gv-uuid z2-axis-group-variable-uuid)))
                                                        first)
        rest-multi-valued-inputs                   (filter (fn [[_ _ gv-uuid]] (not= gv-uuid z2-axis-group-variable-uuid)) multi-valued-inputs)]
    [:div.print__result-table
     (for [value values]
       [:div.print__result-table
        [construct-result-matrices
         {:ws-uuid               ws-uuid
          :title                 @(<t (bp "results"))
          :sub-title             (if (not-empty units-short-code)
                                   (gstring/format "%s: %s (%s)"
                                                   var-name
                                                   @(subscribe [:vms/resolve-enum-translation gv-uuid value])
                                                   units-short-code)
                                   (gstring/format "%s: %s"
                                                   var-name
                                                   @(subscribe [:vms/resolve-enum-translation gv-uuid value])))
          :submatrix-value       value
          :submatrix-gv-uuid     gv-uuid
          :process-map-units?    process-map-units?
          :multi-valued-inputs   rest-multi-valued-inputs
          :output-entities       output-entities
          :units-lookup          units-lookup
          :formatters            formatters
          :table-setting-filters table-setting-filters}]])]))

;;==============================================================================
;; View for Result Matrices
;;==============================================================================

(defn result-matrices [ws-uuid]
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
        multi-valued-inputs             @(subscribe [:print/matrix-table-multi-valued-inputs ws-uuid])]
    (when (seq all-output-gv-uuids)
      [:div.wizard-results
       (when (seq directional-gv-uuids)
         (for [direction directions]
           (let [output-gv-uuids (filter #(deref (subscribe [:vms/group-variable-is-directional? % direction])) directional-gv-uuids)
                 group-variables (map (fn [gv-uuid] @(subscribe [:wizard/group-variable gv-uuid])) output-gv-uuids)
                 output-entities (map (fn [gv] (merge gv {:units (get units-lookup (:bp/uuid gv))})) group-variables)
                 formatters      @(subscribe [:worksheet/result-table-formatters output-gv-uuids])]
             [construct-result-matrices
              {:title                 (str/capitalize (name direction))
               :ws-uuid               ws-uuid
               :process-map-units?    (fn [v-uuid] (and map-units-enabled? (map-unit-convertible-variables v-uuid)))
               :multi-valued-inputs   multi-valued-inputs
               :output-gv-uuids       output-gv-uuids
               :output-entities       output-entities
               :units-lookup          units-lookup
               :formatters            formatters
               :table-setting-filters table-setting-filters}])))
       (when (seq non-directional-output-gv-uuids)
         (let [group-variables (map (fn [gv-uuid] @(subscribe [:wizard/group-variable gv-uuid])) non-directional-output-gv-uuids)
               output-entities (map (fn [gv] (merge gv {:units (get units-lookup (:bp/uuid gv))})) group-variables)
               formatters      @(subscribe [:worksheet/result-table-formatters non-directional-output-gv-uuids])]
           [construct-result-matrices
            {:ws-uuid               ws-uuid
             :title                 @(<t (bp "results"))
             :process-map-units?    (fn [v-uuid] (and map-units-enabled? (map-unit-convertible-variables v-uuid)))
             :multi-valued-inputs   multi-valued-inputs
             :output-gv-uuids       non-directional-output-gv-uuids
             :output-entities       output-entities
             :units-lookup          units-lookup
             :formatters            formatters
             :table-setting-filters table-setting-filters}]))])))
