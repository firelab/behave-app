(ns steps.helpers
  (:require [clojure.string     :as str]
            [cucumber.by        :as by]
            [cucumber.element   :as e]
            [cucumber.webdriver :as w]))

;;; =============================================================================
;;; Parsing Utilities
;;; =============================================================================

(defn parse-step-data
  "Converts a map `data` into a vector representing a path that follows the heirarchy order
  submodule -> group -> subgroup -> value"
  [data]
  (map (fn [{:keys [submodule group subgroup value]}]
         (cond-> []
           (seq submodule) (conj submodule)
           (seq group)     (conj group)
           (seq subgroup)  (conj subgroup)
           (seq value)     (conj value)))
       data))

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
  "Wait for an element matching selector to be present in the DOM."
  ([driver selector] (wait-for-element-by-selector driver selector 2000))
  ([driver selector timeout-ms]
   (let [wait (w/wait driver timeout-ms)]
     (.until wait (w/presence-of (selector->by selector))))))

(defn wait-for-wizard
  "Wait for the wizard interface to be present."
  ([driver] (wait-for-wizard driver 10000))
  ([driver timeout-ms]
   (wait-for-element-by-selector driver {:css ".wizard"} timeout-ms)))

(defn wait-for-working-area
  "Wait for the working area to be present."
  [driver]
  (wait-for-element-by-selector driver {:css ".working-area"} 10000))

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

(defn select-submodule-tab
  "Select a submodule in the wizard header. No-ops if the tab is already active
  (has class 'tab--selected'), avoiding an unnecessary re-render.

   Used primarily in the Outputs tab for selecting submodules."
  [driver submodule]
  (wait-for-nested-element driver {:css ".wizard-header__submodules"} submodule 10000)
  ;; Check if this submodule's tab is already selected by looking for an element
  ;; that has both 'tab--selected' and the target text as a descendant.
  ;; 'tab--selected' is specific enough that contains() is safe here.
  (let [already-selected? (try
                            (find-element driver {:xpath (str "//div[contains(@class,'wizard-header__submodules')]"
                                                              "//div[contains(@class,'tab--selected')"
                                                              " and .//*[text()=\"" submodule "\"]]")})
                            true
                            (catch Exception _ false))]
    (when-not already-selected?
      (-> (find-element driver {:css ".wizard-header__submodules"})
          (find-element {:text submodule})
          (e/click!))
      (wait-for-wizard driver))))

;;; =============================================================================
;;; Output Selection
;;; =============================================================================

(defn select-output
  "Select an output in the wizard outputs section.

   Args:
     driver - WebDriver instance
     output - Name of the output to select"
  [driver output]
  (-> (find-element driver {:css ".wizard-group__outputs"})
      (find-element {:text output})
      (e/click!)))

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

(defn wait-and-click-button-with-text
  "Wait for a button with the given text to appear, then click it.

   Args:
     driver     - WebDriver instance
     text       - Text content of the button to click
     timeout-ms - Max wait in milliseconds (default 2000)"
  ([driver text] (wait-and-click-button-with-text driver text 2000))
  ([driver text timeout-ms]
   (wait-for-element-by-selector driver {:text text} timeout-ms)
   (click-button-with-text driver text)))

;;; =============================================================================
;;; Input Operations
;;; =============================================================================

