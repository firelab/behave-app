(ns behave-cms.test-runner
  (:require [behave-cms.events]
            [behave-cms.subs]
            [behave-cms.simple-test]
            [browser-utils.core :refer [add-script]]
            [cljs-test-display.core]
            [clojure.string :as str]
            [figwheel.main.testing :refer [run-tests]]
            [re-frame.core :as rf]
            [re-posh.core :as rp]))

(defn run-the-tests []
  (run-tests (cljs-test-display.core/init! "app-testing")
             'behave-cms.simple-test))

(defn ^:after-load init []
  (run-the-tests))

(init)

;;;; tests can be asynchronous, we must hook test end
;;(defmethod report [:cljs.test/default :end-run-tests] [test-data]
;;  (if (cljs.test/successful? test-data)
;;    (async-result/send "Tests passed!!")
;;    (async-result/throw-ex (ex-info "Tests Failed" test-data))))
;;
;;(defn -main [& args]
;;  (run-tests 'behave-cms.simple-test)
;;  ;; return a message to the figwheel process that tells it to wait
;;  [:figwheel.main.async-result/wait 5000])

;;(ns behave-cms.test-runner
;;  (:require
;;    [figwheel.main.testing :refer-macros [run-tests-async]]
;;    ;; require all the namespaces that have tests in them
;;    [behave-cms.simple-test]))
;;
;;(defn -main [& args]
;;  ;; this needs to be the last statement in the main function so that it can
;;  ;; return the value `[:figwheel.main.async-result/wait 10000]`
;;  (run-tests-async 10000))
