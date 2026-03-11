(ns behave.test.helpers
  "App-specific test helpers for BehavePlus E2E tests."
  (:require [clojure.string :as str]
            [behave.test.cdp :as cdp]
            [behave.test.dom :as dom])
  (:import (java.util Base64)))

(def ^:private default-base-url "http://localhost:4242")

(defn navigate-home!
  "Navigates to the home/landing page via CDP Page.navigate.
   Sleeps to allow the page to begin loading."
  ([session]
   (navigate-home! session default-base-url))
  ([session base-url]
   (cdp/send-sync! session "Page.navigate" {:url (str base-url "/")} 10000)
   (Thread/sleep 3000)))

(defn select-card!
  "Clicks a .card element whose .card__header__title contains `title-text`.
   Uses MouseEvent dispatch for React/Reagent compatibility."
  [session title-text]
  (dom/wait-for! session ".card" 10000)
  (cdp/evaluate! session
                 (format
                  "(function() {
         var cards = document.querySelectorAll('.card');
         for (var i = 0; i < cards.length; i++) {
           var title = cards[i].querySelector('.card__header__title');
           if (title && title.textContent.includes('%s')) {
             cards[i].dispatchEvent(
               new MouseEvent('click', {bubbles: true, cancelable: true}));
             return true;
           }
         }
         return false;
       })()"
                  title-text)))

(defn select-module!
  "Clicks a module card by name on the home screen."
  [session module-name]
  (select-card! session module-name))

(defn select-workflow!
  "Clicks a workflow card by name in the workflow selection step."
  [session workflow-name]
  (dom/wait-for! session ".workflow-select" 10000)
  (select-card! session workflow-name))

(defn fill-input!
  "Finds an input by its label text and fills it with a value.
   Searches .input-text__label, .input-number__label, .input-dropdown__label."
  [session label value]
  (cdp/evaluate! session
                 (format
                  "(function() {
         var labels = document.querySelectorAll(
           '.input-text__label, .input-number__label, .input-dropdown__label');
         for (var i = 0; i < labels.length; i++) {
           if (labels[i].textContent.includes('%s')) {
             var container = labels[i].closest(
               '.input-text, .input-number, .input-dropdown, .wizard-input');
             if (!container) continue;
             var input = container.querySelector(
               'input, .input-dropdown__select-wrapper__select');
             if (input) {
               if (input.tagName === 'SELECT') {
                 input.value = '%s';
                 input.dispatchEvent(new Event('change', {bubbles: true}));
               } else {
                 var setter = Object.getOwnPropertyDescriptor(
                   HTMLInputElement.prototype, 'value').set;
                 setter.call(input, '%s');
                 input.dispatchEvent(new Event('input', {bubbles: true}));
                 input.dispatchEvent(new Event('change', {bubbles: true}));
               }
               return true;
             }
           }
         }
         return false;
       })()"
                  label value value)))

(defn click-next!
  "Clicks the wizard Next button (button.button--highlight with label 'Next')."
  [session]
  (cdp/evaluate! session
                 "(function() {
       var btns = document.querySelectorAll('.wizard-navigation button, .wizard-navigation__next button');
       for (var i = 0; i < btns.length; i++) {
         var label = btns[i].querySelector('.button__label');
         if (label && label.textContent.trim() === 'Next' && !btns[i].disabled) {
           btns[i].dispatchEvent(
             new MouseEvent('click', {bubbles: true, cancelable: true}));
           return true;
         }
       }
       return false;
     })()"))

(defn click-back!
  "Clicks the wizard Back button."
  [session]
  (cdp/evaluate! session
                 "(function() {
       var btns = document.querySelectorAll('.wizard-navigation button, .wizard-navigation__back button');
       for (var i = 0; i < btns.length; i++) {
         var label = btns[i].querySelector('.button__label');
         if (label && label.textContent.trim() === 'Back') {
           btns[i].dispatchEvent(
             new MouseEvent('click', {bubbles: true, cancelable: true}));
           return true;
         }
       }
       return false;
     })()"))

(defn click-button!
  "Clicks a button by its .button__label text content."
  [session button-text]
  (cdp/evaluate! session
                 (format
                  "(function() {
         var labels = document.querySelectorAll('.button__label');
         for (var i = 0; i < labels.length; i++) {
           if (labels[i].textContent.trim() === '%s') {
             labels[i].closest('.button').click();
             return true;
           }
         }
         return false;
       })()"
                  button-text)))

(defn get-results
  "Extracts the result table as a vector of maps."
  [session]
  (cdp/evaluate! session
                 "(function() {
       var table = document.querySelector('.results-table, table.results, table');
       if (!table) return null;
       var headers = Array.from(table.querySelectorAll('th')).map(function(th) {
         return th.textContent.trim();
       });
       var rows = Array.from(table.querySelectorAll('tbody tr')).map(function(tr) {
         var cells = Array.from(tr.querySelectorAll('td'));
         var row = {};
         cells.forEach(function(td, i) {
           row[headers[i]] = td.textContent.trim();
         });
         return row;
       });
       return rows;
     })()"))

