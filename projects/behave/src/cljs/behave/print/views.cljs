(ns behave.print.views
  (:require [re-frame.core    :as rf]
            [goog.string      :as gstring]
            [behave.components.core         :as c]
            [behave.translate :refer [<t]]))

(defn- indent-name [level s]
  (str (apply str (repeat level "    ")) s))

(defn- groups->row-entires
  "Returns a sequence of row entries of the form
  {:input (required)
   :unit  (optional)
   :value (optional)}"
  [ws-uuid groups & [level]]
  (loop [[current-group & next-groups] groups
         level                         level
         acc                           []]

    (cond
      (and current-group @(rf/subscribe [:wizard/show-group?
                                         ws-uuid
                                         (:db/id current-group)
                                         (:group/conditionals-operator current-group)]))
      (let [variables        (->> current-group (:group/group-variables) (sort-by :group-variable/variable-order))
            single-var?      (= (count variables) 1)
            multi-var?       (> (count variables) 1)
            new-entries      (cond single-var?
                                   [{:input  (indent-name level (:group/name current-group)) ;Use group name instead of var name to match what is in the inputs UI
                                     :units  (:variable/native-units (first variables))
                                     :values @(rf/subscribe [:worksheet/input-value
                                                             ws-uuid
                                                             (:bp/uuid current-group)
                                                             0 ;repeat-id
                                                             (:bp/uuid (first variables))])}]
                                   multi-var?
                                   (into [{:input (indent-name level (:group/name current-group))}]
                                         (let [repeat-ids @(rf/subscribe [:worksheet/group-repeat-ids ws-uuid (:bp/uuid current-group)])]
                                           (mapcat (fn [repeat-id]
                                                     (map (fn [variable]
                                                            {:input  (indent-name (inc level) (:variable/name variable))
                                                             :units  (:variable/native-units variable)
                                                             :values @(rf/subscribe [:worksheet/input-value
                                                                                     ws-uuid
                                                                                     (:bp/uuid current-group)
                                                                                     repeat-id
                                                                                     (:bp/uuid variable)])})
                                                          variables))
                                                   repeat-ids)))
                                   :else
                                   [])
            children         (->> (:group/children current-group)
                                  (sort-by :group/order))
            next-indent      (if single-var?  (inc level) (+ level 2))
            children-entires (when (seq children)
                               (groups->row-entires ws-uuid children next-indent))]
        (recur next-groups level (-> acc
                                     (into new-entries)
                                     (into children-entires))))

      next-groups
      (recur next-groups level acc)

      :else
      acc)))

(defn- build-rows [ws-uuid submodules]
  (reduce (fn [acc submodule]
            (let [{id :db/id
                   op :submodule/conditionals-operator} submodule
                  show-module?                          @(rf/subscribe [:wizard/show-submodule? ws-uuid id op])]
              (if show-module?
                (cond-> (conj acc {:input (:submodule/name submodule)})

                  (:submodule/groups submodule)
                  (into (groups->row-entires ws-uuid (:submodule/groups submodule) 1)))
                acc)))
          []
          submodules))

