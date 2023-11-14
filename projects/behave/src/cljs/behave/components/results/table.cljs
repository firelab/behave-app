(ns behave.components.results.table
  (:require [clojure.string                       :as str]
            [behave.components.core               :as c]
            [re-frame.core                        :refer [subscribe]]
            [behave.units-conversion              :refer [to-map-units]]
            [goog.string                          :as gstring]))

(defn- procces-map-units?
  [map-units-enabled? v-uuid]
  (let [map-units-variables @(subscribe [:wizard/map-unit-convertible-variables])]
    (and map-units-enabled? (get map-units-variables v-uuid))))

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
      (gstring/format "Download CSV (%s.csv / %s)" title (format-bytes (.-size blob) 0))]]))

(defn result-table [ws-uuid]
  (let [*headers                  (subscribe [:worksheet/result-table-headers-sorted ws-uuid])
        *cell-data                (subscribe [:worksheet/result-table-cell-data ws-uuid])
        table-setting-filters     (subscribe [:worksheet/table-settings-filters ws-uuid])
        map-units-settings-entity @(subscribe [:worksheet/map-units-settings-entity ws-uuid])
        map-units-enabled?        (:map-units-settings/enabled? map-units-settings-entity)
        map-units                 (:map-units-settings/units map-units-settings-entity)
        map-rep-frac              (:map-units-settings/map-rep-fraction map-units-settings-entity)
        map-units-variables       @(subscribe [:wizard/map-unit-convertible-variables])
        formatters                @(subscribe [:worksheet/result-table-formatters (map first @*headers)])
        table-data                {:title   "Results Table"
                                   :headers (reduce (fn resolve-uuid [acc [gv-uuid _repeat-id units]]
                                                      (let [var-name (:variable/name @(subscribe [:wizard/group-variable gv-uuid]))]
                                                        (cond-> acc
                                                          :always (conj (str var-name (when-not (empty? units) (gstring/format " (%s)" units))))

                                                          (procces-map-units? map-units-enabled? gv-uuid)
                                                          (conj (str var-name " Map Units " (gstring/format " (%s)" map-units))))))
                                                    []
                                                    @*headers)
                                   :columns (reduce (fn [acc [gv-uuid repeat-id _units]]
                                                      (cond-> acc
                                                        :always (conj (keyword (str gv-uuid "-" repeat-id)))

                                                        (procces-map-units? map-units-enabled? gv-uuid)
                                                        (conj (keyword (str/join "-" [gv-uuid repeat-id "map-units"])))))
                                                    []
                                                    @*headers)
                                   :rows (->> (group-by first @*cell-data)
                                              (sort-by key)
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
                                                                   (assoc uuid+repeat-id-key (fmt-fn value))

                                                                   (procces-map-units? map-units-enabled? uuid)
                                                                   (assoc (keyword (str/join "-" [uuid repeat-id "map-units"]))
                                                                          (to-map-units value
                                                                                        (get map-units-variables uuid)
                                                                                        map-units
                                                                                        map-rep-frac)))))
                                                             {}
                                                             data))))}]
    [:div
     (c/table table-data)
     [table-exporter table-data]]))
