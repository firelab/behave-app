(ns behave.solver.core
  (:require [behave.solver.diagrams   :refer [store-all-diagrams!]]
            [behave.solver.generators :refer [generate-runs inputs-map-to-vector]]
            [behave.solver.queries    :as q]
            [behave.solver.table      :as t]
            [behave.lib.contain       :as contain]
            [behave.lib.crown         :as crown]
            [behave.lib.mortality     :as mortality]
            [behave.lib.surface       :as surface]
            [behave.lib.spot          :as spot]
            [behave.logger            :refer [log]]
            [clojure.string           :as str]
            [clojure.set              :as set]
            [re-frame.core            :as rf]
            [map-utils.interface      :refer [index-by]]))

;;; Helpers

(defn- is-unit? [parameter-type]
  (str/includes? parameter-type "Units"))

(defn filter-module-inputs [all-inputs gv-uuids]
  (into {} (filter (fn [[_group-uuid m]]
                     (gv-uuids (first (ffirst (vals m))))) all-inputs)))

(defn filter-module-outputs [all-outputs gv-uuids]
  (vec (filter gv-uuids all-outputs)))

(def log-solver (comp log vec (partial cons :SOLVER)))

;;; CPP Interop Functions

(defn apply-single-cpp-fn [module-fns module gv-id value unit]
  (let [[fn-id fn-name] (q/group-variable->fn gv-id)
        value           (q/parsed-value gv-id value)
        f               ((symbol fn-name) module-fns)
        params          (q/fn-params fn-id)]
    (log-solver [:SINGLE fn-name value unit])

    (cond
      (nil? value)
      (js/console.error "Cannot process Module with nil value for:" gv-id)

      (= 1 (count params))
      (f module value)

      (and (= 2 (count params)) (some? unit))
      (let [[_ _ param-type] (first params)]
        (if (is-unit? param-type)
          (f module unit value)
          (f module value unit))))))

(defn apply-multi-cpp-fn
  [module-fns module repeat-group]
  (let [[gv-id _]       (first repeat-group)
        [fn-id fn-name] (q/group-variable->fn gv-id)
        f               ((symbol fn-name) module-fns)
                                        ; Step 1 - Lookup parameters of function
        params          (q/fn-params fn-id)
        _               (log-solver [:FN-ID fn-id] [:MULTI-PARAMS params])

                                        ; Step 2 - Match the parameters to group inputs/units
        fn-args (map-indexed (fn [idx [param-id _ param-type]]
                               (if (is-unit? param-type)
                                 ;; Retrieve previous parameter's units
                                 (let [[param-id]    (nth params (dec idx))
                                       gv-uuid       (q/parameter->group-variable param-id)
                                       [_ unit-uuid] (get repeat-group gv-uuid)
                                       unit          (q/unit-uuid->enum-value unit-uuid)]
                                   (log-solver [:MULTI-UNITS gv-uuid unit])
                                   unit)
                                 (let [gv-uuid   (q/parameter->group-variable param-id)
                                       [value _] (get repeat-group gv-uuid)]
                                   (log-solver [:MULTI-VALUE gv-uuid value])
                                   (q/parsed-value gv-uuid value))))
                             params)]

    (log-solver [:MULTI-INPUT fn-name fn-args])

                                        ; Step 3 - Call function with all parameters
    (apply f module fn-args)))

(defn apply-output-cpp-fn
  [module-fns module gv-id unit-uuid]
  (let [[fn-id fn-name] (q/group-variable->fn gv-id)
        unit            (q/unit-uuid->enum-value unit-uuid)
        f               ((symbol fn-name) module-fns)
        params          (q/fn-params fn-id)]

    (log-solver [:OUTPUT fn-name unit f params])

    (cond
      (empty? params)
      (f module)

      (and (= 1 (count params)) (some? unit))
      (f module unit)

      :else nil)))

