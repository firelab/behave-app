(ns cucumber-test-generator.conditional-outputs
  "Phase-1 extraction for conditionally-set output test cases.

   Finds every output group-variable that BehavePlus auto-enables via :select
   actions, resolves the full transitive chain of required inputs (including
   prerequisites that make each input visible), and writes
   development/conditional_outputs_matrix.edn for Phase-2 feature generation.

   Data shape of each entry (keyed by output gv-uuid):
     {:output-name      'Heading Rate of Spread'
      :module           'Surface'
      :required-modules ['surface']
      :required-inputs  [{:submodule 'Fuel Moisture'
                          :group     'Moisture Input Mode'
                          :value     'Individual Size Class'}
                         {:submodule 'Fuel Moisture'
                          :group     'By Size Class'
                          :subgroup  '1-h Fuel Moisture'
                          :value     '1'}]}"
  (:require [clojure.edn                  :as edn]
            [clojure.pprint               :refer [pprint]]
            [clojure.string               :as str]
            [cucumber-test-generator.core :as core]
            [datomic.api                  :as d]))

;;; ============================================================================
;;; Query
;;; ============================================================================

(defn- find-conditionally-set-output-gvs
  "Return eids of output group-variables that are conditionally set
   AND have at least one :select action."
  [db]
  (d/q '[:find [?gv ...]
         :where
         [?gv :group-variable/conditionally-set? true]
         [?gv :group-variable/actions ?action]
         [?action :action/type :select]]
       db))

;;; ============================================================================
;;; Pull helpers
;;; ============================================================================

(defn- pull-select-actions
  "Pull the :select actions with their raw conditionals for a GV eid."
  [db gv-eid]
  (->> (d/pull db
               '[{:group-variable/actions
                  [:action/type
                   :action/conditionals-operator
                   {:action/conditionals
                    [:conditional/type
                     :conditional/operator
                     :conditional/values
                     :conditional/group-variable-uuid
                     :conditional/sub-conditional-operator
                     {:conditional/sub-conditionals
                      [:conditional/type
                       :conditional/operator
                       :conditional/values
                       :conditional/group-variable-uuid]}]}]}]
               gv-eid)
       :group-variable/actions
       (filter #(= (:action/type %) :select))))

(defn- pull-ancestor-conditionals
  "Return the raw gating-conditional entities from the parent group hierarchy
   (up to 5 levels) and owning submodule for a given input gv-uuid."
  [db gv-uuid]
  (let [gv (d/pull db
                   '[{:group/_group-variables
                      [:db/id
                       {:group/conditionals
                        [:conditional/type
                         :conditional/operator
                         :conditional/values
                         :conditional/group-variable-uuid
                         {:conditional/sub-conditionals
                          [:conditional/type
                           :conditional/operator
                           :conditional/values
                           :conditional/group-variable-uuid]}]}
                       {:group/_children
                        [:db/id
                         {:group/conditionals
                          [:conditional/type
                           :conditional/operator
                           :conditional/values
                           :conditional/group-variable-uuid]}
                         {:group/_children
                          [:db/id
                           {:group/conditionals
                            [:conditional/type
                             :conditional/operator
                             :conditional/values
                             :conditional/group-variable-uuid]}]}]}
                       {:submodule/_groups
                        [{:submodule/conditionals
                          [:conditional/type
                           :conditional/operator
                           :conditional/values
                           :conditional/group-variable-uuid]}]}]}]
                   [:bp/uuid gv-uuid])]
    (letfn [(walk [g depth]
              (when (and g (pos? depth))
                (concat (:group/conditionals g)
                        (when-let [pg (:group/_children g)]
                          (walk pg (dec depth)))
                        (:submodule/conditionals (:submodule/_groups g)))))]
      (walk (:group/_group-variables gv) 5))))

;;; ============================================================================
;;; Requirement extraction
;;; ============================================================================

(defn- pick-first-value
  "Return the first satisfying value for :equal/:in operators, nil for :not-equal."
  [processed]
  (case (:operator processed)
    :equal (first (:values processed))
    :in    (first (:values processed))
    nil))

