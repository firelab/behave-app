(ns cucumber.runner
  (:require
   [clojure.java.io    :as io]
   [clojure.string     :as str]
   [tegere.loader      :refer [load-feature-files]]
   [tegere.steps       :refer [registry]]
   [tegere.runner      :refer [run]]
   [tegere.query :as query]
   [cucumber.webdriver :as w]
   ))

;; Debug

(def ^:private driver-atom (atom nil))

;; Step processing

(defn- get-driver [{:keys [debug?] :as opts}]
  (if debug?
    (or @driver-atom (reset! driver-atom (w/driver opts)))
    (w/driver opts)))

(defn load-steps!
  "Loads a directory of steps."
  [steps-dir]
  (let [step-files (filter #(str/ends-with? % ".clj") (seq (.listFiles steps-dir)))]
    (println "Step files:" step-files)
    (doall
     (for [step-file step-files]
       (try
         (load-file (str step-file))
         (catch Exception e
           (println (format "Unable to load namespace: %s \n %s" step-file (.getMessage e)))))))))


(defn run-cucumber-tests
  "Runs cucumber tests.

  Options:
    :features      - Path to feature files directory
    :steps         - Path to step definitions directory
    :url           - URL to run tests against
    :debug?        - Keep browser open after tests (default: false)
    :headless?     - Run browser in headless mode (default: false)
    :query-string  - Filter tests by query
    :stop          - Stop on first failure
    :browser       - Browser type (default: :chrome)
    :browser-path  - Path to browser executable"
  [{:keys [features steps url debug? headless? query-string stop] :as opts}]

  (when steps
    (load-steps! (io/file steps)))

  (let [driver (get-driver opts)]
    (println [:WEBDRIVER ]driver)
    (let [results (run (load-feature-files (io/file features))
                    @registry
                    (cond-> {}
                      query-string (assoc ::query/query-tree query-string)
                      stop         (assoc :tegere.runner/stop stop))
                    {:initial-ctx {:driver driver :url url}})]

      ;; Do something with output

      ;; Quit Driver
      (when-not debug?
        (w/quit driver))
      (:tegere.runner/outcome-summary-report results))))


(comment

  (load-steps! (io/file "./../../steps"))

  ;; Mac
  (run-cucumber-tests {;; :debug?   true
                       :features "./../../features"
                       ;; :steps    "./../../steps"
                       :browser  :chrome
                       :url      "http://localhost:8081/worksheets"})

  (get-driver {:debug? true :browser :chrome})

  ;; Linux
  (run-cucumber-tests {:debug?       true
                       :features     "./../../features"
                       :steps        "./../../steps"
                       :browser      :chrome
                       :browser-path "/usr/bin/google-chrome"
                       :url          "http://localhost:8081/worksheets"})

  ;; Mac - Headless Mode
  (run-cucumber-tests {:headless?  true
                       :features   "./../../features"
                       :browser    :chrome
                       :url        "http://localhost:8081/worksheets"})

  ;; Linux - Headless Mode
  (run-cucumber-tests {:headless?    true
                       :features     "./../../features"
                       :steps        "./../../steps"
                       :browser      :chrome
                       :browser-path "/usr/bin/google-chrome"
                       :url          "http://localhost:8081/worksheets"})

  (def run-test-10-times
    (let [results (doall (map (fn [_]
                                (run-cucumber-tests {:debug?       true
                                                     :features     "./../../features"
                                                     :steps        "./../../steps"
                                                     :browser      :chrome
                                                     :browser-path "/usr/bin/google-chrome"
                                                     :url          "http://localhost:8081/worksheets"}))
                              (range 10)))
          failed  (apply + (map #(get-in % [:tegere.runner/outcome-summary :tegere.runner/features-failed])
                                results))
          passed  (apply + (map #(get-in % [:tegere.runner/outcome-summary :tegere.runner/features-passed])
                                results))]
      (prn "passed: " passed)
      (prn "failed: " failed)))

  )
