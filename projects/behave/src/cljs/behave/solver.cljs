(ns behave.solver
  (:require [re-frame.core      :as rf]
            [datascript.core :as d]
            [clojure.string     :as str]
            [behave.lib.contain :as contain]
            [behave.lib.enums   :as enum]
            [behave.lib.units   :as units]
            [behave.vms.store :refer [vms-conn]]
            [map-utils.interface :refer [index-by]]))

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

(defn separate-multi-values [ws-uuid]
  (mapv (fn [[uuid values]]
          (map (fn [s] [uuid (str/trim s)]) (str/split values #",")))
        @(rf/subscribe [:worksheet/multi-value-input-uuids+values ws-uuid])))

(defn inputs+units+fn+params [module-name]
  (q-vms '[:find ?uuid ?units ?fn ?fn-name (count ?all-params) ?p-name ?p-type ?p-order
           :keys
           group-variable/uuid
           group-variable/units
           function/id
           function/name
           function/num-params
           param/name
           param/type
           param/order

           :in ?module-name

           :where
           [?m :module/name ?module-name]
           (module-input-vars ?m ?gv)
           (lookup ?uuid ?gv)
           (var->fn ?uuid ?fn)
           (var-units ?uuid ?units)
           (var->param ?uuid ?p)
           [?fn :function/name ?fn-name]
           (param-attrs ?p ?p-name ?p-type ?p-order)
           [?fn :function/parameters ?all-params]]
         module-name))

(defn- permutations [single-inputs range-inputs]
  (case (count range-inputs)
    0 single-inputs
    1 (for [x (first range-inputs)]
        (conj single-inputs x))
    2 (for [x (first range-inputs)
            y (second range-inputs)]
        (conj single-inputs x y))
    3 (for [x (first range-inputs)
            y (second range-inputs)
            z (nth range-inputs 2)]
        (conj single-inputs x y z))))

(defn- csv? [s] (< 1 (count (str/split s #","))))

(defn- ->run-plan [inputs]
  (reduce (fn [acc [group-uuid repeat-id group-var-uuid value]]
            (assoc-in acc [group-uuid repeat-id group-var-uuid] value))
          {}
          inputs))

(defn- generate-runs [all-inputs-vector]
  (let [single-inputs          (remove #(-> % (last) (csv?)) all-inputs-vector)
        range-inputs           (filter #(-> % (last) (csv?)) all-inputs-vector)
        separated-range-inputs (map #(let [result (vec (butlast %))
                                           values (map str/trim (str/split  (last %) #","))]
                                       (mapv (fn [v] (conj result v)) values)) range-inputs)]

    (map ->run-plan (permutations (vec single-inputs) separated-range-inputs))))

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
  (q-vms '[:find  [?fn ?fn-name (count ?p)]
           :in    ?gv-uuid
           :where (var->fn ?gv-uuid ?fn)
                  [?fn :function/name ?fn-name]
                  [?fn :function/parameters ?p]]
         group-variable-uuid))

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

    (println [:SOLVER] [:MULTI-INPUT fn-name params fn-args])

  ; Step 3 - Call function with all parameters
    (apply f module fn-args)))

(defn- apply-output-cpp-fn
  [module-fns module gv-id]
  (let [[fn-id fn-name] (group-variable->fn gv-id)
        unit            (units/get-unit (variable-units gv-id))
        f               ((symbol fn-name) module-fns)
        params          (fn-params fn-id)]

    (println [:SOLVER] [:OUTPUT fn-name unit f params])

    (cond
      (empty? params)
      (f module)

      (and (= 1 (count params)) (some? unit))
      (f module unit)

      :else nil)))

(defn- apply-inputs [module fns inputs]
  (doseq [[_ repeats] inputs]
    (cond
      ;; Single Group w/ Single Variable
      (and (= 1 (count repeats) (count (vals repeats))))
      (let [[gv-id value] (ffirst (vals repeats))
            units         (or (variable-units gv-id) "")]
        (println "-- [SOLVER] SINGLE VAR" gv-id value units)
        (apply-single-cpp-fn fns module gv-id value units))

      ;; Multiple Groups w/ Single Variable
      (every? #(= 1 (count %)) (vals repeats))
      (doseq [[_ repeat-group] repeats]
        (let [[gv-id value] (first repeat-group)
              units         (or (variable-units gv-id) "")]
          (println "-- [SOLVER] SINGLE VAR (MULTIPLE)" gv-id value units)
          (apply-single-cpp-fn fns module gv-id value units)))

      ;; Multiple Groups w/ Multiple Variables
      :else
      (doseq [[_ repeat-group] repeats]
        (println "-- [SOLVER] MULTI" repeat-group)
        (apply-multi-cpp-fn fns module repeat-group)))))

(defn- get-outputs [module fns outputs]
  (reduce
   (fn [acc group-variable-uuid]
     (let [units  (variable-units group-variable-uuid)
           result (str (apply-output-cpp-fn fns module group-variable-uuid))]
       (println [:GET-OUTPUTS group-variable-uuid result units])
       (assoc acc group-variable-uuid [result units])))
   {}
   outputs))

(defn surface-solver [ws-uuid results]
  (assoc results :surface []))

(defn crown-solver [ws-uuid results]
  (assoc results :crown []))

(defn contain-solver [ws-uuid results]
  (let [all-inputs @(rf/subscribe [:worksheet/all-inputs-vector ws-uuid])
        outputs    @(rf/subscribe [:worksheet/all-output-uuids ws-uuid])
        fns        (ns-publics 'behave.lib.contain)
        counter    (atom 0)]

    (->> (generate-runs all-inputs)
         (reduce (fn [acc inputs]
                   (let [row    @counter
                         module (contain/init)]

                     ;; Increase counter
                     (swap! counter inc)

                     ;; Apply Inputs
                     (apply-inputs module fns inputs)

                     ;; Run
                     (contain/doContainRun module)

                     ;; Get Outputs
                     (conj acc
                           {:row     @counter
                            :inputs  inputs
                            :outputs (get-outputs module fns outputs)})))
                 '())
         (reverse)
         (assoc results :contain))))

(comment

  (def ws-uuid "640bea87-9cc6-4de2-b44b-30bb9cc9f552")

  (contain-solver ws-uuid {})

  )


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