(defn- processed->input-req
  "Convert a processed conditional into an input requirement map, or nil."
  [processed raw-cond]
  (when (and (= (:type processed) :group-variable)
             (= (get-in processed [:group-variable :io]) :input)
             (not (get-in processed [:group-variable :group-variable/conditionally-set?])))
    (when-let [value (pick-first-value processed)]
      {:gv-uuid (:conditional/group-variable-uuid raw-cond)
       :value   value
       :gv-info (:group-variable processed)})))

(defn- processed->output-req
  "Convert a processed conditional referencing a selected output (:values=['true'])
   into an output-selection row map, or nil.
   These become 'When these output paths are selected' rows in the scenario."
  [processed]
  (when (and (= (:type processed) :group-variable)
             (= (get-in processed [:group-variable :io]) :output)
             (= (:values processed) ["true"]))
    (let [gv      (:group-variable processed)
          comps   (path->components (:path gv))
          gv-name (:group-variable/translated-name gv)]
      (when (and (seq comps) gv-name)
        (cond-> {:submodule            (first comps)
                 :value                gv-name
                 :module               (:name (first (:path gv)))
                 :submodule/order      (:submodule/order gv)
                 :group/order          (:group/order gv)
                 :group-variable/order (:group-variable/order gv)}
          (>= (count comps) 2) (assoc :group (second comps)))))))

