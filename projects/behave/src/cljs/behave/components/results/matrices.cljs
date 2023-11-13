(ns behave.components.results.matrices
  (:require [behave.components.core  :as c]
            [behave.units-conversion :refer [to-map-units]]
            [clojure.string          :as str]
            [goog.string             :as gstring]
            [re-frame.core           :refer [subscribe]]))

(defmulti construct-result-matrices
  (fn [{:keys [multi-valued-inputs]}]
    (let [multi-valued-inputs-count (count multi-valued-inputs)]
      (if (<= 0 multi-valued-inputs-count 2)
        multi-valued-inputs-count
        :not-supported))))

(defmethod construct-result-matrices :not-supported
  [{:keys [multi-valued-inputs]}]
  [:div (gstring/format "Tables for (%d) Multi Valued Inputs are not supported"
                        (count multi-valued-inputs))])

(defmethod construct-result-matrices 0
  [{:keys [ws-uuid process-map-units? output-entities formatters]}]
  (let [map-units-settings-entity @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units                 (:map-units-settings/units map-units-settings-entity)
        map-rep-frac              (:map-units-settings/map-rep-fraction map-units-settings-entity)
        rows                      (reduce (fn [acc {output-uuid    :bp/uuid
                                                    var-name       :variable/name
                                                    var-units-uuid :variable/native-unit-uuid}]
                                            (let [value  @(subscribe [:worksheet/first-row-results-gv-uuid->value
                                                                      ws-uuid
                                                                      output-uuid])
                                                  units  @(subscribe [:vms/units-uuid->short-code var-units-uuid])
                                                  fmt-fn (get formatters output-uuid identity)]
                                              (cond-> acc
                                                :always (conj {:output var-name
                                                               :value  (fmt-fn value)
                                                               :units  units})

                                                (process-map-units? output-uuid)
                                                (conj {:output (gstring/format "%s Map Units" var-name)
                                                       :value  (to-map-units value
                                                                             units
                                                                             map-units
                                                                             map-rep-frac)
                                                       :units  map-units}))))
                                          []
                                          output-entities)]
    [:div.print__result-table
     (c/table {:title   "Results"
               :headers ["Output Variable" "Value" "Units"]
               :columns [:output :value :units]
               :rows    rows})]))

