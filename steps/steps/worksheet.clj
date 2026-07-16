(ns steps.worksheet
  "Worksheet creation logic for BehavePlus Cucumber tests.

   This namespace handles the creation of new worksheets in guided mode,
   including navigating through the workflow wizard and selecting module types."
  (:require [cucumber.webdriver :as w]
            [steps.helpers      :as h]))

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
  {[:surface]            "Surface Only"
   [:surface :contain]   "Surface & Contain"
   [:surface :crown]     "Surface & Crown"
   [:surface :mortality] "Surface & Mortality"
   [:mortality]          "Mortality Only"})

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
  ;; Deterministic viewport: headless `maximize` can leave a short viewport where the
  ;; fixed page__footer overlaps wizard buttons (intercepting clicks). Set an explicit
  ;; size instead.
  (w/set-window-size driver 1920 1080)

  ;; Disable the Disclaimer modal BEFORE the app initializes. The app reads
  ;; "behave-settings" (defined in behave/events.cljs; value is EDN) at :initialize, so the
  ;; value must be in localStorage before its scripts run. A CDP init-script seeds it on the
  ;; app origin at document-start, so a single load is enough (saves a full SPA boot per
  ;; scenario). Non-Chrome drivers can't do CDP → fall back to the old load-set-reload, which
  ;; sets it after the first boot and reloads so the second init reads it.
  (if (w/add-init-script! driver
                          "try{localStorage.setItem('behave-settings','{:show-disclaimer? false}');}catch(e){}")
    (w/goto driver url)                       ; single load — settings already seeded at document-start
    (do
      (w/goto driver url)                     ; establish origin so localStorage is writable
      (w/execute-script! driver
                         "localStorage.setItem('behave-settings', '{:show-disclaimer? false}');")
      (w/goto driver url)))                   ; reload → app inits with :show-disclaimer? false

  ;; Wait for the working area to confirm the page has rendered before proceeding
  (h/wait-for-working-area driver)

  ;; Click "New Run" button — generous timeout for the initial route load
  (h/wait-and-click-button-with-text driver "New Run" 25000)

  ;; Proceed through initial dialog
  (h/wait-and-click-button-with-text driver "Next")

  ;; Select "Guided Workflow"
  (h/wait-and-click-button-with-text driver "Open using Guided Workflow")

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

  ;; Wait for the worksheet wizard to finish rendering before returning
  (h/wait-for-wizard driver 30000)

  {:driver driver})
