(ns behave.components.results.inputs.views
  (:require [behave.components.core :as c]
            [behave.print.subs]
            [behave.components.results.inputs.subs]
            [behave.translate       :refer [<t]]
            [clojure.string         :as str]
            [goog.string            :as gstring]
            [re-frame.core          :refer [subscribe]]))

(defn- indent-name [level s]
  (str (apply str (repeat level "    ")) s))

(defn- csv? [s] (< 1 (count (str/split s #","))))

(defn- groups->row-entires
  "Returns a sequence of row entries of the form
  {:input (required)
   :unit  (optional)
   :value (optional)}"
  [ws-uuid formatters gv-uuid->units groups & [level]]
  (loop [[current-group & next-groups] groups
         level                         level
         acc                           []]

    (cond
      current-group
      (let [variables        (->> current-group
                                  (:group/group-variables)
                                  (remove (fn [gv] (:group-variable/hide-result? gv)))
                                  (sort-by :group-variable/variable-order))
            single-var?      (= (count variables) 1)
            multi-var?       (> (count variables) 1)
            new-entries      (cond single-var?
                                   (let [gv-uuid      (:bp/uuid (first variables))
                                         list-eid     @(subscribe [:vms/gv-uuid->list-eid gv-uuid])
                                         fmt-fn       (get formatters gv-uuid identity)
                                         fvar         (first variables)
                                         units        (get gv-uuid->units gv-uuid)
                                         value        @(subscribe [:worksheet/input-value
                                                                   ws-uuid
                                                                   (:bp/uuid current-group)
                                                                   0 ;repeat-id
                                                                   gv-uuid])]
                                     (when (seq value)
                                       (if (:group-variable/discrete-multiple? fvar)
                                         (let [values (->> (str/split value ",")
                                                           (sort-by #(deref (subscribe [:worksheet/resolve-enum-order list-eid %])))
                                                           (map fmt-fn))]
                                           (into [{:input  (indent-name level @(subscribe [:result.inputs/resolve-group-name (:bp/uuid current-group)]))
                                                   :units  units
                                                   :values (first values)}]
                                                 (map (fn [value]
                                                        {:input  ""
                                                         :units  units
                                                         :values value})
                                                      (rest values))))
                                         [{:input  (indent-name level @(subscribe [:result.inputs/resolve-group-name (:bp/uuid current-group)]))
                                           :units  units
                                           :values (cond-> value
                                                     (not (csv? value))
                                                     fmt-fn)}])))
                                   multi-var?
                                   (into [{:input (indent-name level @(<t (:group/translation-key current-group)))}]
                                         (let [repeat-ids @(subscribe [:worksheet/group-repeat-ids ws-uuid (:bp/uuid current-group)])]
                                           (mapcat (fn [repeat-id]
                                                     (into [{:input (indent-name (inc level) (str @(<t (:group/translation-key current-group)) " " (inc repeat-id)))}]
                                                           (flatten
                                                            (for [variable (sort-by :group-variable/order variables)
                                                                  :let     [gv-uuid (:bp/uuid variable)
                                                                            list-eid     @(subscribe [:vms/gv-uuid->list-eid gv-uuid])
                                                                            value @(subscribe [:worksheet/input-value
                                                                                               ws-uuid
                                                                                               (:bp/uuid current-group)
                                                                                               repeat-id
                                                                                               (:bp/uuid variable)])]
                                                                  :when    (seq value)]
                                                              (let [fmt-fn        (get formatters gv-uuid identity)
                                                                    units-used    (get gv-uuid->units gv-uuid)
                                                                    variable-name @(subscribe [:wizard/gv-uuid->default-variable-name gv-uuid])]
                                                                (if (:group-variable/discrete-multiple? variable)
                                                                  (let [values (->> (str/split value ",")
                                                                                    (sort-by #(deref (subscribe [:worksheet/resolve-enum-order list-eid %])))
                                                                                    (map fmt-fn))]
                                                                    (into [{:input  (indent-name (+ level 2) variable-name)
                                                                            :units  units-used
                                                                            :values (first values)}]
                                                                          (map (fn [value]
                                                                                 {:input  ""
                                                                                  :units  units-used
                                                                                  :values value})
                                                                               (rest values))))
                                                                  [{:input  (indent-name (+ level 2) variable-name)
                                                                    :units  units-used
                                                                    :values (cond-> value
                                                                              (not (csv? value))
                                                                              fmt-fn)}]))))))
                                                   repeat-ids)))
                                   :else
                                   [])
            children         (->> (:group/children current-group)
                                  (sort-by :group/order))
            next-indent      (if single-var?  (inc level) (+ level 2))
            children-entires (when (seq children)
                               (groups->row-entires ws-uuid formatters gv-uuid->units children next-indent))]
        (recur next-groups level (-> acc
                                     (into new-entries)
                                     (into children-entires))))

      next-groups
      (recur next-groups level acc)

      :else
      acc)))

(defn- build-rows [ws-uuid formatters gv-uuid->units submodules]
  (reduce (fn [acc submodule]
            (cond-> (conj acc {:input (:submodule/name submodule)})

              (:submodule/groups submodule)
              (into (groups->row-entires ws-uuid
                                         formatters
                                         gv-uuid->units
                                         (:submodule/groups submodule)
                                         1))))
          []
          submodules))

(defn inputs-table [ws-uuid]
  (let [*worksheet     (subscribe [:worksheet ws-uuid])
        modules        (:worksheet/modules @*worksheet)
        all-inputs     @(subscribe [:worksheet/all-inputs-vector ws-uuid])
        formatters     @(subscribe [:result.inputs/table-formatters (map #(nth % 2) all-inputs)])
        gv-uuid->units @(subscribe [:worksheet/result-table-gv-uuid->units ws-uuid])]
    [:div.print__inputs_tables {:id "inputs"}
     (for [module-kw modules]
       (let [module-name (name module-kw)
             module      @(subscribe [:wizard/*module module-name])
             submodules  @(subscribe [:result.inputs/submodules ws-uuid (:db/id module)])]
         ^{:key module-kw}
         [:div.print__inputs-table
          (c/table {:title   (gstring/format "Inputs: %s"  @(<t (:module/translation-key module)))
                    :headers ["Input Variables" "Units" "Input Value(s)"]
                    :columns [:input :units :values]
                    :rows    (build-rows ws-uuid formatters gv-uuid->units submodules)})]))]))
