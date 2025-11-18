(ns steps.worksheet
  "Worksheet creation logic for BehavePlus Cucumber tests.

   This namespace handles the creation of new worksheets in guided mode,
   including navigating through the workflow wizard and selecting module types."
  (:require [cucumber.webdriver :as w]
            [cucumber.element :as e]
            [steps.helpers :as h]))

;;; =============================================================================
;;; Worksheet Module Mappings
;;; =============================================================================

(def ^:private worksheet-modules
  "Maps module keyword vectors to their display names in the UI.

   Available worksheet types:
   - [:surface]            - Surface fire modeling only
   - [:surface :contain]   - Surface fire with containment
   - [:surface :crown]     - Surface and crown fire modeling
   - [:surface :mortality] - Surface fire with tree mortality
   - [:mortality]          - Tree mortality only"
  {[:surface] "Surface Only"
   [:surface :contain] "Surface and Contain"
   [:surface :crown] "Surface & Crown"
   [:surface :mortality] "Surface & Mortality"
   [:mortality] "Mortality Only"})

;;; =============================================================================
;;; Worksheet Creation
;;; =============================================================================

(defn start-worksheet
  "Create a new worksheet in guided mode.

   This function automates the entire worksheet creation workflow:
   1. Maximizes the browser window
   2. Navigates to the application URL
   3. Waits for the working area to load
   4. Dismisses any disclaimer popup
   5. Clicks through the 'New Run' wizard
   6. Selects 'Guided Workflow' mode
   7. Selects the specified module type(s)
   8. Completes the wizard

   Args:
     modules - Vector of module keywords (e.g., [:surface :crown])
     context - Map containing:
               :driver - WebDriver instance
               :url    - Application URL

   Returns:
     Map with :driver key containing the WebDriver instance

   Example:
     (start-worksheet [:surface :crown] {:driver driver :url \"http://localhost:8081/worksheets\"})"
  [modules {:keys [driver url]}]
  (w/maximize driver)
  (w/goto driver url)

  (h/wait-for-working-area driver)

  ;; Dismiss disclaimer popup if it appears

  (try
    (let [disclaimer-close-button (-> driver
                                      (h/find-element {:class "modal__close"})
                                      (h/find-element {:class "button"}))]
      (h/scroll-to-element driver disclaimer-close-button)
      (Thread/sleep 300)
      (e/click! disclaimer-close-button))
    (catch Exception e
      ;; No disclaimer present, continue
      nil))

  (Thread/sleep 300)

  ;; Click "New Run" button
  (h/click-button-with-text driver "New Run")
  (Thread/sleep 100)

  ;; Proceed through initial dialog
  (h/click-button-with-text driver "Next")
  (Thread/sleep 100)

  ;; Select "Guided Workflow"
  (h/click-button-with-text driver "Open using Guided Workflow")
  (Thread/sleep 100)

  ;; Proceed to module selection
  (h/click-highlighted-button driver)
  (Thread/sleep 100)

  ;; Select the desired module type
  (h/click-button-with-text driver (get worksheet-modules modules))

  ;; Scroll to the next button and click it
  (let [el (h/find-element driver {:text "Next"})]
    (h/scroll-to-element driver el))

  (h/click-button-with-text driver "Next")
  (h/scroll-to-top driver)

  {:driver driver})