(defn re-frame-state
  "Queries the re-frame app-db for the value at the given path.
   Path is a vector of keywords, e.g. [:ui :current-page]."
  [session path]
  (let [js-path (->> path
                     (map #(str "'" (name %) "'"))
                     (str/join ", "))]
    (cdp/evaluate! session
                   (format
                    "(function() {
           var db = re_frame.db.app_db.state;
           var path = [%s];
           var val = db;
           for (var i = 0; i < path.length; i++) {
             if (val == null) return null;
             val = val.get ? val.get(path[i]) : val[path[i]];
           }
           return typeof val === 'object' && val.toJS ? val.toJS() : val;
         })()"
                    js-path))))

(defn console-errors
  "Returns any console errors captured since page load."
  [session]
  (cdp/evaluate! session
                 "(function() {
       if (!window.__e2e_errors) return [];
       return window.__e2e_errors;
     })()"))

(defn install-console-capture!
  "Installs a JS error capture hook to collect console.error calls."
  [session]
  (cdp/evaluate! session
                 "(function() {
       window.__e2e_errors = [];
       var origError = console.error;
       console.error = function() {
         window.__e2e_errors.push(Array.from(arguments).map(String).join(' '));
         origError.apply(console, arguments);
       };
     })()"))

(defn toggle-checkbox!
  "Toggles a checkbox whose .input-checkbox__label contains `label-text`.
   Uses React-compatible event dispatch."
  [session label-text]
  (cdp/evaluate! session
                 (format
                  "(function() {
         var cbs = document.querySelectorAll('.input-checkbox');
         for (var i = 0; i < cbs.length; i++) {
           var label = cbs[i].querySelector('.input-checkbox__label');
           if (label && label.textContent.includes('%s')) {
             var input = cbs[i].querySelector('input[type=checkbox]');
             var nativeSet = Object.getOwnPropertyDescriptor(
               HTMLInputElement.prototype, 'checked').set;
             nativeSet.call(input, !input.checked);
             input.dispatchEvent(new Event('change', {bubbles: true}));
             return input.checked;
           }
         }
         return null;
       })()"
                  label-text)))

(defn checked-outputs
  "Returns a vector of labels for all checked output checkboxes."
  [session]
  (cdp/evaluate! session
                 "(function() {
       var cbs = document.querySelectorAll('.input-checkbox');
       var checked = [];
       for (var i = 0; i < cbs.length; i++) {
         var input = cbs[i].querySelector('input[type=checkbox]');
         var label = cbs[i].querySelector('.input-checkbox__label');
         if (input && input.checked && label) checked.push(label.textContent.trim());
       }
       return checked;
     })()"))

(defn click-progress-step!
  "Clicks a wizard progress step by label name."
  [session step-name]
  (cdp/evaluate! session
                 (format
                  "(function() {
         var nobs = document.querySelectorAll('.progress-nob');
         for (var i = 0; i < nobs.length; i++) {
           var label = nobs[i].querySelector('.progress-nob__label');
           if (label && label.textContent.includes('%s')) {
             nobs[i].dispatchEvent(
               new MouseEvent('click', {bubbles: true, cancelable: true}));
             return true;
           }
         }
         return false;
       })()"
                  step-name)))

(defn save-worksheet-bytes!
  "Triggers a worksheet save and captures the SQLite bytes.
   Returns the raw bytes as a byte array. Uses chunked base64 transfer
   to avoid CDP JSON response size limits."
  [session]
  ;; Step 1: export DB bytes and store in a global variable
  (cdp/evaluate! session
                 "(function() {
       return behave.store.export_db_bytes_BANG_()
         .then(function(bytes) {
           window.__e2e_db_bytes = bytes;
           return bytes.length;
         });
     })()" 30000)
  ;; Step 2: read total length and chunk count
  (let [total-len   (cdp/evaluate! session "window.__e2e_db_bytes.length")
        chunk-size  32768
        chunk-count (int (Math/ceil (/ (double total-len) chunk-size)))
    ;; Step 3: read each chunk as base64
        chunks (mapv (fn [i]
                       (cdp/evaluate!
                        session
                        (format
                         "(function() {
              var bytes = window.__e2e_db_bytes;
              var start = %d;
              var end = Math.min(start + %d, bytes.length);
              var chunk = bytes.subarray(start, end);
              var binary = '';
              for (var j = 0; j < chunk.length; j++) {
                binary += String.fromCharCode(chunk[j]);
              }
              return btoa(binary);
            })()"
                         (* i chunk-size) chunk-size)))
                     (range chunk-count))
        b64-str (str/join chunks)]
      ;; Step 4: cleanup and decode
    (cdp/evaluate! session "delete window.__e2e_db_bytes; true;")
    (.decode (Base64/getDecoder) ^String b64-str)))

