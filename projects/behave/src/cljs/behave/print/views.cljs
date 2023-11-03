(ns behave.print.views
  (:require [re-frame.core    :refer [subscribe dispatch]]
            [goog.string      :as gstring]
            [behave.components.core         :as c]
            [behave.print.subs]
            [behave.components.graph :refer [result-graph]]
            [behave.components.results.diagrams :refer [result-diagrams]]
            [behave.components.results.matrices :refer [result-matrices]]
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
  (dispatch [:dev/close-after-print])
  (js/setTimeout #(dispatch [:dev/print]) 1000)
  (let [worksheet       @(subscribe [:worksheet ws-uuid])
        ws-name         (:worksheet/name worksheet)
        ws-date-created (:worksheet/created worksheet)
        notes           @(subscribe [:wizard/notes ws-uuid])
        graph-data      @(subscribe [:worksheet/result-table-cell-data ws-uuid])]
    [:div.print
     [:div.print__ws-name ws-name]
     [:div.print__ws-date (epoch->date-string ws-date-created)]
     [:div.wizard-print__header "Inputs"]
     [inputs-table ws-uuid]
     [wizard-notes notes]
     [:div.wizard-print__header "Results"]
     [result-matrices ws-uuid]
     [result-graph ws-uuid graph-data]
     [result-diagrams ws-uuid]]))
