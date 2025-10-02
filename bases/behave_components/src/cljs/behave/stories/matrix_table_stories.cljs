(ns behave.stories.matrix-table-stories
  (:require [behave.components.matrix-table      :refer [matrix-table]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Matrix/Matrix"
       :component (r/reactify-component matrix-table)})

(defn template [& [args]]
  (->default {:component matrix-table
              :args      (merge {:title          "Matrix Title"
                                 :sub-title      "Matrix Sub Title (Optional)"
                                 :column-headers [{:name "Column 1" :key :column1}
                                                  {:name "Column 2" :key :column2}
                                                  {:name "Column 3" :key :column3}]

                                 :row-headers [{:name "Row 1" :key :row1}
                                               {:name "Row 2" :key :row2}
                                               {:name "Row 3" :key :row3}]

                                 :data {[:row1 :column1] 1
                                        [:row1 :column2] 2
                                        [:row1 :column3] 3

                                        [:row2 :column1] 4
                                        [:row2 :column2] 5
                                        [:row2 :column3] 6

                                        [:row3 :column1] 7
                                        [:row3 :column2] 8
                                        [:row3 :column3] 9}}
                                args)}))

(def ^:export Default (template))
(def ^:export Labels (template {:rows-label     "Rows"
                                :cols-label     "Cols"
                                :column-headers [{:name "0" :key 0}
                                                 {:name "1" :key 1}
                                                 {:name "2" :key 2}]

                                :row-headers    [{:name "0" :key 0}
                                                 {:name "1" :key 1}
                                                 {:name "2" :key 2}]

                                :data           {[0 0] 1
                                                 [0 1] 2
                                                 [0 2] 3

                                                 [1 0] 4
                                                 [1 1] 5
                                                 [1 2] 6


                                                 [2 0] 7
                                                 [2 1] 8
                                                 [2 2] 9}}))
