(ns behave.print.views
  (:require [re-frame.core    :refer [subscribe]]
            [goog.string      :as gstring]
            [behave.components.core         :as c]
            [behave.print.subs]
            [clojure.string :as str]
            [behave.components.graph :refer [result-graph]]
            [behave.wizard.views :refer [wizard-diagrams]]
            [behave.units-conversion        :refer [to-map-units]]
            [behave.translate :refer [<t bp]]
            [re-frame.core :as rf]))

(defn- indent-name [level s]
  (str (apply str (repeat level "    ")) s))

(defn- procces-map-units?
  [uuid]
  (let [show-map-units?     @(subscribe [:wizard/show-map-units?])
        map-units-variables @(subscribe [:wizard/map-unit-convertible-variables])]
    (and show-map-units? (get map-units-variables uuid))))

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
      (and current-group @(subscribe [:wizard/show-group?
                                      ws-uuid
                                      (:db/id current-group)
                                      (:group/conditionals-operator current-group)]))
      (let [variables        (->> current-group (:group/group-variables) (sort-by :group-variable/variable-order))
            single-var?      (= (count variables) 1)
            multi-var?       (> (count variables) 1)
            new-entries      (cond single-var?
                                   [{:input  (indent-name level (:group/name current-group)) ;Use group name instead of var name to match what is in the inputs UI
                                     :units  (:variable/native-units (first variables))
                                     :values @(subscribe [:worksheet/input-value
                                                          ws-uuid
                                                          (:bp/uuid current-group)
                                                          0 ;repeat-id
                                                          (:bp/uuid (first variables))])}]
                                   multi-var?
                                   (into [{:input (indent-name level (:group/name current-group))}]
                                         (let [repeat-ids @(subscribe [:worksheet/group-repeat-ids ws-uuid (:bp/uuid current-group)])]
                                           (mapcat (fn [repeat-id]
                                                     (into [{:input (indent-name (inc level) (str (:group/name current-group) " " (inc repeat-id)))}]
                                                           (map (fn [variable]
                                                                  {:input  (indent-name (+ level 2) (:variable/name variable))
                                                                   :units  (:variable/native-units variable)
                                                                   :values @(subscribe [:worksheet/input-value
                                                                                        ws-uuid
                                                                                        (:bp/uuid current-group)
                                                                                        repeat-id
                                                                                        (:bp/uuid variable)])})
                                                                variables)))
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
                  show-module?                          @(subscribe [:wizard/show-submodule? ws-uuid id op])]
              (if show-module?
                (cond-> (conj acc {:input (:submodule/name submodule)})

                  (:submodule/groups submodule)
                  (into (groups->row-entires ws-uuid (:submodule/groups submodule) 1)))
                acc)))
          []
          submodules))

(defn- inputs-table [ws-uuid]
  (let [*worksheet (subscribe [:worksheet ws-uuid])
        modules    (:worksheet/modules @*worksheet)]
    [:div.print__inputs_tables
     (for [module-kw modules]
       (let [module-name (name module-kw)
             module      @(subscribe [:wizard/*module module-name])
             submodules  @(subscribe [:wizard/submodules-io-input-only (:db/id module)])]
         ^{:key module-kw}
         [:div.print__inputs-table
          (c/table {:title   (gstring/format "Inputs: %s"  @(<t (:module/translation-key module)))
                    :headers ["Input Variables" "Units" "Input Value(s)"]
                    :columns [:input :units :values]
                    :rows    (build-rows ws-uuid submodules)})]))]))

(defmulti result-tables (fn [_ws-uuid multi-valued-inputs] (count multi-valued-inputs)))

(defmethod result-tables 0
  [ws-uuid _multi-valued-inputs]
  (let [output-uuids @(subscribe [:worksheet/all-output-uuids ws-uuid])
        map-units    @(subscribe [:worksheet/get-map-units-settings-units ws-uuid])
        map-rep-frac @(subscribe [:worksheet/get-map-units-settings-map-rep-fraction ws-uuid])
        rows         (reduce (fn [acc output-uuid]
                               (let [{var-name  :variable/name
                                      var-units :variable/native-units} @(subscribe [:wizard/group-variable output-uuid])
                                     value                              @(subscribe [:worksheet/first-row-results-gv-uuid->value
                                                                                     ws-uuid
                                                                                     output-uuid])]
                                 (cond-> acc
                                   :always (conj {:output var-name
                                                  :value  value
                                                  :units  var-units})

                                   (procces-map-units? output-uuid)
                                   (conj {:output (gstring/format "%s Map Units" var-name)
                                          :value  (to-map-units value
                                                                var-units
                                                                map-units
                                                                map-rep-frac)
                                          :units  map-units}))))
                             []
                             output-uuids)]
    [:div.print__result-table
     (c/table {:title   "Results"
               :headers ["Output Variable" "Value" "Units"]
               :columns [:output :value :units]
               :rows    rows})]))

(defmethod result-tables 1
  [ws-uuid multi-valued-inputs]
  (let [[v-name v-units gv-uuid values] (first multi-valued-inputs)
        output-uuids                    @(subscribe [:print/matrix-table-column-outputs ws-uuid])
        matrix-data                     @(subscribe [:worksheet/matrix-table-data-single-multi-valued-input
                                                     ws-uuid
                                                     gv-uuid
                                                     (str/split values ",")
                                                     (map last output-uuids)])
        map-units                       @(subscribe [:worksheet/get-map-units-settings-units ws-uuid])
        map-rep-frac                    @(subscribe [:worksheet/get-map-units-settings-map-rep-fraction ws-uuid])
        column-headers                  (reduce (fn insert-map-units-columns [acc [col-name units gv-uuid]]
                                                  (cond-> acc
                                                    :always (conj {:name (gstring/format "%s (%s)" col-name units)
                                                                   :key  gv-uuid})

                                                    (procces-map-units? gv-uuid)
                                                    (conj {:name (gstring/format "%s Map Units (%s)" col-name map-units)
                                                           :key  (str gv-uuid "-map-units")})))
                                                []
                                                output-uuids)
        row-headers (map (fn [value] {:name value :key (str value)}) (str/split values ","))
        final-data  (reduce (fn insert-map-units-values [acc [[i j] value]]
                              (cond-> acc
                                (procces-map-units? j)
                                (assoc [i (str j "-map-units")]
                                       (to-map-units value
                                                     @(rf/subscribe [:wizard/gv-uuid->variable-units j])
                                                     map-units
                                                     map-rep-frac))))
                            matrix-data
                            matrix-data)]
    [:div.print__result-table
     (c/matrix-table {:title          "Results"
                      :rows-label     (gstring/format "%s (%s)" v-name v-units)
                      :cols-label     "Outputs"
                      :column-headers column-headers
                      :row-headers    row-headers
                      :data           final-data})]))

(defmethod result-tables 2
  [ws-uuid multi-valued-inputs]
  (let [[row-name row-units row-gv-uuid row-values] (first multi-valued-inputs)
        [col-name col-units col-gv-uuid col-values] (second multi-valued-inputs)
        output-uuids                                @(subscribe [:worksheet/all-output-uuids ws-uuid])
        map-units                                   @(subscribe [:worksheet/get-map-units-settings-units ws-uuid])
        map-rep-frac                                @(subscribe [:worksheet/get-map-units-settings-map-rep-fraction ws-uuid])]
    [:div.print__result-tables
     (for [output-uuid output-uuids]
       (let [{output-uuid  :bp/uuid
              output-name  :variable/name
              output-units :variable/native-units} @(subscribe [:wizard/group-variable output-uuid])
             matrix-data                           @(subscribe [:print/matrix-table-two-multi-valued-inputs ws-uuid
                                                                row-gv-uuid
                                                                (str/split row-values ",")
                                                                col-gv-uuid
                                                                (str/split col-values ",")
                                                                output-uuid])
             row-headers                           (map (fn [value] {:name value :key value})
                                                        (str/split row-values ","))
             column-headers                        (map (fn [value] {:name value :key value})
                                                        (str/split col-values ","))]
         [:<>
          [:div.print__result-table
           (c/matrix-table {:title          (gstring/format "%s (%s)" output-name output-units)
                            :rows-label     (gstring/format "%s (%s)" row-name row-units)
                            :cols-label     (gstring/format "%s (%s)" col-name col-units)
                            :row-headers    row-headers
                            :column-headers column-headers
                            :data           matrix-data})]
          (when (procces-map-units? output-uuid)
            [:div.print__result-table
             (let [data (reduce-kv (fn [acc [i j] value]
                                     (assoc acc [i j] (to-map-units value
                                                                    output-units
                                                                    map-units
                                                                    map-rep-frac)))
                                   matrix-data
                                   matrix-data)]
               (c/matrix-table {:title          (gstring/format "%s Map Units (%s)" output-name map-units)
                                :rows-label     (gstring/format "%s (%s)" row-name row-units)
                                :cols-label     (gstring/format "%s (%s)" col-name col-units)
                                :row-headers    row-headers
                                :column-headers column-headers
                                :data           data}))])]))]))

(defn- wizard-notes [notes]
  (when (seq notes)
    [:div.wizard-notes
     [:div.wizard-print__header "Run's Notes"]
     (doall (for [[id & _rest :as note] notes]
              ^{:key id}
              (let [[_note-id note-name note-content] note]
                [:div.wizard-note
                 [:div.wizard-note__name note-name]
                 [:div.wizard-note__content note-content]])))]))

(defn- epoch->date-string [epoch]
  (.toString (js/Date. epoch)))

(defn print-page [{:keys [ws-uuid]}]
  (let [worksheet           @(subscribe [:worksheet ws-uuid])
        ws-name             (:worksheet/name worksheet)
        ws-date-created     (:worksheet/created worksheet)
        multi-valued-inputs @(subscribe [:print/matrix-table-multi-valued-inputs ws-uuid])
        notes               @(subscribe [:wizard/notes ws-uuid])
        graph-data          @(subscribe [:worksheet/result-table-cell-data ws-uuid])]
    [:div.print
     [:div.print__ws-name ws-name]
     [:div.print__ws-date (epoch->date-string ws-date-created)]
     [:div.wizard-print__header "Inputs"]
     [inputs-table ws-uuid]
     [wizard-notes notes]
     [:div.wizard-print__header "Results"]
     [result-tables ws-uuid multi-valued-inputs]
     [result-graph ws-uuid graph-data]
     [wizard-diagrams ws-uuid]]))
