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

(defn- render-scenario
  "Render one test case as a Gherkin scenario string."
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

;;; ============================================================================
;;; Feature file rendering
;;; ============================================================================

(defn- render-feature
  "Render the full .feature file content for one test case."
  [{:keys [output-name module] :as test-case}]
  (let [module-title (or module "")
        header       (str "@core\n"
                          "Feature: " module-title " Results - " output-name "\n")]
    (str header "\n" (render-scenario test-case))))

;;; ============================================================================
;;; Combined matrix loading
;;; ============================================================================

(defn- load-combined-matrix
  "Read the combined test_matrix_data.edn. Returns a map with
   :input-visibility and :results-page top-level keys."
  [edn-path]
  (let [raw (-> (slurp edn-path) edn/read-string)]
    ;; Support both combined format and legacy flat format
    (if (contains? raw :results-page)
      raw
      {:input-visibility raw :results-page {}})))

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

(defn- sort-by-vms-order
  "Sort rows by VMS hierarchy: module → submodule/order → group/order → group-variable/order.
   Nils / unknown modules sort last."
  [rows]
  (sort-by (juxt #(get module-sort-order (:module %) 99)
                 #(or (:submodule/order %) 999)
                 #(or (:group/order %) 999)
                 #(or (:group-variable/order %) 999))
           rows))

(defn- attach-order
  "Attach :module, :submodule/order, :group/order to a row that lacks them,
   by looking up the first matching :input-visibility entry and extracting the
   module name from the path key."
  [input-visibility row]
  (if (contains? row :submodule/order)
    row
    (let [match (first (keep (fn [[path entity]]
                               (when (and (some #(= (:name %) (:submodule row)) path)
                                          (= (:name (last (remove keyword? path))) (:group row)))
                                 [path entity]))
                             input-visibility))]
      (if match
        (let [[path entity] match]
          (assoc row
                 :module               (:name (first path))
                 :submodule/order      (:submodule/order entity)
                 :group/order          (:group/order entity)
                 :group-variable/order nil))
        row))))

(defn- find-group-entries
  "Return ALL :input-visibility entries whose path contains submodule :name and
   ends with group :name. Multiple entries can exist when the same group name
   appears in different modules with different conditionals (e.g. 'Wind and slope
   are' exists in both Surface and Crown modules)."
  [input-visibility {:keys [submodule group]}]
  (keep (fn [[path entity]]
          (when (and (some #(= (:name %) submodule) path)
                     (= (:name (last (remove keyword? path))) group))
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
                             (let [gv-name (get-in group-variable [:group-variable/translated-name])
                                   cur-val (get known-values gv-name)]
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

(defn- baseline-key
  "Derive the baselines EDN key for a module combo set.
   Surface is always first; remaining modules sorted alphabetically.
   #{:surface}            → :surface
   #{:surface :mortality} → :surface-mortality
   #{:surface :crown}     → :surface-crown
   #{:surface :contain}   → :surface-contain"
  [module-combo]
  ;; Sort keywords first (surface first), then convert to names and join
  (keyword (str/join "-" (map name (sort-by #(if (= % :surface) "" (name %)) module-combo)))))

(defn- merge-with-baselines
  "Merge a test case with the worksheet-combo baseline.
   The baseline is looked up by a single combo key (e.g. :surface-mortality) rather
   than per-module, so each combo can declare its own outputs and inputs.

   :baseline-outputs are prepended before test-case-specific :required-outputs (deduped).
   :baseline-inputs are filtered via :input-visibility conditionals and merged with
   test-case-specific inputs (test-case values override baseline by input-key)."
  [test-case baselines input-visibility]
  (let [module-combo (effective-module-combo (:required-modules test-case) (:module test-case))
        bk           (baseline-key module-combo)
        baseline     (get baselines bk {})
        base-outputs (get baseline :baseline-outputs [])
        base-inputs  (get baseline :baseline-inputs [])
        tc-inputs    (:required-inputs test-case)
        ;; Merge baseline outputs before test-case outputs, deduped
        tc-outputs   (get test-case :required-outputs [])
        all-outputs  (->> (concat base-outputs tc-outputs)
                          (reduce (fn [acc row]
                                    (if (some #(= % row) acc) acc (conj acc row)))
                                  [])
                          vec)
        ;; Seed known-values: input group→value AND all selected output names→"true"
        ;; so output-gated input conditionals (e.g. "Wind and slope are") pass
        init-known   (into {}
                           (concat
                            (map (fn [{:keys [group value]}] [group value]) tc-inputs)
                            (map (fn [{:keys [value]}] [value "true"]) all-outputs)))
        ;; Fixpoint: include baseline inputs whose conditionals pass, in declaration order.
        ;; Uses vector + seen-set to avoid array-map overflow losing ordering.
        included-vec (loop [remaining (vec base-inputs)
                            known     init-known
                            acc       []
                            seen      #{}]
                       (let [newly     (remove (fn [row] (seen (input-key row)))
                                               (filter (fn [row]
                                                         (let [entries (find-group-entries input-visibility row)]
                                                           (or (empty? entries)
                                                               (some #(conditionals-pass? % known) entries))))
                                                       remaining))
                             new-acc   (into acc newly)
                             new-seen  (into seen (map input-key newly))
                             new-known (into known (map (fn [{:keys [group value]}]
                                                          [group value]) newly))
                             leftover  (remove (fn [row] (new-seen (input-key row))) remaining)]
                         (if (= (count new-acc) (count acc))
                           new-acc
                           (recur leftover new-known new-acc new-seen))))
        ;; Attach VMS order to baseline rows that lack it (lookup from :input-visibility)
        ordered-base (mapv #(attach-order input-visibility %) included-vec)
        ;; tc-inputs override baseline values for same key; new keys appended after
        tc-override  (into {} (map #(vector (input-key %) %) tc-inputs))
        base-keys    (set (map input-key included-vec))
        merged-raw   (into (mapv #(get tc-override (input-key %) %) ordered-base)
                           (remove #(base-keys (input-key %)) tc-inputs))]
    (assoc test-case
           :required-inputs  (sort-by-vms-order merged-raw)
           :required-outputs (sort-by-vms-order all-outputs))))

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

   For each :results-page test case, writes one feature file asserting that the
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
         matrix           (:results-page combined)
         input-visibility (:input-visibility combined)
         baselines        (load-baselines baselines-path)
         _                (println (format "Loaded %d :results-page test cases from %s" (count matrix) edn-path))
         deleted          (delete-results-feature-files features-dir)
         _                (when (pos? deleted)
                            (println (format "Deleted %d old results-page_*.feature files" deleted)))
         written          (atom 0)]
     (doseq [[gv-uuid test-case] matrix]
       (let [tc       (-> test-case
                          (assoc :gv-uuid gv-uuid)
                          (merge-with-baselines baselines input-visibility))
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
