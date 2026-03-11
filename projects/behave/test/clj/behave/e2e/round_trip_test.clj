(ns behave.e2e.round-trip-test
  "E2E round-trip test: create worksheet, configure outputs/inputs,
   run simulation, export, reimport, and verify preservation."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [behave.test.cdp :as cdp]
            [behave.test.dom :as dom]
            [behave.test.fixture :as fix]
            [behave.test.helpers :as h]
            [behave.test.lifecycle :as lifecycle]))

;;; Test state — shared session via fixture, local state for round-trip data

(def ^:private *local (atom {}))

(use-fixtures :once fix/e2e-fixture)

(defn- session [] (fix/session))

(defn- wait-for-url-change!
  [cdp-session previous-url timeout-ms]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [current (cdp/evaluate! cdp-session "window.location.href")]
        (if (not= current previous-url)
          current
          (do
            (when (> (System/currentTimeMillis) deadline)
              (throw (ex-info "URL did not change" {:expected-change-from previous-url})))
            (Thread/sleep 250)
            (recur)))))))

(defn- wait-for-solve!
  "Polls until worksheet-computing? is false."
  [cdp-session timeout-ms]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (Thread/sleep 1000)
    (loop []
      (let [computing? (cdp/evaluate! cdp-session
                                      "(function() {
              var db = re_frame.db.app_db.state;
              var k = cljs.core.keyword('worksheet-computing?');
              return cljs.core.get(db, k) === true;
            })()" 5000)]
        (when computing?
          (when (> (System/currentTimeMillis) deadline)
            (throw (ex-info "Timed out waiting for solve" {:timeout-ms timeout-ms})))
          (Thread/sleep 500)
          (recur))))))

;;; Tests

