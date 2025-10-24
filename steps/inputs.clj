(ns steps.inputs
  "Input verification logic for BehavePlus Cucumber tests.

   This namespace handles verifying that expected input groups are displayed
   in the Inputs tab of the worksheet wizard."
  (:require [steps.helpers :as h]))

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
  [driver path]
  (h/navigate-to-group driver path))

(defn- verify-groups-not-exist
  "Verify that groups do NOT exist under a given submodule in the Inputs tab.

   This function:
   1. Selects the submodule in the wizard page
   2. Attempts to find each group element
   3. Throws an exception if any group IS found (because we expect it NOT to be there)

   Args:
     driver          - WebDriver instance
     submodule+groups - Vector where:
                        - First element is the submodule name
                        - Remaining elements are group names that should NOT exist

   Example:
     (verify-groups-not-exist driver [\"Wind and Slope\" \"Wind Speed\"])"
  [driver path]
  (try
    (h/navigate-to-group driver path)
    ;; If we found the element, that's an error - it should NOT exist
    (throw (ex-info (str "Group should NOT be displayed but was found: " path)
                    {:path path}))
    (catch org.openqa.selenium.NoSuchElementException _
      ;; This is good - the element doesn't exist as expected
      nil)))

;;; =============================================================================
;;; Public API
;;; =============================================================================

(defn verify-input-groups-are-displayed
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
     (verify-input-groups-are-displayed {:driver driver}
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

(defn verify-input-groups-not-displayed
  "Verify that input groups are NOT displayed in the Inputs tab.

   This is the inverse of verify-input-groups. It navigates to the Inputs tab,
   parses the expected-to-be-absent groups, and verifies each one is NOT
   present in the UI.

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
     ExceptionInfo if any group IS found (when it should NOT be)

   Example:
     (verify-input-groups-not-displayed {:driver driver}
                                        \"\"\"
                                        -- Wind and Slope > Wind Speed
                                        \"\"\")"
  [{:keys [driver]} submodule-groups-text]
  (h/navigate-to-inputs driver)
  (h/wait-for-wizard driver)

  (let [submodule-groups (h/parse-multiline-list submodule-groups-text)]
    (doseq [sg submodule-groups]
      (verify-groups-not-exist driver sg))
    (assert (pos? (count submodule-groups)))
    {:driver driver}))
