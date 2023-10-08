(ns behave.tool.solver
  (:require [re-frame.core         :as rf]
            [goog.object           :as obj]
            [behave.solver.queries :as q]
            [browser-utils.core    :refer [format-intl-number]]
            [clojure.string        :as str]
            [behave.logger         :refer [log]]
            [behave.lib.ignite]
            [behave.lib.fine-dead-fuel-moisture-tool]
            [behave.lib.slope-tool]
            [behave.lib.vapor-pressure-deficit-calculator]))

(defn kebab->snake
  "Converts a snake_case string to a kebab-case string."
  [s]
  (str/replace s #"-" "_"))

(defn- is-enum? [parameter-type]
  (or (str/includes? parameter-type "Enum")
      (str/includes? parameter-type "Units")))

(defn- variable-units
  ([sv-uuid]
   (variable-units sv-uuid nil))
  ([sv-uuid selected-units-uuid]
   (if selected-units-uuid
     (q/unit-uuid->enum-value selected-units-uuid)
     (q/variable-units sv-uuid))))

(defn- apply-single-cpp-fn [fns tool-obj sv-uuid value units]
  (let [[fn-id fn-name] (q/subtool-variable->fn sv-uuid)
        value           (q/parsed-value sv-uuid value)
        f               (fns fn-name)
        params          (q/fn-params fn-id)]
    (log [:SOLVER] [:INPUT fn-name value units])

    (cond
      (nil? value)
      (js/console.error "Cannot process tool with nil value for:" sv-uuid)

      (= 1 (count params))
      (f tool-obj value)

      (and (= 2 (count params)) (some? units))
      (let [[_ _ param-type] (first params)]
        (if (is-enum? param-type)
          (f tool-obj units value)
          (f tool-obj value units))))))

(defn- apply-output-cpp-fn
  [fns tool-obj sv-uuid units]
  (let [[fn-id fn-name] (q/subtool-variable->fn sv-uuid)
        f               (fns fn-name)
        params          (q/fn-params fn-id)]
    (log [:SOLVER] [:OUTPUT fn-name units f params])

    (cond
      (empty? params)
      (f tool-obj)

      (and (= 1 (count params)) (some? units))
      (f tool-obj units)

      :else nil)))

(defn- run-tool
  [{:keys [fns inputs output-uuids compute-fn]}]
  (let [init-fn  (fns "init")
        tool-obj (init-fn)]

    ;; Set inputs
    (doseq [[sv-uuid variable] inputs]
      (let [{value :input/value units-uuid :input/units-uuid} variable
            units (variable-units sv-uuid units-uuid)]
        (apply-single-cpp-fn fns tool-obj sv-uuid value units)))

    ;; Compute Tool
    (compute-fn tool-obj)

    ;; Get outputs
    (into {}
          (map (fn [output-uuid]
                 (let [units (variable-units output-uuid)
                       value (apply-output-cpp-fn fns tool-obj output-uuid units)]
                   [output-uuid (format-intl-number "en-US" value 2)]))
               output-uuids))))

(defn- get-compute-fn [subtool-uuid fns]
  (let [fn-name (q/subtool-compute->fn-name subtool-uuid)]
    (log [:SOLVER] [:COMPUTE-fn fn-name])
    (fns fn-name)))

(defn- list-cljs-fns [ns-symbol-or-string]
  (let [ns-string (name ns-symbol-or-string)]
    (-> js/window
        (obj/getValueByKeys (clj->js (str/split ns-string #"\.")))
        (obj/get "ns_public_fns")
        (js->clj))))

(defn solve-tool
  "Extracts inputs from the app state and runs the compute function for the given subtool.
  Returns a map of subtool-variable uuids -> value"
  [tool-uuid subtool-uuid]
  (let [tool-entity (rf/subscribe [:tool/entity tool-uuid])
        lib-ns      (kebab->snake (:tool/lib-ns @tool-entity))
        fns         (list-cljs-fns lib-ns)
        params      {:fns          fns
                     :inputs       @(rf/subscribe [:tool/all-inputs tool-uuid subtool-uuid])
                     :output-uuids @(rf/subscribe [:tool/all-output-uuids subtool-uuid])
                     :compute-fn   (get-compute-fn subtool-uuid fns)}]
    (run-tool params)))
