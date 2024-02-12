(ns behave.components.results.inputs
  (:require [behave.components.core :as c]
            [behave.print.subs]
            [behave.translate       :refer [<t]]
            [clojure.string         :as str]
            [goog.string            :as gstring]
            [re-frame.core          :refer [subscribe]]
            [re-frame.core :as rf]))

(defn- indent-name [level s]
  (str (apply str (repeat level "    ")) s))

(defn- csv? [s] (< 1 (count (str/split s #","))))

(defn- groups->row-entires
  "Returns a sequence of row entries of the form
  {:input (required)
   :unit  (optional)
   :value (optional)}"
  [ws-uuid formatters groups & [level]]
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
                                   (let [gv-uuid    (:bp/uuid (first variables))
                                         fmt-fn     (get formatters gv-uuid identity)
                                         fvar       (first variables)
                                         *unit-uuid (subscribe [:worksheet/input-units ws-uuid (:bp/uuid current-group) 0 gv-uuid])
                                         units      @(subscribe [:wizard/units-used-short-code
                                                                 (:variable/uuid fvar)
                                                                 @*unit-uuid
                                                                 (:variable/dimension-uuid fvar)
                                                                 (:variable/native-unit-uuid fvar)
                                                                 (:variable/english-unit-uuid fvar)
                                                                 (:variable/metric-unit-uuid fvar)])
                                         value      @(subscribe [:worksheet/input-value
                                                                 ws-uuid
                                                                 (:bp/uuid current-group)
                                                                 0 ;repeat-id
                                                                 gv-uuid])]
                                     (when (seq value)
                                       [{:input  (indent-name level (:group/name current-group)) ;Use group name instead of var name to match what is in the inputs UI
                                         :units  units
                                         :values (if (:group-variable/discrete-multiple? fvar)
                                                   (->> (str/split value ",")
                                                        (map fmt-fn)
                                                        (str/join ","))
                                                   (cond-> value
                                                     (not (csv? value))
                                                     fmt-fn))}]))
                                   multi-var?
                                   (into [{:input (indent-name level (:group/name current-group))}]
                                         (let [repeat-ids @(subscribe [:worksheet/group-repeat-ids ws-uuid (:bp/uuid current-group)])]
                                           (mapcat (fn [repeat-id]
                                                     (into [{:input (indent-name (inc level) (str (:group/name current-group) " " (inc repeat-id)))}]
                                                           (for [variable variables
                                                                 :let     [value @(subscribe [:worksheet/input-value
                                                                                              ws-uuid
                                                                                              (:bp/uuid current-group)
                                                                                              repeat-id
                                                                                              (:bp/uuid variable)])]
                                                                 :when    (seq value)]
                                                             (let [gv-uuid     (:bp/uuid (first variables))
                                                                   fmt-fn      (get formatters gv-uuid identity)
                                                                   *unit-uuid  (subscribe [:worksheet/input-units
                                                                                           ws-uuid
                                                                                           (:bp/uuid current-group)
                                                                                           repeat-id
                                                                                           (:bp/uuid variable)])
                                                                   *units-used (subscribe [:wizard/units-used-short-code
                                                                                           (:variable/uuid variable)
                                                                                           @*unit-uuid
                                                                                           (:variable/dimension-uuid variable)
                                                                                           (:variable/native-unit-uuid variable)
                                                                                           (:variable/english-unit-uuid variable)
                                                                                           (:variable/metric-unit-uuid variable)])]
                                                               {:input  (indent-name (+ level 2) (:variable/name variable))
                                                                :units  @*units-used
                                                                :values (if (:group-variable/discrete-multiple? variable)
                                                                          (->> (str/split value "")
                                                                               (map fmt-fn)
                                                                               (str/join ","))
                                                                          (cond-> value
                                                                            (not (csv? value))
                                                                            fmt-fn))}))))
                                                   repeat-ids)))
                                   :else
                                   [])
            children         (->> (:group/children current-group)
                                  (sort-by :group/order))
            next-indent      (if single-var?  (inc level) (+ level 2))
            children-entires (when (seq children)
                               (groups->row-entires ws-uuid formatters children next-indent))]
        (recur next-groups level (-> acc
                                     (into new-entries)
                                     (into children-entires))))

      next-groups
      (recur next-groups level acc)

      :else
      acc)))

(defn- build-rows [ws-uuid formatters submodules]
  (reduce (fn [acc submodule]
            (let [{id :db/id
                   op :submodule/conditionals-operator} submodule
                  show-module?                          @(subscribe [:wizard/show-submodule? ws-uuid id op])]
              (if show-module?
                (cond-> (conj acc {:input (:submodule/name submodule)})

                  (:submodule/groups submodule)
                  (into (groups->row-entires ws-uuid formatters (:submodule/groups submodule) 1)))
                acc)))
          []
          submodules))

(defn inputs-table [ws-uuid]
  (let [*worksheet (subscribe [:worksheet ws-uuid])
        modules    (:worksheet/modules @*worksheet)
        all-inputs @(subscribe [:worksheet/all-inputs-vector ws-uuid])
        formatters @(subscribe [:worksheet/result-table-formatters (map #(nth % 2) all-inputs)])]
    [:div.print__inputs_tables {:id "inputs"}
     (for [module-kw modules]
       (let [module-name (name module-kw)
             module      @(subscribe [:wizard/*module module-name])
             submodules  @(subscribe [:wizard/submodules-io-input-only (:db/id module)])]
         ^{:key module-kw}
         [:div.print__inputs-table
          (c/table {:title   (gstring/format "Inputs: %s"  @(<t (:module/translation-key module)))
                    :headers ["Input Variables" "Units" "Input Value(s)"]
                    :columns [:input :units :values]
                    :rows    (build-rows ws-uuid formatters submodules)})]))]))
