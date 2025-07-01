(ns behave.components.results.table
  (:require [behave.components.core  :as c]
            [behave.units-conversion :refer [to-map-units]]
            [behave.translate        :refer [<t bp]]
            [clojure.string          :as str]
            [goog.string             :as gstring]
            [re-frame.core           :refer [subscribe]]
            [string-utils.core       :as s]))

(defn- procces-map-units?
  [map-units-enabled? v-uuid]
  (let [map-units-variables @(subscribe [:wizard/map-unit-convertible-variables])]
    (and map-units-enabled? (map-units-variables v-uuid))))

(defn format-bytes
  "Formats `bytes` into a human-friendly format (e.g. '1 KiB'). Can be called formatted according to `decimals`."
  [bbytes & [decimals]]
  (if (js/isNaN (+ bbytes 0))
    "0 Bytes"
    (let [decimals (or decimals 2)
          k        1024
          dm       (if (< decimals 0) 0 decimals)
          sizes    ["Bytes", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB"]
          i        (js/Math.floor (/ (js/Math.log bbytes) (js/Math.log k)))]
      (gstring/format "%s %s" (.toFixed (js/parseFloat (/ bbytes (js/Math.pow k i))) dm) (nth sizes i)))))

(defn table-exporter
  "Displays a link to download a CSV copy of the table."
  [{:keys [title headers columns rows]}]
  (let [print-row  (fn [row] (str/join "," (map #(get row %) columns)))
        csv-header (str/join "," headers)
        csv-rows   (str/join "\n" (map print-row rows))
        csv        (str csv-header "\n" csv-rows)
        blob       (js/Blob. [csv] #js {:type "text/csv"})
        url        (js/window.URL.createObjectURL blob)]
    [:div
     {:style {:margin "1em"}}
     [:a
      {:href     url
       :download (gstring/format "%s.csv" title)}
      (gstring/format "%s (%s)"
                      (-> @(<t (bp "download_results_table"))
                          s/capitalize-words)
                      @(<t (bp "csv_file")))]]))

(defn- build-result-table-data
  [{:keys [ws-uuid headers title cell-data]
    :or   {headers   @(subscribe [:worksheet/result-table-headers-sorted ws-uuid])
           title     "Results Table"
           cell-data @(subscribe [:worksheet/result-table-cell-data ws-uuid])}}]
  (let [headers-set               (set (map first headers))
        table-setting-filters     (subscribe [:worksheet/table-settings-filters ws-uuid])
        map-units-settings-entity @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units-enabled?        (:map-units-settings/enabled? map-units-settings-entity)
        map-units                 (:map-units-settings/units map-units-settings-entity)
        map-rep-frac              (:map-units-settings/map-rep-fraction map-units-settings-entity)
        map-units-variables       @(subscribe [:worksheet/result-table-units ws-uuid])
        formatters                @(subscribe [:worksheet/result-table-formatters (map first headers)])]
    {:title   title
     :headers (reduce (fn resolve-uuid [acc [gv-uuid _repeat-id units]]
                        (let [header-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name gv-uuid])]
                          (cond-> acc
                            :always (conj (str header-name (when-not (empty? units) (gstring/format " (%s)" units))))

                            (procces-map-units? map-units-enabled? gv-uuid)
                            (conj (str header-name " Map Units " (gstring/format " (%s)" map-units))))))
                      []
                      headers)
     :columns (reduce (fn [acc [gv-uuid repeat-id _units]]
                        (cond-> acc
                          :always (conj (keyword (str gv-uuid "-" repeat-id)))

                          (procces-map-units? map-units-enabled? gv-uuid)
                          (conj (keyword (str/join "-" [gv-uuid repeat-id "map-units"])))))
                      []
                      headers)
     :rows (let [filtered-data (->> (filter (fn [[_k col-uuid]]
                                              (contains? headers-set col-uuid))
                                            cell-data)
                                    (group-by first)
                                    (sort-by key))]
             (map (fn [[_ data]]
                    (reduce (fn [acc [_row-id uuid repeat-id value]]
                              (let [[_ min max enabled?] (first (filter
                                                                 (fn [[gv-uuid]]
                                                                   (= gv-uuid uuid))
                                                                 @table-setting-filters))
                                    fmt-fn               (get formatters uuid identity)
                                    uuid+repeat-id-key   (keyword (str uuid "-" repeat-id))]
                                (cond-> acc
                                  (and min max (not (<= min value max)) enabled?)
                                  (assoc :shaded? true)

                                  :always
                                  (assoc uuid+repeat-id-key (if (neg? value) "-" (fmt-fn value)))

                                  (procces-map-units? map-units-enabled? uuid)
                                  (assoc (keyword (str/join "-" [uuid repeat-id "map-units"]))
                                         (-> value
                                             (to-map-units
                                              (get map-units-variables uuid)
                                              map-units
                                              map-rep-frac)
                                             fmt-fn)))))
                            {}
                            data))
                  filtered-data))}))

(defn result-table-download-link [ws-uuid]
  [:div [table-exporter (build-result-table-data {:ws-uuid ws-uuid
                                                  :headers @(subscribe [:worksheet/csv-export-headers
                                                                        ws-uuid])})]])

(defn raw-result-table [ws-uuid]
  (c/table (build-result-table-data {:ws-uuid ws-uuid})))

(defn pivot-table-data
  "Pivots table data around a collection of pivot rows (these must be keys in the row-data)
   and pivot-valus (an optional collection of tuples [key keyword] where keyword is one of #{:sum
  :count :max :min})."
  [pivot-rows pivot-values row-data]
  (->> row-data
       (group-by (apply juxt pivot-rows))
       (map (fn [[k v]]
              (into (zipmap pivot-rows k)
                    (mapv (fn [[gv-uuid p-fn]]
                            [gv-uuid (reduce
                                      (fn [acc x]
                                        (case p-fn
                                          :sum   (+ (gv-uuid x) acc)
                                          :count (inc acc)
                                          :max   (max acc (gv-uuid x))
                                          :min   (min acc (gv-uuid x))))
                                      (case p-fn
                                        :sum   0
                                        :count 0
                                        :max   ##-Inf
                                        :min   ##Inf)
                                      v)])
                          pivot-values))))))

(defn pivot-tables
  "Returns a collection of pivot table components with specifications (i.e. columns, summation
  functions, etc) from the vms"
  [ws-uuid]
  (let [tables @(subscribe [:worksheet/pivot-tables ws-uuid])]
    (when (seq tables)
      [:div.wizard-results__pivot-tables
       (for [pivot-table tables]
         (let [pivot-fields-uuids     @(subscribe [:worksheet/pivot-table-fields (:db/id pivot-table)])
               pivot-values           @(subscribe [:worksheet/pivot-table-values (:db/id pivot-table)])
               table-data             (build-result-table-data {:ws-uuid ws-uuid
                                                                :title   (:pivot-table/title pivot-table)
                                                                :headers @(subscribe [:worksheet/pivot-table-headers
                                                                                      ws-uuid
                                                                                      (concat pivot-fields-uuids
                                                                                              (map first pivot-values))])})
               gv-uuid->table-keyword (fn [gv-uuid] (keyword (str gv-uuid "-0")))
               pivot-rows             (map gv-uuid->table-keyword pivot-fields-uuids)
               pivot-values           (map (fn [[gv-uuid function]]
                                             [(gv-uuid->table-keyword gv-uuid) function])
                                           pivot-values)]
           (c/table (update table-data
                            :rows
                            #(pivot-table-data pivot-rows pivot-values %)))))])))

(defn directional-result-tables [ws-uuid]
  (let [directions @(subscribe [:worksheet/output-directions ws-uuid])]
    (when (seq directions)
      [:div.wizard-results__directional-tables
       (for [direction directions]
         (c/table (build-result-table-data
                   {:ws-uuid ws-uuid
                    :headers @(subscribe [:worksheet/result-table-headers-sorted-direction ws-uuid direction])
                    :title   (str/capitalize (name direction))})))])))

(defn search-tables
  "A Component for rendering a serch table defined in the VMS.
  A search table has these components
  - group-variable: the group variable in which to apply the search-operation
  - search-operation: operation #{:min :max}
  - filters: filters the result data for only rows that meet the criteria.
  - columns: the columns to show in the search table

  The Table is built following these processing steps:
  1. Filter the cell data for only rows that meet all the filter criterias
  2. Apply either a minimum or maximum function on the desired cell column on the filtered data
  3. Build a table with a single row with the data found in step 2, but only show the columns as specified in the vms"
  [ws-uuid]
  (let [tables                      @(subscribe [:worksheet/search-tables ws-uuid])
        *cell-data                  @(subscribe [:worksheet/result-table-cell-data ws-uuid])
        result-table-headers-sorted @(subscribe [:worksheet/result-table-headers-sorted ws-uuid])
        gv-uuid->units              (reduce (fn [acc [gv-uuid _repeat-id units]]
                                              (assoc acc gv-uuid units))
                                            {}
                                            result-table-headers-sorted)
        formatters                  @(subscribe [:worksheet/result-table-formatters (map first result-table-headers-sorted)])
        multi-valued-input-uuids    @(subscribe [:worksheet/multi-value-input-uuids ws-uuid])]
    (when (seq tables)
      [:div.wizard-results__search-tables
       (for [{search-table-group-variable  :search-table/group-variable
              search-table-operator        :search-table/operator
              search-table-columns         :search-table/columns
              search-filters               :search-table/filters
              search-table-translation-key :search-table/translation-key} tables]
         (let [search-table-group-variable-uuid (:bp/uuid search-table-group-variable)
               filter-fns                       (map (fn [search-filter]
                                                       (let [filter-gv-uuid (:bp/uuid (:search-table-filter/group-variable search-filter))
                                                             operator       (:search-table-filter/operator search-filter)
                                                             filter-value   (:search-table-filter/value search-filter)]
                                                         (fn [[_row gv-uuid _repeat-id value]]
                                                           (and (= gv-uuid filter-gv-uuid)
                                                                (if (= operator :equal)
                                                                  (= value filter-value)
                                                                  (not= value filter-value))))))
                                                     search-filters)
               filtered-rows-set                (->> *cell-data
                                                     (filter (apply every-pred filter-fns))
                                                     (map first)
                                                     set)
               cell-data-after-filter           (filter (fn [[row _gv-uuid _repeat-id _value]] (contains? (set filtered-rows-set) row)) *cell-data)
               row-with-applied-search          (first (apply (if (= search-table-operator :min) min-key max-key)
                                                              (fn [x]
                                                                (js/parseFloat (last x)))
                                                              (filter (fn [[_row gv-uuid _repeat-id _value]]
                                                                        (= gv-uuid search-table-group-variable-uuid))
                                                                      cell-data-after-filter)))
               cell-data-at-row-searched        (filter (fn [[row _gv-uuid _repeat-id _value]] (= row row-with-applied-search)) *cell-data)
               table-row                        (reduce (fn [ acc [_row gv-uuid _repeat-id value]]
                                                          (let [fmt-fn (get formatters gv-uuid identity)]
                                                            (assoc acc
                                                                   (keyword gv-uuid)
                                                                   (-> value
                                                                       fmt-fn))))
                                                        {}
                                                        cell-data-at-row-searched)
               search-table-columns-sorted      (remove (fn [search-table-column] (nil? (get table-row (keyword (:bp/uuid (:search-table-column/group-variable search-table-column))))))
                                                        (sort-by :search-table-column/order search-table-columns))
               table-headers                    (into (mapv (fn [{search-table-column-group-variable  :search-table-column/group-variable
                                                                  search-table-column-translation-key :search-table-column/translation-key}]
                                                              (let [gv-uuid     (:bp/uuid search-table-column-group-variable)
                                                                    units       (get gv-uuid->units gv-uuid)
                                                                    header-name @(<t search-table-column-translation-key)]
                                                                (str header-name (when-not (empty? units) (gstring/format " (%s)" units)))))
                                                            search-table-columns-sorted)
                                                      (mapv #(let [header-name (deref (subscribe [:wizard/gv-uuid->resolve-result-variable-name %]))
                                                                   units       (get gv-uuid->units %)]
                                                               (str header-name (when-not (empty? units) (gstring/format " (%s)" units))))
                                                            multi-valued-input-uuids))
               table-columns                    (into (mapv (fn [column]
                                                              (keyword (:bp/uuid (:search-table-column/group-variable column))))
                                                            search-table-columns-sorted)
                                                      multi-valued-input-uuids)]
           (c/table {:title   @(<t search-table-translation-key)
                     :headers table-headers
                     :columns table-columns
                     :rows    [table-row]})))])))
