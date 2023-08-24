(ns behave.solver.tool
  (:require [re-frame.core :as rf]
            [behave.solver.queries :as q]
            [browser-utils.core :refer [format-intl-number]]
            [clojure.string        :as str]
            [behave.logger         :refer [log]]
            [behave.lib.units      :as units]
            [behave.lib.ignite :as ignite]))

(defn- is-enum? [parameter-type]
  (or (str/includes? parameter-type "Enum")
      (str/includes? parameter-type "Units")))

(defn- apply-single-cpp-fn [tool-fns tool-obj sv-uuid value units]
  (let [[fn-id fn-name] (q/subtool-variable->fn sv-uuid)
        value           (q/parsed-value sv-uuid value)
        unit-enum       (units/get-unit units)
        f               ((symbol fn-name) tool-fns)
        params          (q/fn-params fn-id)]
    (log [:SOLVER] [:INPUT fn-name value unit-enum])

    (cond
      (nil? value)
      (js/console.error "Cannot process Contain Module with nil value for:" sv-uuid)

      (= 1 (count params))
      (f tool-obj value)

      (and (= 2 (count params)) (some? unit-enum))
      (let [[_ _ param-type] (first params)]
        (if (is-enum? param-type)
          (f tool-obj unit-enum value)
          (f tool-obj value unit-enum))))))

(defn- apply-output-cpp-fn
  [tool-fns tool-obj sv-uuid]
  (let [[fn-id fn-name] (q/subtool-variable->fn sv-uuid)
        unit            (units/get-unit (q/variable-units sv-uuid))
        f               ((symbol fn-name) tool-fns)
        params          (q/fn-params fn-id)]
    (log [:SOLVER] [:OUTPUT fn-name unit f params])

    (cond
      (empty? params)
      (f tool-obj)

      (and (= 1 (count params)) (some? unit))
      (f tool-obj unit)

      :else nil)))

(defn- run-tool
  [inputs output-uuids {:keys [init-fn fns compute-fn]}]
  (let [tool-obj (init-fn)]

    ;; Set inputs
    (doseq [[sv-uuid value] inputs]
      (let [units (or (q/variable-units sv-uuid) "")]
        (apply-single-cpp-fn fns tool-obj sv-uuid value units)))

    ;; Compute Tool
    (compute-fn tool-obj)

    ;; Get outputs
    (for [output-uuid output-uuids]
      (let [value (apply-output-cpp-fn fns tool-obj output-uuid)]
        [output-uuid (format-intl-number "en-US" value 2)]))))

(defn- add-compute-fn [subtool-uuid {:keys [fns] :as tool}]
  (let [fn-name (q/subtool-compute->fn-name subtool-uuid)
        f       ((symbol fn-name) fns)]
    (log [:SOLVER] [:COMPUTE-fn fn-name])
    (assoc tool :compute-fn f)))

(defn solve-tool
  [tool-uuid subtool-uuid]
  (let [tools         {:ignite {:init-fn ignite/init
                                :fns     (ns-publics 'behave.lib.ignite)}}
        selected-tool (->> (:ignite tools)
                           (add-compute-fn subtool-uuid))
        inputs        (rf/subscribe [:tool/all-inputs tool-uuid subtool-uuid])
        output-uuids  (rf/subscribe [:tool/all-output-uuids subtool-uuid])]
    (run-tool @inputs @output-uuids selected-tool)))
