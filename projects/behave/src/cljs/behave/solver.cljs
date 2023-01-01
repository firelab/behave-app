(ns behave.solver
  (:require [re-frame.core      :as rf]
            [clojure.string     :as str]
            [behave.lib.contain :as contain]
            [behave.lib.enums   :as enum]
            [behave.lib.units   :as units]))

(defn is-enum? [parameter-type]
  (or (str/includes? parameter-type "Enum")
      (str/includes? parameter-type "Units")))

(defn parsed-value [group-variable-uuid value]
  (let [[kind] @(rf/subscribe [:vms/query '[:find  [?kind]
                                            :in    $ ?gv-uuid
                                            :where [?gv :bp/uuid ?gv-uuid]
                                                   [?v :variable/group-variables ?gv]
                                                   [?v :variable/kind ?kind]]
                               group-variable-uuid])]
    (condp = kind
      "discrete"   (get enum/contain-tactic value)
      "continuous" (js/parseFloat value)
      "text"       value)))

(defn fn-params [function-id]
  (sort-by #(nth % 3) @(rf/subscribe [:vms/query '[:find ?p ?name ?type ?order
                                                   :keys [:db/id :name :type :order]
                                                   :in $ ?fn
                                                   :where [?fn :function/parameters ?p]
                                                          [?p :parameter/name ?name]
                                                          [?p :parameter/type ?type]
                                                          [?p :parameter/order ?order]]
                       function-id])))

(defn variable-units [group-variable-uuid]
  (first @(rf/subscribe [:vms/query '[:find  [?units]
                                      :in    $ ?gv-uuid
                                      :where [?gv :bp/uuid ?gv-uuid]
                                             [?v :variable/group-variables ?gv]
                                             [?v :variable/native-units ?units]]
                         group-variable-uuid])))

;; Cannot use pull due to the use of UUID's to join CPP ns/class/fns
(defn group-variable->fn [group-variable-uuid]
  @(rf/subscribe [:vms/query '[:find  [?fn ?fn-name]
                               :in    $ ?gv-uuid
                               :where [?gv :bp/uuid ?gv-uuid]
                                      [?gv :group-variable/cpp-function ?fn-uuid]
                                      [?fn :bp/uuid ?fn-uuid]
                                      [?fn :function/name ?fn-name]]
                 group-variable-uuid]))

;; Cannot use pull due to the use of UUID's to join CPP ns/class/fns/param
(defn parameter->group-variable [parameter-id]
  @(rf/subscribe [:vms/query '[:find  [?gv-uuid ...]
                               :in    $ ?p
                               :where [?p :bp/uuid ?p-uuid]
                                      [?gv :group-variable/cpp-parameter ?p-uuid]
                                      [?gv :bp/uuid ?gv-uuid]]
                  parameter-id]))

