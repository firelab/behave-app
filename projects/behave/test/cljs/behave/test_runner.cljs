(ns behave.test-runner
  (:require [behave.contain-test]
            [behave.crown-test]
            [behave.diagram-test]
            [behave.events]
            [behave.help.subs]
            [behave.mortality-test]
            [behave.solver-test]
            [behave.subs]
            [behave.surface-test]
            [behave.test-solver-generators]
            [behave.test-solver-queries]
            [behave.tests-used-in-fixtures]
            [behave.utils-test]
            [behave.vms.store :refer [load-vms!]]
            [behave.vms.subs]
            [behave.wizard.events]
            [behave.wizard.subs]
            [behave.worksheet-events-test]
            [behave.worksheet-subs-test]
            [behave.worksheet.events]
            [behave.worksheet.subs]
            [cljs-test-display.core]
            [figwheel.main.testing :refer [run-tests]]
            [re-frame.core :as rf]
            [re-posh.core :as rp]))

(rp/reg-event-fx
 :transact
 (fn [_ [_ datoms]]
   {:transact datoms}))

(defn run-the-tests []
  (run-tests (cljs-test-display.core/init! "app-testing")
             'behave.crown-test
             'behave.contain-test
             'behave.mortality-test
             'behave.diagram-test
             'behave.surface-test
             'behave.solver-test
             'behave.tests-used-in-fixtures
             'behave.test-solver-generators
             'behave.test-solver-queries
             'behave.utils-test
             'behave.worksheet-events-test
             'behave.worksheet-subs-test))

(defn- load-wasm-module!
  "Bootstraps the Emscripten WASM module for the test harness.

  behave-min.js is a MODULARIZE build: it exposes a `createModule()` factory
  and does NOT define a global `Module` until that factory is called (see the
  main app's bootstrap in behave.views/cljs-init and resources/onload.js).
  We mirror that here so the test bundle works on host pages that do not load
  the WASM glue for us (notably figwheel's /figwheel-extra-main/testing).

  Re-enters `init` once `window.Module` is set."
  [init]
  (if (exists? js/createModule)
    ;; behave-min.js is already on the page (e.g. the /api/test host page) —
    ;; just instantiate the module.
    (-> (js/createModule)
        (.then (fn [instance]
                 (set! (.-Module js/window) instance)
                 (init))))
    ;; No WASM glue on the page (figwheel extra-main host page): load it, then
    ;; retry — the retry hits the createModule branch above.
    (let [script (.createElement js/document "script")]
      (set! (.-src script) "/js/behave-min.js")
      (set! (.-onload script) (fn [] (init)))
      (.appendChild (.-body js/document) script))))

(defn ^:after-load init []
  (let [module-loaded? (some? (.-Module js/window))
        vms-loaded?    @(rf/subscribe [:state :vms-loaded?])]

    (cond
      (not module-loaded?)
      (load-wasm-module! init)

      (not vms-loaded?)
      (do
        ;; `version` is only a cache-buster query param; the test server serves
        ;; layout.msgpack statically and ignores it.
        (load-vms! "test")
        (js/setTimeout #(init) 1000))

      :else
      (run-the-tests))))

(init)
