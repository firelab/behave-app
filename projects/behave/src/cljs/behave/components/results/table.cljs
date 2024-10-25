(ns behave.components.results.table
  (:require [behave.units-conversion :refer [to-map-units]]
            [clojure.string          :as str]
            [goog.string             :as gstring]
            [behave.components.core  :as c]
            [re-frame.core           :refer [subscribe]]))

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
      (gstring/format "Download Raw CSV (%s.csv / %s)" title (format-bytes (.-size blob) 0))]]))

(defn- build-result-table-data
  [{:keys [ws-uuid headers title]
    :or   {headers @(subscribe [:worksheet/result-table-headers-sorted ws-uuid])
           title  "Results Table"}}]
  (let [headers-set               (set (map first headers))
        *cell-data                (subscribe [:worksheet/result-table-cell-data ws-uuid])
        table-setting-filters     (subscribe [:worksheet/table-settings-filters ws-uuid])
        map-units-settings-entity @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units-enabled?        (:map-units-settings/enabled? map-units-settings-entity)
        map-units                 (:map-units-settings/units map-units-settings-entity)
        map-rep-frac              (:map-units-settings/map-rep-fraction map-units-settings-entity)
        map-units-variables       @(subscribe [:worksheet/result-table-units ws-uuid])
        formatters                @(subscribe [:worksheet/result-table-formatters (map first headers)])]
    {:title   title
     :headers (reduce (fn resolve-uuid [acc [gv-uuid _repeat-id units]]
                        (let [var-name @(subscribe [:wizard/gv-uuid->resolve-result-variable-name gv-uuid])]
                          (cond-> acc
                            :always (conj (str var-name (when-not (empty? units) (gstring/format " (%s)" units))))

                            (procces-map-units? map-units-enabled? gv-uuid)
                            (conj (str var-name " Map Units " (gstring/format " (%s)" map-units))))))
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
                                            @*cell-data)
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
                                                                :title  (:pivot-table/title pivot-table)
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
                    :title  (str/capitalize (name direction))})))])))
