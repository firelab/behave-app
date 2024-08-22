(ns cucumber.runner
  (:require
   [clojure.java.io    :as io]
   [clojure.string     :as str]
   [tegere.loader      :refer [load-feature-files]]
   [tegere.steps       :refer [registry]]
   [tegere.runner      :refer [run]]
   [cucumber.webdriver :as w]))

;; Step processing

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
  "Runs cucumber tests "
  [{:keys [features steps] :as opts}]

  (when steps
    (load-steps! (io/file steps)))

  (let [driver (w/driver opts)]
    (run (load-feature-files (io/file features))
      @registry
      {}
      {:initial-ctx {:driver driver}})
    ;; Do something with output
    #_(w/quit driver)))


(comment
  (run-cucumber-tests {:features "./../../features"
                       :steps    "./../../steps"
                       :browser  :chrome})
 )