(defn- action->requirements
  "Process a raw :select action map into
   {:modules [...] :input-reqs [...] :output-reqs [...]}."
  [db action]
  (let [raw-conds (:action/conditionals action)]
    {:modules     (->> raw-conds
                       (keep #(core/process-conditional db %))
                       (filter #(= (:type %) :module))
                       (mapcat :values))
     :input-reqs  (->> raw-conds
                       (keep (fn [raw]
                               (when-let [processed (core/process-conditional db raw)]
                                 (processed->input-req processed raw))))
                       (remove #(nil? (:value %))))
     :output-reqs (->> raw-conds
                       (keep (fn [raw]
                               (when-let [processed (core/process-conditional db raw)]
                                 (processed->output-req processed))))
                       (remove nil?))}))

(defn- gating-cond->req
  "Convert a raw gating-conditional entity to an input requirement map, or nil."
  [db raw-cond]
  (when-let [processed (core/process-conditional db raw-cond)]
    (processed->input-req processed raw-cond)))

;;; ============================================================================
;;; Transitive closure (fixpoint)
;;; ============================================================================

(defn- resolve-transitive-closure
  "Walk the ancestor gating conditionals of each required input and add any new
   prerequisites, repeating until no new inputs are found (fixpoint).
   Prerequisites are prepended so ordering is parent→child."
  [db initial-reqs]
  (loop [reqs       (vec initial-reqs)
         seen-uuids (into #{} (keep :gv-uuid initial-reqs))]
    (let [new-reqs (->> reqs
                        (mapcat (fn [{:keys [gv-uuid]}]
                                  (when gv-uuid
                                    (->> (pull-ancestor-conditionals db gv-uuid)
                                         (keep #(gating-cond->req db %))))))
                        (remove #(contains? seen-uuids (:gv-uuid %)))
                        (reduce (fn [acc req]
                                  (if (some #(= (:gv-uuid %) (:gv-uuid req)) acc)
                                    acc
                                    (conj acc req)))
                                []))]
      (if (empty? new-reqs)
        reqs
        ;; Prepend prerequisites so they appear before the inputs that depend on them
        (recur (into (vec new-reqs) reqs)
               (into seen-uuids (keep :gv-uuid new-reqs)))))))

;;; ============================================================================
;;; Row conversion
;;; ============================================================================

(defn- path->components
  "Strip module (first) and io keyword from a path vector, returning display
   :name strings of the remaining elements (submodule, groups...)."
  [path]
  (when (seq path)
    (mapv :name (remove keyword? (rest path)))))

(defn- req->row
  "Convert a requirement map to a row with submodule/group/subgroup/value plus
   VMS order fields (:module :submodule/order :group/order :group-variable/order), or nil."
  [{:keys [value gv-info]}]
  (when-let [comps (seq (path->components (:path gv-info)))]
    (cond-> {:submodule            (first comps)
             :value                value
             :module               (:name (first (:path gv-info)))
             :submodule/order      (:submodule/order gv-info)
             :group/order          (:group/order gv-info)
             :group-variable/order (:group-variable/order gv-info)}
      (>= (count comps) 2) (assoc :group (second comps))
      (>= (count comps) 3) (assoc :subgroup (nth comps 2)))))

;;; ============================================================================
;;; Per-GV test-case builder
;;; ============================================================================

(defn- build-test-case
  "Return a test-case map for an output gv-eid, or nil if the GV is research,
   not an output, or its action conditionals cannot be resolved."
  [db gv-eid]
  (when-let [gv-uuid (:bp/uuid (d/pull db '[:bp/uuid] gv-eid))]
    (let [gv-info (core/resolve-group-variable-uuid db gv-uuid)]
      (when (and gv-info
                 (= (:io gv-info) :output)
                 (not (:group-variable/research? gv-info)))
        (let [actions (pull-select-actions db gv-eid)]
          (when (seq actions)
            ;; Collect requirements from all :select actions, merging across actions
            (let [all-action-reqs (map #(action->requirements db %) actions)
                  modules         (vec (distinct (mapcat :modules all-action-reqs)))
                  initial-reqs    (vec (distinct (mapcat :input-reqs all-action-reqs)))
                  output-reqs     (vec (distinct (mapcat :output-reqs all-action-reqs)))]
              ;; Proceed if there are any requirements at all (input OR output)
              (when (or (seq initial-reqs) (seq output-reqs) (seq modules))
                (let [all-reqs  (if (seq initial-reqs)
                                  (resolve-transitive-closure db initial-reqs)
                                  [])
                      rows      (vec (keep req->row all-reqs))
                      module-kw (or (first (map keyword modules))
                                    (some-> gv-info :path first :name str/lower-case keyword))]
                  {:gv-uuid          gv-uuid
                   :output-name      (or (:group-variable/translated-name gv-info)
                                         "Unknown Output")
                   :module           (some-> module-kw name str/capitalize)
                   :required-modules modules
                   :required-outputs output-reqs
                   :required-inputs  rows})))))))))

;;; ============================================================================
;;; Main entry point
;;; ============================================================================

(defn generate-conditional-outputs-matrix!
  "Generate the :results-page section of the combined test_matrix_data.edn.

   Finds every output group-variable with :group-variable/conditionally-set? true
   and at least one :select action, resolves the full transitive chain of required
   inputs, and merges the :results-page section into the combined EDN file.

   Arguments:
   - db       — Datomic database value (from d/db)
   - edn-path — (optional) combined matrix path; default 'development/test_matrix_data.edn'

   Returns:
   {:edn-path '...' :entries-count N}"
  ([db]
   (generate-conditional-outputs-matrix! db "development/test_matrix_data.edn"))
  ([db edn-path]
   (let [gv-eids    (find-conditionally-set-output-gvs db)
         _          (println (format "Found %d conditionally-set output GVs" (count gv-eids)))
         test-cases (->> gv-eids
                         (keep (fn [eid]
                                 (try (build-test-case db eid)
                                      (catch Exception e
                                        (println (format "  ⚠ eid %d: %s" eid (.getMessage e)))
                                        nil))))
                         (remove nil?)
                         (into {} (map (juxt :gv-uuid #(dissoc % :gv-uuid)))))
         ;; Merge :results-page into existing combined file (preserves :input-visibility)
         existing   (when (.exists (java.io.File. edn-path))
                      (try (edn/read-string (slurp edn-path))
                           (catch Exception _ nil)))
         combined   (assoc (or existing {}) :results-page test-cases)]
     (spit edn-path (with-out-str (pprint combined)))
     (println (format "✓ %d :results-page test cases → %s" (count test-cases) edn-path))
     {:edn-path      edn-path
      :entries-count (count test-cases)})))
