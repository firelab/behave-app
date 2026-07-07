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

(def ^:private actions-pull-spec
  "Pull pattern for a GV's actions with their raw conditionals."
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
          :conditional/group-variable-uuid]}]}]}])

(defn- pull-actions-of-type
  "Pull the actions of a given :action/type (e.g. :select, :disable) with their raw
   conditionals for a GV eid."
  [db gv-eid action-type]
  (->> (d/pull db actions-pull-spec gv-eid)
       :group-variable/actions
       (filter #(= (:action/type %) action-type))))

(defn- pull-select-actions
  "Pull the :select actions with their raw conditionals for a GV eid."
  [db gv-eid]
  (pull-actions-of-type db gv-eid :select))

(defn- pull-disable-actions
  "Pull the :disable actions with their raw conditionals for a GV eid."
  [db gv-eid]
  (pull-actions-of-type db gv-eid :disable))

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

(defn- path->components
  "Strip module (first) and io keyword from a path vector, returning display
   :name strings of the remaining elements (submodule, groups...)."
  [path]
  (when (seq path)
    (mapv :name (remove keyword? (rest path)))))

(defn- pick-first-value
  "Return the first satisfying value for :equal/:in operators, nil for :not-equal."
  [processed]
  (case (:operator processed)
    :equal (first (:values processed))
    :in    (first (:values processed))
    nil))

(defn- processed->input-req
  "Convert a processed conditional into an input requirement map, or nil.
   For :in conditionals with ≥2 values the full value list is preserved
   under :values so downstream generators can emit Scenario Outline / Examples
   tables.  :value always holds the first (representative) value so that
   conditional-evaluation and baseline-merge logic is unaffected."
  [processed raw-cond]
  (when (and (= (:type processed) :group-variable)
             (= (get-in processed [:group-variable :io]) :input)
             (not (get-in processed [:group-variable :group-variable/conditionally-set?])))
    (when-let [value (pick-first-value processed)]
      (cond-> {:gv-uuid (:conditional/group-variable-uuid raw-cond)
               :value   value
               :gv-info (:group-variable processed)}
        (and (= (:operator processed) :in)
             (>= (count (:values processed)) 2))
        (assoc :values (:values processed))))))

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
          (>= (count comps) 2)        (assoc :group (second comps))
          (:group/single-select? gv)  (assoc :group/single-select? true))))))

