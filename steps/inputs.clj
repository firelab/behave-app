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

(defn- enter-single-input
  "Enter a single input value or select a radio/dropdown/multi-select option.

   Uses DOM inspection to detect multi-select components after navigation,
   making the logic more robust and less dependent on path structure.

   Args:
     driver - WebDriver instance
     path   - Vector of path elements, e.g.:
              [\"Fuel Model\" \"Standard\" \"Fuel Model\" \"FB1/1 - Short grass (Static)\"]
              [\"Fuel Moisture\" \"By Size Class\" \"1-h Fuel Moisture\" \"1\"]
              [\"Fuel Moisture\" \"Moisture Input Mode\" \"Individual Size Class\"]

   Flow:
     1. Check if last element is a value (numeric or multi-value)
     2. If value: Navigate to field group, enter value
     3. If option: Navigate to option group, check DOM for multi-select
        - If multi-select exists: Click 'Select More', then click option
        - Otherwise: Click option directly (radio/dropdown)

   Examples:
     (enter-single-input driver [\"Fuel Model\" \"Standard\" \"Fuel Model\" \"FB1/1 - Short grass (Static)\"])
     ; => Navigates to [\"Fuel Model\" \"Standard\" \"Fuel Model\"], detects multi-select, expands, clicks option

     (enter-single-input driver [\"Fuel Moisture\" \"By Size Class\" \"1-h Fuel Moisture\" \"1\"])
     ; => Navigates to [\"Fuel Moisture\" \"By Size Class\"], enters value \"1\" in field

     (enter-single-input driver [\"Fuel Moisture\" \"Moisture Input Mode\" \"Individual Size Class\"])
     ; => Navigates to [\"Fuel Moisture\" \"Moisture Input Mode\"], clicks radio option directly"
  [driver path]
  (let [last-element (last path)
        is-value?    (h/numeric-or-multi-value? last-element)]
    (if is-value?
      ;; Case 1: Value input - navigate to field and enter value
      (let [field-name      (nth path (- (count path) 2))
            value           last-element
            navigation-path (vec (drop-last 2 path))]
        (h/navigate-to-group driver navigation-path)
        (h/enter-text-value driver field-name value))
      ;; Case 2: Option selection - navigate to group and click option
      ;; Check DOM to determine if multi-select expansion is needed
      (let [option-name     last-element
            navigation-path (vec (drop-last path))]
        (h/navigate-to-group driver navigation-path)
        (if (h/multi-select-exists? driver)
          ;; Multi-select: expand options first, then click
          (do
            (h/click-select-more-button driver)
            (h/click-radio-or-dropdown-option driver option-name))
          ;; Radio/dropdown: click directly
          (h/click-radio-or-dropdown-option driver option-name))))))

;;; =============================================================================
;;; Public API
;;; =============================================================================

(defn enter-inputs
  "Enter input values and select options from a multiline Gherkin string.

   This function handles two types of input operations:
   1. Value entry: Enter numeric values into text input fields
   2. Option selection: Select radio buttons or dropdown options

   The function navigates to the Inputs tab, parses the multiline text,
   and processes each input line by navigating through the submodule/group
   hierarchy and either entering a value or selecting an option.

   Args:
     context     - Map containing :driver key with WebDriver instance
     inputs-text - Multiline string in format:
                   \"\"\"
                   --- Submodule > Group > Input Name > Value
                   --- Submodule > Group > Option Name
                   \"\"\"
                   Note: Uses '---' (three dashes) delimiter

   Returns:
     Map with :driver key for passing to next step

   Examples:
     (enter-inputs {:driver driver}
                   \"\"\"
                   --- Fuel Moisture > Moisture Input Mode > Individual Size Class
                   --- Fuel Moisture > By Size Class > 1-h Fuel Moisture > 1
                   --- Fuel Moisture > By Size Class > Live Woody Fuel Moisture > 5, 10, 15
                   \"\"\")"
  [{:keys [driver]} inputs-text]
  (h/wait-for-input-tab driver)
  (h/navigate-to-inputs driver)
  (h/wait-for-wizard driver)
  (let [inputs (h/parse-multiline-list inputs-text)]
    (doseq [input inputs]
      (enter-single-input driver input))
    {:driver driver}))

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
