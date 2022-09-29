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
  (rf/subscribe [:query '[:find [?p ?name ?type]
                          :in $ ?fn
                          :where [?fn :function/parameters ?p]
                                 [?p :parameter/name ?name]
                                 [?p :parameter/type ?type]]
                 [function-id]]))

(defn variable-units [group-variable-id]
  (let [unit-short (rf/subscribe [:query '[:find  [?units]
                                           :in    $ ?gv
                                           :where [?v :variable/group-variables ?gv]
                                                  [?v :variable/native-units ?units]]
                                  [group-variable-id]])]
    (units/get-unit (first @unit-short))))

;; Cannot use pull due to the use of UUID's to join CPP ns/class/fns
(defn group-variable->fn [group-variable-id]
  (rf/subscribe [:query '[:find  [?fn ?fn-name]
                          :in    $ ?gv
                          :where [?v :variable/group-variables ?gv]
                                 [?v :variable/name ?v-name]
                                 [?gv :group-variable/cpp-namespace ?ns-uuid]
                                 [?gv :group-variable/cpp-class ?class-uuid]
                                 [?gv :group-variable/cpp-function ?fn-uuid]
                                 [?ns :bp/uuid ?ns-id]
                                 [?ns :namespace/name ?ns-name]
                                 [?class :bp/uuid ?class-uuid]
                                 [?class :class/name ?class-name]
                                 [?fn :bp/uuid ?fn-uuid]
                                 [?fn :function/name ?fn-name]]
                 [group-variable-id]]))

(defn module-variables [module-name io]
  (rf/subscribe [:query '[:find [?gv ...]
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

(defn contain-solver [worksheet]
  (let [{:keys [inputs outputs]} worksheet
        input-vars  (map (fn [gv-id] [gv-id @(group-variable->fn gv-id)]) @(all-input-variables "Contain"))
        output-vars (map (fn [gv-id] [gv-id @(group-variable->fn gv-id)]) @(all-output-variables "Contain"))
        output-vars (filter #(get outputs (first %)) output-vars)
        module      (contain/init)]

    ; Load Inputs
    (doseq [[gv-id [fn-id fn-name]] input-vars]
      (let [value  (if (discrete? gv-id) (get enum/contain-tactic "HeadAttack") (get inputs gv-id))
            unit   (variable-units gv-id)
            f      (aget module fn-name)
            params @(fn-params fn-id)]
        (println "Applying Input:" fn-name params value unit)
        (cond
          (nil? value)
          (println "Cannot process Contain Module with nil value for:" @(rf/subscribe [:pull '[{:variable/_group-variables [:variable/name]}] gv-id]))

          (= 1 (count params))
          (f value)

          (and (= 2 (count params)) (some? unit))
          (if (-> params (first) (last) (is-enum?))
            (f unit value)
            (f value unit)))))

    (contain/addResource
      module
      (get inputs 2903)
      (get inputs 2873)
      (get enum/time-units "Hours")
      (get inputs 2904)
      (get enum/speed-units "ChainsPerHour")
      "test")

    ; Run
    (contain/doContainRun module)

    ; Get Outputs
    (reduce (fn [acc [gv-id [fn-id fn-name]]]
              (let [unit   (variable-units gv-id)
                    f      (aget module fn-name)
                    params @(fn-params fn-id)
                    result (cond
                             (empty? params)
                             (f)

                             (and (= 1 (count params)) (some? unit))
                             (f unit)

                             :else nil)]
                (js/console.log f)
                (println "Getting Ouputs:" fn-name f params unit result)
                (assoc acc gv-id result)))
            {}
            output-vars)))

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
