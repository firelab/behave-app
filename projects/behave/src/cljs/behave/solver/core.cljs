(ns behave.solver.core
  (:require [behave.solver.queries :as q]
            [behave.solver.table   :as t]
            [behave.lib.contain    :as contain]
            [behave.lib.crown      :as crown]
            [behave.lib.mortality  :as mortality]
            [behave.lib.surface    :as surface]
            [behave.lib.spot       :as spot]
            [behave.lib.units      :as units]
            [behave.logger         :refer [log]]
            [clojure.string        :as str]
            [clojure.set           :as set]
            [re-frame.core         :as rf]))

;;; Helpers

(defn- csv? [s] (< 1 (count (str/split s #","))))

(defn- is-enum? [parameter-type]
  (or (str/includes? parameter-type "Enum")
      (str/includes? parameter-type "Units")))

(defn filter-module-ios [ios gv-uuids]
  (filter #(-> % (butlast) (last) (gv-uuids)) ios))

;;; Run Generation

(defn permutations [single-inputs range-inputs]
  (case (count range-inputs)
    0 [single-inputs]
    1 (for [x (first range-inputs)]
        (conj single-inputs x))
    2 (for [x (first range-inputs)
            y (second range-inputs)]
        (conj single-inputs x y))
    3 (for [x (first range-inputs)
            y (second range-inputs)
            z (nth range-inputs 2)]
        (conj single-inputs x y z))))

(defn ->run-plan [inputs]
  (reduce (fn [acc [group-uuid repeat-id group-var-uuid value]]
            (assoc-in acc [group-uuid repeat-id group-var-uuid] value))
          {}
          inputs))

(defn generate-runs [all-inputs-vector]
  (let [single-inputs          (remove #(-> % (last) (csv?)) all-inputs-vector)
        range-inputs           (filter #(-> % (last) (csv?)) all-inputs-vector)
        separated-range-inputs (map #(let [result (vec (butlast %))
                                           values (map str/trim (str/split  (last %) #","))]
                                       (mapv (fn [v] (conj result v)) values)) range-inputs)]

    (map ->run-plan (permutations (vec single-inputs) separated-range-inputs))))


;;; CPP Interop Functions

(defn apply-single-cpp-fn [module-fns module gv-id value units]
  (let [[fn-id fn-name] (q/group-variable->fn gv-id)
        value           (q/parsed-value gv-id value)
        unit-enum       (units/get-unit units)
        f               ((symbol fn-name) module-fns)
        params          (q/fn-params fn-id)]
     (log "Input:" fn-name value unit-enum)

    (cond
      (nil? value)
      (js/console.error "Cannot process Contain Module with nil value for:" gv-id)

      (= 1 (count params))
      (f module value)

      (and (= 2 (count params)) (some? unit-enum))
      (let [[_ _ param-type] (first params)]
        (if (is-enum? param-type)
          (f module unit-enum value)
          (f module value unit-enum))))))

(defn apply-multi-cpp-fn
  [module-fns module repeat-group]
  (let [[gv-id _]       (first repeat-group)
        [fn-id fn-name] (q/group-variable->fn gv-id)
        f               ((symbol fn-name) module-fns)
      ; Step 1 - Lookup parameters of function
        params          (q/fn-params fn-id)

      ; Step 2 - Match the parameters to group inputs/units
        fn-args (map-indexed (fn [idx [param-id _ param-type]]
                               (if (is-enum? param-type)
                                 (let [[param-id] (nth params (dec idx))
                                       gv-uuid    (q/parameter->group-variable param-id)]
                                   (units/get-unit (q/variable-units gv-uuid)))
                                 (let [gv-uuid (q/parameter->group-variable param-id)]
                                   (q/parsed-value gv-uuid (get repeat-group gv-uuid)))))
                             params)]

    (log [:SOLVER] [:MULTI-INPUT fn-name params fn-args])

  ; Step 3 - Call function with all parameters
    (apply f module fn-args)))

(defn apply-output-cpp-fn
  [module-fns module gv-id]
  (let [[fn-id fn-name] (q/group-variable->fn gv-id)
        unit            (units/get-unit (q/variable-units gv-id))
        f               ((symbol fn-name) module-fns)
        params          (q/fn-params fn-id)]

    (log [:SOLVER] [:OUTPUT fn-name unit f params])

    (cond
      (empty? params)
      (f module)

      (and (= 1 (count params)) (some? unit))
      (f module unit)

      :else nil)))

(defn apply-inputs [module fns inputs]
  (doseq [[_ repeats] inputs]
    (cond
      ;; Single Group w/ Single Variable
      (and (= 1 (count repeats)) (= 1 (count (first (vals repeats)))))
      (let [[gv-id value] (ffirst (vals repeats))
            units         (or (q/variable-units gv-id) "")]
        (log "-- [SOLVER] SINGLE VAR" gv-id value units)
        (apply-single-cpp-fn fns module gv-id value units))

      ;; Multiple Groups w/ Single Variable
      (every? #(= 1 (count %)) (vals repeats))
      (doseq [[_ repeat-group] repeats]
        (let [[gv-id value] (first repeat-group)
              units         (or (q/variable-units gv-id) "")]
          (log "-- [SOLVER] SINGLE VAR (MULTIPLE)" gv-id value units)
          (apply-single-cpp-fn fns module gv-id value units)))

      ;; Multiple Groups w/ Multiple Variables
      :else
      (doseq [[_ repeat-group] repeats]
        (log "-- [SOLVER] MULTI" repeat-group)
        (apply-multi-cpp-fn fns module repeat-group)))))

(defn get-outputs [module fns outputs]
  (reduce
   (fn [acc group-variable-uuid]
     (let [units  (q/variable-units group-variable-uuid)
           result (str (apply-output-cpp-fn fns module group-variable-uuid))]
       (log [:GET-OUTPUTS group-variable-uuid result units])
       (assoc acc group-variable-uuid [result units])))
   {}
   outputs))

;;; Links
(defn add-links [{:keys [gv-uuids] :as module}]
  (assoc module
         :destination-links (q/destination-links gv-uuids)
         :source-links      (q/source-links gv-uuids)))

;;; Solvers

(defn apply-links [prev-outputs inputs destination-links]
  (reduce
   (fn [acc [src-uuid dst-uuid]]
     (let [[_ [output _]] (first (filter #(= src-uuid (first %)) prev-outputs))
           group-uuid     (q/group-variable->group dst-uuid)]
       (assoc-in acc [group-uuid 0 dst-uuid] output)))
   inputs
   destination-links))

(defn run-module [{:keys [inputs outputs] :as row}
                  {:keys [init-fn
                          run-fn
                          fns
                          gv-uuids
                          destination-links]}]
  (let [module         (init-fn)

        ;; Apply links
        inputs         (apply-links outputs inputs destination-links)

        ;; Filter IO's for module
        module-inputs  (filter-module-ios inputs gv-uuids)
        module-outputs (filter-module-ios outputs gv-uuids)]

    ;; Set inputs
    (apply-inputs module fns module-inputs)

    ;; Run module
    (run-fn module)

    ;; Get outputs, merge existing inputs/outputs with new inputs/outputs
    (update row :outputs merge (get-outputs module fns module-outputs))))

(defn solve-worksheet
  ([ws-uuid]
   (let [modules     (set (q/worksheet-modules ws-uuid))
         all-inputs  @(rf/subscribe [:worksheet/all-inputs-vector ws-uuid])
         all-outputs @(rf/subscribe [:worksheet/all-output-uuids ws-uuid])]

     (-> (solve-worksheet modules all-inputs all-outputs)
         (t/add-to-results-table ws-uuid))))

  ([modules all-inputs all-outputs]
   (let [counter (atom 0)

        surface-module
        (-> {:init-fn  surface/init
             :run-fn   surface/doSurfaceRun
             :fns      (ns-publics 'behave.lib.surface)
             :gv-uuids (q/class-to-group-variables "SIGSurface")}
            (add-links))

        crown-module
        (-> {:init-fn  crown/init
             :run-fn   crown/doCrownRun
             :fns      (ns-publics 'behave.lib.crown)
             :gv-uuids (q/class-to-group-variables "SIGCrown")}
            (add-links))

        contain-module
        (-> {:init-fn  contain/init
             :run-fn   contain/doContainRun
             :fns      (ns-publics 'behave.lib.contain)
             :gv-uuids (q/class-to-group-variables "SIGContainAdapter")}
            (add-links))

        mortality-module
        (-> {:init-fn  mortality/init
             :run-fn   mortality/calculateMortality
             :fns      (ns-publics 'behave.lib.mortality)
             :gv-uuids (q/class-to-group-variables "SIGMortality")}
            (add-links))

        spot-module
        (-> {:init-fn  spot/init
             :run-fn   spot/calculateSpottingDistanceFromSurfaceFire
             :fns      (ns-publics 'behave.lib.spot)
             :gv-uuids (q/class-to-group-variables "SIGSpot")}
            (add-links))]

    (->> all-inputs
         (generate-runs)
         (reduce (fn [acc inputs]
                   (let [add-row (partial conj acc)
                         row {:inputs inputs
                              :outputs all-outputs
                              :row-id @counter}]

                     (swap! count inc)

                     (cond-> row
                       (contains? modules :surface)
                       (run-module surface-module)

                       (contains? modules :crown)
                       (run-module crown-module)

                       (contains? modules :contain)
                       (run-module contain-module)

                       (contains? modules :mortality)
                       (run-module mortality-module)

                       (pos? (count (set/intersection modules #{:surface :crown})))
                       (run-module spot-module)

                       :always
                       (add-row))))
                 [])))))