(deftest round-trip-test
  (println "[round-trip] START")

  (testing "Step 1: Create new Surface Only worksheet"
    (println "[round-trip] Step 1: Creating new Surface Only worksheet")
    (println "[round-trip]   navigating home...")
    (h/navigate-home! (session))
    (lifecycle/wait-for-app-ready! (session) 60000)
    (dom/wait-for! (session) ".card-group" 10000)

    ;; Select New Run
    (println "[round-trip]   selecting 'New Run'...")
    (h/select-card! (session) "New Run")
    (Thread/sleep 500)
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (println "[round-trip]   clicking Next -> workflow selection")
      (h/click-next! (session))
      (wait-for-url-change! (session) url-before 5000))

    ;; Select Standard workflow
    (dom/wait-for! (session) ".workflow-select" 5000)
    (Thread/sleep 500)
    (println "[round-trip]   selecting 'Standard'...")
    (h/select-card! (session) "Standard")
    (Thread/sleep 500)
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (println "[round-trip]   clicking Next -> module selection")
      (h/click-next! (session))
      (wait-for-url-change! (session) url-before 5000))

    ;; Select Surface Only module
    (Thread/sleep 500)
    (println "[round-trip]   selecting 'Surface Only'...")
    (h/select-card! (session) "Surface Only")
    (Thread/sleep 500)
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (println "[round-trip]   clicking Next -> create worksheet")
      (h/click-next! (session))
      (let [new-url (wait-for-url-change! (session) url-before 5000)]
        (println "[round-trip]   worksheet URL:" new-url)
        (is (re-find #"worksheets/.+/standard" new-url)
            "Should navigate to the worksheet"))))

  (testing "Step 2: Select outputs"
    (println "[round-trip] Step 2: Selecting outputs")
    (Thread/sleep 2000)
    ;; We should be on the output selection page
    (dom/wait-for! (session) ".input-checkbox" 10000)

    ;; Toggle Rate of Spread and Flame Length
    (println "[round-trip]   toggling 'Rate of Spread'...")
    (let [ros-result (h/toggle-checkbox! (session) "Rate of Spread")]
      (println "[round-trip]   toggle result:" ros-result))
    (Thread/sleep 300)
    (println "[round-trip]   toggling 'Flame Length'...")
    (let [fl-result (h/toggle-checkbox! (session) "Flame Length")]
      (println "[round-trip]   toggle result:" fl-result))
    (Thread/sleep 300)

    (let [checked (h/checked-outputs (session))]
      (println "[round-trip]   checked outputs:" checked)
      (is (some #(re-find #"Rate of Spread" %) checked)
          "Rate of Spread should be checked")
      (is (some #(re-find #"Flame Length" %) checked)
          "Flame Length should be checked")))

  (testing "Step 3: Navigate to inputs and fill values"
    (println "[round-trip] Step 3: Navigating to inputs")
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (h/click-next! (session))
      (let [new-url (wait-for-url-change! (session) url-before 5000)]
        (println "[round-trip]   input page URL:" new-url)))
    (Thread/sleep 2000)

    ;; Fuel model dropdown
    (let [has-dropdown (dom/exists? (session) ".input-dropdown__select-wrapper__select")]
      (println "[round-trip]   fuel model dropdown present:" has-dropdown)
      (when has-dropdown
        ;; Inspect the dropdown first
        (let [dropdown-info (cdp/evaluate! (session)
                                           "(function() {
             var selects = document.querySelectorAll('.input-dropdown__select-wrapper__select');
             return {
               count: selects.length,
               options: selects.length > 0
                 ? Array.from(selects[0].options).slice(0, 5).map(function(o) {
                     return {value: o.value, text: o.textContent.trim()};
                   })
                 : [],
               currentValue: selects.length > 0 ? selects[0].value : null
             };
           })()")]
          (println "[round-trip]   dropdown info:" dropdown-info))
        (try
          (let [selected-val (cdp/evaluate! (session)
                                            "(function() {
               var selects = document.querySelectorAll('.input-dropdown__select-wrapper__select');
               if (selects.length > 0 && selects[0].options.length > 1) {
                 var nativeSetter = Object.getOwnPropertyDescriptor(
                   HTMLSelectElement.prototype, 'value').set;
                 nativeSetter.call(selects[0], selects[0].options[1].value);
                 selects[0].dispatchEvent(new Event('change', {bubbles: true}));
                 return selects[0].value;
               }
               return null;
             })()")]
            (println "[round-trip]   selected fuel model:" selected-val))
          (catch Exception e
            (println "[round-trip]   ERROR selecting fuel model:" (.getMessage e))))))
    (Thread/sleep 1000)

    ;; Fill numeric inputs
    (try
      (let [input-labels (cdp/evaluate! (session)
                                        "Array.from(document.querySelectorAll(
                            '.wizard-input .input-number__label, .wizard-input label'))
                          .map(function(el) { return el.textContent.trim(); })
                          .filter(function(t) { return t.length > 0; })")]
        (println "[round-trip]   input labels on page:" input-labels)

        (doseq [label-value [["1-Hour" "6"]
                             ["10-Hour" "7"]
                             ["100-Hour" "8"]
                             ["Live Herb" "60"]
                             ["Live Woody" "90"]
                             ["Midflame" "5"]
                             ["Slope" "20"]
                             ["Wind" "5"]]]
          (let [[label value] label-value]
            (when (some #(re-find (re-pattern (str "(?i)" label)) %) (or input-labels []))
              (try
                (println "[round-trip]   filling" label "=" value)
                (h/fill-input! (session) label value)
                (Thread/sleep 200)
                (catch Exception e
                  (println "[round-trip]   ERROR filling" label ":" (.getMessage e))))))))
      (catch Exception e
        (println "[round-trip]   ERROR reading input labels:" (.getMessage e)))))

  (testing "Step 4: Run the simulation"
    (println "[round-trip] Step 4: Running simulation")
    (Thread/sleep 1000)
    (let [has-run-btn (cdp/evaluate! (session)
                                     "(function() {
           var labels = document.querySelectorAll('.button__label');
           for (var i = 0; i < labels.length; i++) {
             if (labels[i].textContent.trim().match(/run|calculate|solve/i)) {
               return labels[i].textContent.trim();
             }
           }
           return null;
         })()")
          ws-uuid (h/worksheet-uuid (session))]
      (println "[round-trip]   run button:" has-run-btn)
      (println "[round-trip]   worksheet UUID:" ws-uuid)

      (if has-run-btn
        ;; Click the run button via UI
        (do
          (println "[round-trip]   clicking run button...")
          (h/click-button! (session) has-run-btn)
          (wait-for-solve! (session) 30000))
        ;; Dispatch solve via re-frame
        (do
          (println "[round-trip]   no run button, dispatching solve via re-frame...")
          (cdp/evaluate! (session)
                         (format
                          "(function() {
               var params = new cljs.core.PersistentArrayMap(null, 1,
                 [cljs.core.keyword('ws-uuid'), '%s'], null);
               re_frame.core.dispatch(
                 cljs.core.vector(cljs.core.keyword('wizard','run-solve'), params));
               return true;
             })()"
                          ws-uuid)
                         10000)
          (wait-for-solve! (session) 30000)))

      (println "[round-trip]   solve completed")))

  (testing "Step 5: Export worksheet bytes"
    (println "[round-trip] Step 5: Exporting worksheet bytes")
    (Thread/sleep 1000)
    (let [db-bytes (h/save-worksheet-bytes! (session))]
      (is (some? db-bytes) "Should get worksheet bytes")
      (is (> (alength ^bytes db-bytes) 0) "Bytes should not be empty")
      (println "[round-trip]   exported" (alength ^bytes db-bytes) "bytes")

      ;; Store bytes for reimport
      (swap! *local assoc :exported-bytes db-bytes)))

  (testing "Step 6: Capture original state for comparison"
    (println "[round-trip] Step 6: Capturing original state")
    ;; Navigate to outputs page to capture checked outputs
    (println "[round-trip]   navigating to Output step...")
    (h/click-progress-step! (session) "Output")
    (Thread/sleep 2000)
    (let [original-outputs (h/checked-outputs (session))]
      (println "[round-trip]   original outputs:" original-outputs)
      (swap! *local assoc :original-outputs original-outputs))

    ;; Navigate to inputs to capture input values
    (println "[round-trip]   navigating to Input step...")
    (h/click-progress-step! (session) "Input")
    (Thread/sleep 2000)
    (let [original-inputs (cdp/evaluate! (session)
                                         "(function() {
           var inputs = {};
           var wizInputs = document.querySelectorAll('.wizard-input');
           for (var j = 0; j < wizInputs.length; j++) {
             var lbl = wizInputs[j].querySelector(
               '.input-number__label, .input-dropdown__label');
             var inp = wizInputs[j].querySelector('input, select');
             if (lbl && inp && inp.value) {
               inputs[lbl.textContent.trim()] = inp.value;
             }
           }
           return inputs;
         })()")]
      (println "[round-trip]   original inputs:" original-inputs)
      (swap! *local assoc :original-inputs original-inputs)))

  (testing "Step 7: Import the exported worksheet"
    (println "[round-trip] Step 7: Importing exported worksheet")
    ;; Navigate home and start Open existing flow
    (println "[round-trip]   navigating home...")
    (h/navigate-home! (session))
    (lifecycle/wait-for-app-ready! (session) 60000)
    (dom/wait-for! (session) ".card-group" 10000)

    (println "[round-trip]   selecting 'existing'...")
    (h/select-card! (session) "existing")
    (Thread/sleep 500)
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (println "[round-trip]   clicking Next -> workflow selection")
      (h/click-next! (session))
      (wait-for-url-change! (session) url-before 5000))

    ;; Select Standard workflow
    (dom/wait-for! (session) ".workflow-select" 5000)
    (Thread/sleep 500)
    (println "[round-trip]   selecting 'Standard'...")
    (h/select-card! (session) "Standard")
    (Thread/sleep 500)
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (println "[round-trip]   clicking Next -> import page")
      (h/click-next! (session))
      (wait-for-url-change! (session) url-before 5000))

    ;; Should be on import page
    (Thread/sleep 500)
    (let [url (cdp/evaluate! (session) "window.location.href")]
      (println "[round-trip]   import page URL:" url)
      (is (re-find #"import" url)
          "Should be on import page"))

    ;; Import the exported bytes
    (let [db-bytes (:exported-bytes @*local)]
      (println "[round-trip]   uploading" (alength ^bytes db-bytes) "bytes via chunked base64...")
      (h/open-worksheet-from-bytes! (session) db-bytes))
    (println "[round-trip]   worksheet imported")

    ;; Click Next to open
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (println "[round-trip]   clicking Next -> open reimported worksheet")
      (h/click-next! (session))
      (let [new-url (wait-for-url-change! (session) url-before 10000)]
        (println "[round-trip]   reimported worksheet URL:" new-url)
        (is (re-find #"worksheets/.+/standard" new-url)
            "Should navigate to the reimported worksheet"))))

  (testing "Step 8: Verify outputs are preserved"
    (println "[round-trip] Step 8: Verifying outputs")
    (Thread/sleep 2000)
    (dom/wait-for! (session) ".input-checkbox" 10000)
    (let [reimported-outputs (h/checked-outputs (session))
          original-outputs (:original-outputs @*local)]
      (println "[round-trip]   reimported outputs:" reimported-outputs)
      (println "[round-trip]   original outputs:  " original-outputs)
      (is (= (set original-outputs) (set reimported-outputs))
          "Outputs should be preserved after round-trip")))

  (testing "Step 9: Verify inputs are preserved"
    (println "[round-trip] Step 9: Verifying inputs")
    (println "[round-trip]   navigating to Input step...")
    (h/click-progress-step! (session) "Input")
    (Thread/sleep 2000)
    (let [reimported-inputs (cdp/evaluate! (session)
                                           "(function() {
           var inputs = {};
           var wizInputs = document.querySelectorAll('.wizard-input');
           for (var j = 0; j < wizInputs.length; j++) {
             var lbl = wizInputs[j].querySelector(
               '.input-number__label, .input-dropdown__label');
             var inp = wizInputs[j].querySelector('input, select');
             if (lbl && inp && inp.value) {
               inputs[lbl.textContent.trim()] = inp.value;
             }
           }
           return inputs;
         })()")
          original-inputs (:original-inputs @*local)]
      (println "[round-trip]   reimported inputs:" reimported-inputs)
      (println "[round-trip]   original inputs:  " original-inputs)
      ;; Check that non-empty original inputs are present in reimported
      (doseq [[k v] original-inputs]
        (when (and v (not= v ""))
          (is (= v (get reimported-inputs k))
              (str "Input '" k "' should be preserved (expected " v ")"))))))

  (println "[round-trip] DONE"))