(defn apply-inputs [module fns inputs]
  (doseq [[_ repeats] inputs]
    (log-solver [:REPEATS repeats])
    (cond
      ;; Single Group w/ Single Variable
      (and (= 1 (count repeats)) (= 1 (count (first (vals repeats)))))
      (let [[gv-id [value unit-uuid]] (ffirst (vals repeats))
            unit                      (q/unit-uuid->enum-value unit-uuid)]
        (log-solver [:SINGLE-VAR gv-id value unit])
        (apply-single-cpp-fn fns module gv-id value unit))

      ;; Multiple Groups w/ Single Variable
      (every? #(= 1 (count %)) (vals repeats))
      (doseq [[_ repeat-group] repeats]
        (let [[gv-id [value unit-uuid]] (first repeat-group)
              unit                      (q/unit-uuid->enum-value unit-uuid)]
          (log-solver [:SINGLE-VAR-MULTIPLE gv-id value unit])
          (apply-single-cpp-fn fns module gv-id value unit)))

      ;; Multiple Groups w/ Multiple Variables
      :else
      (doseq [[_ repeat-group] repeats]
        (log-solver [:MULTI repeat-group])
        (apply-multi-cpp-fn fns module repeat-group)))))

(defn get-outputs [module fns outputs]
  (reduce
   (fn [acc group-variable-uuid]
     (let [var-uuid     (q/variable-uuid group-variable-uuid)
           *var-entity  (rf/subscribe [:vms/entity-from-uuid var-uuid])
           domain-uuid  (:variable/domain-uuid @*var-entity)
           *cached-unit (rf/subscribe [:settings/cached-unit domain-uuid])
           unit-uuid    (or @*cached-unit
                            (q/variable-native-units-uuid group-variable-uuid)
                            :none)
           result       (str (apply-output-cpp-fn fns module group-variable-uuid unit-uuid))]
       (log-solver [:GET-OUTPUTS group-variable-uuid result unit-uuid])
       (assoc acc group-variable-uuid [result unit-uuid])))
   {}
   outputs))

;;; Links
(defn add-links [{:keys [gv-uuids] :as module}]
  (assoc module
         :destination-links   (q/destination-links gv-uuids)
         :source-links        (q/source-links gv-uuids)
         :output-source-links (q/output-source-links gv-uuids)))

;;; Solvers

(defn apply-output-links [prev-outputs inputs destination-links]
  (let [prev-output-uuids (set (keys prev-outputs))]
    (reduce
     (fn [acc [src-uuid dst-uuid]]
       (if (prev-output-uuids src-uuid)
         (let [output     (get prev-outputs src-uuid)
               group-uuid (q/group-variable->group dst-uuid)]
           (log-solver [:ADD-LINK [:SRC-UUID src-uuid :DST-UUID dst-uuid] [:OUTPUT output :GROUP-UUID group-uuid]])
           (assoc-in acc [group-uuid 0 dst-uuid] output))
         acc))
     inputs
     destination-links)))

(defn apply-input-links [inputs destination-links]
  (let [inputs-vec        (inputs-map-to-vector inputs)
        inputs-by-gv-uuid (index-by #(nth % 2) inputs-vec)
        input-gv-uuids    (set (map #(nth % 2) inputs-vec))]
    (reduce
     (fn [acc [src-uuid dst-uuid]]
       (if (input-gv-uuids src-uuid)
         (let [[src-group-uuid _ _ value unit-uuid] (get inputs-by-gv-uuid src-uuid)
               group-uuid              (q/group-variable->group dst-uuid)]
           (log-solver [:ADD-LINK
                        [:SRC src-group-uuid 0 src-uuid [value unit-uuid]]
                        [:DST group-uuid 0 dst-uuid [value unit-uuid]]])
           (assoc-in acc
                     [group-uuid 0 dst-uuid]
                     [value unit-uuid]))
         acc))
     inputs
     destination-links)))

(defn add-source-link-outputs [outputs source-links]
  (vec (concat outputs (keys source-links))))

(defn run-module [{:keys [inputs all-outputs outputs row-id] :as row}
                  {:keys [init-fn
                          run-fn
                          fns
                          gv-uuids
                          output-source-links
                          destination-links
                          diagrams
                          ws-uuid]}]
  (let [module         (init-fn)
        ;; Apply links
        inputs         (apply-output-links outputs inputs destination-links)
        inputs         (apply-input-links inputs destination-links)

        ;; Filter IO's for module
        module-inputs  (filter-module-inputs inputs gv-uuids)
        module-outputs (-> all-outputs
                           (filter-module-outputs gv-uuids)
                           (add-source-link-outputs output-source-links))]

    ;; Set inputs
    (apply-inputs module fns module-inputs)

    ;; Run module
    (run-fn module)

    ;; Store diagrams
    (store-all-diagrams! {:ws-uuid  ws-uuid
                          :row-id   row-id
                          :diagrams diagrams
                          :module   module})

    ;; Get outputs, merge existing inputs/outputs with new inputs/outputs
    (update row :outputs merge (get-outputs module fns module-outputs))))

(defn remove-source-link-outputs [row surface-module]
  (let [{:keys [outputs all-outputs]} row
        {:keys [output-source-links]} surface-module
        to-remove                     (set/difference (set (keys output-source-links)) (set all-outputs))]
    (assoc row :outputs (apply dissoc outputs to-remove))))

(defn solve-worksheet
  ([ws-uuid]
   (let [modules     (set (q/worksheet-modules ws-uuid))
         all-inputs  @(rf/subscribe [:worksheet/all-inputs+units-vector ws-uuid])
         all-outputs @(rf/subscribe [:worksheet/output-uuids-filtered ws-uuid])]

     (-> (solve-worksheet ws-uuid modules all-inputs all-outputs)
         (t/add-to-results-table ws-uuid))))

  ([ws-uuid modules all-inputs all-outputs]
   (let [counter (atom 0)
         surface-module
         (-> {:init-fn     surface/init
              :run-fn      surface/doSurfaceRun
              :fns         (ns-publics 'behave.lib.surface)
              :gv-uuids    (q/class-to-group-variables "SIGSurface")
              :diagrams    (q/module-diagrams "surface")
              :ws-uuid     ws-uuid}
             (add-links))

         crown-module
         (-> {:init-fn  crown/init
              :run-fn   crown/doCrownRun
              :fns      (ns-publics 'behave.lib.crown)
              :gv-uuids (q/class-to-group-variables "SIGCrown")}
             (add-links))

         contain-module
         (-> {:init-fn     contain/init
              :run-fn      contain/doContainRun
              :fns         (ns-publics 'behave.lib.contain)
              :gv-uuids    (q/class-to-group-variables "SIGContainAdapter")
              :diagrams    (q/module-diagrams "contain")
              :ws-uuid     ws-uuid}
             (add-links))

         mortality-module
         (-> {:init-fn  mortality/init
              :run-fn   mortality/calculateMortalityAllDirections
              :fns      (ns-publics 'behave.lib.mortality)
              :gv-uuids (q/class-to-group-variables "SIGMortality")}
             (add-links))

         spot-module
         (-> {:init-fn  spot/init
              :run-fn   spot/calculateAll
              :fns      (ns-publics 'behave.lib.spot)
              :gv-uuids (q/class-to-group-variables "SIGSpot")}
             (add-links))]

     (->> all-inputs
          (generate-runs)
          (reduce (fn [acc inputs]
                    (let [add-row (partial conj acc)
                          row     {:inputs      inputs
                                   :all-outputs all-outputs
                                   :outputs     {}
                                   :row-id      @counter}]

                      (swap! counter inc)

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
                        (remove-source-link-outputs surface-module)

                        :always
                        (add-row))))
                  [])))))
