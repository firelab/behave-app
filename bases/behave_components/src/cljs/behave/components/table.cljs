(ns behave.components.table)

(defn table-row [columns {:keys [shaded?] :as row}]
  [:tr {:class ["table-row"
                (when shaded? "table-row--strikethrough")]}
   (for [column columns]
     [:td {:class ["table-cell"]} (get row column)])])

(defmulti table-header
  (fn [_title headers]
    (if (vector? headers)
      :v1
      :v2))) ;else map?

(defmethod table-header :v1
  [title headers col-cnt]
  [:thead.table-header
   [:tr {:class ["table__title"]}
    [:th {:col-span col-cnt} title]]
   [:tr
    (for [header headers]
      [:th.table-header__header {:scope "col"} header])]])

(defn- map->leaf-paths
  "Converts map to a sequence of paths for each leaf node.

  Given:
  {:sh1 {:sh1-1 [:column1 :column2]
         :sh1-2 [:column3 :column4]}
   :sh2 {:sh2-1  [:column5 :column6]
         :sh-2-2 [:column7 :column8]}}

  Returns:
  ([:sh1 :sh1-1 :column1]
   [:sh1 :sh1-1 :column2]
   [:sh1 :sh1-2 :column3]
   [:sh1 :sh1-2 :column4]
   [:sh2 :sh2-1 :column5]
   [:sh2 :sh2-1 :column6]
   [:sh2 :sh-2-2 :column7]
   [:sh2 :sh-2-2 :column8])"
  [data]
  (letfn [(helper [node path]
            (cond
              (vector? node)
              (map #(conj path %) node)

              (map? node)
              (mapcat (fn [[k v]]
                        (helper v (conj path k)))
                      node)

              :else
              [(conj path node)]))]
    (helper data [])))

(defn- transpose [xs]
  (apply map vector xs))

(defmethod table-header :v2
  [title headers col-cnt]
  (let [leaf-paths    (map->leaf-paths headers)
        group-headers (->> leaf-paths
                           transpose
                           butlast
                           (map #(mapcat frequencies (partition-by identity %))))]
    [:thead.table-header
     [:tr.table__title
      [:th {:col-span col-cnt} title]]
     (for [header group-headers]
       [:tr.table-header__group
        (map (fn [[header-name cnt]]
               [:th {:col-span cnt} header-name])
             header)])
     [:tr
      (for [header (map last leaf-paths)]
        [:th.table-header__header {:scope "col"} header])]]))

(defn table [{:keys [title headers columns rows]}]
  (let [columns (mapv keyword (js->clj columns :keywordize-keys true))
        headers (js->clj headers)
        rows    (js->clj rows :keywordize-keys true)]
    [:table {:class ["table"]}
     [table-header title headers (count columns)]
     [:tbody.table__body
      (for [row rows]
        [table-row columns row])]]))
