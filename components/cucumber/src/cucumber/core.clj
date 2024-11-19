(ns cucumber.core
  (:require
   [clojure.java.io   :as io]
   [clojure.string    :as s]
   [clojure.tools.cli :refer [parse-opts]]
   [cucumber.runner   :as r]))

(def ^:private valid-options
  {:systems #{"Windows" "OS X" "android" "ios"}
   :browsers #{"android" "chrome" "edge" "firefox" "ie" "ipad" "iphone" "opera" "safari"}})

(def ^:private cli-options
  [["-s" "--steps" "Path to steps directory."
    :id :steps
    :validate #(-> (io/file %) (.exists))]
   ["-f" "--features" "Path to features directory."
    :id :features
    :validate #(-> (io/file %) (.exists))]
   ["-b" "--browser BROWSER" "Browser to test, default: 'chrome'"
    :id :browser
    :default "chrome"
    :parse-fn #(s/lower-case %)
    :validate [#(contains? (:browsers valid-options) %) (str "Must be one of: " (:browsers valid-options))]]
   [nil "--url" "URL to run the tests on."
    :id :url
    :validate [#(string? %) "Must be a string"]]
   [nil "--browser-path" "Path to the browser executable (e.g. '/usr/bin/google-chrome')"
    :id :browser-path]
   ["-r" "--remote" "Run using a remote driver"]
   [nil "--browser-version BROWSER VERSION" "Browser version to test in Remote Driver (-r), defaults to '88.0'"
    :id :browser-version
    :default "88.0"
    :validate [string? "Must be a string"]]
   ["-o" "--os OPERATING SYSTEM" "System to use in Remote Driver (-r), defaults to 'windows'"
    :id :os
    :default "Windows"
    :validate [#(contains? (:systems valid-options) %) (str "Must be one of: " (:systems valid-options))]]
   [nil "--os-version SYSTEM VERSION" "System Version to use in Remote Driver (-r), defaults to 10"
    :id :os-version
    :default "10"
    :validate [#(string? %) "Must be a string"]]
   ["-t" "--test-name TEST NAME" "Browserstack test name to use in Remote Driver (-r)"
    :id :test-name
    :validate [#(string? %) "Must be a string"]]
   ["-u" "--username USERNAME" "Browserstack Username to use in Remote Driver (-r), defaults to environment variable 'BS_USERNAME'"
    :id :username
    :parse-fn #(or % (System/getenv "BS_USERNAME"))
    :validate [#(string? %) "Must be a string"]]
   ["-p" "--api-key API KEY" "Browserstack API Key to use in Remote Driver (-r), defaults to environment variable 'BS_API_KEY'"
    :id :api-key
    :parse-fn #(or % (System/getenv "BS_API_KEY"))
    :validate [#(string? %) "Must be a string"]]
   ["-o" "--output-dir DIR" "Output directory for log files. When a directory is not provided, output will be to stdout."
    :id :output
    :default ""]
   ["-h" "--help"]])

(defn- usage [options-summary]
  (->> ["Runs automated BDD-style tests in the browser"
        ""
        "Usage: clj -M:cucumber [options]"
        ""
        "Options:"
        options-summary] (s/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occured while parsing your command:\n\n"
       (s/join \newline errors)))

(defn- validate-args [args]
  (let [{:keys [options summary errors]} (parse-opts args cli-options)]
    (cond
      (:help options) {:exit-message (usage summary) :ok? true}
      errors {:exit-message (error-msg errors)}
      (< 0 (count options)) {:options options}
      :else {:exit-message (usage summary)})))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn start-tests
  "Run cucumber tests with `opts`"
  [opts]
  (r/run-cucumber-tests opts))

(defn -main
  "CLI entry point."
  [& args]
  (let [{:keys [options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (start-tests options))))