(defn inputs-table [ws-uuid]
  (let [*worksheet (rf/subscribe [:worksheet ws-uuid])
        modules    (:worksheet/modules @*worksheet)]
    (for [module-kw modules]
      (let [module-name (name module-kw)
            module      @(rf/subscribe [:wizard/*module module-name])
            submodules  @(rf/subscribe [:wizard/submodules-io-input-only (:db/id module)])]
        [:div.print__inputs-table
         (c/table {:title   (gstring/format "Inputs: %s"  @(<t (:module/translation-key module)))
                   :headers ["Input Variables" "Units" "Input Value(s)"]
                   :columns [:input :units :values]
                   :rows    (build-rows ws-uuid submodules)})]))))

#_(defn- matrix-tables [ws-uuid]
    (let [num-multi-valued-input 1]
      (case num-multi-valued-input
        0 [:div (c/table {:title   "Case 0 Multi-valued inputs"
                          :headers ["Output Variable" "Value" "Units"]
                          :columns [:output :value :units]
                          :rows    [{:output "outupt1" :value 1 :units "%"}
                                    {:output "outupt2" :value 1 :units "%"}
                                    {:output "outupt3" :value 1 :units "%"}]})]
        1 [:div (c/matrix-table {:title          "Matrix Title"
                                 :rows-label     "Rows"
                                 :cols-label     "Columns"
                                 :column-headers [{:name "Column 1" :key :column1}
                                                  {:name "Column 2" :key :column2}
                                                  {:name "Column 3" :key :column3}]
                                 :row-headers    [{:name "Row 1" :key :row1}
                                                  {:name "Row 2" :key :row2}
                                                  {:name "Row 3" :key :row3}]
                                 :data           {[:row1 :column1] 1
                                                  [:row1 :column2] 2
                                                  [:row1 :column3] 3

                                                  [:row2 :column1] 4
                                                  [:row2 :column2] 5
                                                  [:row2 :column3] 6

                                                  [:row3 :column1] 7
                                                  [:row3 :column2] 8
                                                  [:row3 :column3] 9}})]
        2 [:div (c/matrix-table {:title          "Matrix Title"
                                 :column-headers [{:name "Row Name"}
                                                  {:name "Column 1" :key :column1}
                                                  {:name "Column 2" :key :column2}
                                                  {:name "Column 3" :key :column3}]
                                 :row-headers    [{:name "Row 1" :key :row1}
                                                  {:name "Row 2" :key :row2}
                                                  {:name "Row 3" :key :row3}]
                                 :data           {[:row1 :column1] 1
                                                  [:row1 :column2] 2
                                                  [:row1 :column3] 3

                                                  [:row2 :column1] 4
                                                  [:row2 :column2] 5
                                                  [:row2 :column3] 6

                                                  [:row3 :column1] 7
                                                  [:row3 :column2] 8
                                                  [:row3 :column3] 9}})]
        nil [:div (str num-multi-valued-input " multi valued inputs not supported currently.")])))

(defn- matrix-tables [ws-uuid]
  (let [num-multi-valued-input 1]
    [:div
     [:div (c/table {:title   "Case 0 Multi-valued inputs"
                     :headers ["Output Variable" "Value" "Units"]
                     :columns [:output :value :units]
                     :rows    [{:output "outupt1" :value 1 :units "%"}
                               {:output "outupt2" :value 1 :units "%"}
                               {:output "outupt3" :value 1 :units "%"}]})]
     [:div (c/matrix-table {:title          "Matrix Title"
                            :rows-label     "Rows"
                            :cols-label     "Columns"
                            :column-headers [{:name "Column 1" :key :column1}
                                             {:name "Column 2" :key :column2}
                                             {:name "Column 3" :key :column3}]
                            :row-headers    [{:name "0" :key 0}
                                             {:name "1" :key 1}
                                             {:name "2" :key 2}]
                            :data           {[0 :column1] 1
                                             [0 :column2] 2
                                             [0 :column3] 3

                                             [1 :column1] 4
                                             [1 :column2] 5
                                             [1 :column3] 6

                                             [2 :column1] 7
                                             [2 :column2] 8
                                             [2 :column3] 9}})]
     [:div (c/matrix-table {:title          "Matrix Title"
                            :rows-label     "Rows"
                            :cols-label     "Columns"
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
                                             [2 2] 9}})]
     [:div (str num-multi-valued-input " multi valued inputs not supported currently.")]]))

(defn print-page [{:keys [ws-uuid]}]
  [:div.print
   (inputs-table ws-uuid)
   [:div "Run Option Notes"]
   [:div "Results"
    (matrix-tables ws-uuid)]
   [:div "Graphs"]
   [:div "Diagrams"]])
