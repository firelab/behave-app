(ns cucumber-test-generator.generate-scenarios
  (:require [clojure.edn                :as edn]
            [clojure.java.io            :as io]
            [clojure.math.combinatorics :as combo]
            [clojure.set                :as set]
            [clojure.string             :as str]))

;; ===========================================================================================================
;; Gherkin Indentation Constants
;; ===========================================================================================================

(def ^:const SCENARIO-INDENT "  ") ; 2 spaces for Scenario line
(def ^:const STEP-INDENT "    ") ; 4 spaces for Given/When/Then/And
(def ^:const TABLE-INDENT "      ") ; 6 spaces for table rows

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

;; ===========================================================================================================
;; Nil-Path Detection (data-integrity guard)
;; ===========================================================================================================

(defn path-element-nil?
  "True if a path element is bare nil, or a {:name :key} tuple with a nil :name or :key.
   Used to detect VMS entries whose module/submodule translations are missing."
  [el]
  (or (nil? el)
      (and (map? el)
           (or (nil? (:name el)) (nil? (:key el))))))

(defn path-has-nil?
  "True if any element of the path is nil-degraded.
   Signals a VMS entry whose module/submodule translations are missing."
  [path]
  (boolean (some path-element-nil? path)))

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
             raw     (edn/read-string content)
             ;; Support both combined {:input-visibility {...}} and legacy flat format
             data    (or (:input-visibility raw) raw)]
         (when-not (map? data)
           (throw (ex-info "Malformed EDN: expected a map with path keys"
                           {:edn-path edn-path})))
         ;; Drop any entry whose path key contains a nil element (missing VMS translations).
         (into {} (->> data
                       (remove (fn [[k _]] (path-has-nil? k)))
                       (map (fn [[k v]] [k (assoc v :path k)]))))))
     (catch Exception e
       (throw (ex-info (str "Failed to load test matrix: " (.getMessage e))
                       {:edn-path edn-path}
                       e))))))

;; ===========================================================================================================
;; Path Formatting Functions (Task 5.3)
;; ===========================================================================================================

(defn- pname
  "Return the display name for a path element.
   Path elements are {:name :key} tuples when extracted from the VMS, or plain strings
   in legacy data. Keywords (:input/:output) are returned as-is.
   This is the single place that unwraps the tuple — all rendering code calls pname;
   lookup code (get all-data path) uses the raw element so map-equality still holds."
  [el]
  (if (map? el) (:name el) el))

