(ns behave.components.matrix-table
  (:require [behave.stories.utils :refer [->params]]))

(defn- table-header [title rows-label cols-label header-names & [sub-title]]
  [:thead.table-header
   [:tr.table__title
    [:th {:col-span (inc (count header-names))} title]]
   (when sub-title
     [:tr.table__sub-title
      [:th {:col-span (inc (count header-names))} sub-title]])
   (when (and rows-label cols-label)
     [:tr
      [:th.table-header__header {:col-span 1 :scope "row"} rows-label]
      [:th.table-header__header {:col-span (inc (count header-names)) :scope "row"} cols-label]])
   [:tr
    (-> (for [header-name header-names]
          [:th.table-header__header {:scope "col"} header-name])
        (conj [:th.table-header__header {:scope "col"}]))]])

(defn matrix-table
  "A component for constructing a table
  - title : string
  - sub-title : string
  - column-headers : sequence of column ids [col1 col2, ... ]
  - row-headers : sequence of row ids [row1, row2, ... ]
  - data : a sequence of [row1 col1] -> value
  - rows-label : string
  - cols-labl : string
  - cell-colors : a map of [row col] -> hex color
  "
  [{:keys [title sub-title column-headers row-headers data rows-label cols-label cell-colors]}]
  (let [column-headers      (->params column-headers)
        row-headers         (->params row-headers)
        data                (->params data)
        column-header-names (map :name column-headers)]
    [:table.table
     [table-header title rows-label cols-label column-header-names sub-title]
     [:tbody.table__body
      (for [row-header row-headers
            :let       [i    (:key row-header)
                        row-name (:name row-header)]]
        ^{:key i}
        [:tr.table-row
         [:th.table-header__header {:scope "row"} row-name]
         (for [column-header column-headers
               :let          [j (:key column-header)
                              color (get cell-colors [i j])]]
           ^{:key j}
           [:td.table-cell {:style (when color {:background-color color})} (get data [i j])])])]]))
