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

(defn- resolve-units-uuid-from-vms-or-cached-settings
  [sv-uuid]
  (let [var-uuid     (q/variable-uuid sv-uuid)
        *var-entity  (rf/subscribe [:vms/entity-from-uuid var-uuid])
        domain-uuid  (:variable/domain-uuid @*var-entity)
        *cached-unit (rf/subscribe [:settings/cached-unit domain-uuid])]
    (or @*cached-unit
        (q/variable-native-units-uuid sv-uuid)
        :none)))

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
  [{:keys [fns inputs outputs compute-fn]}]
  (let [init-fn  (fns "init")
        tool-obj (init-fn)]

    ;; Set inputs
    (doseq [[sv-uuid variable] inputs]
      (let [{value :input/value units-uuid :input/units-uuid} variable
            units-uuid                                        (or units-uuid
                                                                  (resolve-units-uuid-from-vms-or-cached-settings
                                                                   sv-uuid))
            units-enum                                        (q/unit-uuid->enum-value units-uuid)]
        (apply-single-cpp-fn fns tool-obj sv-uuid value units-enum)))

    ;; Compute Tool
    (compute-fn tool-obj)

    ;; Get outputs
    (into {}
          (map (fn [[output-uuid selected-unit]]
                 (let [units-uuid   (or selected-unit
                                        (resolve-units-uuid-from-vms-or-cached-settings output-uuid))
                       units-enum   (q/unit-uuid->enum-value units-uuid)
                       output-value (apply-output-cpp-fn fns tool-obj output-uuid units-enum)]
                   [output-uuid {:output/value      (format-intl-number "en-US" output-value 2)
                                 :output/units-uuid units-uuid}]))
               outputs))))

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
        params      {:fns        fns
                     :inputs     @(rf/subscribe [:tool/all-inputs tool-uuid subtool-uuid])
                     :outputs    @(rf/subscribe [:tool/all-outputs tool-uuid subtool-uuid])
                     :compute-fn (get-compute-fn subtool-uuid fns)}]
    (run-tool params)))
