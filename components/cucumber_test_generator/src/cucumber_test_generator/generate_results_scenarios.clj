(ns cucumber-test-generator.generate-results-scenarios
  "Phase-2 feature-file generation for conditionally-set output test cases.

   Reads conditional_outputs_matrix.edn (written by Phase-1
   cucumber-test-generator.conditional-outputs) and emits one
   results-page-style Cucumber scenario per entry, matching the shape of
   features/test_results_page.feature:

     @core
     Feature: Surface Results - <output name>

       @core
       Scenario: <output name> is displayed in results
         Given I have started a new Surface Worksheet in Guided Mode
         When these input paths are selected
           | submodule | group | subgroup | value |
           | ...       | ...   | ...      | ...   |
         Then \"the following outputs are displayed in the results page\"
           | output          |
           | <output name>   |

   Rendering primitives are copied from generate-scenarios (render-table,
   present-headers, module-to-given-statement, indent constants, filename/header
   helpers) rather than imported, to keep both namespaces independently usable."
  (:require [clojure.edn     :as edn]
            [clojure.java.io :as io]
            [clojure.string  :as str]))

;;; ============================================================================
;;; Gherkin constants (mirrors generate-scenarios)
;;; ============================================================================

(def ^:const SCENARIO-INDENT "  ")
(def ^:const STEP-INDENT "    ")
(def ^:const TABLE-INDENT "      ")

;;; ============================================================================
;;; Rendering primitives (copied from generate-scenarios to avoid coupling)
;;; ============================================================================