(defn open-worksheet-file!
  "Opens a .bp7 worksheet by fetching it from the server and dispatching
   re-frame events as if the user had selected it via the file dialog.
   `file-url` should be a URL accessible from the browser (e.g. '/behave-test.bp7').
   Must be called while on the import page (/worksheets/import)."
  [session file-url]
  (cdp/evaluate! session
                 (format
                  "(function() {
         return fetch('%s')
           .then(function(resp) { return resp.arrayBuffer(); })
           .then(function(buf) {
             var filename = '%s'.split('/').pop();
             var file = new File([buf], filename, {type: 'application/x-sqlite3'});
             var dt = new DataTransfer();
             dt.items.add(file);
             re_frame.core.dispatch_sync(
               cljs.core.vector(
                 cljs.core.keyword('ws', 'worksheet-selected'),
                 dt.files));
             re_frame.core.dispatch_sync(
               cljs.core.vector(
                 cljs.core.keyword('wizard', 'open'),
                 file));
             return filename;
           });
       })()"
                  file-url file-url))
  (Thread/sleep 3000))

(defn selected-card-title
  "Returns the title text of the currently selected card, or nil."
  [session]
  (cdp/evaluate! session
                 "(function() {
       var card = document.querySelector('.card--selected .card__header__title');
       return card ? card.textContent.trim() : null;
     })()"))

(defn progress-step
  "Returns the label of the currently active progress step."
  [session]
  (cdp/evaluate! session
                 "(function() {
       var nob = document.querySelector('.progress-nob--selected .progress-nob__label');
       return nob ? nob.textContent.trim() : null;
     })()"))

(defn worksheet-uuid
  "Returns the current worksheet UUID from the URL hash."
  [session]
  (cdp/evaluate! session
                 "(function() {
       var match = window.location.href.match(/worksheets\\/([^/]+)/);
       return match ? match[1] : null;
     })()"))

(defn run-solve!
  "Dispatches the solve event for the current worksheet.
   Waits for computation to complete (worksheet-computing? becomes false)."
  [session timeout-ms]
  (let [ws-uuid (worksheet-uuid session)]
    (cdp/evaluate! session
                   (format
                    "(function() {
           var params = cljs.core.PersistentArrayMap.fromArray(
             [cljs.core.keyword('ws-uuid'), '%s'], true, false);
           re_frame.core.dispatch(
             cljs.core.vector(cljs.core.keyword('wizard','run-solve'), params));
           return true;
         })()"
                    ws-uuid)
                   10000)
    (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
      (Thread/sleep 2000)
      (loop []
        (let [computing? (cdp/evaluate! session
                                        "(function() {
              var db = re_frame.db.app_db.state;
              var k = cljs.core.keyword('worksheet-computing?');
              return cljs.core.get(db, k) === true;
            })()" 5000)]
          (when computing?
            (when (> (System/currentTimeMillis) deadline)
              (throw (ex-info "Timed out waiting for solve to complete"
                              {:timeout-ms timeout-ms})))
            (Thread/sleep 500)
            (recur)))))))

(defn result-tables
  "Returns the result table data from the results page as a vector of maps.
   Each map has :label and :value keys."
  [session]
  (cdp/evaluate! session
                 "(function() {
       var rows = document.querySelectorAll('.results-table tr, .result-row, table tr');
       if (!rows || rows.length === 0) return [];
       return Array.from(rows).map(function(tr) {
         var cells = Array.from(tr.querySelectorAll('td, th'));
         return cells.map(function(c) { return c.textContent.trim(); });
       }).filter(function(r) { return r.length > 0; });
     })()"))

(defn open-worksheet-from-bytes!
  "Opens a worksheet from raw bytes by uploading via a data URL and dispatching
   re-frame events. The bytes should be a byte array (from save-worksheet-bytes!)."
  [session ^bytes db-bytes]
  (let [b64 (.encodeToString (Base64/getEncoder) db-bytes)
        ;; Upload in chunks to avoid CDP limits
        chunk-size 32768
        chunks (mapv (fn [i]
                       (subs b64
                             (* i chunk-size)
                             (min (* (inc i) chunk-size) (count b64))))
                     (range (int (Math/ceil (/ (double (count b64)) chunk-size)))))]
    ;; Store chunks in JS
    (cdp/evaluate! session "window.__e2e_b64_chunks = [];")
    (doseq [b64-chunk chunks]
      (cdp/evaluate! session
                     (format "window.__e2e_b64_chunks.push('%s'); true;" b64-chunk)))
    ;; Reconstruct and import
    (cdp/evaluate! session
                   "(function() {
         var b64 = window.__e2e_b64_chunks.join('');
         delete window.__e2e_b64_chunks;
         var binary = atob(b64);
         var bytes = new Uint8Array(binary.length);
         for (var i = 0; i < binary.length; i++) {
           bytes[i] = binary.charCodeAt(i);
         }
         var file = new File([bytes], 'round-trip.bp7', {type: 'application/x-sqlite3'});
         var dt = new DataTransfer();
         dt.items.add(file);
         re_frame.core.dispatch_sync(
           cljs.core.vector(
             cljs.core.keyword('ws', 'worksheet-selected'),
             dt.files));
         re_frame.core.dispatch_sync(
           cljs.core.vector(
             cljs.core.keyword('wizard', 'open'),
             file));
         return true;
       })()" 30000))
  (Thread/sleep 3000))
