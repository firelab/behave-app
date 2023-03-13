(ns behave.solver
  (:require [re-frame.core      :as rf]
            [datascript.core :as d]
            [clojure.string     :as str]
            [behave.lib.contain :as contain]
            [behave.lib.enums   :as enum]
            [behave.lib.units   :as units]
            [behave.vms.store :refer [vms-conn]]))

;; VMS Data Queries

(def rules '[;; Lookup by UUID
             [(lookup ?uuid ?e) [?e :bp/uuid ?uuid]]

             ;; Lookup another entity by a shared UUID
             [(ref ?uuid1 ?attr ?e2)
              (lookup ?uuid1 ?e1)
              [?e1 ?rel ?uuid2]
              (lookup ?uuid2 ?e2)]

             ;; Find a group variable's variable
             [(gv->var ?uuid ?v)
              (lookup ?uuid ?gv)
              [?v :variable/group-variables ?gv]]

             ;; Find a variable's units
             [(var-units ?uuid ?units)
              (gv->var ?uuid ?v)
              [?v :variable/native-units ?units]]

             ;; Find a variable's kind
             [(kind ?uuid ?kind)
              (gv->var ?uuid ?v)
              [?v :variable/kind ?kind]]

             ;; Find a group variable's function
             [(var->fn ?uuid ?fn)
              (ref ?uuid :group-variable/cpp-function ?fn)]

             ;; Find a group variable's parameter
             [(var->param ?uuid ?p)
              (ref ?uuid :group-variable/cpp-parameter ?p)]

             [(param-attrs ?p ?p-name ?p-type ?p-order)
              [?p :parameter/name ?p-name]
              [?p :parameter/type ?p-type]
              [?p :parameter/order ?p-order]]

             ;; Find the function's parameters
             [(fn-params ?fn ?p ?p-name ?p-type ?p-order)
              [?fn :function/parameters ?p]
              (param-attrs ?p ?p-name ?p-type ?p-order)]

             [(subgroup ?g ?sg) [?g :group/children ?sg]]

             [(module-output-vars ?m ?gv)
              [?m :module/submodules ?s]
              [?s :submodule/io :output]
              [?s :submodule/groups ?g]
              [?g :group/group-variables ?gv]]

             [(module-output-fns ?m ?fn ?fn-name)
              (module-output-vars ?m ?gv)
              (lookup ?uuid ?gv)
              (var->fn ?uuid ?fn)
              [?fn :function/name ?fn-name]]

             [(module-input-vars ?m ?gv)
              [?m :module/submodules ?s]
              [?s :submodule/io :input]
              [?s :submodule/groups ?g]
              [?g :group/group-variables ?gv]]

             [(module-input-fns ?m ?fn ?fn-name)
              (module-input-vars ?m ?gv)
              (lookup ?uuid ?gv)
              (var->fn ?uuid ?fn)
              [?fn :function/name ?fn-name]]])

;; Helpers

