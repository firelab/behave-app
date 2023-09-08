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
            children         (sort-by :group/order (:group/children current-group))
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

(defn print-page [{:keys [ws-uuid]}]
  (let [*worksheet (rf/subscribe [:worksheet ws-uuid])
        modules    (:worksheet/modules @*worksheet)]
    [:div.print
     (for [module-kw modules]
       (let [module-name (name module-kw)
             module      @(rf/subscribe [:wizard/*module module-name])
             submodules  @(rf/subscribe [:wizard/submodules-io-input-only (:db/id module)])]
         [:div.print__inputs-table
          (c/table {:title   (gstring/format "Inputs: %s"  @(<t (:module/translation-key module)))
                    :headers ["Input Variables" "Units" "Input Value(s)"]
                    :columns [:input :units :values]
                    :rows    (build-rows ws-uuid submodules)})]))
     [:div "Run Option Notes"]
     [:div "Tables"]
     [:div "Graphs"]
     [:div "Diagrams"]]))
