(ns behave.test-support
  "Shared bootstrap for the DOM [[behave.test-runner]] and headless
  [[behave.headless-test-runner]] runners: loads the WASM module + VMS, then
  calls a `run` callback. Requires no test namespaces and does not self-run."
  (:require [behave.vms.store :refer [load-vms!]]
            [re-frame.core    :as rf]
            [re-posh.core     :as rp]))

;; Fixtures dispatch [:transact ...]; register it once for either runner.
(rp/reg-event-fx
 :transact
 (fn [_ [_ datoms]]
   {:transact datoms}))

(defn- load-wasm-module!
  "Instantiate the WASM module (`createModule` from behave-min.js), injecting the
  glue script first if it isn't on the page. Calls `continue` once `window.Module`
  is set."
  [continue]
  (if (exists? js/createModule)
    (-> (js/createModule)
        (.then (fn [instance]
                 (set! (.-Module js/window) instance)
                 (continue))))
    (let [script (.createElement js/document "script")]
      (set! (.-src script) "/js/behave-min.js")
      (set! (.-onload script) (fn [] (continue)))
      (.appendChild (.-body js/document) script))))

(defn ensure-test-env!
  "Bootstrap the WASM module + VMS, then call `run` once both are ready.
  Re-enters itself as each async step completes."
  [run]
  (let [module-loaded? (some? (.-Module js/window))
        vms-loaded?    @(rf/subscribe [:state :vms-loaded?])]
    (cond
      (not module-loaded?)
      (load-wasm-module! #(ensure-test-env! run))

      (not vms-loaded?)
      (do
        (load-vms! "test")                       ; "test" is just a cache-buster
        (js/setTimeout #(ensure-test-env! run) 1000))

      :else
      (run))))