(defn- apply-single-cpp-fn [module-fns module gv-id value units]
  (let [[fn-id fn-name] (group-variable->fn gv-id)
        value           (parsed-value gv-id value)
        unit-enum       (units/get-unit units)
        f               ((symbol fn-name) module-fns)
        params          (fn-params fn-id)]
    (println "Input:" fn-name value unit-enum)
    (cond
      (nil? value)
      (js/console.error "Cannot process Contain Module with nil value for:" @(rf/subscribe [:vms/pull '[{:variable/_group-variables [:variable/name]}] gv-id]))

      (= 1 (count params))
      (f module value)

      (and (= 2 (count params)) (some? unit-enum))
      (let [[_ _ param-type] (first params)]
        (if (is-enum? param-type)
          (f module unit-enum value)
          (f module value unit-enum))))))

(defn- apply-multi-cpp-fn [module-fns module repeat-group]
  (let [[gv-id _]       (first repeat-group)
        [fn-id fn-name] (group-variable->fn gv-id)
        f               ((symbol fn-name) module-fns)
        ; Step 1 - Lookup parameters of function
        params          (fn-params fn-id)

        ; Step 2 - Match the parameters to group inputs/units
        fn-args (map-indexed (fn [idx [param-id _ param-type]]
                               (if (is-enum? param-type)
                                 (let [[param-id] (nth params (dec idx))
                                       [gv-uuid]  (parameter->group-variable param-id)]
                                   (units/get-unit (variable-units gv-id)))
                                 (let [[gv-uuid] (parameter->group-variable param-id)]
                                   (get repeat-group gv-uuid))))
                             params)]

    (println "Input:" fn-name fn-args)

    ; Step 3 - Call function with all parameters
    (apply f module fn-args)))

(defn apply-output-cpp-fn [module-fns module gv-id]
  (let [[fn-id fn-name] (group-variable->fn gv-id)
        unit            (units/get-unit (variable-units gv-id))
        f               ((symbol fn-name) module-fns)
        params          (fn-params fn-id)]
    (cond
      (empty? params)
      (f module)

      (and (= 1 (count params)) (some? unit))
      (f module unit)

      :else nil)))

(defn surface-solver [ws-uuid results]
  (assoc results :surface []))

(defn crown-solver [ws-uuid results]
  (assoc results :crown []))

(defn contain-solver [ws-uuid results]
  (let [inputs  @(rf/subscribe [:worksheet/all-inputs ws-uuid])
        outputs @(rf/subscribe [:worksheet/all-outputs ws-uuid])
        module  (contain/init)]

    (rf/dispatch [:worksheet/add-result-table-row ws-uuid 0])
    (println inputs outputs)

    (doseq [[_ repeats] inputs]
      (cond
        ; Single Group w/ Single Variable
        (and (= 1 (count repeats)) (count (vals repeats)))
        (let [[gv-id value] (ffirst (vals repeats))
              units         (or (variable-units gv-id) "")]
          (println "-- SINGLE VAR" gv-id value units)
          (rf/dispatch [:worksheet/add-result-table-header ws-uuid gv-id units])
          (rf/dispatch [:worksheet/add-result-table-cell ws-uuid gv-id value])
          (apply-single-cpp-fn (ns-publics 'behave.lib.contain) module gv-id value units))

        ; Multiple Groups w/ Single Variable
        (every? #(= 1 (count %)) (vals repeats))
        (doseq [[_ repeat-group] repeats]
          (let [[gv-id value] (first repeat-group)
                units         (or (variable-units gv-id) "")]
            (rf/dispatch [:worksheet/add-result-table-header ws-uuid gv-id units])
            (rf/dispatch [:worksheet/add-result-table-cell ws-uuid gv-id value])
            (apply-single-cpp-fn (ns-publics 'behave.lib.contain) module gv-id value units)))

        ; Multiple Groups w/ Multiple Variables
        :else
        (doseq [[_ repeat-group] repeats]
          (println "MULTI" repeat-group)
          (apply-multi-cpp-fn (ns-publics 'behave.lib.contain) module repeat-group))))

    ; Run
    (contain/doContainRun module)

    ; Get Outputs
    (doseq [group-variable-uuid outputs]
      (let [units  (variable-units group-variable-uuid) 
            result (apply-output-cpp-fn (ns-publics 'behave.lib.contain) module group-variable-uuid)]
        (rf/dispatch [:worksheet/add-result-table-header ws-uuid group-variable-uuid units])
        (rf/dispatch [:worksheet/add-result-table-cell ws-uuid group-variable-uuid result])))))

(defn mortality-solver [ws-uuid results]
  (assoc results :mortality []))

(defn solve-worksheet [ws-uuid]
  (let [modules (set @(rf/subscribe [:worksheet/modules ws-uuid]))
        results {}]

    (rf/dispatch [:worksheet/add-result-table ws-uuid])

    (println modules)
    (cond->> results
      (contains? modules :surface)
      (surface-solver ws-uuid)

      (contains? modules :crown)
      (crown-solver ws-uuid)

      (contains? modules :contain)
      (contain-solver ws-uuid)

      (contains? modules :mortality)
      (mortality-solver ws-uuid))))
