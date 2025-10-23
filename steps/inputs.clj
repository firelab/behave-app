(ns steps.inputs
  "Input verification logic for BehavePlus Cucumber tests.

   This namespace handles verifying that expected input groups are displayed
   in the Inputs tab of the worksheet wizard."
  (:require [cucumber.by :as by]
            [steps.helpers :as h]))

;;; =============================================================================
;;; Private Helper Functions
;;; =============================================================================

(defn- verify-groups-exist
  "Verify that all groups exist under a given submodule in the Inputs tab.

   This function:
   1. Selects the submodule in the wizard page
   2. Waits for each group to appear (300ms timeout per group)
   3. Throws an exception if any group is not found

   Args:
     driver          - WebDriver instance
     submodule+groups - Vector where:
                        - First element is the submodule name
                        - Remaining elements are group names to verify

   Example:
     (verify-groups-exist driver [\"Wind and Slope\" \"Wind measured at\" \"Wind Speed\"])"
  [driver [submodule & groups]]
  (h/select-submodule-in-page driver submodule)
  (h/wait-for-groups driver groups))

;;; =============================================================================
;;; Public API
;;; =============================================================================

(defn verify-input-groups
  "Verify that expected input groups are displayed in the Inputs tab.

   This is the main entry point for the Then step that verifies inputs.
   It navigates to the Inputs tab, parses the expected groups, and verifies
   each one is present in the UI.

   Args:
     context                - Map containing :driver key with WebDriver instance
     submodule-groups-text  - Multiline string in format:
                              \"\"\"
                              -- Submodule > Group1 > Group2
                              -- Submodule > Group3
                              \"\"\"

   Returns:
     Map with :driver key for passing to next step

   Throws:
     AssertionError if no groups are specified or any group is not found

   Example:
     (verify-input-groups {:driver driver}
                          \"\"\"
                          -- Fuel Model > Standard > Fuel Model
                          -- Fuel Moisture > Moisture Input Mode
                          \"\"\")"
  [{:keys [driver]} submodule-groups-text]
  (h/navigate-to-inputs driver)
  (h/wait-for-wizard driver)

  (let [submodule-groups (h/parse-multiline-list submodule-groups-text)]
    (doseq [sg submodule-groups]
      (verify-groups-exist driver sg))
    (assert (pos? (count submodule-groups)))
    {:driver driver}))
