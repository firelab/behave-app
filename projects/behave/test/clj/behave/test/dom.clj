(ns behave.test.dom
  "DOM query and interaction helpers for E2E testing via CDP.
   All interaction is done through Runtime.evaluate with JS snippets."
  (:require [behave.test.cdp :as cdp]))

(defn click!
  "Clicks the first element matching `selector`.
   Uses MouseEvent dispatch for React/Reagent compatibility."
  [session selector]
  (cdp/evaluate! session
                 (format "document.querySelector('%s').dispatchEvent(
                    new MouseEvent('click', {bubbles: true, cancelable: true}))"
                         selector)))

(defn fill!
  "Fills an input element matching `selector` with `text`.
   Uses the native value setter trick for React/Reagent compatibility."
  [session selector text]
  (cdp/evaluate! session
                 (format
                  "(function() {
         var el = document.querySelector('%s');
         var setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
         setter.call(el, '%s');
         el.dispatchEvent(new Event('input', {bubbles: true}));
         el.dispatchEvent(new Event('change', {bubbles: true}));
       })()"
                  selector
                  text)))

(defn text
  "Returns the textContent of the first element matching `selector`."
  [session selector]
  (cdp/evaluate! session
                 (format "document.querySelector('%s').textContent" selector)))

(defn inner-html
  "Returns the innerHTML of the first element matching `selector`."
  [session selector]
  (cdp/evaluate! session
                 (format "document.querySelector('%s').innerHTML" selector)))

(defn visible?
  "Returns true if the element matching `selector` exists and is visible."
  [session selector]
  (cdp/evaluate! session
                 (format
                  "(function() {
         var el = document.querySelector('%s');
         return el !== null && el.offsetParent !== null;
       })()"
                  selector)))

(defn exists?
  "Returns true if an element matching `selector` exists in the DOM."
  [session selector]
  (cdp/evaluate! session
                 (format "document.querySelector('%s') !== null" selector)))

(defn wait-for!
  "Polls until an element matching `selector` exists, checking every 250ms.
   Throws if `timeout-ms` (default 10000) elapses."
  ([session selector]
   (wait-for! session selector 10000))
  ([session selector timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (if (exists? session selector)
         true
         (do
           (when (> (System/currentTimeMillis) deadline)
             (throw (ex-info "Timed out waiting for element"
                             {:selector selector :timeout-ms timeout-ms})))
           (Thread/sleep 250)
           (recur)))))))

(defn query-all
  "Returns the count of elements matching `selector`."
  [session selector]
  (cdp/evaluate! session
                 (format "document.querySelectorAll('%s').length" selector)))

(defn select-option!
  "Selects an option in a <select> element by value."
  [session selector value]
  (cdp/evaluate! session
                 (format
                  "(function() {
         var el = document.querySelector('%s');
         el.value = '%s';
         el.dispatchEvent(new Event('change', {bubbles: true}));
       })()"
                  selector
                  value)))

(defn attribute
  "Returns the value of `attr` on the first element matching `selector`."
  [session selector attr]
  (cdp/evaluate! session
                 (format "document.querySelector('%s').getAttribute('%s')" selector attr)))
