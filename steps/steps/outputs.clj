(ns steps.outputs
  "Output selection logic for BehavePlus Cucumber tests.

   This namespace handles selecting outputs in the worksheet wizard,
   including navigating submodules, waiting for groups, and clicking outputs."
  (:require [steps.helpers :as h]
            [cucumber.element :as e]))

;;; =============================================================================
;;; Private Helper Functions
;;; =============================================================================

(defn- select-single-output
  "Select a single output by navigating through submodule hierarchy.

   This function handles the three-level hierarchy:
   1. Selects the submodule (e.g., 'Fire Behavior')
   2. Waits for intermediate groups (e.g., 'Direction Mode')
   3. Clicks the final output (e.g., 'Heading')

   Args:
     driver          - WebDriver instance
     submodule+groups - Vector where:
                        - First element is the submodule name
                        - Middle elements are group names
                        - Last element is the output name

   Example:
     (select-single-output driver [\"Fire Behavior\" \"Direction Mode\" \"Heading\"])"
  [driver [submodule & groups]]
  (h/select-submodule-tab driver submodule)
  (h/wait-for-groups driver (butlast groups))
  (h/wait-for-element-by-selector driver {:text (last groups)})
  (h/select-output driver (last groups)))

;;; =============================================================================
;;; Public API
;;; =============================================================================

(defn select-outputs
  "Select multiple outputs from a multiline Gherkin string.

   This is the main entry point for the When step that selects outputs.
   It parses the multiline text and selects each output in sequence.

   Args:
     context      - Map containing :driver key with WebDriver instance
     paths-text - Multiline string in format:
                    \"\"\"
                    -- Submodule > Group > Output
                    -- Submodule > Group > Output
                    \"\"\"

   Returns:
     Map with :driver key for passing to next step

   Example:
     (select-outputs {:driver driver}
                     \"\"\"
                     -- Fire Behavior > Direction Mode > Heading
                     -- Fire Behavior > Surface Fire > Rate of Spread
                     \"\"\")"
  [{:keys [driver]} paths-text]
  (h/wait-for-wizard driver)
  (let [outputs (h/parse-multiline-list paths-text)]
    (doseq [output outputs]
      (select-single-output driver output))
    {:driver driver}))

(defn verify-outputs-not-selected
  "Verify that specified outputs are NOT currently selected.

   This function checks that each output in the list is not checked/selected.
   If any output is found to be selected, it throws an assertion error.

   Args:
     context      - Map containing :driver key with WebDriver instance
     paths-text - Multiline string in format:
                    \"\"\"
                    -- Submodule > Group > Output
                    -- Submodule > Group > Output
                    \"\"\"

   Returns:
     Map with :driver key for passing to next step

   Throws:
     ExceptionInfo if any output is found to be selected

   Example:
     (verify-outputs-not-selected {:driver driver}
                                   \"\"\"
                                   -- Fire Behavior > Direction Mode > Heading
                                   -- Fire Behavior > Surface Fire > Rate of Spread
                                   \"\"\")"
  [{:keys [driver]} paths-text]
  (h/wait-for-wizard driver)
  (let [parsed-paths (h/parse-multiline-list paths-text)]
    (doseq [path parsed-paths]
      (let [[submodule & groups] path
            output-name          (last path)
            last-group           (h/navigate-to-group driver path)
            is-checked?          (h/output-checked? last-group)]
        (when is-checked?
          (throw (ex-info (str "Output should NOT be selected but was: " output-name)
                          {:output    output-name
                           :submodule submodule
                           :groups    groups})))))
    {:driver driver}))
