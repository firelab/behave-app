#!/usr/bin/env bb
;; Run the full cucumber suite and generate a timestamped cucumber_test_results_<ts>.org.
;;
;; Babashka can't drive Selenium/tegere, so this is a thin orchestrator:
;;   1. preflight (app reachable? layout.msgpack fresh?)
;;   2. write a config EDN and shell out to the JVM driver
;;      (scripts/cucumber_run_driver.clj), which runs tegere against a live Chrome
;;      and REWRITES the org after every feature (incremental — survives a crash).
;;   3. read the driver's final summary and report.
;;
;; Usage:
;;   bb scripts/run_cucumber_tests.clj [opts]      (or:  bb cucumber [opts])
;;   opts: --features-dir --steps-dir --url --query --headless --stop
;;         --org --edn --retry-failed --help
(ns run-cucumber-tests
  (:require [babashka.cli     :as cli]
            [babashka.fs      :as fs]
            [babashka.process :as p]
            [clojure.edn      :as edn]
            [clojure.java.io  :as io]
            [clojure.string   :as str]))

;;; ---------------------------------------------------------------------------
;;; Options
;;; ---------------------------------------------------------------------------

(def cli-spec
  {:features-dir {:desc "Feature files dir" :default "features"}
   :steps-dir    {:desc "Step definitions dir" :default "steps"}
   :url          {:desc "App URL" :default "http://localhost:8081/worksheets"}
   :query        {:desc "tegere query-tree (EDN string)" :default "(and \"core\" (not \"extended\"))"}
   :headless     {:desc "Run Chrome headless" :coerce :boolean :default false}
   :stop         {:desc "Stop on first failure" :coerce :boolean :default false}
   :org          {:desc "Output org file (default: cucumber_test_results_<timestamp>.org)"}
   :edn          {:desc "Raw results EDN (default: cucumber_results_<timestamp>.edn)"}
   :retry-failed {:desc "Re-run failed files N times" :coerce :long :default 0}
   :browser-path {:desc "Chrome/Chromium binary for Selenium (default: auto-detect)"}
   :help         {:desc "Show help" :coerce :boolean}})

(def driver-script "scripts/cucumber_run_driver.clj")
(def run-log "cucumber_run.log")

(defn run-stamp
  "Current local date-time as a filename-safe 'yyyy-MM-dd_HH-mm-ss'."
  []
  (.format (java.time.LocalDateTime/now)
           (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd_HH-mm-ss")))

(defn fmt-duration [secs]
  (let [s (long secs) m (quot s 60) r (rem s 60)] (format "%dm %02ds" m r)))

;;; ---------------------------------------------------------------------------
;;; Preflight
;;; ---------------------------------------------------------------------------

(defn http-status [url]
  (try (str/trim (:out (p/shell {:out :string :err :string}
                                "curl" "-s" "-o" "/dev/null" "-w" "%{http_code}"
                                "--max-time" "10" url)))
       (catch Exception _ "000")))

(defn preflight! [{:keys [url features-dir steps-dir]}]
  (when-not (fs/exists? driver-script)
    (println "ERROR: missing" driver-script) (System/exit 1))
  (doseq [d [features-dir steps-dir]]
    (when-not (fs/exists? d)
      (println "ERROR: dir not found:" d) (System/exit 1)))
  (let [code (http-status url)]
    (when-not (= "200" code)
      (println (format "ERROR: app not reachable at %s (HTTP %s). Start it first." url code))
      (System/exit 1))
    (println (format "✓ App reachable at %s (HTTP %s)" url code)))
  ;; warn if the served layout.msgpack differs from the source (stale served copy)
  (let [src "projects/behave/resources/public/layout.msgpack"]
    (when (fs/exists? src)
      (try
        (let [served (:out (p/shell {:out :bytes} "curl" "-s" "--max-time" "10"
                                    (str (str/replace url #"/worksheets.*$" "") "/layout.msgpack")))
              smd5   (str/trim (:out (p/shell {:out :string :in served} "md5sum")))
              fmd5   (str/trim (:out (p/shell {:out :string} "md5sum" src)))]
          (when (not= (first (str/split smd5 #"\s")) (first (str/split fmd5 #"\s")))
            (println "⚠ WARNING: served /layout.msgpack differs from" src
                     "\n  The running app may be serving stale VMS data (restart/rebuild to refresh).")))
        (catch Exception _ nil)))))

;;; ---------------------------------------------------------------------------
;;; Main
;;; ---------------------------------------------------------------------------

(defn -main [args]
  (let [opts (cli/parse-opts args {:spec cli-spec})]
    (when (:help opts)
      (println "Run the full cucumber suite and write a timestamped org log.\n")
      (println (cli/format-opts {:spec cli-spec}))
      (System/exit 0))
    (spit run-log "")                          ; fresh run log
    (preflight! opts)
    (let [ts       (run-stamp)
          org-file (or (:org opts) (str "cucumber_test_results_" ts ".org"))
          edn-file (or (:edn opts) (str "cucumber_results_" ts ".edn"))
          cfg      {:features-dir (:features-dir opts)   :steps-dir (:steps-dir opts)
                    :url          (:url opts)            :headless  (boolean (:headless opts))
                    :stop         (boolean (:stop opts)) :query     (:query opts)
                    :org          org-file               :edn       edn-file                   :retry-failed (:retry-failed opts)
                    :browser-path (:browser-path opts)}
          cfg-file (str (fs/create-temp-file {:prefix "cuke-cfg-" :suffix ".edn"}))
          logf     (io/file run-log)]
      (spit cfg-file (pr-str cfg))
      (println (format "▶ Running cucumber → %s  (incremental; live: tail -f %s)" org-file run-log))
      (p/shell {:out :append :out-file logf :err :append :err-file logf :continue true}
               "clojure" "-M:dev:behave/cms" driver-script cfg-file)
      (println "\n==== DONE ====")
      (if (fs/exists? edn-file)
        (let [{:keys [summary elapsed-seconds]} (edn/read-string (slurp edn-file))]
          (when elapsed-seconds (println "Time:" (fmt-duration elapsed-seconds)))
          (if summary
            (do
              (println (format "Files: %d passed, %d failed, %d skipped | Scenarios: %d passed, %d failed"
                               (:passed summary) (:failed summary) (:skipped summary)
                               (:scenarios-passed summary) (:scenarios-failed summary)))
              (when (seq (:failed-files summary))
                (println "Failing files:")
                (doseq [f (:failed-files summary)] (println "  -" f))))
            (println "⚠ Run ended without a final summary (interrupted?). Partial report kept.")))
        (println "⚠ No results EDN produced — see" run-log))
      (println "Org:" org-file "| raw:" edn-file "| run log:" run-log))))

(-main *command-line-args*)

;; Usage
;; bb cucumber                       # full suite, visible Chrome → cucumber_test_results_<ts>.org
;; bb cucumber --headless true       # headless
;; bb cucumber --retry-failed 2      # re-run failures up to 2× (in-driver)
;; bb cucumber --help                # all options
