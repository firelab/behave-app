(ns cucumber-test-generator.generate-scenarios
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.math.combinatorics :as combo]))

;; ===========================================================================================================
;; Gherkin Indentation Constants
;; ===========================================================================================================

(def ^:const SCENARIO-INDENT "  ") ; 2 spaces for Scenario line
(def ^:const STEP-INDENT "    ") ; 4 spaces for Given/When/Then/And
(def ^:const DOCSTRING-INDENT "      ") ; 6 spaces for """ and content

;; ===========================================================================================================
;; Utility Functions
;; ===========================================================================================================

(defn collect-parent-groups
  "Given a path like [\"Surface\" \"Fuel Moisture\" \"By Size Class\" \"Live Woody Fuel Moisture\"],
   return all parent group paths in order from module/submodule to leaf.

   Skips module (first element) and submodule (second element) in intermediate paths,
   but includes them in final paths for lookup.

   Example input:  [\"Surface\" \"Fuel Moisture\" \"By Size Class\" \"Live Woody Fuel Moisture\"]
   Example output: [[\"Surface\" \"Fuel Moisture\"]
                    [\"Surface\" \"Fuel Moisture\" \"By Size Class\"]
                    [\"Surface\" \"Fuel Moisture\" \"By Size Class\" \"Live Woody Fuel Moisture\"]]

   Arguments:
   - path: Vector of path elements [Module Submodule Groups...]

   Returns:
   Sequence of parent paths, or nil if path has 2 or fewer elements"
  [path]
  (when (> (count path) 2)
    (let [module     (take 1 path)
          rest-path  (drop 1 path)
          num-groups (count rest-path)]
      (-> (for [i (range 1 num-groups)]
            (vec (concat module (take i rest-path))))
          rest))))

(defn get-ancestors
  "Get all ancestor entities for a given path by looking them up in the data map.

   Arguments:
   - all-data: Map with path vectors as keys, entity info as values
   - entity-path: Path vector like ['Crown' 'Spot' :input 'Torching Trees']

   Returns:
   Vector of ancestor entity maps (submodules and parent groups)"
  [all-data entity-path]
  (let [parent-paths (collect-parent-groups entity-path)]
    (keep #(get all-data %) parent-paths)))

(defn collect-all-ancestral-entities
  "Recursively collect all entities referenced by conditionals' group-variable paths.

   When a conditional references a path like [\"Surface\" \"Fire Behavior\" :output \"Direction Mode\"],
   this function:
   1. Extracts ancestral paths (e.g., [\"Surface\" \"Fire Behavior\" :output])
   2. Looks them up in all-data map
   3. For any found entities with conditionals, recursively processes those too
   4. Returns all ancestral entities, deduplicated by :path

   Arguments:
   - all-data: Map with path vectors as keys, entity info as values
   - conditionals: Sequence of conditionals to process

   Returns:
   Vector of ancestral entity maps (deduplicated by :path)"
  [all-data conditionals]
  (letfn [(collect-from-conditional [cond seen-paths]
            (let [path (get-in cond [:group-variable :path])]
              (if (and path (not (contains? seen-paths path)))
                ;; Extract ancestral paths from this conditional's path
                (let [ancestral-paths    (collect-parent-groups path)
                      ;; Also check if the path itself exists (when it's a 3-element path with no parents)
                      direct-path-entity (get all-data path)
                      ;; Look up ancestral entities in all-data
                      ancestral-entities (keep #(get all-data %) ancestral-paths)
                      ;; Combine direct path entity (if exists) with ancestral entities
                      all-found-entities (if direct-path-entity
                                           (cons direct-path-entity ancestral-entities)
                                           ancestral-entities)
                      ;; Mark this path as seen
                      updated-seen       (conj seen-paths path)
                      ;; Recursively process conditionals from all found entities
                      nested-results     (mapcat
                                          (fn [entity]
                                            (when-let [entity-conds (get-in entity [:conditionals :conditionals])]
                                              (collect-from-conditional-seq entity-conds updated-seen)))
                                          all-found-entities)]
                  ;; Return all found entities plus any nested results
                  (concat all-found-entities nested-results))
                ;; No path or already seen, return empty
                [])))
          (collect-from-conditional-seq [cond-seq seen-paths]
            (mapcat #(collect-from-conditional % seen-paths) cond-seq))]
    ;; Start with empty seen set
    (let [all-entities (collect-from-conditional-seq conditionals #{})]
      ;; Deduplicate by path
      (vec (vals (into {} (map (juxt :path identity) all-entities)))))))

(defn find-group-by-path
  "Find a group/submodule in the data map by its path.
   Returns the entity map or nil if not found.

   Arguments:
   - all-data: Map with path vectors as keys, entity info as values
   - path: Path vector to match

   Returns:
   Entity map or nil"
  [all-data path]
  (get all-data path))

;; ===========================================================================================================
;; EDN Loading Functions (Task 5.2)
;; ===========================================================================================================

(defn load-test-matrix
  "Read and parse test_matrix_data.edn file.

   Arguments:
   - edn-path (optional): Path to input EDN file
     Default: 'development/test_matrix_data.edn'

   Returns:
   Map with path vectors as keys, entity info as values

   Throws:
   Exception if file doesn't exist or is malformed"
  ([]
   (load-test-matrix "development/test_matrix_data.edn"))
  ([edn-path]
   (try
     (let [file (io/file edn-path)]
       (when-not (.exists file)
         (throw (ex-info (str "EDN file not found: " edn-path)
                         {:edn-path edn-path})))
       (let [content (slurp edn-path)
             data    (edn/read-string content)]
         (when-not (map? data)
           (throw (ex-info "Malformed EDN: expected a map with path keys"
                           {:edn-path edn-path})))
         data))
     (catch Exception e
       (throw (ex-info (str "Failed to load test matrix: " (.getMessage e))
                       {:edn-path edn-path}
                       e))))))

;; ===========================================================================================================
;; Path Formatting Functions (Task 5.3)
;; ===========================================================================================================

(defn format-path-for-gherkin
  "Convert path vector to Gherkin-style format.
   Skips first element (module name) and :io keywords, joins remaining with ' -> '.

   Arguments:
   - path: Vector like [\"Surface\" \"Fuel Moisture\" :input \"By Size Class\"]

   Returns:
   String like \"Fuel Moisture -> By Size Class\"

   Examples:
   [\"Surface\" \"Fuel Moisture\" :input \"By Size Class\"] -> \"Fuel Moisture -> By Size Class\"
   [\"Crown\" \"Spot\" :output] -> \"Spot\"
   [\"Surface\" \"Wind and Slope\" :input \"Wind Adjustment Factor\"] -> \"Wind and Slope -> Wind Adjustment Factor\""
  [path]
  (when (seq path)
    (let [;; Skip first element (module) and filter out keywords (:input/:output)
          without-module-and-io (remove keyword? (rest path))]
      (str/join " -> " without-module-and-io))))

(defn format-output-line
  "Format path as output line with '-- ' prefix for Gherkin steps.

   Arguments:
   - path: Vector like [\"Surface\" \"Fire Behavior\" \"Direction Mode\"]

   Returns:
   String like \"-- Fire Behavior -> Direction Mode\"

   Used in 'When these outputs are selected' steps."
  [path]
  (str "-- " (format-path-for-gherkin path)))

(defn format-input-value-line
  "Format path and value as input value line with '--- ' prefix.

   Arguments:
   - path: Vector like [\"Surface\" \"Fuel Model\" \"Standard\" \"Fuel Model\"]
   - value: String like \"FB2/2 - Timber grass\"

   Returns:
   String like \"--- Fuel Model -> Standard -> Fuel Model -> FB2/2 - Timber grass\"

   Used in 'And these inputs are entered' steps."
  [path value]
  (str "-- " (format-path-for-gherkin path) " -> " value))

(defn format-input-group-line
  "Format path as input group line with '-- ' prefix for Gherkin steps.

   Arguments:
   - path: Vector like [\"Surface\" \"Fuel Moisture\" \"By Size Class\"]

   Returns:
   String like \"-- Fuel Moisture -> By Size Class\"

   Used in 'Then these input groups are displayed' steps."
  [path]
  (str "-- " (format-path-for-gherkin path)))

;; ===========================================================================================================
;; Conditional Categorization Functions (Task 5.4)
;; ===========================================================================================================

(defn categorize-conditional
  "Identify the pattern type of a conditional.

   Checks :type, :operator, :values, and :io to determine the pattern.

   Arguments:
   - conditional: Conditional map with :type, :operator, :values, and optionally :group-variable

   Returns:
   Keyword indicating pattern type:
   - :output-selected - Output with values=[\"true\"], io=:output
   - :output-not-selected - Output with values=[\"false\"], io=:output
   - :input-value - Input with specific value(s), io=:input
   - :module-enabled - Module-level conditional, type=:module
   - :compound - Has :sub-conditionals
   - :skip - Cannot be categorized (should be filtered)"
  [conditional]
  (let [cond-type            (:type conditional)
        values               (:values conditional)
        io                   (get-in conditional [:group-variable :io])
        has-sub-conditionals (seq (:sub-conditionals conditional))]

    (cond
      ;; Compound scenario with sub-conditionals
      has-sub-conditionals
      :compound

      ;; Module-level conditional
      (= cond-type :module)
      :module-enabled

      ;; Output selected (true)
      (and (= io :output) (= values ["true"]))
      :output-selected

      ;; Output NOT selected (false)
      (and (= io :output) (= values ["false"]))
      :output-not-selected

      ;; Input with specific value
      (= io :input)
      :input-value

      ;; Unknown/unsupported pattern
      :else
      :skip)))

(defn determine-scenario-pattern
  "Categorize a group's conditional set and determine scenario pattern.

   Analyzes the conditionals operator and individual conditional types
   to determine how scenarios should be generated.

   Arguments:
   - group: Group map with :conditionals containing :conditionals and :conditionals-operator

   Returns:
   Keyword indicating pattern:
   - :output-enables-input - Simple output selection pattern
   - :mixed-output-enables-input - Mix of selected and NOT selected outputs
   - :input-value-enables-input - Input value determines visibility
   - :compound-scenario - Has sub-conditionals
   - :or-branches - OR operator, needs separate scenarios per branch"
  [group]
  (let [conditionals-info       (:conditionals group)
        conditionals            (:conditionals conditionals-info)
        operator                (:conditionals-operator conditionals-info)
        categories              (map categorize-conditional conditionals)
        has-compound            (some #(= :compound %) categories)
        has-output-selected     (some #(= :output-selected %) categories)
        has-output-not-selected (some #(= :output-not-selected %) categories)
        has-input-value         (some #(= :input-value %) categories)]

    (cond
      ;; Compound scenarios with sub-conditionals
      has-compound
      :compound-scenario

      ;; OR operator needs separate scenarios
      (= operator :or)
      :or-branches

      ;; Mixed outputs (some selected, some NOT selected)
      (and has-output-selected has-output-not-selected)
      :mixed-output-enables-input

      ;; Input value conditional
      has-input-value
      :input-value-enables-input

      ;; Simple output selection
      has-output-selected
      :output-enables-input

      ;; Default
      :else
      :output-enables-input)))

;; ===========================================================================================================
;; Module Detection Functions (Task 5.5)
;; ===========================================================================================================

(defn extract-module-from-path
  "Extract module keyword from first path element.

   Arguments:
   - path: Vector like [\"Surface\" \"Fuel\" \"Standard\"]

   Returns:
   Keyword like :surface, :crown, :mortality, :contain

   Examples:
   [\"Surface\" ...] -> :surface
   [\"Crown\" ...] -> :crown
   [\"Mortality\" ...] -> :mortality
   [\"Contain\" ...] -> :contain"
  [path]
  (when-let [module-name (first path)]
    (keyword (str/lower-case module-name))))

(defn collect-all-paths-from-conditionals
  "Recursively collect all paths from conditionals, including sub-conditionals and ancestors.

   Arguments:
   - all-data: Map with path vectors as keys, entity info as values
   - group: Group map with :conditionals

   Returns:
   Set of all paths found in the group's conditionals and ancestors"
  [all-data group]
  (letfn [(collect-from-conditional [cond]
            (let [path      (get-in cond [:group-variable :path])
                  sub-paths (when-let [subs (:sub-conditionals cond)]
                              (mapcat collect-from-conditional subs))]
              (if path
                (cons path sub-paths)
                sub-paths)))
          (collect-from-conditionals-info [conds-info]
            (when-let [conds (:conditionals conds-info)]
              (mapcat collect-from-conditional conds)))]

    (let [main-paths     (collect-from-conditionals-info (:conditionals group))
          ancestors      (get-ancestors all-data (:path group))
          ancestor-paths (mapcat collect-from-conditionals-info
                                 (map :conditionals ancestors))]
      (set (concat main-paths ancestor-paths)))))

(defn extract-modules-from-paths
  "Extract unique module keywords from a collection of paths.

   Arguments:
   - paths: Set/sequence of path vectors

   Returns:
   Set of module keywords like #{:surface :crown}"
  [paths]
  (set (keep extract-module-from-path paths)))

(defn extract-modules-from-module-conditionals
  "Find module keywords from conditionals with :type :module.

   Arguments:
   - all-data: Map with path vectors as keys, entity info as values
   - group: Group map with :conditionals

   Returns:
   Set of module keywords"
  [all-data group]
  (letfn [(find-module-conds [conds-info]
            (when-let [conds (:conditionals conds-info)]
              (for [cond  conds
                    :when (= (:type cond) :module)
                    value (:values cond)]
                (keyword value))))]

    (let [main-modules     (find-module-conds (:conditionals group))
          ancestors        (get-ancestors all-data (:path group))
          ancestor-modules (mapcat find-module-conds
                                   (map :conditionals ancestors))]
      (set (concat main-modules ancestor-modules)))))

(defn determine-module-combination
  "Determine module combination from a set of modules.

   Maps sets of modules to their corresponding module combination sets.
   Returns set of modules for valid combinations, or :unsupported for invalid ones.

   Valid combinations:
   - Single surface: #{:surface}
   - Surface & Crown: #{:surface :crown}
   - Surface & Mortality: #{:surface :mortality}
   - Surface & Contain: #{:surface :contain}

   Note: Mortality and Contain always require Surface to be included.

   Arguments:
   - modules: Set of module keywords (e.g., #{:surface :crown})

   Returns:
   Set of modules for valid combinations, or :unsupported keyword for invalid"
  [modules]
  (let [module-count (count modules)]
    (cond
      ;; Single modules
      (= modules #{:surface})   #{:surface}
      (= modules #{:crown})     #{:surface :crown} ;Always includes surface
      (= modules #{:mortality}) #{:surface :mortality} ; Always includes surface
      (= modules #{:contain})   #{:surface :contain} ; Always includes surface

      ;; Two module combinations
      (= modules #{:surface :crown})     #{:surface :crown}
      (= modules #{:surface :mortality}) #{:surface :mortality}
      (= modules #{:surface :contain})   #{:surface :contain}

      ;; Unsupported combinations (3+ modules or invalid pairs)
      (>= module-count 3) :unsupported
      :else               :unsupported)))

(defn module-set-to-string
  "Convert module set to kebab-case string for filenames.
   Sorts modules alphabetically for consistency.

   Arguments:
   - module-set: Set of module keywords (e.g., #{:surface :crown})

   Returns:
   String representation for filenames

   Examples:
   #{:surface} -> \"surface\"
   #{:surface :crown} -> \"crown-surface\"
   #{:surface :mortality} -> \"mortality-surface\""
  [module-set]
  (str/join "-" (sort (map name module-set))))

(defn module-set-to-title
  "Convert module set to human-readable title with capitalization.

   Arguments:
   - module-set: Set of module keywords (e.g., #{:surface :crown})

   Returns:
   String representation for titles

   Examples:
   #{:surface} -> \"Surface\"
   #{:surface :crown} -> \"Crown & Surface\"
   #{:surface :mortality} -> \"Mortality & Surface\""
  [module-set]
  (let [sorted-modules (sort (map name module-set))
        capitalized    (map str/capitalize sorted-modules)]
    (str/join " & " capitalized)))

(defn module-to-given-statement
  "Generate Gherkin 'Given' statement from module combination.

   Maps module combination sets to their corresponding worksheet initialization statements.

   Arguments:
   - module-combo: Set of modules (e.g., #{:surface :crown}) or :unsupported keyword

   Returns:
   String containing the 'Given' step for starting the appropriate worksheet"
  [module-combo]
  (cond
    (= module-combo #{:surface})
    "Given I have started a new Surface Worksheet in Guided Mode"

    (= module-combo #{:crown})
    "Given I have started a new Crown Worksheet in Guided Mode"

    (= module-combo #{:surface :crown})
    "Given I have started a new Surface & Crown Worksheet in Guided Mode"

    (= module-combo #{:surface :mortality})
    "Given I have started a new Surface & Mortality Worksheet in Guided Mode"

    (= module-combo #{:surface :contain})
    "Given I have started a new Surface & Contain Worksheet in Guided Mode"

    ;; Default for unsupported or unknown
    :else
    (str "Given I have started a new " (module-set-to-title module-combo) " Worksheet in Guided Mode")))

;; ===========================================================================================================
;; Research Filtering Functions (Task 5.6)
;; ===========================================================================================================

(defn has-research-dependency?
  "Check if a conditional references research variables.

   Recursively checks :group-variable/research?, :submodule/research?,
   and sub-conditionals.

   Arguments:
   - conditional: Conditional map

   Returns:
   Boolean true if any research flag is true"
  [conditional]
  (let [gv-research?        (get-in conditional [:group-variable :group-variable/research?])
        submodule-research? (get-in conditional [:group-variable :submodule/research?])
        sub-conds           (:sub-conditionals conditional)]

    (or (true? gv-research?)
        (true? submodule-research?)
        (when (seq sub-conds)
          (some has-research-dependency? sub-conds)))))

(defn should-skip-group?
  "Determine if a group should be excluded from feature generation.

   Checks for research dependencies in:
   - Group's :group/research? field

   Note: Does NOT check individual conditionals for research - those will be filtered
   during scenario generation to remove research items while keeping non-research alternatives.

   Arguments:
   - group: Group map with :conditionals and optional :ancestors

   Returns:
   Boolean true if group should be skipped"
  [group]
  (let [group-research? (:group/research? group)]
    (true? group-research?)))

(defn has-only-module-conditionals?
  "Check if group has ONLY module-type conditionals for contain/crown/mortality.

   Skip groups that have:
   - ALL conditionals are module-type (:type = :module)
   - AND any module value is 'contain', 'crown', or 'mortality'

   Do NOT skip if:
   - Group has any group-variable conditionals (outputs/inputs)
   - Module is 'surface' (surface-only scenarios are kept)

   Arguments:
   - group: Group map with :conditionals and :ancestors

   Returns:
   Boolean true if group should be skipped"
  [group]
  (let [;; Collect all conditionals from group and ancestors
        group-conds    (get-in group [:conditionals :conditionals] [])
        ancestor-conds (mapcat #(get-in % [:conditionals :conditionals] [])
                               (:ancestors group []))
        all-conds      (concat group-conds ancestor-conds)

        ;; Check if ALL conditionals are module-type
        all-module-type? (every? #(= :module (:type %)) all-conds)

        ;; Check if any module is contain/crown/mortality
        skip-modules     #{"contain" "crown" "mortality"}
        has-skip-module? (boolean (some (fn [cond]
                                          (and (= :module (:type cond))
                                               (some skip-modules (:values cond))))
                                        all-conds))]

    (and all-module-type? has-skip-module?)))

(defn should-skip-submodule?
  "Determine if a submodule should be excluded from feature generation.

   Checks for research dependencies in:
   - Submodule's :submodule/research? field
   - Module-only conditionals (contain/crown/mortality)

   Note: Does NOT check individual conditionals for research - those will be filtered
   during scenario generation to remove research items while keeping non-research alternatives.

   Arguments:
   - submodule: Submodule map with :conditionals and optional :ancestors

   Returns:
   Boolean true if submodule should be skipped"
  [submodule]
  (let [submodule-research? (:submodule/research? submodule)]
    (or (true? submodule-research?)
        (has-only-module-conditionals? submodule))))

;; ===========================================================================================================
;; Module Compatibility Filtering Functions (Task 5.7)
;; ===========================================================================================================

(defn is-path-compatible-with-modules?
  "Check if a path's module is compatible with active modules.

   Arguments:
   - path: Path vector like [\"Surface\" \"Fuel\" \"Standard\"]
   - active-modules: Set of active module keywords like #{:surface :crown}, or :unsupported

   Returns:
   Boolean true if path's module is in active modules"
  [path active-modules]
  (if (= active-modules :unsupported)
    false
    (when-let [path-module (extract-module-from-path path)]
      (contains? active-modules path-module))))

(defn filter-conditionals-by-module
  "Remove conditionals with modules incompatible with active modules.

   Recursively filters :sub-conditionals as well.

   Arguments:
   - conditionals: Sequence of conditional maps
   - active-modules: Set of active module keywords

   Returns:
   Filtered sequence of conditionals"
  [conditionals active-modules]
  (keep (fn [cond]
          (let [path        (get-in cond [:group-variable :path])
                compatible? (if path
                              (is-path-compatible-with-modules? path active-modules)
                              true) ; module-type conditionals don't have paths
                sub-conds   (:sub-conditionals cond)]

            (when compatible?
              (if (seq sub-conds)
                ;; Recursively filter sub-conditionals
                (assoc cond :sub-conditionals
                       (filter-conditionals-by-module sub-conds active-modules))
                cond))))
        conditionals))

;; ===========================================================================================================
;; Ancestor Expansion Logic (Task 6.2)
;; ===========================================================================================================

(defn flatten-conditional-to-paths
  "Recursively flatten a single conditional into all possible paths via DFS.

   Handles nested sub-conditionals with :or operators by creating separate paths.
   Handles :in operator with multiple values by optimizing to first value only.

   Arguments:
   - conditional: A conditional map with optional :sub-conditionals

   Returns:
   Sequence of paths, where each path is a sequence of conditionals.

   Examples:
   - Simple conditional (no subs): [[conditional]]
   - Conditional with :in [v1 v2]: [[cond-with-v1]] (optimized to first value)
   - Conditional with sub-OR [s1 s2]: [[cond s1] [cond s2]]"
  [conditional]
  (let [sub-conditionals (:sub-conditionals conditional)
        sub-operator     (:sub-conditional-operator conditional)
        values           (:values conditional)
        operator         (:operator conditional)
        ;; Apply optimization for :in operator - use only first value
        optimized-values (if (and (= operator :in) (> (count values) 1))
                           [(first values)]
                           values)]

    (cond
      ;; No sub-conditionals: return the conditional with optimized values
      (empty? sub-conditionals)
      [[(assoc conditional :values optimized-values)]]

      ;; Sub-conditionals with :or operator - create separate branches
      (= sub-operator :or)
      (let [;; Recursively flatten each sub-conditional
            sub-paths   (map flatten-conditional-to-paths sub-conditionals)
            ;; Parent conditional without sub-conditionals, with optimized values
            parent-cond (-> conditional
                            (dissoc :sub-conditionals :sub-conditional-operator)
                            (assoc :values optimized-values))]
        ;; Create paths combining parent with each sub-path
        (for [sub-path (apply concat sub-paths)]
          (cons parent-cond sub-path)))

      ;; Sub-conditionals with :and or nil - keep in same path
      :else
      (let [;; Recursively flatten each sub-conditional
            sub-paths        (map flatten-conditional-to-paths sub-conditionals)
            ;; Create cartesian product of all sub-paths
            sub-combinations (apply combo/cartesian-product sub-paths)
            ;; Parent conditional without sub-conditionals, with optimized values
            parent-cond      (-> conditional
                                 (dissoc :sub-conditionals :sub-conditional-operator)
                                 (assoc :values optimized-values))]
        ;; Create paths combining parent with each sub-combination
        (for [sub-combo sub-combinations
              :let      [flattened-subs (apply concat sub-combo)]]
          (cons parent-cond flattened-subs))))))

(defn expand-or-conditionals
  "Expand conditionals into all possible paths using DFS to handle nested sub-conditionals.

   For :or operator: each conditional creates separate branches.
   For :and or nil: conditionals are combined via cartesian product.
   Recursively handles nested sub-conditionals with :or operators.

   IMPORTANT: Filters out malformed conditionals that are missing required fields.
   A valid conditional must have either:
   - :type :module (module conditionals), OR
   - :type :group-variable with a non-nil :group-variable map (output/input conditionals)

   Arguments:
   - conditionals-info: Map with :conditionals and :conditionals-operator

   Returns:
   Sequence of paths, where each path is a flat sequence of conditionals"
  [conditionals-info]
  (let [operator     (:conditionals-operator conditionals-info)
        conditionals (:conditionals conditionals-info)

        ;; Filter out malformed conditionals
        valid-conditionals (filter
                            (fn [cond]
                              (or
                               ;; Module conditionals are valid (have :type :module)
                               (= (:type cond) :module)
                               ;; Group-variable conditionals must have :group-variable map
                               (and (= (:type cond) :group-variable)
                                    (some? (:group-variable cond)))))
                            conditionals)]
    (if (= operator :or)
      ;; :or operator - create separate branch for each conditional
      ;; Each conditional may have nested sub-conditionals that need flattening
      (mapcat flatten-conditional-to-paths valid-conditionals)
      ;; :and operator or nil - combine all conditionals
      ;; Create cartesian product of all conditional paths
      (let [all-paths (map flatten-conditional-to-paths valid-conditionals)]
        (if (empty? all-paths)
          [[]] ; no conditionals
          (let [combinations (apply combo/cartesian-product all-paths)]
            (map #(apply concat %) combinations)))))))

(defn conditionals-equal?
  "Check if two conditionals are equivalent.

   Compares type, operator, values, group-variable path, and translated-name
   to determine equality. This is crucial for detecting overlapping conditionals
   across different ancestors.

   The translated-name comparison is essential to distinguish between different
   outputs in the same group (e.g., 'Flame Length' vs 'Fireline Intensity' both
   in the 'Surface Fire' group)."
  [cond1 cond2]
  (and (= (:type cond1) (:type cond2))
       (= (:operator cond1) (:operator cond2))
       (= (set (:values cond1)) (set (:values cond2)))
       (= (get-in cond1 [:group-variable :path])
          (get-in cond2 [:group-variable :path]))
       ;; Also compare the translated-name to distinguish different variables in the same group
       (= (get-in cond1 [:group-variable :group-variable/translated-name])
          (get-in cond2 [:group-variable :group-variable/translated-name]))))

(defn find-overlapping-conditionals
  "Find conditionals that appear in multiple ancestors' branches.

   Analyzes all expanded branches from all ancestors to identify which conditionals
   are shared across different ancestors' :or lists. Uses conditional equality rather
   than identity to detect overlaps.

   IMPORTANT: Excludes :module type conditionals from overlap detection. Module
   conditionals represent different worksheet contexts (e.g., Surface vs. Contain+Surface)
   and should always be included in cartesian products, not deduplicated.

   Arguments:
   - expanded-branches: Sequence of sequences, one per ancestor, where each inner
                        sequence contains all possible branches for that ancestor

   Returns:
   Map of {conditional → #{ancestor-index-0 ancestor-index-1 ...}}
   Only includes conditionals that appear in 2+ ancestors"
  [expanded-branches]
  (let [;; First, collect all unique conditionals by value (using conditionals-equal?)
        ;; Build a map from "canonical" conditional to ancestor indices
        conditional-to-ancestors
        (reduce
         (fn [acc [ancestor-idx branches]]
           (reduce
            (fn [acc2 branch]
              (reduce
               (fn [acc3 conditional]
                 ;; Skip :module type conditionals - they should not be deduplicated
                 (if (= (:type conditional) :module)
                   acc3
                   ;; Find if this conditional already exists (by value equality)
                   (let [existing-key (some (fn [[k _v]]
                                              (when (conditionals-equal? k conditional)
                                                k))
                                            acc3)]
                     (if existing-key
                       ;; Use existing key
                       (update acc3 existing-key (fnil conj #{}) ancestor-idx)
                       ;; New conditional
                       (assoc acc3 conditional #{ancestor-idx})))))
               acc2
               branch))
            acc
            branches))
         {}
         (map-indexed vector expanded-branches))]
    ;; Filter to only overlapping (appear in 2+ ancestors)
    (into {}
          (filter (fn [[_cond ancestor-set]]
                    (>= (count ancestor-set) 2))
                  conditional-to-ancestors))))

(defn group-ancestors-by-overlaps
  "Partition ancestors into groups based on shared overlapping conditionals.

   Ancestors that share ANY overlapping conditional belong to the same group.
   Uses a union-find-like approach to build connected components.

   Arguments:
   - num-ancestors: Total number of ancestors
   - overlaps: Map from find-overlapping-conditionals {conditional → #{ancestor-indices}}

   Returns:
   Vector of groups, where each group is:
   {:ancestor-indices #{idx1 idx2 ...}
    :shared-conditionals [cond1 cond2 ...]}

   Examples:
   - 7 ancestors, conditionals appear in #{0 5} → [{:ancestor-indices #{0 5} :shared-conditionals [...]}
                                                    {:ancestor-indices #{1} :shared-conditionals []}
                                                    {:ancestor-indices #{2} :shared-conditionals []}
                                                    ...]
   - All ancestors overlap → [{:ancestor-indices #{0 1 2 3 4 5 6} :shared-conditionals [...]}]"
  [num-ancestors overlaps]
  (if (empty? overlaps)
    ;; No overlaps - each ancestor is its own group
    (mapv (fn [idx] {:ancestor-indices    #{idx}
                     :shared-conditionals []})
          (range num-ancestors))
    ;; Build connected components
    (let [;; Start with each ancestor in its own group
          initial-groups (vec (map (fn [idx] {:ancestor-indices    #{idx}
                                              :shared-conditionals []})
                                   (range num-ancestors)))

          ;; For each overlapping conditional, merge the groups containing those ancestors
          merged-groups
          (reduce
           (fn [groups [conditional ancestor-set]]
             (let [;; Find all groups that contain any of these ancestors
                   affected-group-indices
                   (keep-indexed (fn [idx group]
                                   (when (some (:ancestor-indices group) ancestor-set)
                                     idx))
                                 groups)]
               (if (<= (count affected-group-indices) 1)
                 ;; Only one group affected (or none) - just add conditional to it
                 (if (seq affected-group-indices)
                   (update-in groups [(first affected-group-indices) :shared-conditionals]
                              conj conditional)
                   groups)
                 ;; Multiple groups need to be merged
                 (let [;; Get all affected groups
                       affected-groups  (map #(nth groups %) affected-group-indices)
                       ;; Merge them
                       merged-group     {:ancestor-indices    (apply clojure.set/union
                                                                     (map :ancestor-indices affected-groups))
                                         :shared-conditionals (vec (concat [conditional]
                                                                           (mapcat :shared-conditionals affected-groups)))}
                       ;; Remove old groups and add merged one
                       remaining-groups (keep-indexed (fn [idx group]
                                                        (when-not (some #{idx} affected-group-indices)
                                                          group))
                                                      groups)]
                   (conj (vec remaining-groups) merged-group)))))
           initial-groups
           overlaps)]

      ;; Remove duplicate conditionals from shared-conditionals lists
      (mapv (fn [group]
              (update group :shared-conditionals
                      #(vec (distinct %))))
            merged-groups))))

(defn create-minimal-ancestor-branches
  "Create minimal branches by deduplicating overlapping conditionals.

   For each overlapping conditional:
   1. Create a branch with just that conditional
   2. Determine which ancestors it doesn't satisfy
   3. Combine with non-overlapping branches from unsatisfied ancestors

   This ensures each overlapping conditional creates minimal branches that satisfy
   all ancestors, avoiding redundant scenario combinations.

   Arguments:
   - ancestors: Sequence of ancestor maps
   - expanded-branches: Expanded branches for each ancestor (from expand-or-conditionals)
   - overlaps: Map from find-overlapping-conditionals {conditional → #{ancestor-indices}}

   Returns:
   Sequence of minimal ancestor setups (flat lists of conditionals)"
  [ancestors expanded-branches overlaps]
  (if (empty? overlaps)
    ;; No overlaps - use cartesian product as before
    (let [combinations (apply combo/cartesian-product expanded-branches)]
      (map #(apply concat %) combinations))
    ;; Has overlaps - create minimal branches per overlapping conditional
    (let [num-ancestors (count ancestors)

          ;; Get branches that don't contain ANY overlapping conditional, per ancestor
          non-overlapping-by-ancestor
          (map-indexed
           (fn [idx branches]
             (filter (fn [branch]
                       (not-any? (fn [cond]
                                   (contains? overlaps cond))
                                 branch))
                     branches))
           expanded-branches)

          ;; For each overlapping conditional, create branches
          overlapping-branches
          (for [[cond ancestor-indices] overlaps]
            (let [uncovered-ancestors         (clojure.set/difference (set (range num-ancestors))
                                                                      ancestor-indices)
                  uncovered-branches          (map #(nth non-overlapping-by-ancestor %)
                                                   uncovered-ancestors)
                  uncovered-branches-filtered (remove empty? uncovered-branches)]
              (if (empty? uncovered-ancestors)
                ;; Conditional satisfies ALL ancestors
                [[cond]]
                ;; Conditional satisfies SOME ancestors - combine with uncovered
                (if (empty? uncovered-branches-filtered)
                  ;; No non-overlapping branches in uncovered ancestors - skip this
                  []
                  ;; Cartesian product with uncovered branches
                  (let [uncovered-combos (apply combo/cartesian-product uncovered-branches-filtered)]
                    (for [uncovered-combo uncovered-combos]
                      (concat [cond] (apply concat uncovered-combo))))))))

          overlapping-branches-flat (apply concat overlapping-branches)

          ;; Also include pure non-overlapping (all ancestors use non-overlapping branches)
          non-empty-non-overlapping (remove empty? non-overlapping-by-ancestor)
          pure-non-overlapping      (if (= (count non-empty-non-overlapping) num-ancestors)
                                      (let [combos (apply combo/cartesian-product non-empty-non-overlapping)]
                                        (map #(apply concat %) combos))
                                      [])]

      (concat overlapping-branches-flat pure-non-overlapping))))

(defn find-overlapping-between-branch-sets
  "Find conditionals that overlap between two sets of branches (e.g., ancestor branches and entity branches).

   Returns a map of overlapping conditionals to their locations:
   {:conditional {:in-set-a boolean, :in-set-b boolean}}"
  [branches-a branches-b]
  (let [;; Collect all unique conditionals from set A
        conditionals-a (into #{}
                             (mapcat identity branches-a))
        ;; Collect all unique conditionals from set B
        conditionals-b (into #{}
                             (mapcat identity branches-b))

        ;; Find conditionals that appear in both sets (by value equality)
        overlaps (for [cond-a conditionals-a
                       cond-b conditionals-b
                       :when  (conditionals-equal? cond-a cond-b)]
                   cond-a)]

    (into #{} overlaps)))

(defn create-minimal-combined-branches
  "Create minimal branches by deduplicating overlapping conditionals between ancestor and entity branches.

   Algorithm:
   1. Detect conditionals that overlap between ancestor branches and entity branches
   2. For each overlapping conditional:
      - Find ancestor branches containing it
      - Find entity branches containing it
      - Create ONE combined branch with the overlapping conditional + non-overlapping parts
   3. For non-overlapping portions, do cartesian product as normal

   Arguments:
   - ancestor-branches: Sequence of branches (each branch is a sequence of conditionals)
   - entity-branches: Sequence of branches (each branch is a sequence of conditionals)

   Returns:
   Sequence of combined branches [ancestor-setup entity-setup]"
  [ancestor-branches entity-branches]
  (if (or (empty? ancestor-branches) (empty? entity-branches))
    ;; No combination possible if either is empty
    (if (empty? entity-branches)
      (map vector ancestor-branches (repeat []))
      [])
    (let [overlapping-conds (find-overlapping-between-branch-sets ancestor-branches entity-branches)]

      (if (empty? overlapping-conds)
        ;; No overlaps - traditional cartesian product
        (for [ancestor-setup ancestor-branches
              entity-setup   entity-branches]
          [ancestor-setup entity-setup])

        ;; Has overlaps - create minimal branches
        (let [;; For each overlapping conditional, create one minimal branch
              overlapping-branches
              (for [overlap-cond overlapping-conds]
                ;; Find ANY ancestor branch containing this conditional
                (let [ancestor-branch (some #(when (some (fn [c] (conditionals-equal? c overlap-cond)) %)
                                               %)
                                            ancestor-branches)
                      ;; Find ANY entity branch containing this conditional
                      entity-branch   (some #(when (some (fn [c] (conditionals-equal? c overlap-cond)) %)
                                               %)
                                            entity-branches)]
                  ;; Combine them, the overlapping conditional appears in both
                  [ancestor-branch entity-branch]))

              ;; Filter to get non-overlapping branches
              non-overlapping-ancestor-branches
              (filter (fn [branch]
                        (not-any? (fn [cond]
                                    (some #(conditionals-equal? cond %) overlapping-conds))
                                  branch))
                      ancestor-branches)

              non-overlapping-entity-branches
              (filter (fn [branch]
                        (not-any? (fn [cond]
                                    (some #(conditionals-equal? cond %) overlapping-conds))
                                  branch))
                      entity-branches)

              ;; Do cartesian product on non-overlapping parts
              non-overlapping-combos
              (if (or (empty? non-overlapping-ancestor-branches)
                      (empty? non-overlapping-entity-branches))
                []
                (for [ancestor-setup non-overlapping-ancestor-branches
                      entity-setup   non-overlapping-entity-branches]
                  [ancestor-setup entity-setup]))]

          ;; Combine overlapping branches with non-overlapping combinations
          (concat overlapping-branches non-overlapping-combos))))))

(defn expand-ancestor-or-branches
  "Expand ancestors using only the FIRST branch from each ancestor's OR conditionals.

   This significantly reduces the number of generated scenarios by selecting just one
   valid path through each ancestor's conditionals rather than exploring all combinations.

   For ancestors with :or operators, only the first option is selected.
   For ancestors with :and operators, all conditionals are included.

   Arguments:
   - ancestors: Sequence of ancestor maps with :conditionals

   Returns:
   Sequence with a single ancestor setup (flat list of conditionals from first branches)"
  [ancestors]
  (if (empty? ancestors)
    [[]] ; no ancestors, return single empty setup
    (let [;; Expand each ancestor's conditionals into all possible paths
          expanded-branches (map #(expand-or-conditionals (:conditionals %)) ancestors)]
      (if (every? empty? expanded-branches)
        [[]] ; all ancestors have no conditionals
        ;; Take only the FIRST branch from each ancestor and combine them
        (let [first-branches (map first expanded-branches)
              ;; Combine all first branches into a single ancestor setup
              combined-setup (apply concat first-branches)]
          [combined-setup])))))

(defn has-any-research-conditional?
  "Check if any conditional in a list has research dependencies.

   Arguments:
   - conditionals: Sequence of conditional maps

   Returns:
   Boolean true if any conditional has research dependencies"
  [conditionals]
  (some has-research-dependency? conditionals))

(defn deduplicate-ancestor-conditionals
  [conditionals]
  (into #{} conditionals))

;; ===========================================================================================================
;; Setup Step Generation (Task 6.3)
;; ===========================================================================================================

(defn generate-output-step-line
  "Create output selection step line.
   Check conditional :values = [\"true\"] (positive selection).
   Skip if :values = [\"false\"] (negative conditionals).

   Arguments:
   - conditional: Conditional map

   Returns:
   Formatted string like '-- Spot -> Maximum Spotting Distance -> Burning Pile' or nil"
  [conditional]
  (when (and (= (get-in conditional [:group-variable :io]) :output)
             (= (:values conditional) ["true"]))
    (let [path          (get-in conditional [:group-variable :path])
          variable-name (get-in conditional [:group-variable :group-variable/translated-name])
          full-path     (conj path variable-name)]
      (format-output-line full-path))))

(defn generate-negative-output-step-line
  "Create negative output step line.
   Check conditional :values = [\"false\"].

   Arguments:
   - conditional: Conditional map

   Returns:
   Formatted string for 'these outputs are NOT selected' step or nil"
  [conditional]
  (when (and (= (get-in conditional [:group-variable :io]) :output)
             (= (:values conditional) ["false"]))
    (let [path          (get-in conditional [:group-variable :path])
          variable-name (get-in conditional [:group-variable :group-variable/translated-name])
          full-path     (conj path variable-name)]
      (format-output-line full-path))))

(defn generate-input-step-line
  "Create input value entry step line.
   Get value from conditional :values (first element if multiple).

   Arguments:
   - conditional: Conditional map

   Returns:
   Formatted string like '--- Fuel Model -> Standard -> Fuel Model -> FB2/2' or nil"
  [conditional]
  (when (= (get-in conditional [:group-variable :io]) :input)
    (let [value (first (:values conditional))
          path  (get-in conditional [:group-variable :path])]
      (format-input-value-line path value))))

(defn collect-and-sort-setup-steps
  "Organize all setup steps.
   Separate outputs (positive), negative outputs, and inputs into three lists.
   Sort each list by :submodule/order first, then :group/order.

   Arguments:
   - conditionals: Sequence of conditional maps

   Returns:
   Map with :outputs, :negative-outputs, :inputs keys"
  [conditionals]
  (let [;; Separate by type
        outputs          (filter #(and (= (get-in % [:group-variable :io]) :output)
                                       (= (:values %) ["true"]))
                                 conditionals)
        negative-outputs (filter #(and (= (get-in % [:group-variable :io]) :output)
                                       (= (:values %) ["false"]))
                                 conditionals)
        inputs           (filter #(= (get-in % [:group-variable :io]) :input)
                                 conditionals)
        ;; Sort function
        sort-fn          (fn [conds]
                           (sort-by (juxt #(get-in % [:group-variable :submodule/order])
                                          #(get-in % [:group-variable :group/order]))
                                    conds))]
    {:outputs          (sort-fn outputs)
     :negative-outputs (sort-fn negative-outputs)
     :inputs           (sort-fn inputs)}))

;; ===========================================================================================================
;; Scenario Generation Functions (Task 6.4)
;; ===========================================================================================================

(defn generate-output-enables-input-scenario
  "Output selection shows input group.
   Structure: Given (worksheet) / When (outputs selected) / Then (input groups displayed)

   Arguments:
   - group: Group map with :conditionals
   - ancestor-setup: Sequence of conditionals from ancestors (already deduplicated and combined)
   - target-path: Path of target group
   - module-combo: Pre-determined module combination (e.g., :surface, :surface-crown)

   Returns:
   Gherkin scenario string"
  [group ancestor-setup target-path module-combo]
  (let [;; Use pre-determined module combination
        given-stmt (module-to-given-statement module-combo)

        ;; Use the already deduplicated and combined conditionals from ancestor-setup
        ;; (this includes both ancestor and group conditionals, already processed)
        all-conds ancestor-setup

        ;; Organize and sort setup steps
        sorted-steps          (collect-and-sort-setup-steps all-conds)
        output-lines          (keep generate-output-step-line (:outputs sorted-steps))
        negative-output-lines (keep generate-negative-output-step-line (:negative-outputs sorted-steps))
        input-lines           (keep generate-input-step-line (:inputs sorted-steps))

        ;; Format target group
        target-line (format-input-group-line target-path)]

    (str/join "\n"
              (concat
               [(str STEP-INDENT given-stmt)]
               (when (seq output-lines)
                 [(str STEP-INDENT "When these outputs are selected Submodule -> Group -> Output:")
                  (str DOCSTRING-INDENT "\"\"\"")
                  (str/join "\n" (map #(str DOCSTRING-INDENT %) output-lines))
                  (str DOCSTRING-INDENT "\"\"\"")])
               (when (seq negative-output-lines)
                 [(str STEP-INDENT "When these outputs are NOT selected Submodule -> Group -> Output:")
                  (str DOCSTRING-INDENT "\"\"\"")
                  (str/join "\n" (map #(str DOCSTRING-INDENT %) negative-output-lines))
                  (str DOCSTRING-INDENT "\"\"\"")])
               (when (seq input-lines)
                 [(str STEP-INDENT "When these inputs are entered Submodule -> Group -> Input:")
                  (str DOCSTRING-INDENT "\"\"\"")
                  (str/join "\n" (map #(str DOCSTRING-INDENT %) input-lines))
                  (str DOCSTRING-INDENT "\"\"\"")])
               [(str STEP-INDENT "Then the following input Submodule -> Groups are displayed:")
                (str DOCSTRING-INDENT "\"\"\"")
                (str DOCSTRING-INDENT target-line)
                (str DOCSTRING-INDENT "\"\"\"")]))))

(defn generate-mixed-output-scenario
  "Mixed output/negative output shows input.
   Structure: Given / When (outputs selected) / And (outputs NOT selected) / Then

   This is an alias for generate-output-enables-input-scenario as it handles both cases."
  [group ancestor-setup target-path module-combo]
  (generate-output-enables-input-scenario group ancestor-setup target-path module-combo))

(defn generate-input-value-enables-input-scenario
  "Input value shows input group.
   Structure: Given / When (outputs from ancestors) / And (inputs entered) / Then

   This is an alias for generate-output-enables-input-scenario as it handles all cases."
  [group ancestor-setup target-path module-combo]
  (generate-output-enables-input-scenario group ancestor-setup target-path module-combo))

(defn generate-compound-scenario
  "Output AND input value shows input group.
   Structure: Given / When (outputs) / And (inputs) / And (more outputs from sub-conditionals) / Then

   This is an alias for generate-output-enables-input-scenario as it handles compound cases."
  [group ancestor-setup target-path module-combo]
  (generate-output-enables-input-scenario group ancestor-setup target-path module-combo))

(defn wrap-scenario-with-header
  "Add Feature and Scenario headers.
   Generate scenario name from target group and trigger condition.

   Arguments:
   - scenario-text: The scenario body text
   - scenario-name: Name for the scenario
   - feature-title: Title for the feature

   Returns:
   Complete scenario text with headers"
  [scenario-text scenario-name feature-title]
  (str SCENARIO-INDENT scenario-name "\n" scenario-text))

(defn build-scenario-name
  "Build a descriptive scenario name based on entity's own conditionals.

   Creates scenario names that describe what outputs are selected to make the
   entity (group or submodule) appear. Only includes conditionals from the
   entity itself, not from ancestors.

   Arguments:
   - entity-setup: Sequence of conditionals from the entity's own conditionals (not ancestors)
   - entity-name: Name of the entity (group or submodule)

   Returns:
   String scenario name like:
   - 'Slope is displayed' (no outputs)
   - 'Slope is displayed when Critical Surface Fireline Intensity is selected' (1 output)
   - 'Slope is displayed when Heading and Rate of Spread are selected' (2 outputs)
   - 'Slope is displayed when Heading, Rate of Spread, and Flame Length are selected' (3+ outputs)"
  [entity-setup entity-name]
  (let [;; Filter to only output conditionals with values=["true"]
        output-conditionals (filter (fn [cond]
                                      (and (= (get-in cond [:group-variable :io]) :output)
                                           (= (:values cond) ["true"])))
                                    entity-setup)

        ;; Extract and sort output names alphabetically
        output-names (sort (map #(get-in % [:group-variable :group-variable/translated-name])
                                output-conditionals))

        output-count (count output-names)]

    (case output-count
      0 (str entity-name " is displayed")
      1 (str entity-name " is displayed when " (first output-names) " is selected")
      2 (str entity-name " is displayed when "
             (first output-names) " and " (second output-names) " are selected")
      ;; 3+ outputs: use Oxford comma
      (str entity-name " is displayed when "
           (str/join ", " (butlast output-names))
           ", and " (last output-names) " are selected"))))

;; ===========================================================================================================
;; Scenario Orchestration (Task 6.5)
;; ===========================================================================================================

(defn determine-module-combo-for-combination
  "Determine the module combination for a specific ancestor + entity conditional combination.

   Analyzes the paths in both ancestor-setup and entity-setup to determine
   which worksheet type (module combination) is needed for this scenario.

   Arguments:
   - ancestor-setup: Sequence of conditionals from ancestors
   - entity-setup: Sequence of conditionals from the entity itself
   - entity-path: Path of the entity being processed (e.g., [\"Surface\" \"Wind and Slope\" :input \"Slope\"])

   Returns:
   Module combination keyword like :surface, :surface-crown, :surface-mortality, etc."
  [ancestor-setup entity-setup entity-path]
  (let [;; Collect all paths from both setups
        all-conditionals (concat ancestor-setup entity-setup)
        all-paths        (keep #(get-in % [:group-variable :path]) all-conditionals)

        ;; Extract entity's own module
        entity-module (extract-module-from-path entity-path)

        ;; Extract modules from paths
        path-modules (extract-modules-from-paths all-paths)

        ;; Combine with entity module
        all-modules (if entity-module
                      (conj path-modules entity-module)
                      path-modules)

        ;; Determine combination
        module-combo (determine-module-combination all-modules)]
    module-combo))

(defn generate-scenarios-for-entity
  "Generate all scenarios for one entity (group or submodule) using cartesian product with deduplication.

   Expands both ancestor conditionals and the target entity's own conditionals,
   then creates cartesian product with overlap deduplication to generate minimal
   scenario combinations. This prevents redundant scenarios like selecting both
   'Flame Length' and 'Fireline Intensity' when either would satisfy all requirements.

   Module detection happens PER COMBINATION, not at entity level, since different
   combinations may require different worksheet types.

   Filters out any combinations that contain research dependencies to ensure
   all generated scenarios are completely free of research variables.

   Works with both groups (have :group/translated-name) and submodules (have :submodule/name).

   Arguments:
   - all-data: Map with path vectors as keys, entity info as values
   - entity: Group or submodule map with :conditionals

   Returns:
   Sequence of scenario maps with :text, :module, :group-path"
  [all-data entity]
  (when-not (or (and (:group/translated-name entity) (should-skip-group? entity))
                (and (:submodule/name entity) (should-skip-submodule? entity))
                (has-only-module-conditionals? entity))
    (let [;; Get direct ancestors from entity's own path
          direct-ancestors (get-ancestors all-data (:path entity))

          ;; Recursively collect ancestors from BOTH:
          ;; 1. Direct ancestors' conditionals
          ;; 2. Entity's own conditionals (to capture references like Fuel Model)
          all-direct-ancestors-conds (mapcat #(get-in % [:conditionals :conditionals]) direct-ancestors)
          entity-own-conds           (get-in entity [:conditionals :conditionals])

          ;; Combine both sources for ancestral entity collection
          all-conds-to-follow (concat all-direct-ancestors-conds entity-own-conds)

          conditional-ancestors (when (seq all-conds-to-follow)
                                  (collect-all-ancestral-entities all-data all-conds-to-follow))

          ;; Combine and deduplicate all ancestors by :path BEFORE expansion
          ;; This prevents combinatoric explosion from duplicate entities
          ancestors (vec (vals (into {} (map (juxt :path identity)
                                             (concat direct-ancestors conditional-ancestors)))))

          ;; Expand ancestor OR branches using cartesian product with deduplication
          ancestor-branches (expand-ancestor-or-branches ancestors)

          ;; Deduplicate each ancestor branch
          deduped-ancestor-branches (map deduplicate-ancestor-conditionals ancestor-branches)

          ;; Expand target entity's own conditionals
          entity-conditionals (:conditionals entity)
          entity-branches     (expand-or-conditionals entity-conditionals)

          ;; Deduplicate each entity branch
          deduped-entity-branches (map deduplicate-ancestor-conditionals entity-branches)

          ;; Create cartesian product with overlap deduplication
          all-combinations (for [ancestor-setup deduped-ancestor-branches
                                 entity-setup   deduped-entity-branches]
                             [ancestor-setup entity-setup])

          ;; Remove ancestor conditionals made redundant by entity conditionals
          ;; Strategy: If entity has OUTPUT conditionals, they enable both entity and ancestor
          ;;          If entity has only INPUT conditionals, ancestor outputs are still needed
          deduplicated-combinations (map (fn [[ancestor-setup entity-setup]]
                                           (let [;; Check if entity has any output conditionals
                                                 entity-has-outputs? (some #(= :output (get-in % [:group-variable :io]))
                                                                           entity-setup)
                                                 ;; If entity has outputs, remove ALL ancestor :group-variable conditionals
                                                 ;; If entity has only inputs, keep ancestor :group-variable conditionals
                                                 minimal-ancestor    (if entity-has-outputs?
                                                                       (filter #(= (:type %) :module) ancestor-setup)
                                                                       ancestor-setup)]
                                             [minimal-ancestor entity-setup]))
                                         all-combinations)

          ;; Filter out combinations that contain ANY research dependencies
          non-research-combinations (remove (fn [[ancestor-setup entity-setup]]
                                              (has-any-research-conditional?
                                               (concat ancestor-setup entity-setup)))
                                            deduplicated-combinations)

          ;; Generate scenario for each non-research combination
          scenarios (for [[ancestor-setup entity-setup] non-research-combinations]
                      ;; Determine module combo for THIS specific combination
                      (let [module-combo (determine-module-combo-for-combination ancestor-setup entity-setup (:path entity))]
                        ;; Skip unsupported module combinations early
                        (when (not= module-combo :unsupported)
                          (let [module-conditionals                (->> entity-setup
                                                                        (concat ancestor-setup)
                                                                        (filter #(= (:type %) :module))
                                                                        (map #(set (map keyword (:values %)))))
                                ;; Module conditionals must be subsets of the determined combo
                                ;; e.g., ancestor has #{:surface}, combo is #{:surface :crown} -> valid
                                all-compatible-module-conditionals (every? #(clojure.set/subset? % module-combo) module-conditionals)]
                            ;; Skip if module conditionals don't match
                            (when all-compatible-module-conditionals
                              (let [;; Filter ancestor setup by this combination's module
                                    filtered-ancestor-setup (filter-conditionals-by-module ancestor-setup module-combo)

                                    ;; Filter entity setup by this combination's module
                                    filtered-entity-setup (filter-conditionals-by-module entity-setup module-combo)

                                    ;; Combine and deduplicate
                                    all-conditionals (concat filtered-ancestor-setup filtered-entity-setup)
                                    deduped-setup    (deduplicate-ancestor-conditionals all-conditionals)

                                    ;; Generate scenario
                                    scenario-text (generate-output-enables-input-scenario
                                                   {:conditionals {:conditionals filtered-entity-setup}
                                                    :path         (:path entity)}
                                                   deduped-setup
                                                   (:path entity)
                                                   module-combo)

                                    ;; Generate scenario name (works for both groups and submodules)
                                    entity-name   (or (:group/translated-name entity)
                                                      (:submodule/name entity))
                                    scenario-name (str "Scenario: " (build-scenario-name filtered-entity-setup entity-name))]

                                {:text       (wrap-scenario-with-header scenario-text scenario-name "")
                                 :module     module-combo
                                 :group-path (:path entity)}))))))]

      ;; Remove nil scenarios (from unsupported module combos)
      (remove nil? scenarios))))

(defn generate-scenarios-for-group
  "Generate scenarios for a group. Delegates to generate-scenarios-for-entity."
  [all-data group]
  (generate-scenarios-for-entity all-data group))

(defn generate-scenarios-for-submodule
  "Generate scenarios for a submodule. Delegates to generate-scenarios-for-entity."
  [all-data submodule]
  (generate-scenarios-for-entity all-data submodule))

(defn group-scenarios-by-feature
  "Organize scenarios into feature files.
   Group by module combination AND target group path.

   Arguments:
   - scenarios: Sequence of scenario maps

   Returns:
   Map of feature-key -> scenarios list"
  [scenarios]
  (group-by (fn [scenario]
              [(:module scenario) (:group-path scenario)])
            scenarios))

(defn split-large-feature-files
  "No longer splits feature files - returns all scenarios in a single file.

   Arguments:
   - feature-map: Map of feature-key -> scenarios list

   Returns:
   Updated feature file structure (no splitting)"
  [feature-map]
  (into {}
        (map (fn [[[module path] scenarios]]
               [[module path] {:split?    false
                               :scenarios scenarios}])
             feature-map)))

;; ===========================================================================================================
;; Filename Generation Functions (Task 6.6)
;; ===========================================================================================================

(defn sanitize-path-component
  "Convert path element to filename-safe string.
   - Convert to lowercase
   - Replace spaces with hyphens
   - Remove special characters except hyphens

   Arguments:
   - component: String like 'By Size Class'

   Returns:
   String like 'by-size-class'"
  [component]
  (-> component
      str/lower-case
      (str/replace #"\s+" "-")
      (str/replace #"[^a-z0-9\-]" "")))

(defn generate-feature-filename
  "Create filename from module and path.
   Pattern: {module}-input_{parent-groups}_{target-group}.feature

   Arguments:
   - module: Module set like #{:surface} or module keyword (for backward compat)
   - path: Path vector like ['Surface' 'Fuel Moisture' 'By Size Class']

   Returns:
   String like 'surface-input_fuel-moisture_by-size-class.feature'"
  [module path]
  (let [module-str                 (if (set? module)
                                     (module-set-to-string module)
                                     (name module))
        ;; Skip module (first element) and filter out :io keywords from path
        path-without-module-and-io (remove keyword? (rest path))
        sanitized-parts            (map sanitize-path-component path-without-module-and-io)
        filename                   (str module-str "-input_" (str/join "_" sanitized-parts) ".feature")]
    filename))

(defn generate-split-directory-name
  "Create directory for split scenarios.
   Same pattern as filename but without .feature extension.

   Arguments:
   - module: Module set like #{:surface} or module keyword (for backward compat)
   - path: Path vector

   Returns:
   String like 'surface-input_fuel-moisture'"
  [module path]
  (let [module-str                 (if (set? module)
                                     (module-set-to-string module)
                                     (name module))
        ;; Skip module (first element) and filter out :io keywords from path
        path-without-module-and-io (remove keyword? (rest path))
        sanitized-parts            (map sanitize-path-component path-without-module-and-io)
        dirname                    (str module-str "-input_" (str/join "_" sanitized-parts))]
    dirname))

(defn generate-part-filename
  "Create part filename for split scenarios.
   Pattern: {directory}/part-{NN}.feature (zero-padded)

   Arguments:
   - directory: Directory name
   - part-number: Part number (1-indexed)

   Returns:
   String like 'surface-input_fuel-moisture/part-01.feature'"
  [directory part-number]
  (format "%s/part-%02d.feature" directory part-number))

;; ===========================================================================================================
;; Feature File Writing (Task 6.7)
;; ===========================================================================================================

(defn generate-feature-header
  "Create feature title and description.
   Title includes module and target group path.

   Arguments:
   - module: Module set like #{:surface} or module keyword (for backward compat)
   - path: Path vector

   Returns:
   String like 'Feature: Surface Input - Fuel Moisture -> By Size Class'"
  [module path]
  (let [module-str (if (set? module)
                     (module-set-to-title module)
                     (str/capitalize (name module)))
        path-str   (format-path-for-gherkin path)]
    (str "Feature: " module-str " Input - " path-str "\n")))

(defn write-feature-file
  "Write scenarios to file with header.
   Use spit to write to file path.
   Create parent directories if needed.

   Arguments:
   - file-path: File path to write to
   - header: Feature header text
   - scenarios: Sequence of scenario maps

   Returns:
   nil (side effect: writes file)"
  [file-path header scenarios]
  (io/make-parents file-path)
  (let [scenario-texts (map :text scenarios)
        content        (str header "\n" (str/join "\n\n" scenario-texts))]
    (spit file-path content)))

(defn delete-old-generated-files
  "Clean up before generation.
   Delete all .feature files in features/ directory except core_conditional_scenarios.feature.

   Arguments:
   - features-dir: Features directory path

   Returns:
   Number of files deleted"
  [features-dir]
  (let [dir           (io/file features-dir)
        files         (file-seq dir)
        feature-files (filter #(and (.isFile %)
                                    (str/ends-with? (.getName %) ".feature")
                                    (not= (.getName %) "core_conditional_scenarios.feature"))
                              files)]
    (doseq [file feature-files]
      (.delete file))
    (count feature-files)))

;; ===========================================================================================================
;; Main Generation Function (Task 6.8)
;; ===========================================================================================================

(defn generate-feature-files!
  "Generate Cucumber feature files from test_matrix_data.edn.

   Orchestrate full file generation workflow:
   - Load test matrix EDN
   - Filter out research groups/submodules
   - Delete old generated files
   - Generate scenarios for all entities
   - Group scenarios by feature
   - Split large features
   - Generate filenames and write files
   - Track and print statistics

   Arguments:
   - edn-path (optional): Path to input EDN file
     Default: 'development/test_matrix_data.edn'
   - features-dir (optional): Directory for output feature files
     Default: 'features/'

   Returns:
   Map with :features-dir, :files-written, :scenarios-generated"
  ([]
   (generate-feature-files! "development/test_matrix_data.edn" "features/"))
  ([edn-path]
   (generate-feature-files! edn-path "features/"))
  ([edn-path features-dir]
   (try
     ;; Load test matrix (returns map with path keys)
     (let [all-data (load-test-matrix edn-path)

           ;; Get all entities (groups and submodules) from map values
           all-entities (vals all-data)

           ;; Separate groups from submodules
           groups     (filter :group/translated-name all-entities)
           submodules (filter :submodule/name all-entities)

           ;; Filter out research groups and submodules
           non-research-groups     (remove should-skip-group? groups)
           non-research-submodules (remove should-skip-submodule? submodules)

           ;; Delete old generated files
           _ (delete-old-generated-files features-dir)

           ;; Generate scenarios for all groups and submodules (pass all-data)
           group-scenarios     (mapcat #(generate-scenarios-for-group all-data %) non-research-groups)
           submodule-scenarios (mapcat #(generate-scenarios-for-submodule all-data %) non-research-submodules)
           all-scenarios       (concat group-scenarios submodule-scenarios)

           ;; Group scenarios by feature
           grouped-scenarios (group-scenarios-by-feature all-scenarios)

           ;; Split large features
           split-features (split-large-feature-files grouped-scenarios)

           ;; Write files
           files-written       (atom 0)
           scenarios-generated (atom 0)]

       (doseq [[[module path] feature-data] split-features]
         (let [header (generate-feature-header module path)]
           (if (:split? feature-data)
             ;; Write split parts
             (let [dirname (generate-split-directory-name module path)]
               (doseq [part (:parts feature-data)]
                 (let [part-filename (generate-part-filename dirname (:part-number part))
                       part-path     (str features-dir part-filename)]
                   (write-feature-file part-path header (:scenarios part))
                   (swap! files-written inc)
                   (swap! scenarios-generated + (count (:scenarios part))))))
             ;; Write single file
             (let [filename  (generate-feature-filename module path)
                   file-path (str features-dir filename)]
               (write-feature-file file-path header (:scenarios feature-data))
               (swap! files-written inc)
               (swap! scenarios-generated + (count (:scenarios feature-data)))))))

       (println (format "✓ Generated %d feature files with %d scenarios" @files-written @scenarios-generated))

       {:features-dir        features-dir
        :files-written       @files-written
        :scenarios-generated @scenarios-generated})
     (catch Exception e
       (println (str "Error generating feature files: " (.getMessage e)))
       {:features-dir        features-dir
        :files-written       0
        :scenarios-generated 0
        :error               (.getMessage e)}))))
