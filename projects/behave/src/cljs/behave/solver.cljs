(ns behave.solver
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [behave.lib.core       :as lib]
            [behave.lib.contain    :as contain]
            [behave.lib.enums      :as enum]
            [behave.lib.units      :as units]
            [datascript.core       :as d]
            [datom-utils.interface :as du]))

; SOLVER DESIGN
; - Worksheet is a Datascript DB
; - Worksheet
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

(defonce module (atom nil))

(defn contain-solver [worksheet]
  (let [{:keys [inputs outputs]} worksheet
        input-vars  (map (fn [gv-id] [gv-id @(group-variable->fn gv-id)]) @(all-input-variables "Contain"))
        output-vars (map (fn [gv-id] [gv-id @(group-variable->fn gv-id)]) @(all-output-variables "Contain"))
        output-vars (filter #(get outputs (first %)) output-vars)
        ;module      (contain/init)
        ]

    (reset! module (contain/init))

    ; Load Inputs
    (doseq [[gv-id [fn-id fn-name]] input-vars]
      (let [value  (if (discrete? gv-id) (get enum/contain-tactic "HeadAttack") (get inputs gv-id))
            unit   (variable-units gv-id)
            f      (symbol (str "contain/" fn-name))
            params @(fn-params fn-id)]
        (println "Applying Input:" fn-name params value unit)
        (cond
          (nil? value)
          (println "Cannot process Contain Module with nil value for:" @(rf/subscribe [:pull '[{:variable/_group-variables [:variable/name]}] gv-id]))

          (= 1 (count params))
          (apply f @module value)

          (and (= 2 (count params)) (some? unit))
          (if (-> params (first) (last) (is-enum?))
            (apply f @module unit value)
            (apply f @module value unit)))))

    (contain/addResource
      @module
      (get inputs 2903)
      (get inputs 2873)
      (get enum/time-units "Hours")
      (get inputs 2904)
      (get enum/speed-units "ChainsPerHour")
      "test")

    ; Run
    (contain/doContainRun @module)

    ; Get Outputs
    (reduce (fn [acc [gv-id [fn-id fn-name]]]
              (let [unit   (variable-units gv-id)
                    f      (symbol (str "contain/" fn-name))
                    params @(fn-params fn-id)]
                (println "Getting Ouputs:" fn-name params unit)
                (assoc-in acc
                          [:results gv-id]
                          (cond
                            (= 0 (count params))
                            (apply f @module)

                            (and (= 1 (count params)) (some? unit))
                            (apply f @module unit)))))
            worksheet
            output-vars)))


(comment

  (rf/subscribe [:pull '[*] 2556])

  (reset! module (contain/init))

  (contain/setAttackDistance @module 0 (get enum/length-units "Chains"))
  (contain/setReportRate @module 5 (get enum/speed-units "ChainsPerHour"))
  (contain/setLwRatio @module 3.0)
  (contain/setReportRate @module 5 (get enum/speed-units "ChainsPerHour"))
  (contain/setReportSize @module 1 (get enum/area-units "Acres"))
  (contain/setTactic @module (get enum/contain-tactic "HeadAttack"))
  (contain/addResource @module 2 8 (get enum/time-units "Hours") 20 (get enum/speed-units "ChainsPerHour") "test")

  (contain/doContainRun @module)

  (contain-solver {:inputs {3005 3
                            3004 2
                            2905 1
                            2875 3
                            2873 3
                            2903 2
                            2264 1
                            2904 1}
                   :outputs {3007 true 3006 true}})

  )

(defn mortality-solver [worksheet]
  (assoc-in worksheet [:results :mortality] []))

(defn solve-worksheet [{:keys [modules] :as worksheet}]
  (cond-> worksheet

    (modules :surface)
    (surface-solver)

    (modules :crown)
    (crown-solver)

    (modules :contain)
    (contain-solver)

    (modules :mortality)
    (mortality-solver)))