(defn- action->requirements
  "Process a raw :select action map into
   {:modules [...] :input-reqs [...] :output-reqs [...] :output-operator <:and|:or|nil>}.
   :output-operator carries the action's conditionals operator so downstream generators
   can tell an :or gate (any one output reveals the target) from an :and gate (all required)."
  [db action]
  (let [raw-conds (:action/conditionals action)]
    {:modules         (->> raw-conds
                           (keep #(core/process-conditional db %))
                           (filter #(= (:type %) :module))
                           (mapcat :values))
     :input-reqs      (->> raw-conds
                           (keep (fn [raw]
                                   (when-let [processed (core/process-conditional db raw)]
                                     (processed->input-req processed raw))))
                           (remove #(nil? (:value %))))
     :output-reqs     (->> raw-conds
                           (keep (fn [raw]
                                   (when-let [processed (core/process-conditional db raw)]
                                     (processed->output-req processed))))
                           (remove nil?))
     ;; uuids of the output-true conditionals — i.e. the OUTPUTS this action selects.
     ;; Lets build-test-case look up those outputs' own :disable actions.
     :output-uuids    (->> raw-conds
                           (keep (fn [raw]
                                   (when-let [p (core/process-conditional db raw)]
                                     (when (and (= (get-in p [:group-variable :io]) :output)
                                                (= (:values p) ["true"]))
                                       (:conditional/group-variable-uuid raw)))))
                           distinct)
     :output-operator (:action/conditionals-operator action)}))

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

(defn- req->row
  "Convert a requirement map to a row with submodule/group/subgroup/value plus
   VMS order fields (:module :submodule/order :group/order :group-variable/order), or nil.
   When the requirement carries :values (a full :in list) that field is forwarded
   onto the row so the feature-file generator can emit an Examples table."
  [{:keys [value values gv-info]}]
  (when-let [comps (seq (path->components (:path gv-info)))]
    (cond-> {:submodule            (first comps)
             :value                value
             :module               (:name (first (:path gv-info)))
             :submodule/order      (:submodule/order gv-info)
             :group/order          (:group/order gv-info)
             :group-variable/order (:group-variable/order gv-info)}
      (>= (count comps) 2) (assoc :group (second comps))
      (>= (count comps) 3) (assoc :subgroup (nth comps 2))
      (seq values)         (assoc :values values))))

;;; ============================================================================
;;; Input value overrides (conditionally auto-set input values)
;;; ============================================================================

(defn- find-input-value-setter-eids
  "Return eids of group-variables that have a :select action carrying an
   :action/target-value — i.e. the action auto-sets the GV's value when its
   conditionals hold (e.g. 'Wind Measured at' -> 20-Foot for spot outputs).
   Caller filters to :input GVs."
  [db]
  (d/q '[:find [?gv ...]
         :where
         [?gv :group-variable/actions ?a]
         [?a :action/type :select]
         [?a :action/target-value _]]
       db))

(defn- pull-select-target-actions
  "Pull a GV's :select actions that carry an :action/target-value."
  [db gv-eid]
  (->> (d/pull db
               '[{:group-variable/actions
                  [:action/type
                   :action/target-value
                   :action/conditionals-operator
                   {:action/conditionals
                    [:conditional/type
                     :conditional/operator
                     :conditional/values
                     :conditional/group-variable-uuid]}]}]
               gv-eid)
       :group-variable/actions
       (filter #(and (= (:action/type %) :select) (:action/target-value %)))))

(defn- action-fires?
  "True when a value-setting :select action's conditionals are satisfied by the
   test's active output names and module-name set (mirrors the app: :module is
   set-equality/intersection; an output conditional passes when its translated
   name is among the selected outputs)."
  [db action active-output-names module-name-set]
  (let [conds (:action/conditionals action)
        op    (:action/conditionals-operator action)
        res   (map (fn [c]
                     (case (:conditional/type c)
                       :module
                       (case (:conditional/operator c)
                         :equal (= (set (:conditional/values c)) module-name-set)
                         :in    (boolean (some module-name-set (:conditional/values c)))
                         false)
                       :group-variable
                       (boolean
                        (when-let [p (core/process-conditional db c)]
                          (and (= (get-in p [:group-variable :io]) :output)
                               (= (:conditional/values c) ["true"])
                               (contains? active-output-names
                                          (get-in p [:group-variable :group-variable/translated-name])))))
                       false))
                   conds)]
    (if (= op :or) (boolean (some true? res)) (every? true? res))))

(defn- compute-input-value-overrides
  "For each candidate input value-setter GV, if one of its value-setting :select
   actions fires for the test's active outputs, emit an override row
   {:submodule :group [:subgroup] :value} with the action's target-value resolved
   to a display value via the GV's list options. Returns a deduped vec."
  [db setter-eids active-output-names module-name-set]
  (->> setter-eids
       (keep (fn [eid]
               (when-let [uuid (:bp/uuid (d/pull db '[:bp/uuid] eid))]
                 (let [info (core/resolve-group-variable-uuid db uuid)]
                   (when (= (:io info) :input)
                     (when-let [act (first (filter #(action-fires? db % active-output-names module-name-set)
                                                   (pull-select-target-actions db eid)))]
                       (let [comps (path->components (:path info))
                             val   (get (core/get-variable-list-options db uuid)
                                        (:action/target-value act))]
                         (when (and (>= (count comps) 2) val)
                           (cond-> {:submodule (first comps)
                                    :group     (second comps)
                                    :value     val}
                             (>= (count comps) 3) (assoc :subgroup (nth comps 2)))))))))))
       distinct
       vec))

;;; ============================================================================
;;; Default (auto-selected) outputs — seed-only
;;; ============================================================================

(defn- find-output-select-gv-eids
  "Return eids of group-variables that have at least one :select action. These
   are candidates for a worksheet's auto-default outputs (the app auto-selects an
   output when a :select action's conditionals pass on a fresh worksheet — see
   process-output-actions->fx). Caller filters to :output GVs."
  [db]
  (d/q '[:find [?gv ...]
         :where
         [?gv :group-variable/actions ?a]
         [?a :action/type :select]]
       db))

(defn- output-default-candidates
  "Resolve every :output GV that has a :select action into a candidate row
   {:module :submodule :group [:subgroup] :value <translated-name>
    :actions <raw :select actions>}. Computed once per generation run and filtered
   per module-combo by default-outputs-for-combo."
  [db]
  (->> (find-output-select-gv-eids db)
       (keep (fn [eid]
               (when-let [uuid (:bp/uuid (d/pull db '[:bp/uuid] eid))]
                 (let [info (core/resolve-group-variable-uuid db uuid)]
                   (when (and info (= (:io info) :output))
                     (when-let [comps (seq (path->components (:path info)))]
                       (cond-> {:module    (:name (first (:path info)))
                                :submodule (first comps)
                                :value     (:group-variable/translated-name info)
                                :actions   (pull-select-actions db eid)}
                         (>= (count comps) 2) (assoc :group (second comps))
                         (>= (count comps) 3) (assoc :subgroup (nth comps 2)))))))))
       vec))

(defn- default-outputs-for-combo
  "Given the effective worksheet module-name set (lower-cased strings, e.g.
   #{\"surface\" \"contain\"}), return the outputs the app auto-selects on a fresh
   worksheet: candidate :output GVs whose module is in the combo and one of whose
   :select actions fires with NO outputs yet selected (so only :module-type
   conditionals can pass — mirrors process-output-actions->fx on worksheet start).
   Rows are {:module :submodule :group [:subgroup] :value}; :value is the
   translated output name. Seed-only — never rendered as a selected output."
  [db candidates module-name-set]
  (->> candidates
       (filter (fn [{:keys [module actions]}]
                 (and (contains? module-name-set (some-> module str/lower-case))
                      (some #(action-fires? db % #{} module-name-set) actions))))
       (map #(select-keys % [:module :submodule :group :subgroup :value]))
       distinct
       vec))

;;; ============================================================================
;;; Per-GV test-case builder
;;; ============================================================================

(defn- build-test-case
  "Return a test-case map for an output gv-eid, or nil if the GV is research,
   not an output, or its action conditionals cannot be resolved.
   setter-eids = candidate input value-setter GV eids (from
   find-input-value-setter-eids), used to derive :input-value-overrides.
   output-candidates = auto-default output candidates (from
   output-default-candidates), used to derive :default-outputs."
  [db gv-eid setter-eids output-candidates]
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
                  output-reqs     (vec (distinct (mapcat :output-reqs all-action-reqs)))
                  ;; Operator of the action that actually contributed the output gate
                  ;; (:or = any one output reveals the target; :and = all required).
                  out-operator    (->> all-action-reqs
                                       (filter #(seq (:output-reqs %)))
                                       first
                                       :output-operator)]
              ;; Proceed if there are any requirements at all (input OR output)
              (when (or (seq initial-reqs) (seq output-reqs) (seq modules))
                (let [all-reqs            (if (seq initial-reqs)
                                            (resolve-transitive-closure db initial-reqs)
                                            [])
                      rows                (vec (keep req->row all-reqs))
                      module-kw           (or (first (map keyword modules))
                                              (some-> gv-info :path first :name str/lower-case keyword))
                      ;; Outputs that, when selected, DISABLE one of the outputs this test
                      ;; must click (its gating outputs — e.g. "Burning Pile" — plus the
                      ;; target). Each such output GV carries its own :disable action whose
                      ;; conditionals name the outputs that disable it. Downstream
                      ;; (merge-with-baselines) drops any baseline output in this set so the
                      ;; prerequisite baseline never disables an output the test selects.
                      selected-out-uuids  (vec (distinct (mapcat :output-uuids all-action-reqs)))
                      disabling-outputs   (->> (cons gv-uuid selected-out-uuids)
                                               (mapcat (fn [u]
                                                         (->> (pull-disable-actions db [:bp/uuid u])
                                                              (mapcat #(:output-reqs (action->requirements db %))))))
                                               (map #(select-keys % [:module :submodule :group :value]))
                                               distinct
                                               vec)
                      ;; Some inputs are conditionally auto-set by the app via a
                      ;; value-setting :select action (e.g. "Wind Measured at" -> 20-Foot
                      ;; for spot outputs). Derive those overrides from the test's active
                      ;; outputs so merge-with-baselines applies the correct value instead
                      ;; of the (surface-fire) baseline default.
                      output-name         (or (:group-variable/translated-name gv-info)
                                              "Unknown Output")
                      active-output-names (into #{} (conj (mapv :value output-reqs) output-name))
                      module-name-set     (into #{} (or (seq modules) ["surface"]))
                      input-overrides     (compute-input-value-overrides
                                           db setter-eids active-output-names module-name-set)
                      ;; Effective worksheet module set: Crown/Mortality/Contain always
                      ;; promote to include Surface (mirrors effective-module-combo). Used to
                      ;; derive the outputs the app auto-defaults on a fresh worksheet.
                      effective-mns       (let [base (into #{} (map str/lower-case)
                                                           (or (seq modules) [(name module-kw)]))]
                                            (if (some #{"crown" "mortality" "contain"} base)
                                              (conj base "surface")
                                              base))
                      ;; Auto-defaulted outputs (e.g. Rate of Spread / Flame Length for a
                      ;; Surface & Contain worksheet). Seed-only: they un-gate baseline inputs
                      ;; in merge-with-baselines but are never rendered as selected outputs.
                      default-outputs     (default-outputs-for-combo db output-candidates effective-mns)]
                  {:gv-uuid                   gv-uuid
                   :output-name               output-name
                   :module                    (some-> module-kw name str/capitalize)
                   :required-modules          modules
                   :required-outputs          output-reqs
                   :required-outputs-operator out-operator
                   :required-inputs           rows
                   :disabling-outputs         disabling-outputs
                   :input-value-overrides     input-overrides
                   :default-outputs           default-outputs})))))))))

;;; ============================================================================
;;; Main entry point
;;; ============================================================================

(defn generate-conditional-outputs-matrix!
  "Generate the :results-visibility section of the combined test_matrix_data.edn.

   Finds every output group-variable with :group-variable/conditionally-set? true
   and at least one :select action, resolves the full transitive chain of required
   inputs, and merges the :results-visibility section into the combined EDN file.

   Arguments:
   - db       — Datomic database value (from d/db)
   - edn-path — (optional) combined matrix path; default 'development/test_matrix_data.edn'

   Returns:
   {:edn-path '...' :entries-count N}"
  ([db]
   (generate-conditional-outputs-matrix! db "development/test_matrix_data.edn"))
  ([db edn-path]
   (let [gv-eids     (find-conditionally-set-output-gvs db)
         _           (println (format "Found %d conditionally-set output GVs" (count gv-eids)))
         setter-eids (find-input-value-setter-eids db)
         candidates  (output-default-candidates db)
         test-cases  (->> gv-eids
                          (keep (fn [eid]
                                  (try (build-test-case db eid setter-eids candidates)
                                       (catch Exception e
                                         (println (format "  ⚠ eid %d: %s" eid (.getMessage e)))
                                         nil))))
                          (remove nil?)
                          (into {} (map (juxt :gv-uuid #(dissoc % :gv-uuid)))))
         ;; Merge :results-visibility into existing combined file (preserves :input-visibility).
         ;; select-keys strips any legacy bare path-vector keys that may have accumulated
         ;; from the old flat format, keeping only the two canonical keyword sections.
         existing    (when (.exists (java.io.File. edn-path))
                       (try (edn/read-string (slurp edn-path))
                            (catch Exception _ nil)))
         combined    (assoc (select-keys (or existing {}) [:input-visibility :results-visibility])
                            :results-visibility test-cases)]
     (spit edn-path (with-out-str (pprint combined)))
     (println (format "✓ %d :results-visibility test cases → %s" (count test-cases) edn-path))
     {:edn-path      edn-path
      :entries-count (count test-cases)})))
