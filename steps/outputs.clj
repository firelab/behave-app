(ns steps.outputs
  "Output selection logic for BehavePlus Cucumber tests.

   This namespace handles selecting outputs in the worksheet wizard,
   including navigating submodules, waiting for groups, and clicking outputs."
  (:require [steps.helpers :as h]))

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
  (h/select-submodule-in-wizard driver submodule)
  (h/wait-for-groups driver (butlast groups))
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
     outputs-text - Multiline string in format:
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
  [{:keys [driver]} outputs-text]
  (h/wait-for-wizard driver)
  (let [outputs (h/parse-multiline-list outputs-text)]
    (doseq [output outputs]
      (select-single-output driver output))
    {:driver driver}))