(defmethod construct-result-matrices 1
  [{:keys [ws-uuid process-map-units? multi-valued-inputs formatters output-entities]}]
  (let [[multi-var-name
         multi-var-units-uuid
         multi-var-gv-uuid
         multi-var-values]        (first multi-valued-inputs)
        matrix-data-raw           @(subscribe [:worksheet/matrix-table-data-single-multi-valued-input
                                               ws-uuid
                                               multi-var-gv-uuid
                                               (str/split multi-var-values ",")
                                               (map :bp/uuid output-entities)])
        matrix-data-formatted     (reduce-kv (fn [acc [_row col-uuid :as k] _v]
                                               (let [fmt-fn (get formatters col-uuid identity)]
                                                 (update acc k #(fmt-fn %))))
                                             matrix-data-raw
                                             matrix-data-raw)
        map-units-settings-entity @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units                 (:map-units-settings/units map-units-settings-entity)
        map-rep-frac              (:map-units-settings/map-rep-fraction map-units-settings-entity)
        column-headers            (reduce (fn insert-map-units-columns [acc {output-gv-uuid           :bp/uuid
                                                                             output-name              :variable/name
                                                                             output-native-units-uuid :variable/native-unit-uuid}]
                                            (let [units @(subscribe [:vms/units-uuid->short-code output-native-units-uuid])]
                                              (cond-> acc
                                                :always (conj {:name (gstring/format "%s (%s)"
                                                                                     output-name
                                                                                     units)
                                                               :key  output-gv-uuid})

                                                (process-map-units? output-gv-uuid)
                                                (conj {:name (gstring/format "%s Map Units (%s)"
                                                                             output-name
                                                                             map-units)
                                                       :key  (str output-gv-uuid "-map-units")}))))
                                          []
                                          output-entities)
        row-headers (map (fn [value] {:name value :key (str value)}) (str/split multi-var-values ","))
        final-data  (reduce (fn insert-map-units-values [acc [[i j] value]]
                              (cond-> acc
                                (process-map-units? j)
                                (assoc [i (str j "-map-units")]
                                       (to-map-units value
                                                     @(subscribe [:wizard/gv-uuid->variable-units j])
                                                     map-units
                                                     map-rep-frac))))
                            matrix-data-formatted
                            matrix-data-formatted)]
    [:div.print__result-table
     (c/matrix-table {:title          "Results"
                      :rows-label     (gstring/format "%s (%s)"
                                                      multi-var-name
                                                      @(subscribe [:vms/units-uuid->short-code multi-var-units-uuid]))
                      :cols-label     "Outputs"
                      :column-headers column-headers
                      :row-headers    row-headers
                      :data           final-data})]))

(defmethod construct-result-matrices 2
  [{:keys [ws-uuid process-map-units? multi-valued-inputs formatters output-entities]}]
  (let [[row-name row-units-uuid row-gv-uuid row-values] (first multi-valued-inputs)
        [col-name col-units-uuid col-gv-uuid col-values] (second multi-valued-inputs)
        row-units-code                                   (:unit/short-code @(subscribe [:vms/entity-from-uuid row-units-uuid]))
        col-units-code                                   (:unit/short-code @(subscribe [:vms/entity-from-uuid col-units-uuid]))
        map-units-settings-entity                        @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units                                        (:map-units-settings/units map-units-settings-entity)
        map-rep-frac                                     (:map-units-settings/map-rep-fraction map-units-settings-entity)]
    [:div.print__construct-result-matrices
     (for [{output-uuid             :bp/uuid
            output-name             :variable/name
            output-native-unit-uuid :variable/native-unit-uuid} output-entities]
       (let [output-units          @(subscribe [:vms/entity-from-uuid output-native-unit-uuid])
             output-units-code     (:unit/short-code output-units)
             fmt-fn                (get formatters output-uuid identity)
             matrix-data-raw       @(subscribe [:print/matrix-table-two-multi-valued-inputs ws-uuid
                                                row-gv-uuid
                                                (str/split row-values ",")
                                                col-gv-uuid
                                                (str/split col-values ",")
                                                output-uuid])
             matrix-data-formatted (reduce-kv (fn [acc k _v]
                                                (update acc k #(fmt-fn %)))
                                              matrix-data-raw
                                              matrix-data-raw)
             row-headers           (map (fn [value] {:name value :key value})
                                        (str/split row-values ","))
             column-headers        (map (fn [value] {:name value :key value})
                                        (str/split col-values ","))]
         [:<>
          [:div.print__result-table
           (c/matrix-table {:title          (gstring/format "%s (%s)" output-name output-units-code)
                            :rows-label     (gstring/format "%s (%s)" row-name row-units-code)
                            :cols-label     (gstring/format "%s (%s)" col-name col-units-code)
                            :row-headers    row-headers
                            :column-headers column-headers
                            :data           matrix-data-formatted})]
          (when (process-map-units? output-uuid)
            [:div.print__result-table
             (let [data (reduce-kv (fn [acc [i j] value]
                                     (assoc acc [i j] (to-map-units value
                                                                    output-units-code
                                                                    map-units
                                                                    map-rep-frac)))
                                   matrix-data-formatted
                                   matrix-data-formatted)]
               (c/matrix-table {:title          (gstring/format "%s Map Units (%s)" output-name map-units)
                                :rows-label     (gstring/format "%s (%s)" row-name row-units-code)
                                :cols-label     (gstring/format "%s (%s)" col-name col-units-code)
                                :row-headers    row-headers
                                :column-headers column-headers
                                :data           data}))])]))]))

(defn result-matrices [ws-uuid]
  (let [map-units-settings-entity      @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units-enabled?             (:map-units-settings/enabled? map-units-settings-entity)
        map-unit-convertible-variables @(subscribe [:wizard/map-unit-convertible-variables])
        output-uuids                   @(subscribe [:worksheet/all-output-uuids ws-uuid])]
    [construct-result-matrices
     {:ws-uuid             ws-uuid
      :process-map-units?  (fn [v-uuid]
                             (and map-units-enabled?
                                  (get map-unit-convertible-variables v-uuid)))
      :multi-valued-inputs @(subscribe [:print/matrix-table-multi-valued-inputs ws-uuid])
      :output-uuids        output-uuids
      :output-entities     (map (fn [gv-uuid]
                                  @(subscribe [:wizard/group-variable gv-uuid])) output-uuids)
      :formatters          @(subscribe [:worksheet/result-table-formatters output-uuids])}]))
