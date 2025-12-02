(ns steps.helpers
  "Shared utility functions for Cucumber step definitions.

   This namespace provides reusable helper functions for common operations
   in BDD tests including navigation, element selection, waiting, and parsing."
  (:require [cucumber.by :as by]
            [cucumber.element :as e]
            [cucumber.webdriver :as w]
            [clojure.string :as str]))

;;; =============================================================================
;;; Parsing Utilities
;;; =============================================================================

(defn parse-multiline-list
  "Parse triple-quoted multiline list into vector of vectors.

   Converts Gherkin multiline strings into structured data for processing.

   Example:
     Input:  \"\"\"
             -- Fire Behavior > Direction Mode > Heading
             -- Fire Behavior > Surface Fire > Rate of Spread
             \"\"\"
     Output: [[\"Fire Behavior\" \"Direction Mode\" \"Heading\"]
              [\"Fire Behavior\" \"Surface Fire\" \"Rate of Spread\"]]"
  [text]
  (-> text
      (str/replace "\"\"\"" "")
      (str/split #"-- ")
      (->> (map str/trim)
           (remove empty?)
           (map #(str/split % #" > ")))))

(defn numeric-or-multi-value?
  "Check if a string looks like a numeric value or comma-separated values.

   Returns true if the string contains only digits, spaces, commas, decimal points,
   and minus signs. This helps distinguish input values from field/option names.

   Args:
     s - String to check

   Returns:
     Boolean - true if it looks like a value to enter

   Examples:
     (numeric-or-multi-value? \"1\")         ; => true
     (numeric-or-multi-value? \"3.14\")     ; => true
     (numeric-or-multi-value? \"1, 2, 3\")  ; => true
     (numeric-or-multi-value? \"10.5, 20\") ; => true
     (numeric-or-multi-value? \"Individual Size Class\") ; => false"
  [s]
  (and (string? s)
       (not (str/blank? s))
       (re-matches #"^[0-9.,\s-]+$" (str/trim s))))

;;; =============================================================================
;;; Select Element by By
;;; =============================================================================

(defn selector->by
  "Convert a selector map to a Selenium By object.

   This function provides the bridge between our map-based selector API
   and Selenium's By objects for use in WebDriverWait conditions.

   Selector Types:
   - :id        - Find by element ID
   - :css       - Find by CSS selector
   - :xpath     - Find by XPath expression
   - :tag       - Find by tag name
   - :class     - Find by class name
   - :text      - Find by exact text content
   - :name      - Find by name attribute

   Args:
     selector - Map with a single selector key/value pair

   Returns:
     Selenium By object

   Throws:
     ExceptionInfo if selector type is unknown

   Examples:
     (selector->by {:id \"submit-button\"})      ; => By.id(\"submit-button\")
     (selector->by {:css \".wizard-header\"})    ; => By.cssSelector(\".wizard-header\")
     (selector->by {:text \"New Run\"})          ; => By with xpath for text
     (selector->by {:tag :div})                  ; => By.tagName(\"div\")"
  [selector]
  (cond
    (:id selector) (by/id (:id selector))
    (:css selector) (by/css (:css selector))
    (:xpath selector) (by/xpath (:xpath selector))
    (:tag selector) (by/tag-name (name (:tag selector)))
    (:class selector) (by/class-name (:class selector))
    (:text selector) (by/attr= :text (:text selector))
    (:name selector) (by/input-name (:name selector))
    :else (throw (ex-info "Unknown selector type" {:selector selector}))))

;;; =============================================================================
;;; Waiting Utilities
;;; =============================================================================

(defn wait-for-element-by-selector
  "Wait for the wizard interface to be present (up to 300 miliseconds)."
  [driver selector]
  (let [wait (w/wait driver 300)]
    (.until wait (w/presence-of (selector->by selector)))))

(defn wait-for-wizard
  "Wait for the wizard interface to be present."
  [driver]
  (wait-for-element-by-selector driver {:css ".wizard"}))

(defn wait-for-working-area
  "Wait for the working area to be present."
  [driver]
  (wait-for-element-by-selector driver {:css ".working-area"}))

(defn wait-for-nested-element
  "Wait for a nested element to appear within a parent element.

   Args:
     driver          - WebDriver instance
     parent-selector - Selector map for parent element (e.g., {:css \".wizard\"})
     text            - Text content to search for in child element
     timeout-ms      - Maximum wait time in milliseconds

   Examples:
     (wait-for-nested-element driver {:css \".wizard-group__header\"} \"Fire Behavior\" 300)"
  [driver parent-selector text timeout-ms]
  (let [wait (w/wait driver timeout-ms)]
    (.until wait (w/presence-of-nested-elements
                  (selector->by parent-selector)
                  (selector->by {:text text})))))

(defn wait-for-groups
  "Wait for groups to appear as properly nested elements in hierarchical order.

   This function verifies that groups form a parent-child chain in the DOM.
   For example, if groups = [\"Fire Behavior\" \"Direction Mode\" \"Heading\"],
   it ensures:
   1. \"Fire Behavior\" exists under .wizard-page__body
   2. \"Direction Mode\" is nested within \"Fire Behavior\"
   3. \"Heading\" is nested within \"Direction Mode\"

   Args:
     driver - WebDriver instance
     groups - Collection of group names in hierarchical order (parent to child)

   Example:
     (wait-for-groups driver [\"parent-a\" \"parent-b\" \"parent-c\" \"last-child\"])"
  [driver groups]
  (when (seq groups)
    (wait-for-nested-element driver
                             {:css ".wizard-page__body"}
                             (first groups)
                             300)
    (doseq [[parent child] (partition 2 1 groups)]
      (let [wait (w/wait driver 300)]
        (.until wait (w/presence-of-nested-elements
                      (selector->by {:text parent})
                      (selector->by {:text child})))))))

;;; =============================================================================
;;; Element Finding
;;; =============================================================================

(defn find-element
  "Find an element using various selector strategies.

   This function provides a unified interface for finding elements using
   different selector types. It delegates to selector->by for converting
   the selector map to a Selenium By object.

   Selector Types:
   - :id        - Find by element ID
   - :css       - Find by CSS selector
   - :xpath     - Find by XPath expression
   - :tag       - Find by tag name
   - :class     - Find by class name
   - :text      - Find by exact text content
   - :name      - Find by name attribute

   Args:
     driver   - WebDriver instance
     selector - Map with a single selector key/value pair

   Returns:
     WebElement if found

   Throws:
     NoSuchElementException if element not found
     ExceptionInfo if selector type is unknown

   Examples:
     (find-element driver {:id \"submit-button\"})
     (find-element driver {:css \".wizard-header\"})
     (find-element driver {:xpath \"//button[text()='Next']\"})
     (find-element driver {:tag :div})
     (find-element driver {:class \"button--highlight\"})
     (find-element driver {:text \"New Run\"})
     (find-element driver {:name \"username\"})

   See also:
     selector->by - For converting selectors to Selenium By objects"
  [driver selector]
  ;; (wait-for-element-by-selector driver selector)
  (e/find-el driver (selector->by selector)))

(defn find-input-by-label
  "Find an input element (text field, radio, or dropdown) by its label text.

   This function searches for a label with the given text and returns the
   associated input element. Works with text inputs, radio buttons, and dropdowns.

   Args:
     driver - WebDriver instance
     label-text - The label text to search for

   Returns:
     WebElement of the input field

   Throws:
     NoSuchElementException if label or input not found

   Examples:
     (find-input-by-label driver \"1-h Fuel Moisture\")
     (find-input-by-label driver \"Air Temperature\")"
  [driver label-text]
  (find-element driver {:text label-text}))

;;; =============================================================================
;;; Submodule Selection
;;; =============================================================================

(defn select-submodule
  "Select a submodule within a given container.

   Args:
     driver    - WebDriver instance
     submodule - Name of the submodule to select
     selector  - Selector map (e.g., {:css \".wizard\"})"
  [driver submodule selector]
  (find-element driver selector)
  (-> (find-element driver {:text submodule})
      (e/click!)))

(defn select-submodule-in-wizard
  "Select a submodule in the wizard header.

   Used primarily in the Outputs tab for selecting submodules."
  [driver submodule]
  (select-submodule driver submodule {:css ".wizard-header__submodules"}))

(defn select-submodule-in-page
  "Select a submodule in the wizard page body.

   Used primarily in the Inputs tab for selecting submodules."
  [driver submodule]
  (select-submodule driver submodule {:css ".wizard"}))

;;; =============================================================================
;;; Output Selection
;;; =============================================================================

(defn select-output
  "Select an output in the wizard outputs section.

   Args:
     driver - WebDriver instance
     output - Name of the output to select"
  [driver output]
  (let [outputs-container (find-element driver {:css ".wizard-group__outputs"})]
    (-> (e/find-el outputs-container (selector->by {:text output}))
        (e/click!))))

;;; =============================================================================
;;; Button Operations
;;; =============================================================================

(defn click-highlighted-button
  "Click the highlighted button in the current view.

   This is typically the primary action button (e.g., Next, Finish)."
  [driver]
  (-> (find-element driver {:css ".button--highlight"})
      (e/click!)))

(defn click-button-with-text
  "Click a button with specific text content.

   Args:
     driver - WebDriver instance
     text   - Text content of the button to click"
  [driver text]
  (-> (find-element driver {:text text})
      (e/click!)))

;;; =============================================================================
;;; Input Operations
;;; =============================================================================

(defn enter-text-value
  "Enter a value into a text input field.

   This function finds the input field by searching for text content,
   then finding the nearest input element.

   Args:
     driver     - WebDriver instance
     label-text - The label text of the input field
     value      - The value to enter (string, can be comma-separated)

   Examples:
     (enter-text-value driver \"1-h Fuel Moisture\" \"1\")
     (enter-text-value driver \"Air Temperature\" \"77\")
     (enter-text-value driver \"Wind Speed\" \"5, 10, 15\")"
  [driver label-text value]
  ;; Use XPath to find input that's near any element containing the label text
  ;; This is more flexible than requiring a <label> tag specifically
  (let [xpath (str "//*[contains(text(), '" label-text "')]/ancestor::div[contains(@class, 'wizard-input')]//input | "
                   "//*[contains(text(), '" label-text "')]/following-sibling::*//input[1] | "
                   "//*[contains(text(), '" label-text "')]/following::input[1]")
        input-element (find-element driver {:xpath xpath})]
    (e/clear! input-element)
    ;; Call sendKeys directly to avoid the into-array bug in e/send-keys!
    (.sendKeys input-element (into-array String [value]))))

(defn click-radio-or-dropdown-option
  "Click a radio button or dropdown option by text.

   This function assumes options are already visible. For multi-select components,
   use click-select-more-button first to expand options before calling this.

   Args:
     driver      - WebDriver instance
     option-text - The text of the option to select

   Examples:
     (click-radio-or-dropdown-option driver \"Individual Size Class\")
     (click-radio-or-dropdown-option driver \"GR4\")"
  [driver option-text]
  (try
    (-> (find-element driver {:text option-text})
        (e/click!))
    (catch Exception e
      (throw (ex-info (str "Could not find or select option: " option-text)
                      {:option option-text :error e})))))

(defn multi-select-exists?
  "Check if a multi-select component exists in the current wizard group.

   Looks for the presence of a .multi-select element within .wizard-group__inputs.

   Args:
     driver - WebDriver instance

   Returns:
     Boolean - true if multi-select component is found, false otherwise

   Example:
     (multi-select-exists? driver) ; => true or false"
  [driver]
  (try
    (find-element driver {:css ".wizard-group__inputs .multi-select"})
    true
    (catch Exception _
      false)))

(defn click-select-more-button
  "Click the 'Select More' button in a multi-select component to expand options.

   Finds the expand button in the multi-select header and clicks it, then waits
   for the expansion animation to complete.

   Args:
     driver - WebDriver instance

   Example:
     (click-select-more-button driver)"
  [driver]
  (try
    (let [expand-btn (find-element driver {:css ".multi-select__selections__header__button button"})]
      (e/click! expand-btn)
      (Thread/sleep 500)) ; Wait for expansion animation and options to load
    (catch Exception e
      (throw (ex-info "Could not find or click 'Select More' button"
                      {:error e})))))

;;; =============================================================================
;;; Checkbox Utilities
;;; =============================================================================

(defn output-checked?
  "Check if an output checkbox is checked by looking for 'input-checkbox--checked' class on parent elements.

   This function takes an element and walks up the DOM tree checking parent divs
   for the 'input-checkbox--checked' class. It stops when it finds the class,
   reaches a .wizard-output parent, or runs out of parents.

   Args:
     element - WebElement to start checking from

   Returns:
     Boolean - true if checked (class found), false otherwise

   Example:
     (let [output-elem (h/find-element driver {:text \"Rate of Spread\"})]
       (h/output-checked? output-elem))
     ; => true if the Rate of Spread output is checked"
  [element]
  (try
    (loop [current-element element
           iterations 0]
      (if (and current-element (< iterations 20)) ; Safety limit
        (let [class-attr (.getAttribute current-element "class")
              classes (when class-attr (str/split class-attr #"\s+"))]
          (cond
            ;; Found the checked class
            (some #(= "input-checkbox--checked" %) classes)
            true

            ;; Reached the wizard-output boundary
            (some #(= "wizard-output" %) classes)
            false

            ;; Keep walking up
            :else
            (let [parent (try
                           (e/find-el current-element (by/xpath ".."))
                           (catch Exception _ nil))]
              (recur parent (inc iterations)))))
        false))
    (catch Exception _
      false)))

;;; =============================================================================
;;; Scrolling
;;; =============================================================================

(defn scroll-to-element
  "Scroll the page so that the given element is visible.

   Args:
     driver  - WebDriver instance
     element - WebElement to scroll to"
  [driver element]
  (w/execute-script! driver "arguments[0].scrollIntoView(true)" element))

(defn scroll-to-top
  "Scroll the page to the top."
  [driver]
  (w/execute-script! driver "window.scrollTo(0,0)"))

;;; =============================================================================
;;; Navigation
;;; =============================================================================

(defn navigate-to-tab
  "Navigate to a specific tab in the wizard interface.

   Args:
     driver   - WebDriver instance
     tab-name - Name of the tab (e.g., \"Inputs\", \"Outputs\")"
  [driver tab-name]
  (-> (find-element driver {:css ".wizard-header__io-tabs"})
      (e/find-el (by/attr= :text tab-name))
      (e/click!)))

(defn navigate-to-inputs
  "Navigate to the Inputs tab in the wizard."
  [driver]
  (navigate-to-tab driver "Inputs"))

(defn navigate-to-outputs
  "Navigate to the Outputs tab in the wizard."
  [driver]
  (navigate-to-tab driver "Outputs"))

(defn navigate-to-group
  "Navigate through submodule and groups in Outputs wizard, returning driver and last group element.

   This helper navigates to a specific group by:
   1. Selecting the submodule in the wizard header
   2. Waiting for all groups in the hierarchy to appear
   3. Finding and returning the last group element

   Args:
     driver             - WebDriver instance
     submodule+groups   - Collection where:
                         - First element is the submodule name
                         - Remaining elements are group names in hierarchical order
                         Example: [\"Fire Behavior\" \"Direction Mode\"]

   Returns:
     Map with:
       :group-element - The DOM element of the last group

   Example:
     (navigate-to-group driver [\"Fire Behavior\" \"Direction Mode\"])
     ; => {:driver driver, :group-element <WebElement for Direction Mode>}

     ;; Use in a step definition:
     (let [{:keys [driver group-element]} (navigate-to-group driver [\"Fire Behavior\" \"Direction Mode\"])]
       (e/find-el group-element (by/css \".some-class\")))"
  [driver submodule+groups]
  (let [[submodule & groups] submodule+groups]
    (select-submodule-in-wizard driver submodule)
    (if (seq groups)
      (do (wait-for-groups driver groups)
          (let [last-group-name (last groups)]
            (find-element driver {:text last-group-name})))
      (find-element driver {:text submodule}))))