(defn enter-text-value
  "Enter a value into a text input field.

   Finds the input by locating the wizard-group whose header exactly matches
   `label-text`, then targeting the first <input> within its sibling
   wizard-group__inputs container.  Using the group-header as the anchor (exact
   match via normalize-space) prevents false positives when the field name is a
   substring of an earlier element in the page (e.g. \"Slope\" vs.
   \"Wind and Slope\" submodule tab).

   Args:
     driver     - WebDriver instance
     label-text - The wizard-group header text (exact match)
     value      - The value to enter (string, can be comma-separated)

   Examples:
     (enter-text-value driver \"1-h Fuel Moisture\" \"1\")
     (enter-text-value driver \"Wind Speed\" \"5, 10, 15\")
     (enter-text-value driver \"Slope\" \"30\")"
  [driver label-text value]
  (let [xpath         (str "//div[contains(@class, 'wizard-group__header')]"
                           "[normalize-space(.) = \"" label-text "\"]"
                           "/following-sibling::div[contains(@class, 'wizard-group__inputs')]"
                           "//input[1]")
        input-element (find-element driver {:xpath xpath})
        set-and-fire  (str "var el = arguments[0];"
                           "var nativeSetter = Object.getOwnPropertyDescriptor("
                           "window.HTMLInputElement.prototype,'value').set;"
                           "nativeSetter.call(el," (pr-str value) ");"
                           "el.dispatchEvent(new Event('input',{bubbles:true,cancelable:true}));"
                           "el.dispatchEvent(new FocusEvent('blur',{bubbles:false,cancelable:false}));")]
    (w/execute-script! driver set-and-fire input-element)))

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
           iterations      0]
      (if (and current-element (< iterations 20)) ; Safety limit
        (let [class-attr (.getAttribute current-element "class")
              classes    (when class-attr (str/split class-attr #"\s+"))]
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
      (e/click!))
  (wait-for-wizard driver))

(defn navigate-to-inputs
  "Navigate to the Inputs tab in the wizard."
  [driver]
  (navigate-to-tab driver "Inputs"))

(defn navigate-to-outputs
  "Navigate to the Outputs tab in the wizard."
  [driver]
  (navigate-to-tab driver "Outputs"))

;;; =============================================================================
;;; Results Page Navigation
;;; =============================================================================

(defn review-page?
  "Return true if the browser is currently on the Review page.

   Detects by the presence of .wizard-review — the unique container that only
   exists on the Review page. (The Review page hand-rolls its nav bar without
   .wizard-navigation__next, so that selector cannot be used here.)

   Args:
     driver - WebDriver instance"
  [driver]
  (try
    (find-element driver {:css ".wizard-review"})
    true
    (catch Exception _
      false)))

(defn results-settings-page?
  "Return true if the browser is currently on the Results Settings page.

   Detects by looking for the .wizard-results__table-settings element.

   Args:
     driver - WebDriver instance"
  [driver]
  (try
    (find-element driver {:css ".wizard-results__table-settings"})
    true
    (catch Exception _
      false)))

(defn advance-to-review
  "Click 'Next' through wizard pages until the Review page is reached.

   On the Review page the wizard shows a 'Run' highlight button inside
   .wizard-review. This function clicks the Next button repeatedly (up to
   max-attempts times) until the Review page is detected.

   Args:
     driver       - WebDriver instance
     max-attempts - Maximum number of Next clicks before giving up (default 10)"
  ([driver] (advance-to-review driver 10))
  ([driver max-attempts]
   (loop [attempts 0]
     (cond
       (review-page? driver)
       :on-review-page

       (>= attempts max-attempts)
       (throw (ex-info "Could not reach the Review page after clicking Next repeatedly"
                       {:attempts attempts}))

       :else
       (do
         (wait-for-element-by-selector driver {:css ".wizard-navigation__next .button--highlight"} 10000)
         (-> (find-element driver {:css ".wizard-navigation__next .button--highlight"})
             (e/click!))
         (Thread/sleep 500)
         (recur (inc attempts)))))))

(defn wait-for-results
  "Wait for the Results page output table to appear in the DOM.

   The output table div.wizard-results__table#outputs is only rendered after a
   successful solve. Uses a generous timeout since computation can be slow.

   Args:
     driver     - WebDriver instance
     timeout-ms - Max wait time in ms (default 60000)"
  ([driver] (wait-for-results driver 60000))
  ([driver timeout-ms]
   (wait-for-element-by-selector driver {:css ".wizard-results__table"} timeout-ms)))

(defn run-worksheet
  "Click the 'Run' button on the Review page and wait for the solve to finish.

   After clicking Run, the app shows 'Computing...' while solving, then
   navigates to the Results Settings page. This function waits for the
   Results Settings page to appear (or times out).

   Args:
     driver - WebDriver instance"
  [driver]
  (wait-for-element-by-selector driver {:css ".wizard-review"} 10000)
  (let [run-btn (find-element driver {:css ".wizard-navigation .button--highlight"})]
    (scroll-to-element driver run-btn)
    (e/click! run-btn))
  ;; Wait for computing to finish — results-settings page has .wizard-results__table-settings
  (wait-for-element-by-selector driver {:css ".wizard-results__table-settings"} 120000))

(defn navigate-to-results
  "Navigate from anywhere in the wizard to the Results page.

   Steps:
   1. Click Next through wizard pages until the Review page.
   2. Click Run and wait for the solve to complete (lands on Results Settings).
   3. Click Next on Results Settings to reach the Results page.
   4. Wait for the output table to render.

   Args:
     driver - WebDriver instance"
  [driver]
  (advance-to-review driver)
  (run-worksheet driver)
  ;; On results-settings, click Next to go to results
  (wait-for-element-by-selector driver {:css ".wizard-navigation__next .button--highlight"} 10000)
  (-> (find-element driver {:css ".wizard-navigation__next .button--highlight"})
      (e/click!))
  (wait-for-results driver))

(defn output-in-results?
  "Check if the given output name text appears in the Results page output table.

   Searches for the text within div.wizard-results__table (#outputs).

   Args:
     driver      - WebDriver instance
     output-name - The output name string to look for (e.g. 'Flame Length')

   Returns:
     true if the text is found, false otherwise"
  [driver output-name]
  (try
    (let [results-table (find-element driver {:css ".wizard-results__table"})
          xpath         (str ".//*[contains(text(), '" output-name "')]")]
      (e/find-el results-table (by/xpath xpath))
      true)
    (catch Exception _
      false)))

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
    (select-submodule-tab driver submodule)
    (if (seq groups)
      (do (wait-for-groups driver groups)
          (find-element driver {:text (last groups)}))
      (find-element driver {:text submodule}))))

