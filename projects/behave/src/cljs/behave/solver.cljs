(ns behave.solver
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [behave.lib.core       :as lib]
            [behave.lib.contain    :as contain]
            [behave.lib.enums      :as enum]
            [behave.lib.units      :as units]
            [datascript.core       :as d]
            [datom-utils.interface :as du]))

(defn is-enum? [parameter-type]
  (or (str/includes? parameter-type "Enum")
      (str/includes? parameter-type "Units")))

(defn discrete? [group-variable-id]
  (let [[kind] @(rf/subscribe [:query '[:find  [?kind]
                                       :in    $ ?gv
                                       :where [?v :variable/group-variables ?gv]
                                              [?v :variable/kind ?kind]]
                              [group-variable-id]])]
    (= kind "discrete")))

(defn fn-params [function-id]
  (sort-by #(nth % 3) @(rf/subscribe [:query '[:find ?p ?name ?type ?order
                                :keys [:db/id :name :type :order]
                                :in $ ?fn
                                :where [?fn :function/parameters ?p]
                                       [?p :parameter/name ?name]
                                       [?p :parameter/type ?type]
                                       [?p :parameter/order ?order]]
                       [function-id]])))

(defn variable-units [group-variable-id]
  (let [unit-short @(rf/subscribe [:query '[:find  [?units]
                                           :in    $ ?gv
                                           :where [?v :variable/group-variables ?gv]
                                                  [?v :variable/native-units ?units]]
                                  [group-variable-id]])]
    (units/get-unit (first unit-short))))

;; Cannot use pull due to the use of UUID's to join CPP ns/class/fns
(defn group-variable->fn [group-variable-id]
  @(rf/subscribe [:query '[:find  [?fn ?fn-name]
                          :in    $ ?gv
                          :where [?gv :group-variable/cpp-function ?fn-uuid]
                                 [?fn :bp/uuid ?fn-uuid]
                                 [?fn :function/name ?fn-name]]
                 [group-variable-id]]))

;; Cannot use pull due to the use of UUID's to join CPP ns/class/fns/param
(defn group-variable->parameter [group-variable-id]
  @(rf/subscribe [:query '[:find  [?p ?p-name]
                          :in    $ ?gv
                          :where [?gv :group-variable/cpp-parameter ?p-uuid]
                                 [?p :bp/uuid ?p-uuid]
                                 [?p :parameter/name ?p-name]
                                 [?p :parameter/type ?p-type]
                                 [?p :parameter/order ?p-order]]
                 [group-variable-id]]))

;; Cannot use pull due to the use of UUID's to join CPP ns/class/fns/param
(defn parameter->group-variable [parameter-id]
  @(rf/subscribe [:query '[:find  [?gv ...]
                           :in    $ ?p
                           :where [?p :bp/uuid ?p-uuid]
                           [?gv :group-variable/cpp-parameter ?p-uuid]]
                  [parameter-id]]))

(defn module-variables [module-name io]
  @(rf/subscribe [:query '[:find [?gv ...]
                          :in $ ?module-name ?io
                          :where [?e :module/name ?module-name]
                          [?e :module/submodules ?s]
                          [?s :submodule/io ?io]
                          [?s :submodule/groups ?g]
                          [?g :group/group-variables ?gv]]
                 [module-name io]]))

(defn all-input-variables [module-name]
  (module-variables module-name :input))

(defn all-output-variables [module-name]
  (module-variables module-name :output))

(defn surface-solver [worksheet]
  (assoc-in worksheet [:results :surface] []))

(defn crown-solver [worksheet]
  (assoc-in worksheet [:results :crown] []))

(defn- apply-single-cpp-fn [module-fns module gv-id value]
  (let [[fn-id fn-name] (group-variable->fn gv-id)
        value  (if (discrete? gv-id) (get enum/contain-tactic value) value)
        unit   (variable-units gv-id)
        f      (get module-fns fn-name)
        _      (println fn-name)
        params (fn-params fn-id)]
    (println "Input:" fn-name value unit)
    (cond
      (nil? value)
      (js/console.error "Cannot process Contain Module with nil value for:" @(rf/subscribe [:pull '[{:variable/_group-variables [:variable/name]}] gv-id]))

      (= 1 (count params))
      (f module value)

      (and (= 2 (count params)) (some? unit))
      (let [[_ _ param-type] (first params)]
        (if (is-enum? param-type)
          (f module unit value)
          (f module value unit))))))

(defn- apply-multi-cpp-fn [module-fns module repeat-group]
  (let [[gv-id _]       (first repeat-group)
        [fn-id fn-name] (group-variable->fn gv-id)
        f               (get module-fns fn-name)
        ; Step 1 - Lookup parameters of function
        params          (fn-params fn-id)

        ; Step 2 - Match the parameters to group inputs/units
        fn-args (map-indexed (fn [idx [param-id _ param-type]]
                               (if (is-enum? param-type)
                                 (let [[param-id] (nth params (dec idx))
                                       [gv-id]    (parameter->group-variable param-id)]
                                   (variable-units gv-id))
                                 (let [[gv-id] (parameter->group-variable param-id)]
                                   (get repeat-group gv-id))))
                             params)]

    (println "Input:" fn-name fn-args)

    ; Step 3 - Call function with all parameters
    (apply f module fn-args)))

(defn apply-output-cpp-fn [module-fns module gv-id]
  (let [[fn-id fn-name] (group-variable->fn gv-id)
        unit            (variable-units gv-id)
        f               (get module-fns fn-name)
        params          (fn-params fn-id)]
    (println "Output:" fn-name unit)
    (cond
      (empty? params)
      (f module)

      (and (= 1 (count params)) (some? unit))
      (f module unit)

      :else nil)))

(defn contain-solver [worksheet]
  (let [{:keys [inputs outputs]} worksheet
        module (contain/init)]

    (doseq [[_ repeats] inputs]
      (cond
        ; Single Group w/ Single Variable
        (and (= 1 (count repeats)) (count (vals repeats)))
        (let [[gv-id value] (ffirst (vals repeats))]
          (apply-single-cpp-fn contain/fns module gv-id value))

        ; Multiple Groups w/ Single Variable
        (every? #(= 1 (count %)) (vals repeats))
        (doseq [[_ repeat-group] repeats]
          (let [[gv-id value] (first repeat-group)]
            (apply-single-cpp-fn contain/fns module gv-id value)))

        ; Multiple Groups w/ Multiple Variables
        :else
        (doseq [[_ repeat-group] repeats]
          (println "MULTI" repeat-group)
          (apply-multi-cpp-fn contain/fns module repeat-group))))

    ; Run
    (contain/doContainRun module)

    ; Get Outputs
    (assoc-in worksheet
              [:results :contain]
              (reduce (fn [acc [gv-id]]
                        (assoc acc gv-id (apply-output-cpp-fn contain/fns module gv-id)))
                      {}
                      outputs))))

(defn mortality-solver [worksheet]
  (assoc-in worksheet [:results :mortality] []))

(defn solve-worksheet [{:keys [modules] :as worksheet}]
  (cond-> worksheet
    (contains? modules :surface)
    (surface-solver)

    (contains? modules :crown)
    (crown-solver)

    (contains? modules :contain)
    (contain-solver)

    (contains? modules :mortality)
    (mortality-solver)))