(defn q-vms [query & args]
  (let [[find in+where] (split-with (complement #{:in :where}) query)
        [in where]      (split-with (complement #{:where}) in+where)
        query-after     (vec (concat find '(:in $ %) (rest in) where))]
    (apply d/q query-after @@vms-conn rules args)))

(comment

  (q-vms '[:find ?fn ?fn-name (count ?p)
           :where [?m :module/name "Contain"]
           (module-input-fns ?m ?fn ?fn-name)
           [?fn :function/parameters ?p]])

  (def input-uuid+fn+params
    (q-vms '[:find ?uuid ?fn ?fn-name (count ?all-params) ?p-name ?p-type ?p-order
             :keys group-var-uuid fn fn-name total-params param-name param-type param-order
             :where [?m :module/name "Contain"]
             (module-input-vars ?m ?gv)
             (lookup ?uuid ?gv)
             (var->fn ?uuid ?fn)
             (var->param ?uuid ?p)
             [?fn :function/name ?fn-name]
             (param-attrs ?p ?p-name ?p-type ?p-order)
             [?fn :function/parameters ?all-params]]))

  (def ws-uuid @(rf/subscribe [:worksheet/latest])))

(defn- is-enum? [parameter-type]
  (or (str/includes? parameter-type "Enum")
      (str/includes? parameter-type "Units")))

(defn- parsed-value [group-variable-uuid value]
  (let [[kind] (q-vms '[:find  [?kind]
                        :in    ?gv-uuid
                        :where (kind ?gv-uuid ?kind)]
                    group-variable-uuid)]
    (condp = kind
      "discrete"   (get enum/contain-tactic value)
      "continuous" (js/parseFloat value)
      "text"       value)))

(defn- fn-params [function-id]
  (->> (q-vms '[:find ?p ?p-name ?p-type ?p-order
                :in ?fn
                :where (fn-params ?fn ?p ?p-name ?p-type ?p-order)]
              function-id)

       (sort-by #(nth % 3))))

(defn- variable-units [group-variable-uuid]
  (first (q-vms '[:find  [?units]
                  :in    ?gv-uuid
                  :where (var-units ?gv-uuid ?units)]
                group-variable-uuid)))

;; Cannot use pull due to the use of UUID's to join CPP ns/class/fns
(defn- group-variable->fn [group-variable-uuid]
  (or (q-vms '[:find  [?fn ?fn-name (count ?p)]
               :in    ?gv-uuid
               :where (var->fn ?gv-uuid ?fn)
                      [?fn :function/name ?fn-name]
                      [?fn :function/parameters ?p]]
             group-variable-uuid)
      (-> (q-vms '[:find  [?fn ?fn-name]
                   :in    ?gv-uuid
                   :where (var->fn ?gv-uuid ?fn)
                          [?fn :function/name ?fn-name]]
                 group-variable-uuid)
          (conj 0))))

;; Cannot use pull due to the use of UUID's to join CPP ns/class/fns/param
(defn- parameter->group-variable [parameter-id]
  (q-vms '[:find  [?gv-uuid ...]
           :in    ?p
           :where (ref ?p-uuid :group-variable/cpp-parameter ?gv)]
         parameter-id))

(defn- apply-single-cpp-fn [module-fns module gv-id value units]
  (let [[fn-id fn-name] (group-variable->fn gv-id)
        value           (parsed-value gv-id value)
        unit-enum       (units/get-unit units)
        f               ((symbol fn-name) module-fns)
        params          (fn-params fn-id)]
     (println "Input:" fn-name value unit-enum)

    (cond
      (nil? value)
      (js/console.error "Cannot process Contain Module with nil value for:"
                        (d/pull @@vms-conn '[{:variable/_group-variables [:variable/name]}] gv-id))

      (= 1 (count params))
      (f module value)

      (and (= 2 (count params)) (some? unit-enum))
      (let [[_ _ param-type] (first params)]
        (if (is-enum? param-type)
          (f module unit-enum value)
          (f module value unit-enum))))))

(defn- apply-multi-cpp-fn
  [module-fns module repeat-group]
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

    (println "MULTI INPUT:" fn-name fn-args)

    (tap> [:MULTI-INPUT fn-name params fn-args])

  ; Step 3 - Call function with all parameters
    (apply f module fn-args)))

(defn- apply-output-cpp-fn
  [module-fns module gv-id]
  (let [[fn-id fn-name] (group-variable->fn gv-id)
        unit            (units/get-unit (variable-units gv-id))
        f               ((symbol fn-name) module-fns)
        params          (fn-params fn-id)]

    (tap> [:OUTPUT fn-name unit f params])

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
        outputs @(rf/subscribe [:worksheet/all-output-uuids ws-uuid])
        module  (contain/init)]

    (rf/dispatch [:worksheet/add-result-table-row ws-uuid 0])
    (println inputs outputs)

    (doseq [[_ repeats] inputs]
      (cond
        ; Single Group w/ Single Variable
        (and (= 1 (count repeats) (count (vals repeats))))
        (let [[gv-id value] (ffirst (vals repeats))
              units         (or (variable-units gv-id) "")]
          (println "-- SINGLE VAR" gv-id value units)
          (rf/dispatch [:worksheet/add-result-table-header ws-uuid gv-id units])
          (rf/dispatch [:worksheet/add-result-table-cell ws-uuid 0 gv-id value])
          (apply-single-cpp-fn (ns-publics 'behave.lib.contain) module gv-id value units))

        ; Multiple Groups w/ Single Variable
        (every? #(= 1 (count %)) (vals repeats))
        (doseq [[_ repeat-group] repeats]
          (let [[gv-id value] (first repeat-group)
                units         (or (variable-units gv-id) "")]
            (rf/dispatch [:worksheet/add-result-table-header ws-uuid gv-id units])
            (rf/dispatch [:worksheet/add-result-table-cell ws-uuid 0 gv-id value])
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
      (let [units  (or (variable-units group-variable-uuid) "")
            result (str (apply-output-cpp-fn (ns-publics 'behave.lib.contain) module group-variable-uuid))]
        (rf/dispatch [:worksheet/add-result-table-header ws-uuid group-variable-uuid units])
        (rf/dispatch [:worksheet/add-result-table-cell ws-uuid 0 group-variable-uuid result])))))

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

(comment

  (require '[portal.web :as p])
  (p/open) ; Open portal
  (add-tap #'p/submit) ; Add portal as a tap> target
  (p/clear)
  (tap> :hello)

  (def ws-uuid @(rf/subscribe [:worksheet/latest]))
  (rf/dispatch [:worksheet/solve ws-uuid])

;; First: 

  (doseq [[_ repeats] @(rf/subscribe [:worksheet/all-inputs ws-uuid])]
    (doseq [[_repeat-id variables] repeats]
      (let [var-count (count variables)]
        (if (= 1 var-count)
          (tap> [:SINGLE variables])
          (tap> [:MULTI variables])))))

;;; New Solver
;; 1. Find functions and parameters

;; 2. Create run 'template' from funcs / params
;; 3. Split inputs (single, range, multi)
;; 4. Build runs from Create run template
;; 5. Execute runs
;; 6. Store values in DS

  (parsed-value "29dbe7d2-bb05-4744-8624-034636b31cfb" "3.0")
  (group-variable->fn "29dbe7d2-bb05-4744-8624-034636b31cfb")
  (variable-units "29dbe7d2-bb05-4744-8624-034636b31cfb")

  )




