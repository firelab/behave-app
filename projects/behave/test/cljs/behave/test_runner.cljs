(ns behave.test-runner
  (:require [behave.events]
            [behave.help.subs]
            [behave.subs]
            [behave.vms.store :refer [load-vms!]]
            [behave.vms.subs]
            [behave.wizard.events]
            [behave.wizard.subs]
            [behave.tests-used-in-fixtures]
            [behave.worksheet-events-test]
            [behave.worksheet-subs-test]
            [behave.worksheet.events]
            [behave.worksheet.subs]
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
             'behave.tests-used-in-fixtures
             'behave.worksheet-events-test
             'behave.worksheet-subs-test))

(defn add-script [js-path]
  (let [script-el (.createElement js/document "script")]
    (set! (.-src script-el) js-path)
    (set! (.-type script-el) "text/javascript")
    (-> js/document
        (.-body)
        (.appendChild script-el))))

(defn ^:after-load init []
  (let [window-keys    (js->clj (.keys js/Object js/window))
        module-loaded? (seq (filter #(str/starts-with? % "Module") window-keys))
        vms-loaded?    @(rf/subscribe [:state :vms-loaded?])]
    (cond

      (not module-loaded?)
      (do (add-script "/js/behave.js")
          (js/setTimeout #(init) 100))

      (not vms-loaded?)
      (do
        (load-vms!)
        (js/setTimeout #(init) 1000))

      :else
      (run-the-tests))))

(init)
