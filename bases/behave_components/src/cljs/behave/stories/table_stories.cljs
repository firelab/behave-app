(ns behave.stories.table-stories
  (:require [behave.components.table :refer [table]]
            [behave.stories.utils :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Tables/Tables"
       :component (r/reactify-component table)})

(defn template [& [args]]
  (->default {:component table
              :args      (merge {:title   "Table Title"
                                 :headers ["Column 1" "Column 2" "Column 3"]
                                 :columns [:column1 :column2 :column3]
                                 :rows    [{:column1 1
                                            :column2 2
                                            :column3 3
                                            :shaded? true}
                                           {:column1 4
                                            :column2 5
                                            :column3 6}
                                           {:column1 7
                                            :column2 8
                                            :column3 9}]}
                                args)}))

(def ^:export Default (template))

(def ^:export GroupHeaders (template {:title   "Table Title"
                                      :headers {"SH 1" {"SH 1-1" ["Column 1" "Column 2"]
                                                        "SH 1-2" ["Column 3" "Column 4"]}
                                                "SH 2" {"SH 2-1" ["Column 5" "Column 6"]
                                                        "SH 2-2" ["Column 7" "Column 8"]}}
                                      :columns [:column1
                                                :column2
                                                :column3
                                                :column4
                                                :column5
                                                :column6
                                                :column7
                                                :column8]
                                      :rows    [{:column1 1
                                                 :column2 2
                                                 :column3 3
                                                 :column4 4
                                                 :column5 4
                                                 :column6 5
                                                 :column7 6
                                                 :column8 7
                                                 :shaded? true}
                                                {:column1 8
                                                 :column2 9
                                                 :column3 10
                                                 :column4 11
                                                 :column5 12
                                                 :column6 13
                                                 :column7 14
                                                 :column8 15}
                                                {:column1 16
                                                 :column2 17
                                                 :column3 18
                                                 :column4 19
                                                 :column5 20
                                                 :column6 21
                                                 :column7 22
                                                 :column8 23}]}))