(defn- present-headers
  "Return only headers that have a non-empty value in at least one row."
  [preferred-order rows]
  (filter (fn [h] (some #(seq (str (get % h ""))) rows)) preferred-order))

(defn- render-table
  "Column-aligned Gherkin data table string."
  [headers rows]
  (when (and (seq headers) (seq rows))
    (let [get-cell   (fn [row h] (str (get row h "")))
          col-widths (map (fn [h]
                            (max (count (name h))
                                 (apply max 0 (map #(count (get-cell % h)) rows))))
                          headers)
          pad        (fn [s w] (str s (apply str (repeat (- w (count s)) " "))))
          fmt-row    (fn [cells]
                       (str TABLE-INDENT "| "
                            (str/join " | " (map pad cells col-widths))
                            " |"))
          header-row (fmt-row (map name headers))
          data-rows  (map #(fmt-row (map (fn [h] (get-cell % h)) headers)) rows)]
      (str/join "\n" (cons header-row data-rows)))))

(defn- effective-module-combo
  "Promote a module or module list to the effective worksheet combo.
   Mirrors determine-module-combination from generate_scenarios.clj:
   Crown/Mortality/Contain always require Surface."
  [required-modules module]
  (let [m-set (->> (or (seq required-modules) [(or module "surface")])
                   (map #(keyword (str/lower-case %)))
                   set)]
    (cond
      (= m-set #{:surface})            #{:surface}
      (or (= m-set #{:crown})
          (= m-set #{:surface :crown}))     #{:surface :crown}
      (or (= m-set #{:mortality})
          (= m-set #{:surface :mortality})) #{:surface :mortality}
      (or (= m-set #{:contain})
          (= m-set #{:surface :contain}))   #{:surface :contain}
      :else                            m-set)))

(defn- module-to-given-statement
  "Map a module combo set to its 'Given I have started a new … Worksheet' step."
  [module-combo]
  (cond
    (= module-combo #{:surface})            "Given I have started a new Surface Worksheet in Guided Mode"
    (= module-combo #{:surface :crown})     "Given I have started a new Surface & Crown Worksheet in Guided Mode"
    (= module-combo #{:surface :mortality}) "Given I have started a new Surface & Mortality Worksheet in Guided Mode"
    (= module-combo #{:surface :contain})   "Given I have started a new Surface & Contain Worksheet in Guided Mode"
    :else (str "Given I have started a new "
               (str/join " & " (map #(str/capitalize (name %)) (sort-by name module-combo)))
               " Worksheet in Guided Mode")))

;;; ============================================================================
;;; File naming
;;; ============================================================================

(defn- sanitize
  "Lowercase, replace spaces/slashes with hyphens, strip non-alphanumeric."
  [s]
  (-> (str s)
      str/lower-case
      (str/replace #"[\s/]+" "-")
      (str/replace #"[^a-z0-9\-]" "")))

(defn- feature-filename
  "Generate a feature filename: results-page_{module}_{output}.feature"
  [module output-name]
  (str "results-page_" (sanitize module) "_" (sanitize output-name) ".feature"))

;;; ============================================================================
;;; Scenario rendering
;;; ============================================================================

;; Defined later (Baseline loading section); render-output-outline sorts its
;; Examples rows with it.
(declare sort-by-vms-order)

(defn- find-varying-row
  "Return the first required-input row that carries a :values list of length ≥2,
   or nil when no such row exists."
  [required-inputs]
  (some #(when (>= (count (:values %)) 2) %) required-inputs))

(defn- render-scenario
  "Render one test case as a plain Gherkin Scenario string (no Examples table)."
  [{:keys [output-name module required-modules required-outputs required-inputs]}]
  (let [module-combo   (effective-module-combo required-modules module)
        given          (str SCENARIO-INDENT "@core\n"
                            SCENARIO-INDENT "Scenario: "
                            output-name " is displayed in results when inputs are set")
        given-step     (str STEP-INDENT (module-to-given-statement module-combo))
        output-headers (when (seq required-outputs)
                         (present-headers [:submodule :group :value] required-outputs))
        output-step    (when (seq required-outputs)
                         (str STEP-INDENT "When these output paths are selected\n"
                              (render-table output-headers required-outputs)))
        input-headers  (present-headers [:submodule :group :subgroup :value] required-inputs)
        input-step     (str STEP-INDENT "When these input paths are selected\n"
                            (render-table input-headers required-inputs))
        then-step      (str STEP-INDENT "Then \"the following outputs are displayed in the results page\"\n"
                            (render-table [:output] [{:output output-name}]))]
    (str/join "\n" (remove nil? [given given-step output-step input-step then-step]))))

(defn- render-scenario-outline
  "Render a Scenario Outline for a test case that has a multi-value (:in) input.

   The varying input is emitted as a parameterized step,
   `When this input path is entered <submodule> : <group> [: <subgroup>] : <value>`,
   with its path columns supplied by the Examples table. Placeholders MUST live in
   the step text: the tegere runner reifies Scenario Outline variables only in a
   step's ::text, never in an attached data table (tegere/parser.clj reify-outline-step),
   so a `<placeholder>` inside the 'these input paths are selected' table would reach
   the step verbatim and fail ('Could not find or select option: <species>'). The
   non-varying inputs stay in 'these input paths are selected' tables, split around
   the varying step so VMS / visibility ordering is preserved.

   tag is :core (one representative value) or :extended (all values)."
  [{:keys [output-name module required-modules required-outputs required-inputs]}
   varying-row
   tag]
  (let [module-combo   (effective-module-combo required-modules module)
        tag-str        (if (= tag :core) "@core" "@extended")
        sname          (if (= tag :core)
                         (str output-name " is displayed in results when inputs are set")
                         (str output-name " is displayed in results when inputs are set (Extended)"))
        outline-header (str SCENARIO-INDENT tag-str "\n"
                            SCENARIO-INDENT "Scenario Outline: " sname)
        given-step     (str STEP-INDENT (module-to-given-statement module-combo))
        output-headers (when (seq required-outputs)
                         (present-headers [:submodule :group :value] required-outputs))
        output-step    (when (seq required-outputs)
                         (str STEP-INDENT "When these output paths are selected\n"
                              (render-table output-headers required-outputs)))
        ;; Split static inputs around the varying row, preserving sorted VMS order so
        ;; the varying value is still set in the same position (visibility-gating
        ;; inputs such as Canopy Height stay after the species that reveals them).
        [before after] (split-with #(not= % varying-row) required-inputs)
        after          (rest after)
        sel-step       (fn [rows]
                         (when (seq rows)
                           (str STEP-INDENT "When these input paths are selected\n"
                                (render-table (present-headers [:submodule :group :subgroup :value] rows) rows))))
        before-step    (sel-step before)
        after-step     (sel-step after)
        ;; Varying input — placeholders in step TEXT (reified by tegere). Use the
        ;; 4-arg form when the row has a subgroup, else the 3-arg form, matching the
        ;; registered steps in steps/When.clj.
        has-subgroup?  (seq (str (:subgroup varying-row "")))
        varying-step   (str STEP-INDENT
                            (if has-subgroup?
                              "When this input path is entered <submodule> : <group> : <subgroup> : <value>"
                              "When this input path is entered <submodule> : <group> : <value>"))
        then-step      (str STEP-INDENT "Then \"the following outputs are displayed in the results page\"\n"
                            (render-table [:output] [{:output output-name}]))
        ;; Examples table — one row per value, carrying the full input path columns
        example-values (if (= tag :core)
                         [(first (:values varying-row))]
                         (:values varying-row))
        ex-cols        (if has-subgroup? [:submodule :group :subgroup :value] [:submodule :group :value])
        examples-rows  (mapv (fn [v]
                               (-> (select-keys varying-row [:submodule :group :subgroup])
                                   (assoc :value v)))
                             example-values)
        examples-step  (str STEP-INDENT "Examples: This scenario is repeated for each of these rows\n"
                            (render-table ex-cols examples-rows))]
    (str/join "\n" (remove nil? [outline-header given-step output-step
                                 before-step varying-step after-step
                                 then-step "" examples-step]))))

(defn- render-output-outline
  "Render a Scenario Outline for an output whose visibility is gated by an :or over
   several outputs — selecting ANY ONE of them reveals the asserted output. Each
   gating output becomes one Examples row, selected via a parameterized
   `When this output path is selected <submodule> : <group> : <value>` step
   (registered in steps/When.clj). The baseline outputs (Direction Mode / Surface
   Fire that make the worksheet compute) and all required inputs stay static.

   tag is :core (one representative gating output) or :extended (all of them)."
  [{:keys [output-name module required-modules required-outputs required-inputs gating-outputs]}
   tag]
  (let [module-combo   (effective-module-combo required-modules module)
        tag-str        (if (= tag :core) "@core" "@extended")
        sname          (if (= tag :core)
                         (str output-name " is displayed in results when inputs are set")
                         (str output-name " is displayed in results when inputs are set (Extended)"))
        outline-header (str SCENARIO-INDENT tag-str "\n"
                            SCENARIO-INDENT "Scenario Outline: " sname)
        given-step     (str STEP-INDENT (module-to-given-statement module-combo))
        ;; Static outputs = the merged outputs minus the gating set (the baseline
        ;; Direction Mode + Surface Fire prerequisites), set once for every row.
        gating-set     (set gating-outputs)
        static-outputs (remove gating-set required-outputs)
        out-headers    (when (seq static-outputs)
                         (present-headers [:submodule :group :value] static-outputs))
        output-step    (when (seq static-outputs)
                         (str STEP-INDENT "When these output paths are selected\n"
                              (render-table out-headers static-outputs)))
        input-headers  (when (seq required-inputs)
                         (present-headers [:submodule :group :subgroup :value] required-inputs))
        input-step     (when (seq required-inputs)
                         (str STEP-INDENT "When these input paths are selected\n"
                              (render-table input-headers required-inputs)))
        ;; Varying output — placeholders in step TEXT (reified by tegere), mirroring
        ;; the input outline. Output rows never carry a subgroup, so the 3-arg form.
        varying-step   (str STEP-INDENT
                            "When this output path is selected <submodule> : <group> : <value>")
        then-step      (str STEP-INDENT "Then \"the following outputs are displayed in the results page\"\n"
                            (render-table [:output] [{:output output-name}]))
        gating-sorted  (sort-by-vms-order gating-outputs)
        example-rows   (if (= tag :core) [(first gating-sorted)] gating-sorted)
        examples-rows  (mapv #(select-keys % [:submodule :group :value]) example-rows)
        examples-step  (str STEP-INDENT "Examples: This scenario is repeated for each of these rows\n"
                            (render-table [:submodule :group :value] examples-rows))]
    (str/join "\n" (remove nil? [outline-header given-step output-step
                                 input-step varying-step
                                 then-step "" examples-step]))))

;;; ============================================================================
;;; Feature file rendering
;;; ============================================================================

(defn- render-feature
  "Render the full .feature file content for one test case.

   When the asserted output is gated by an :or over ≥2 outputs (any one reveals it),
   emits two output Scenario Outlines (@core: one representative gating output;
   @extended: all of them). Otherwise, when any required-input row carries a :values
   list of length ≥2 (an :in range), emits two input Scenario Outlines. Otherwise
   falls back to a single plain Scenario."
  [{:keys [output-name module required-inputs required-outputs-operator gating-outputs]
    :as   test-case}]
  (let [module-title (or module "")
        header       (str "@core\n"
                          "Feature: " module-title " Results - " output-name "\n")
        ;; :or gate over multiple outputs → vary the output selection (inputs stay
        ;; static). Takes precedence over an :in input outline; a case with both would
        ;; keep its varying input at its representative value.
        or-outputs?  (and (= :or required-outputs-operator)
                          (>= (count gating-outputs) 2))
        varying-row  (find-varying-row required-inputs)
        body         (cond
                       or-outputs?
                       (str/join "\n\n"
                                 [(render-output-outline test-case :core)
                                  (render-output-outline test-case :extended)])
                       varying-row
                       (str/join "\n\n"
                                 [(render-scenario-outline test-case varying-row :core)
                                  (render-scenario-outline test-case varying-row :extended)])
                       :else
                       (render-scenario test-case))]
    (str header "\n" body)))

;;; ============================================================================
;;; Combined matrix loading
;;; ============================================================================

(defn- load-combined-matrix
  "Read the combined test_matrix_data.edn. Returns a map with
   :input-visibility and :results-visibility top-level keys."
  [edn-path]
  (let [raw (-> (slurp edn-path) edn/read-string)]
    ;; Support both combined format and legacy flat format
    (if (contains? raw :results-visibility)
      raw
      {:input-visibility raw :results-visibility {}})))

;;; ============================================================================
;;; Baseline loading + forward-dependency filtering
;;; ============================================================================

(defn- load-baselines
  "Read and parse results_test_baselines.edn."
  [baselines-path]
  (-> (slurp baselines-path) edn/read-string))

(defn- input-key
  "Dedup key for an input row — submodule + group + subgroup."
  [row]
  [(:submodule row) (:group row) (:subgroup row)])

;; Module display order for multi-module worksheet sorting.
;; Surface inputs always precede Crown/Mortality/Contain inputs in the wizard.
(def ^:private module-sort-order
  {"Surface" 0 "Crown" 1 "Mortality" 2 "Contain" 3})

;; Same worksheet order, keyed by module-combo keyword. Used to assemble a
;; combo's baseline from each constituent module's per-module entry (Surface first).
(def ^:private module-combo-order
  {:surface 0 :crown 1 :mortality 2 :contain 3})

(defn- sort-by-vms-order
  "Sort rows by VMS hierarchy: module → submodule/order → group/order → group-variable/order.
   Nils / unknown modules sort last."
  [rows]
  (sort-by (juxt #(get module-sort-order (:module %) 99)
                 #(or (:submodule/order %) 999)
                 #(or (:group/order %) 999)
                 #(or (:group-variable/order %) 999))
           rows))

(defn- build-output-order-index
  "Build a [module submodule group value] → {VMS order fields} map from every
   test case's :required-outputs. Output rows carry the authoritative VMS order,
   so this lets baseline output rows (which no longer hand-code order) recover it
   — the baseline output groups (Direction Mode / Heading, Surface Fire / Rate of
   Spread) always recur as test-case outputs, so they are always covered."
  [matrix]
  (reduce (fn [m r]
            (assoc m [(:module r) (:submodule r) (:group r) (:value r)]
                   (select-keys r [:submodule/order :group/order :group-variable/order])))
          {}
          (mapcat :required-outputs (vals matrix))))

(defn- find-group-entries
  "Return ALL :input-visibility entries whose path contains submodule :name and
   ends with the row's deepest hierarchy element — :subgroup when present, else
   :group. Nested groups (e.g. 'Wind Direction (from upslope)' under 'Wind and
   slope are') carry their own visibility conditional under the subgroup path, so
   matching on :group alone would wrongly resolve to the parent group's gate.
   Multiple entries can exist when the same name appears in different modules with
   different conditionals (e.g. 'Wind and slope are' in both Surface and Crown)."
  [input-visibility {:keys [submodule group subgroup]}]
  (let [target (or subgroup group)]
    (keep (fn [[path entity]]
            (when (and (some #(= (:name %) submodule) path)
                       (= (:name (last (remove keyword? path))) target))
              entity))
          input-visibility)))

(defn- find-submodule-entries
  "Return :input-visibility INPUT submodule-level entries (whose value carries
   :submodule/name) matching the row's :submodule, scoped to the row's :module when
   known (a submodule name like 'Size' or 'Weather' recurs across modules). A
   baseline input is only visible when its submodule is visible, so these
   conditionals gate it in addition to its own group/subgroup conditional. E.g. the
   Mortality 'Scorch' submodule is shown only for species in its :in list, hiding
   'Air Temperature' for outputs like Bole Char Height whose species do not require
   scorch inputs. Air Temperature has no group-level entry, so without this its
   submodule gate would never be evaluated.

   Only :input submodules gate input rows. A same-named OUTPUT submodule (e.g. Surface
   'Size' exists as both input:size and output:size) can carry a module-only
   conditional that conditionals-pass? treats as always satisfied; since the gate
   passes if ANY matched entry passes, including the output entry would always satisfy
   it and defeat the real input gate (e.g. 'Elapsed Time' would leak into every feature
   instead of only those selecting a Size output)."
  [input-visibility {:keys [module submodule]}]
  (keep (fn [[path entity]]
          (when (and (:submodule/name entity)
                     (= (:submodule/io entity) :input)
                     (= (:submodule/name entity) submodule)
                     (or (nil? module) (some #(= (:name %) module) path)))
            entity))
        input-visibility))

(defn- conditionals-pass?
  "Evaluate the :conditionals block from an :input-visibility entry against
   known-values (a map of group-name → value-string).
   Supports :group-variable type with :equal and :in operators."
  [conditionals-block known-values]
  (let [conds    (get-in conditionals-block [:conditionals :conditionals])
        operator (get-in conditionals-block [:conditionals :conditionals-operator])]
    (if (empty? conds)
      true
      (let [results (map (fn [{:keys [type operator values group-variable]}]
                           (case type
                             :group-variable
                             ;; Key on the conditional gv's :io. Output gates are
                             ;; seeded by selected-output value (== translated-name);
                             ;; input gates resolve against the gv's deepest group
                             ;; name, matching how known-values keys input rows.
                             (let [gv-key  (if (= (:io group-variable) :output)
                                             (:group-variable/translated-name group-variable)
                                             (:name (last (remove keyword? (:path group-variable)))))
                                   cur-val (get known-values gv-key)]
                               (case operator
                                 :equal (= cur-val (first values))
                                 :in    (some #(= cur-val %) values)
                                 false))
                             ;; :module type — treat as satisfied (module already chosen by Given)
                             :module true
                             false))
                         conds)]
        (if (= operator :or)
          (some true? results)
          (every? true? results))))))

(defn- merge-with-baselines
  "Merge a test case with the worksheet-combo baseline.
   The baseline is assembled per-module: each module in the effective combo is
   looked up by its own key (:surface, :mortality, :crown, :contain) and their
   :baseline-inputs / :baseline-outputs are concatenated in worksheet order
   (Surface first). No module re-declares another module's rows.

   :baseline-outputs are prepended before test-case-specific :required-outputs (deduped).
   :baseline-inputs are filtered via :input-visibility conditionals and merged with
   test-case-specific inputs (test-case values override baseline by input-key)."
  [test-case baselines input-visibility output-order-index]
  (let [module-combo  (effective-module-combo (:required-modules test-case) (:module test-case))
        ordered-keys  (sort-by #(get module-combo-order % 99) module-combo)
        baseline      {:baseline-outputs (vec (mapcat #(:baseline-outputs (get baselines % {})) ordered-keys))
                       :baseline-inputs  (vec (mapcat #(:baseline-inputs (get baselines % {})) ordered-keys))}
        ;; Baseline output rows no longer hand-code VMS order; recover it from the
        ;; matrix-derived index so the downstream drop/dedup/sort pipeline is
        ;; unchanged.
        base-outputs  (mapv #(merge % (get output-order-index
                                           [(:module %) (:submodule %) (:group %) (:value %)]))
                            (get baseline :baseline-outputs []))
        tc-inputs     (:required-inputs test-case)
        ;; Drop baseline inputs the test case already provides (same submodule/
        ;; group/subgroup). The test-case value is authoritative; keeping the
        ;; baseline row would otherwise pollute known-values during the fixpoint
        ;; and mis-gate dependent inputs (e.g. baseline "Wind and slope are =
        ;; Aligned" overwriting a test case's "Not Aligned", which gates the
        ;; "Wind Direction (from upslope)" input).
        tc-keys       (into #{} (map input-key) tc-inputs)
        base-inputs   (remove #(tc-keys (input-key %)) (get baseline :baseline-inputs []))
        ;; Merge baseline outputs before test-case outputs, deduped
        tc-outputs    (get test-case :required-outputs [])
        ;; Baseline outputs are fallback defaults (a direction mode + a fire-behavior
        ;; result output) that make the worksheet compute. Drop any baseline output
        ;; whose [:submodule :group] the test case already selects — the test's own
        ;; output covers that group, so the default is redundant (and for single-select
        ;; groups like Direction Mode, only one value may be active). E.g. Backing Flame
        ;; Length keeps only its own Flame Length and not the baseline Rate of Spread.
        tc-out-groups (into #{} (map (juxt :submodule :group)) tc-outputs)
        base-outputs  (remove #(tc-out-groups [(:submodule %) (:group %)]) base-outputs)
        all-outputs   (->> (concat base-outputs tc-outputs)
                           (reduce (fn [acc row]
                                     (if (some #(= % row) acc) acc (conj acc row)))
                                   [])
                           vec)
        ;; Seed known-values: input group→value AND all selected output names→"true"
        ;; so output-gated input conditionals (e.g. "Wind and slope are") pass
        init-known    (into {}
                            (concat
                             (map (fn [{:keys [group subgroup value]}] [(or subgroup group) value]) tc-inputs)
                             (map (fn [{:keys [value]}] [value "true"]) all-outputs)))
        ;; Fixpoint: include baseline inputs whose conditionals pass, in declaration order.
        ;; Uses vector + seen-set to avoid array-map overflow losing ordering.
        included-vec  (loop [remaining (vec base-inputs)
                             known     init-known
                             acc       []
                             seen      #{}]
                        (let [newly     (remove (fn [row] (seen (input-key row)))
                                                (filter (fn [row]
                                                          ;; Visible only when BOTH its group/subgroup conditional
                                                          ;; AND its submodule conditional pass (empty = no gate).
                                                          (let [g (find-group-entries input-visibility row)
                                                                s (find-submodule-entries input-visibility row)]
                                                            (and (or (empty? g) (some #(conditionals-pass? % known) g))
                                                                 (or (empty? s) (some #(conditionals-pass? % known) s)))))
                                                        remaining))
                              new-acc   (into acc newly)
                              new-seen  (into seen (map input-key newly))
                              new-known (into known (map (fn [{:keys [group subgroup value]}]
                                                           [(or subgroup group) value]) newly))
                              leftover  (remove (fn [row] (new-seen (input-key row))) remaining)]
                          (if (= (count new-acc) (count acc))
                            new-acc
                            (recur leftover new-known new-acc new-seen))))
        ;; Input order is implicit: the declaration order of :baseline-inputs (already
        ;; worksheet order). Several baseline inputs (DBH, Air Temperature) appear in no
        ;; test case, so declaration order is the only complete ordering source.
        base-order    (into {} (map-indexed (fn [i r] [(input-key r) i])
                                            (get baseline :baseline-inputs [])))
        ;; tc-inputs override baseline values for same key; new keys appended after
        tc-override   (into {} (map #(vector (input-key %) %) tc-inputs))
        base-keys     (set (map input-key included-vec))
        merged-raw    (into (mapv #(get tc-override (input-key %) %) included-vec)
                            (remove #(base-keys (input-key %)) tc-inputs))]
    (assoc test-case
           ;; stable sort: any test-case-only input (not in baseline) trails in order
           :required-inputs  (sort-by #(get base-order (input-key %) Long/MAX_VALUE) merged-raw)
           :required-outputs (sort-by-vms-order all-outputs)
           ;; The test case's own outputs (the conditional gate). Preserved separately
           ;; from the baseline outputs so an :or-gated render can vary just these.
           :gating-outputs   (vec tc-outputs))))

;;; ============================================================================
;;; File cleanup (only removes results-page_*.feature files)
;;; ============================================================================

(defn- delete-results-feature-files
  "Delete previously generated results-page_*.feature files in features-dir."
  [features-dir]
  (let [dir   (io/file features-dir)
        files (filter #(and (.isFile %)
                            (str/starts-with? (.getName %) "results-page_")
                            (str/ends-with? (.getName %) ".feature"))
                      (file-seq dir))]
    (doseq [f files] (.delete f))
    (count files)))

;;; ============================================================================
;;; Main entry point
;;; ============================================================================

(defn generate-results-feature-files!
  "Generate results-page Cucumber feature files from the combined test_matrix_data.edn.

   For each :results-visibility test case, writes one feature file asserting that the
   conditionally-set output appears on the results page when its required inputs are set.
   Uses the :input-visibility section to determine which baseline inputs are visible
   (forward-dependency filtering).

   Arguments:
   - edn-path     — (optional) combined matrix EDN; default 'development/test_matrix_data.edn'
   - features-dir — (optional) output dir; default 'features/results-page/'
   - baselines-path — (optional); default 'development/results_test_baselines.edn'

   Returns:
   {:features-dir '...' :files-written N}"
  ([]
   (generate-results-feature-files!
    "development/test_matrix_data.edn"
    "features/results-page/"
    "development/results_test_baselines.edn"))
  ([edn-path]
   (generate-results-feature-files! edn-path "features/results-page/" "development/results_test_baselines.edn"))
  ([edn-path features-dir]
   (generate-results-feature-files! edn-path features-dir "development/results_test_baselines.edn"))
  ([edn-path features-dir baselines-path]
   (let [combined         (load-combined-matrix edn-path)
         matrix           (:results-visibility combined)
         input-visibility (:input-visibility combined)
         baselines        (load-baselines baselines-path)
         output-order-idx (build-output-order-index matrix)
         _                (println (format "Loaded %d :results-visibility test cases from %s" (count matrix) edn-path))
         deleted          (delete-results-feature-files features-dir)
         _                (when (pos? deleted)
                            (println (format "Deleted %d old results-page_*.feature files" deleted)))
         written          (atom 0)]
     (doseq [[gv-uuid test-case] matrix]
       (let [tc       (-> test-case
                          (assoc :gv-uuid gv-uuid)
                          (merge-with-baselines baselines input-visibility output-order-idx))
             filename (feature-filename (:module tc) (:output-name tc))
             filepath (str features-dir filename)
             content  (render-feature tc)]
         (io/make-parents filepath)
         (spit filepath content)
         (swap! written inc)
         (println (format "  ✓ %s" filename))))
     (println (format "✓ Wrote %d feature files to %s" @written features-dir))
     {:features-dir  features-dir
      :files-written @written})))