(defn format-path-for-gherkin
  "Convert path vector to Gherkin-style format.
   Skips first element (module name) and :io keywords, joins remaining with ' -> '.

   Arguments:
   - path: Vector like [{:name \"Surface\" :key ...} {:name \"Fuel Moisture\" :key ...} :input ...]

   Returns:
   String like \"Fuel Moisture -> By Size Class\""
  [path]
  (when (seq path)
    (str/join " -> " (map pname (remove keyword? (rest path))))))

(defn path->table-components
  "Extract table path components from a path vector.
   Strips module (first element) and any keyword (:input/:output).

   Arguments:
   - path: Vector like [{:name \"Surface\" ...} {:name \"Wind and Slope\" ...} :input ...]

   Returns:
   Vector of string elements like [\"Wind and Slope\" \"Wind and slope are\" \"Wind Direction\"]"
  [path]
  (when (seq path)
    (mapv pname (remove keyword? (rest path)))))

(defn present-headers
  "Return only those headers that have a non-empty value in at least one row.

   Arguments:
   - preferred-order: Ordered sequence of keyword headers (e.g. [:submodule :group :value])
   - rows: Sequence of row maps

   Returns:
   Filtered sequence of headers"
  [preferred-order rows]
  (filter (fn [h]
            (some #(seq (str (get % h ""))) rows))
          preferred-order))

(defn render-table
  "Render a Gherkin data table with column-aligned padding.

   Arguments:
   - headers: Ordered vector of column keywords to include
   - rows: Sequence of row maps

   Returns:
   Multi-line string of the table (all rows including header)"
  [headers rows]
  (when (and (seq headers) (seq rows))
    (let [get-cell   (fn [row h] (str (get row h "")))
          col-widths (map (fn [h]
                            (let [header-w (count (name h))
                                  data-w   (apply max 0 (map #(count (get-cell % h)) rows))]
                              (max header-w data-w)))
                          headers)
          pad        (fn [s w] (str s (apply str (repeat (- w (count s)) " "))))
          fmt-row    (fn [cells]
                       (str TABLE-INDENT "| "
                            (str/join " | " (map pad cells col-widths))
                            " |"))
          header-row (fmt-row (map name headers))
          data-rows  (map (fn [row]
                            (fmt-row (map #(get-cell row %) headers)))
                          rows)]
      (str/join "\n" (cons header-row data-rows)))))

(defn- comps->row
  "Build a table-row map from stripped path components.
   Always includes :submodule; adds :group, :subgroup, and :value when present."
  [comps value & {:keys [subgroup?]}]
  (cond-> {:submodule (first comps)}
    (>= (count comps) 2)             (assoc :group (second comps))
    (and subgroup? (>= (count comps) 3)) (assoc :subgroup (nth comps 2))
    value                            (assoc :value value)))

(defn conditional->output-row
  "Convert a positive output conditional (:values=[\"true\"]) to a table row map.
   Returns nil for non-positive-output conditionals."
  [conditional]
  (when (and (= (get-in conditional [:group-variable :io]) :output)
             (= (:values conditional) ["true"]))
    (comps->row (path->table-components (get-in conditional [:group-variable :path]))
                (get-in conditional [:group-variable :group-variable/translated-name]))))

(defn conditional->negative-output-row
  "Convert a negative output conditional (:values=[\"false\"]) to a table row map.
   Returns nil for non-negative-output conditionals."
  [conditional]
  (when (and (= (get-in conditional [:group-variable :io]) :output)
             (= (:values conditional) ["false"]))
    (comps->row (path->table-components (get-in conditional [:group-variable :path]))
                (get-in conditional [:group-variable :group-variable/translated-name]))))

(defn conditional->input-row
  "Convert an input conditional to a table row map.
   Returns nil for non-input conditionals."
  [conditional]
  (when (= (get-in conditional [:group-variable :io]) :input)
    (comps->row (path->table-components (get-in conditional [:group-variable :path]))
                (first (:values conditional))
                :subgroup? true)))

(defn target->table-row
  "Convert a target entity path to a Then-table row map.
   Strips module and :io keyword; maps remaining components to
   submodule / group / value."
  [path]
  (let [comps (path->table-components path)]
    (cond-> {:submodule (first comps)}
      (>= (count comps) 2) (assoc :group (second comps))
      (>= (count comps) 3) (assoc :value (nth comps 2)))))

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
  (when-let [module-el (first path)]
    (keyword (str/lower-case (pname module-el)))))

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

(defn extract-modules-from-paths
  "Extract unique module keywords from a collection of paths.

   Arguments:
   - paths: Set/sequence of path vectors

   Returns:
   Set of module keywords like #{:surface :crown}"
  [paths]
  (set (keep extract-module-from-path paths)))

(defn group-conditionals-by-module-combo
  "Group output conditionals by the module-combo implied by their path,
   plus the entity's own module.

   Arguments:
   - entity-mod: Keyword for the entity's own module (e.g. :crown), or nil
   - conds: Sequence of output conditional maps

   Returns:
   Map of {module-combo -> [conditionals]}"
  [entity-mod conds]
  (group-by (fn [c]
              (let [path (get-in c [:group-variable :path])
                    mods (cond-> (extract-modules-from-paths [path])
                           entity-mod (conj entity-mod))]
                (determine-module-combination mods)))
            conds))

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
   Surface always appears first to match worksheet naming convention.

   Arguments:
   - module-set: Set of module keywords (e.g., #{:surface :crown})

   Returns:
   String representation for titles

   Examples:
   #{:surface} -> \"Surface\"
   #{:surface :crown} -> \"Surface & Crown\"
   #{:surface :mortality} -> \"Surface & Mortality\""
  [module-set]
  (let [surface-first  (fn [m] (if (= m "surface") "" m))
        sorted-modules (sort-by surface-first (map name module-set))
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

(defn conditionally-set?
  "True if a conditional references a conditionally-set group-variable.
   These are auto-set by the app and never shown in the UI, so they are
   never valid output triggers."
  [conditional]
  (true? (get-in conditional [:group-variable :group-variable/conditionally-set?])))

(defn single-non-surface-module-conditional?
  "True if a conditional gates visibility on a SINGLE non-surface module run
   (crown-, contain-, or mortality-only) — a combination the harness can't run
   (it supports surface-only and surface-paired combos only)."
  [conditional]
  (and (= :module (:type conditional))
       (= 1 (count (:values conditional)))
       (not= "surface" (first (:values conditional)))))

(defn requires-unsupported-single-module?
  "True if any of the entity's conditionals — its own or inherited from
   :ancestors, including nested :sub-conditionals — requires a single
   non-surface module run."
  [entity]
  (let [own      (get-in entity [:conditionals :conditionals] [])
        ancestor (mapcat #(get-in % [:conditionals :conditionals] [])
                         (:ancestors entity []))
        walk     (fn walk [cs] (mapcat (fn [c] (cons c (walk (:sub-conditionals c)))) cs))]
    (boolean (some single-non-surface-module-conditional?
                   (walk (concat own ancestor))))))

(defn should-skip-group?
  "Determine if a group should be excluded from feature generation.

   Checks:
   - Group's :group/research? field
   - Group's :group/hidden? field
   - Group is gated on a single non-surface module run (unsupported combo)

   Note: Does NOT check individual conditionals for research - those will be filtered
   during scenario generation to remove research items while keeping non-research alternatives.

   Arguments:
   - group: Group map with :conditionals and optional :ancestors

   Returns:
   Boolean true if group should be skipped"
  [group]
  (or (true? (:group/research? group))
      (true? (:group/hidden? group))
      (requires-unsupported-single-module? group)))

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
        group-conds      (get-in group [:conditionals :conditionals] [])
        ancestor-conds   (mapcat #(get-in % [:conditionals :conditionals] [])
                                 (:ancestors group []))
        all-conds        (concat group-conds ancestor-conds)

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
        (has-only-module-conditionals? submodule)
        (requires-unsupported-single-module? submodule))))

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
  (let [operator           (:conditionals-operator conditionals-info)
        conditionals       (:conditionals conditionals-info)

        ;; Filter out malformed and conditionally-set conditionals
        valid-conditionals (filter
                            (fn [cond]
                              (and
                               (not (conditionally-set? cond))
                               (or
                                ;; Module conditionals are valid (have :type :module)
                                (= (:type cond) :module)
                                ;; Group-variable conditionals must have :group-variable map
                                (and (= (:type cond) :group-variable)
                                     (some? (:group-variable cond))))))
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

(defn collect-and-sort-setup-steps
  "Organize all setup steps.
   Separate outputs (positive), negative outputs, and inputs into three lists.
   Sort each list by :submodule/order first, then :group/order.

   Arguments:
   - conditionals: Sequence of conditional maps

   Returns:
   Map with :outputs, :negative-outputs, :inputs keys"
  [conditionals]
  (let [;; Separate by type — drop conditionally-set variables from all categories
        outputs          (filter #(and (= (get-in % [:group-variable :io]) :output)
                                       (= (:values %) ["true"])
                                       (not (conditionally-set? %)))
                                 conditionals)
        negative-outputs (filter #(and (= (get-in % [:group-variable :io]) :output)
                                       (= (:values %) ["false"])
                                       (not (conditionally-set? %)))
                                 conditionals)
        inputs           (filter #(and (= (get-in % [:group-variable :io]) :input)
                                       (not (conditionally-set? %)))
                                 conditionals)
        ;; Sort function — nil-safe on all three keys so stale EDN entries don't throw
        sort-fn          (fn [conds]
                           (sort-by (juxt #(or (get-in % [:group-variable :submodule/order]) 0)
                                          #(or (get-in % [:group-variable :group/order]) 0)
                                          #(or (get-in % [:group-variable :group-variable/order]) 0))
                                    conds))]
    {:outputs          (sort-fn outputs)
     :negative-outputs (sort-fn negative-outputs)
     :inputs           (sort-fn inputs)}))

;; ===========================================================================================================
;; Scenario Rendering Functions
;; ===========================================================================================================

(defn render-setup-blocks
  "Render the fixed setup step blocks (output/negative-output/input tables).
   Returns a sequence of strings (step line + table string) for each non-empty block.

   Arguments:
   - setup-conds: Sequence of setup conditionals

   Returns:
   Sequence of strings to join with newlines"
  [setup-conds]
  (let [sorted-steps (collect-and-sort-setup-steps setup-conds)
        output-rows  (keep conditional->output-row      (:outputs sorted-steps))
        neg-rows     (keep conditional->negative-output-row (:negative-outputs sorted-steps))
        input-rows   (keep conditional->input-row       (:inputs sorted-steps))]
    (concat
     (when (seq output-rows)
       [(str STEP-INDENT "When these output paths are selected")
        (render-table (present-headers [:submodule :group :value] output-rows) output-rows)])
     (when (seq neg-rows)
       [(str STEP-INDENT "When these output paths are NOT selected")
        (render-table (present-headers [:submodule :group :value] neg-rows) neg-rows)])
     (when (seq input-rows)
       [(str STEP-INDENT "When these input paths are entered")
        (render-table (present-headers [:submodule :group :subgroup :value] input-rows) input-rows)]))))

(defn render-scenario
  "Render a plain Scenario block (no Examples table).

   Arguments:
   - scenario-name: Display name without 'Scenario:' prefix (added here)
   - module-combo: Module set (for Given statement)
   - setup-conds: All setup conditionals (ancestor + entity)
   - target-path: Entity path (for Then table)

   Returns:
   Complete scenario text"
  [scenario-name module-combo setup-conds target-path]
  (let [given-stmt   (module-to-given-statement module-combo)
        setup-blocks (render-setup-blocks setup-conds)
        target-row   (target->table-row target-path)
        then-headers (present-headers [:submodule :group :value] [target-row])]
    (str/join "\n"
              (concat
               [(str SCENARIO-INDENT "@core")
                (str SCENARIO-INDENT "Scenario: " scenario-name)
                (str STEP-INDENT given-stmt)]
               setup-blocks
               [(str STEP-INDENT "Then the following input paths are displayed:")
                (render-table then-headers [target-row])]))))

(defn render-outline
  "Render a Scenario Outline block with Examples table.

   Arguments:
   - scenario-name: Display name without 'Scenario Outline:' prefix (added here)
   - module-combo: Module set (for Given statement)
   - setup-conds: Fixed setup conditionals (ancestor)
   - trigger-type: :output or :input
   - has-subgroup?: Boolean - whether trigger path has 3 stripped components
   - example-rows: Sequence of row maps for Examples table
   - target-path: Entity path (for Then table)
   - tag: :core or :extended

   Returns:
   Complete scenario outline text"
  [scenario-name module-combo setup-conds trigger-type has-subgroup? example-rows target-path tag]
  (let [given-stmt   (module-to-given-statement module-combo)
        setup-blocks (render-setup-blocks setup-conds)
        target-row   (target->table-row target-path)
        then-headers (present-headers [:submodule :group :value] [target-row])
        tag-str      (if (= tag :core) "@core" "@extended")
        when-step    (if (= trigger-type :output)
                       (str STEP-INDENT "When this output path is selected <submodule> : <group> : <value>")
                       (if has-subgroup?
                         (str STEP-INDENT "When this input path is entered <submodule> : <group> : <subgroup> : <value>")
                         (str STEP-INDENT "When this input path is entered <submodule> : <group> : <value>")))
        ex-headers   (if (= trigger-type :output)
                       [:submodule :group :value]
                       (if has-subgroup?
                         [:submodule :group :subgroup :value]
                         [:submodule :group :value]))]
    (str/join "\n"
              (concat
               [(str SCENARIO-INDENT tag-str)
                (str SCENARIO-INDENT "Scenario Outline: " scenario-name)
                (str STEP-INDENT given-stmt)]
               setup-blocks
               [when-step
                (str STEP-INDENT "Then the following input paths are displayed:")
                (render-table then-headers [target-row])
                ""
                (str STEP-INDENT "Examples: This scenario is repeated for each of these rows")
                (render-table ex-headers example-rows)]))))

(defn classify-entity-trigger
  "Classify the entity's own conditionals to determine what kind of scenario to emit.

   Returns a map with :type and relevant conditionals:
   - {:type :compound-output :in-cond {...} :output-conds [...]} - :in input with output :sub-conditionals
   - {:type :output-or  :conditionals [...]} - :or with ≥2 positive output conditionals
   - {:type :input-in   :conditional {...}}  - single conditional with :in and ≥2 values
   - {:type :single     :conditionals [...]} - everything else (plain Scenario)"
  [entity-conditionals]
  (let [operator    (:conditionals-operator entity-conditionals)
        all-conds   (:conditionals entity-conditionals)
        valid-conds (filter (fn [c]
                              (and
                               (not (conditionally-set? c))
                               (or (= (:type c) :module)
                                   (and (= (:type c) :group-variable)
                                        (some? (:group-variable c))))))
                            all-conds)
        in-cond     (first (filter #(= (:operator %) :in) valid-conds))
        compound-in (first (filter (fn [c]
                                     (and (= (:operator c) :in)
                                          (seq (:sub-conditionals c))
                                          (every? #(= (get-in % [:group-variable :io]) :output)
                                                  (:sub-conditionals c))))
                                   valid-conds))]
    (cond
      ;; :in input conditional with output :sub-conditionals → compound outline
      ;; (outputs drive Examples:, input value becomes fixed setup per scenario)
      compound-in
      {:type :compound-output :in-cond compound-in :output-conds (:sub-conditionals compound-in)}

      ;; Single :in conditional with multiple values → input-in outline
      (and in-cond (> (count (:values in-cond)) 1))
      {:type :input-in :conditional in-cond}

      ;; :or operator with ≥2 positive output group-variable conditionals → output-or outline
      ;; Module conditionals are allowed alongside outputs and are excluded from the rows.
      (and (= operator :or)
           (let [gv-conds (filter #(= (:type %) :group-variable) valid-conds)]
             (and (>= (count gv-conds) 2)
                  (every? (fn [c]
                            (and (= (get-in c [:group-variable :io]) :output)
                                 (= (:values c) ["true"])))
                          gv-conds))))
      {:type :output-or :conditionals (filter #(= (:type %) :group-variable) valid-conds)}

      ;; Everything else → single / plain Scenario
      :else
      {:type :single :conditionals valid-conds})))

(defn build-scenario-name
  "Build a descriptive scenario name from entity's own conditionals.

   Arguments:
   - entity-setup: Sequence of conditionals (entity's own, not ancestors)
   - entity-name: Name of the entity (group or submodule)

   Returns:
   String like 'Slope is displayed' or 'Burning Pile is displayed when Burning Pile is selected'"
  [entity-setup entity-name]
  (let [output-conditionals (filter (fn [cond]
                                      (and (= (get-in cond [:group-variable :io]) :output)
                                           (= (:values cond) ["true"])))
                                    entity-setup)
        output-names        (sort (map #(get-in % [:group-variable :group-variable/translated-name])
                                       output-conditionals))
        output-count        (count output-names)]
    (case output-count
      0 (str entity-name " is displayed")
      1 (str entity-name " is displayed when " (first output-names) " is selected")
      2 (str entity-name " is displayed when "
             (first output-names) " and " (second output-names) " are selected")
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
        entity-module    (extract-module-from-path entity-path)

        ;; Extract modules from paths
        path-modules     (extract-modules-from-paths all-paths)

        ;; Combine with entity module
        all-modules      (if entity-module
                           (conj path-modules entity-module)
                           path-modules)

        ;; Determine combination
        module-combo     (determine-module-combination all-modules)]
    module-combo))

(defn generate-scenarios-for-entity
  "Generate all scenarios for one entity (group or submodule).

   Classifies the entity's trigger type, expands ancestor setup via first-branch
   optimization, then renders the appropriate Scenario or Scenario Outline.

   - :output-or  → one @core Scenario Outline per module-combo (Examples = all OR rows)
   - :input-in   → @core outline (first row) + @extended outline (all rows)
   - :single     → plain @core Scenario with all conditionals folded into setup tables

   Arguments:
   - all-data: Map with path vectors as keys, entity info as values
   - entity: Group or submodule map with :conditionals

   Returns:
   Sequence of scenario maps with :text, :module, :group-path"
  [all-data entity]
  (when-not (or (and (:group/translated-name entity) (should-skip-group? entity))
                (and (:submodule/name entity) (should-skip-submodule? entity))
                (has-only-module-conditionals? entity)
                ;; Skip :output parent entities whose only conditionals are :type :module —
                ;; they are structural containers (always visible when module is active)
                ;; and do not represent "which inputs are displayed" test scenarios.
                (and (= :output (:submodule/io entity))
                     (let [conds (get-in entity [:conditionals :conditionals] [])]
                       (and (seq conds) (every? #(= :module (:type %)) conds)))))
    (let [entity-path           (:path entity)
          entity-name           (or (:group/translated-name entity) (pname (:submodule/name entity)))
          entity-mod            (extract-module-from-path entity-path)

          ;; ===== Classify the entity's own trigger (must precede ancestor expansion) =====
          entity-conditionals   (:conditionals entity)
          trigger               (classify-entity-trigger entity-conditionals)

          ;; ===== Ancestor expansion =====
          ;; For :or entities the sibling conditionals are mutually exclusive. Seeding
          ;; ancestor collection from all of them pulls in contradictory ancestor outputs
          ;; (e.g. Wind-Driven both selected AND NOT selected). Narrow to the active
          ;; trigger branch so only the relevant ancestor tree is followed.
          direct-ancestors      (get-ancestors all-data entity-path)
          all-direct-anc-conds  (mapcat #(get-in % [:conditionals :conditionals]) direct-ancestors)
          entity-own-conds      (get-in entity [:conditionals :conditionals])
          entity-operator       (get-in entity [:conditionals :conditionals-operator])
          conds-for-ancestry    (if (= entity-operator :or)
                                  (case (:type trigger)
                                    :input-in        [(:conditional trigger)]
                                    :compound-output [(:in-cond trigger)]
                                    entity-own-conds)
                                  entity-own-conds)
          all-conds-to-follow   (concat all-direct-anc-conds conds-for-ancestry)
          conditional-ancestors (when (seq all-conds-to-follow)
                                  (collect-all-ancestral-entities all-data all-conds-to-follow))
          ancestors             (vec (vals (into {} (map (juxt :path identity)
                                                         (concat direct-ancestors conditional-ancestors)))))
          ancestor-setup        (first (map deduplicate-ancestor-conditionals
                                            (expand-ancestor-or-branches ancestors)))]

      (case (:type trigger)

        ;; --------- :compound-output ---------
        ;; :in input with output :sub-conditionals: outputs drive Examples:, each input value
        ;; becomes fixed setup → cross-product of (module-combo × input-value) → @core outlines
        :compound-output
        (let [in-cond       (:in-cond trigger)
              in-values     (:values in-cond)
              out-conds     (remove #(or (has-research-dependency? %) (conditionally-set? %)) (:output-conds trigger))
              rows-by-combo (group-conditionals-by-module-combo entity-mod out-conds)
              multi?        (> (count rows-by-combo) 1)
              var-name      (get-in in-cond [:group-variable :group-variable/translated-name])]
          (for [[module-combo combo-outs] rows-by-combo
                value                     in-values
                :when                     (and (not= module-combo :unsupported) (seq combo-outs))]
            (let [filtered-anc  (filter #(= (:type %) :module)
                                        (filter-conditionals-by-module ancestor-setup module-combo))
                  input-cond    (-> in-cond
                                    (assoc :operator :equal :values [value])
                                    (dissoc :sub-conditionals :sub-conditional-operator))
                  anc-conds     (deduplicate-ancestor-conditionals filtered-anc)
                  example-rows  (keep conditional->output-row combo-outs)
                  sname         (str entity-name " is displayed"
                                     (when multi?
                                       (str " with " (module-set-to-title module-combo) " outputs"))
                                     " (" var-name " " value ")")
                  ;; Render inline: output placeholder first, then fixed input block, then Then.
                  ;; This matches the committed step order (output trigger → input setup → Then).
                  given-stmt    (module-to-given-statement module-combo)
                  input-row     (conditional->input-row input-cond)
                  input-table   (render-table (present-headers [:submodule :group :subgroup :value]
                                                               [input-row])
                                              [input-row])
                  target-row    (target->table-row entity-path)
                  then-headers  (present-headers [:submodule :group :value] [target-row])
                  when-step     (str STEP-INDENT "When this output path is selected <submodule> : <group> : <value>")
                  scenario-text (str/join "\n"
                                          (concat
                                           [(str SCENARIO-INDENT "@core")
                                            (str SCENARIO-INDENT "Scenario Outline: " sname)
                                            (str STEP-INDENT given-stmt)]
                                           (render-setup-blocks anc-conds)
                                           [when-step
                                            (str STEP-INDENT "When these input paths are entered")
                                            input-table
                                            (str STEP-INDENT "Then the following input paths are displayed:")
                                            (render-table then-headers [target-row])
                                            ""
                                            (str STEP-INDENT "Examples: This scenario is repeated for each of these rows")
                                            (render-table [:submodule :group :value] example-rows)]))]
              {:text       scenario-text
               :module     module-combo
               :group-path entity-path})))

        ;; --------- :output-or ---------
        ;; Group the OR output rows by module-combo; one @core outline per group
        :output-or
        (let [or-conds      (remove #(or (has-research-dependency? %) (conditionally-set? %)) (:conditionals trigger))
              rows-by-combo (group-conditionals-by-module-combo entity-mod or-conds)
              multi?        (> (count rows-by-combo) 1)]
          (keep (fn [[module-combo or-conds-for-combo]]
                  (when (and (not= module-combo :unsupported)
                             (seq or-conds-for-combo))
                    ;; For output-or, strip ancestor group-variable conds (entity outputs
                    ;; enable both entity and its ancestors)
                    (let [filtered-anc (filter #(= (:type %) :module)
                                               (filter-conditionals-by-module ancestor-setup module-combo))
                          setup-conds  (deduplicate-ancestor-conditionals filtered-anc)
                          example-rows (keep conditional->output-row or-conds-for-combo)
                          sname        (str entity-name " is displayed"
                                            (when multi?
                                              (str " with " (module-set-to-title module-combo) " outputs")))]
                      {:text       (render-outline sname module-combo setup-conds
                                                   :output false example-rows entity-path :core)
                       :module     module-combo
                       :group-path entity-path})))
                rows-by-combo))

        ;; --------- :input-in ---------
        ;; @core outline (first value row) + @extended outline (all value rows)
        :input-in
        (let [in-cond       (:conditional trigger)
              path          (get-in in-cond [:group-variable :path])
              var-name      (get-in in-cond [:group-variable :group-variable/translated-name])
              comps         (path->table-components path)
              has-subgroup? (= 3 (count comps))
              all-values    (:values in-cond)
              ;; Filter research from ancestor setup
              anc-paths     (keep #(get-in % [:group-variable :path]) ancestor-setup)
              anc-mods      (extract-modules-from-paths anc-paths)
              all-mods      (cond-> anc-mods entity-mod (conj entity-mod))
              module-combo  (determine-module-combination all-mods)]
          (when (not= module-combo :unsupported)
            (let [filtered-anc (filter-conditionals-by-module ancestor-setup module-combo)
                  setup-conds  (deduplicate-ancestor-conditionals filtered-anc)
                  make-row     (fn [v]
                                 (cond-> {:submodule (first comps)}
                                   (>= (count comps) 2) (assoc :group (second comps))
                                   has-subgroup?        (assoc :subgroup (nth comps 2))
                                   v                    (assoc :value v)))
                  first-row    [(make-row (first all-values))]
                  all-rows     (map make-row all-values)
                  sname        (str entity-name " is displayed with these " var-name)]
              (when-not (has-any-research-conditional? setup-conds)
                [{:text       (render-outline sname module-combo setup-conds
                                              :input has-subgroup? first-row entity-path :core)
                  :module     module-combo
                  :group-path entity-path}
                 {:text       (render-outline (str sname " (Extended)") module-combo setup-conds
                                              :input has-subgroup? all-rows entity-path :extended)
                  :module     module-combo
                  :group-path entity-path}]))))

        ;; --------- :single ---------
        ;; All entity conditionals fixed → fold into setup tables → plain Scenario
        :single
        (let [entity-conds  (filter (fn [c]
                                      (or (= (:type c) :module)
                                          (and (= (:type c) :group-variable)
                                               (some? (:group-variable c)))))
                                    (:conditionals trigger))
              all-setup-raw (concat ancestor-setup entity-conds)
              all-paths     (keep #(get-in % [:group-variable :path]) all-setup-raw)
              all-mods      (cond-> (extract-modules-from-paths all-paths)
                              entity-mod (conj entity-mod))
              module-combo  (determine-module-combination all-mods)]
          (when (not= module-combo :unsupported)
            (let [mod-conds   (->> all-setup-raw
                                   (filter #(= (:type %) :module))
                                   (map #(set (map keyword (:values %)))))
                  compatible? (every? #(set/subset? % module-combo) mod-conds)]
              (when compatible?
                (let [filtered-anc    (filter-conditionals-by-module ancestor-setup module-combo)
                      filtered-entity (filter-conditionals-by-module entity-conds module-combo)
                      all-setup-conds (remove #(or (has-research-dependency? %) (conditionally-set? %))
                                              (deduplicate-ancestor-conditionals
                                               (concat filtered-anc filtered-entity)))]
                  [{:text       (render-scenario (build-scenario-name filtered-entity entity-name)
                                                 module-combo all-setup-conds entity-path)
                    :module     module-combo
                    :group-path entity-path}])))))

        ;; fallback
        nil))))

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
   Group by entity path only — all module-combo outlines for one entity go in one file.

   Arguments:
   - scenarios: Sequence of scenario maps

   Returns:
   Map of entity-path -> scenarios list"
  [scenarios]
  (group-by :group-path scenarios))

;; ===========================================================================================================
;; Filename Generation Functions
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
  "Create filename from entity path.
   Pattern: {entity-module}-input_{parent-groups}_{target-group}.feature
   The module prefix is always derived from path[0] (the entity's own module),
   regardless of what worksheet module-combo the scenarios use.

   Arguments:
   - path: Entity path vector like ['Surface' 'Fuel Moisture' 'By Size Class']

   Returns:
   String like 'surface-input_fuel-moisture_by-size-class.feature'"
  [path]
  (let [module-str      (name (extract-module-from-path path))
        path-without-mi (remove keyword? (rest path))
        sanitized-parts (map (comp sanitize-path-component pname) path-without-mi)]
    (str module-str "-input_" (str/join "_" sanitized-parts) ".feature")))

(defn- leaf-key-segment
  "Sanitized last colon-segment of the path's leaf :key, or nil if the leaf has no key."
  [path]
  (let [leaf (last path)
        k    (when (map? leaf) (:key leaf))]
    (when k (sanitize-path-component (last (str/split k #":"))))))

(defn- disambiguated-filename
  "Like generate-feature-filename but derives the leaf component from the :key rather
   than the display name, allowing siblings that share a display name to differ."
  [path]
  (let [module-str (name (extract-module-from-path path))
        non-kw     (remove keyword? (rest path))
        head       (map (comp sanitize-path-component pname) (butlast non-kw))
        leaf       (or (leaf-key-segment path)
                       (sanitize-path-component (pname (last non-kw))))]
    (str module-str "-input_" (str/join "_" (concat head [leaf])) ".feature")))

(defn assign-feature-filenames
  "Map each entity path to a unique .feature filename.
   Non-colliding paths keep the display-name filename from generate-feature-filename.
   Within any collision group each path is rebuilt via disambiguated-filename.
   Throws if names are still not unique after disambiguation (leaf :keys are globally unique,
   so this should never happen)."
  [paths]
  (let [base     (map (juxt identity generate-feature-filename) paths)
        by-name  (group-by second base)
        assigned (into {}
                       (mapcat (fn [[_ entries]]
                                 (if (= 1 (count entries))
                                   [[(ffirst entries) (second (first entries))]]
                                   (map (fn [[p _]] [p (disambiguated-filename p)]) entries)))
                               by-name))]
    (let [dupes (->> assigned (group-by val) (filter (fn [[_ v]] (> (count v) 1))))]
      (when (seq dupes)
        (throw (ex-info "Unresolvable feature filename collision after disambiguation."
                        {:duplicates (mapv (fn [[f es]] {:filename f :paths (mapv first es)}) dupes)}))))
    assigned))

;; ===========================================================================================================
;; Feature File Writing
;; ===========================================================================================================

(defn generate-feature-header
  "Create the feature header block (file-level @core tag + Feature title).
   Module title uses the actual worksheet module combo (e.g. 'Surface & Crown').

   Arguments:
   - path: Entity path vector
   - module-combo: Module set (e.g. #{:surface :crown})

   Returns:
   String like '@core\\nFeature: Surface & Crown Input - Canopy Fuel\\n'"
  [path module-combo]
  (let [module-str (module-set-to-title module-combo)
        path-str   (format-path-for-gherkin path)]
    (str "@core\nFeature: " module-str " Input - " path-str "\n")))

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
   Delete .feature files directly in features-dir (non-recursive) except
   core_conditional_scenarios.feature. Subdirectories such as results-page/ are
   left untouched — those are managed by generate-results-feature-files!.

   Arguments:
   - features-dir: Features directory path

   Returns:
   Number of files deleted"
  [features-dir]
  (let [dir           (io/file features-dir)
        files         (or (.listFiles dir) [])
        feature-files (filter #(and (.isFile %)
                                    (str/ends-with? (.getName %) ".feature"))
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
     (let [all-data                (load-test-matrix edn-path)

           ;; Get all entities (groups and submodules) from map values
           all-entities            (vals all-data)

           ;; Separate groups from submodules
           groups                  (filter :group/translated-name all-entities)
           submodules              (filter :submodule/name all-entities)

           ;; Filter out research groups and submodules
           non-research-groups     (remove should-skip-group? groups)
           non-research-submodules (remove should-skip-submodule? submodules)

           ;; Generate scenarios for all groups and submodules (pass all-data)
           group-scenarios         (mapcat #(generate-scenarios-for-group all-data %) non-research-groups)
           submodule-scenarios     (mapcat #(generate-scenarios-for-submodule all-data %) non-research-submodules)
           all-scenarios           (concat group-scenarios submodule-scenarios)

           ;; Group scenarios by entity path (all module-combo outlines in one file)
           grouped-scenarios       (group-scenarios-by-feature all-scenarios)

           ;; Assign unique filenames — disambiguates siblings that share a display name
           ;; by falling back to their leaf :key segment. Must succeed before any deletion.
           filenames               (assign-feature-filenames (keys grouped-scenarios))

           ;; Safe to delete now: scenarios and filenames are fully computed.
           _                       (delete-old-generated-files features-dir)

           ;; Write files
           files-written           (atom 0)
           scenarios-generated     (atom 0)]

       (doseq [[entity-path scenarios] grouped-scenarios]
         (let [module-combo (or (:module (first scenarios)) #{(extract-module-from-path entity-path)})
               header       (generate-feature-header entity-path module-combo)
               filename     (get filenames entity-path)
               file-path    (str features-dir filename)]
           (write-feature-file file-path header scenarios)
           (swap! files-written inc)
           (swap! scenarios-generated + (count scenarios))))

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
