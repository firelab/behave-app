(ns behave.test-runner
  (:require [behave.events]
            [behave.help.subs]
            [behave.subs]
            [behave.vms.store :refer [load-vms!]]
            [behave.contain-test]
            [behave.crown-test]
            [behave.diagram-test]
            [behave.mortality-test]
            [behave.solver-test]
            [behave.surface-test]
            [behave.test-solver-generators]
            [behave.test-solver-queries]
            [behave.tests-used-in-fixtures]
            [behave.utils-test]
            [behave.vms.subs]
            [behave.wizard.events]
            [behave.wizard.subs]
            [behave.worksheet-events-test]
            [behave.worksheet-subs-test]
            [behave.worksheet.events]
            [behave.worksheet.subs]
            [browser-utils.core :refer [add-script]]
            [cljs-test-display.core]
            [clojure.string :as str]
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
             'behave.worksheet-subs-test
             ))

(defn ^:after-load init []
  (let [window-keys    (js->clj (.keys js/Object js/window))
        module-loaded? (seq (filter #(str/starts-with? % "Module") window-keys))
        vms-loaded?    @(rf/subscribe [:state :vms-loaded?])]

    (cond
      (not module-loaded?)
      (do (set! (.-onWASMModuleLoaded js/window) init)
          (add-script "/js/behave-min.js"))

      (not vms-loaded?)
      (do
        (load-vms!)
        (js/setTimeout #(init) 1000))

      :else
      (run-the-tests))))

(init)
