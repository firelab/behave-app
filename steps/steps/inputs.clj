(ns steps.inputs
  "Input verification logic for BehavePlus Cucumber tests.

   This namespace handles verifying that expected input groups are displayed
   in the Inputs tab of the worksheet wizard."
  (:require [steps.helpers :as h]
            [cucumber.webdriver :as w]
            [clojure.string :as str]))

;;; =============================================================================
;;; Private Helper Functions
;;; =============================================================================

(defn- verify-groups-exist
  [driver path]
  (h/navigate-to-group driver path))

(defn- verify-groups-not-exist
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
  [driver path]
  ;; (h/wait-for-groups driver (butlast path))
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

(defn enter-input
  [{:keys [driver]} & path]
  (h/wait-for-wizard driver)
  (h/navigate-to-inputs driver)
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
          (h/click-radio-or-dropdown-option driver option-name)))))
  {:driver driver})

(defn enter-inputs
  [{:keys [driver] :as context}]
  (h/wait-for-wizard driver)
  (h/navigate-to-inputs driver)
  (let [step-data    (get-in context [:tegere.parser/step :tegere.parser/step-data])
        paths (h/parse-step-data step-data)]
    (doseq [path paths]
      (enter-single-input driver path))
    {:driver driver}))

(defn verify-input-groups-are-displayed
  [{:keys [driver] :as context}]
  (h/wait-for-wizard driver)
  (h/navigate-to-inputs driver)
  (let [step-data (get-in context [:tegere.parser/step :tegere.parser/step-data])
        paths     (h/parse-step-data step-data)]
    (doseq [path paths]
      (verify-groups-exist driver path))
    (assert (pos? (count paths)))
    {:driver driver}))

(defn verify-input-groups-not-displayed
  [{:keys [driver] :as context}]
  (h/navigate-to-inputs driver)
  (h/wait-for-wizard driver)

  (let [step-data        (get-in context [:tegere.parser/step :tegere.parser/step-data])
        paths            (h/parse-step-data step-data)]
    (doseq [path paths]
      (verify-groups-not-exist driver path))
    (assert (pos? (count paths)))
    {:driver driver}))
