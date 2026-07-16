(ns cucumber-run-driver
  "JVM-side cucumber runner. Invoked by the babashka orchestrator
   (scripts/run_cucumber_tests.clj) because babashka can't drive Selenium/tegere.
   Reporting (org/EDN rendering + failure formatting) lives in cucumber.report.

   Usage:
     clojure -M:dev:behave/cms scripts/cucumber_run_driver.clj <config-edn-file>

   The config EDN is:
     {:features-dir :steps-dir :url :headless :stop :query :org :edn :retry-failed}

   Runs each feature file one at a time (reusing one browser), and REWRITES the
   org file after every feature completes — so a partial, up-to-date report
   survives an interruption/crash. Also dumps raw results EDN incrementally."
  (:require [clojure.edn        :as edn]
            [clojure.java.io    :as io]
            [clojure.string     :as str]
            [cucumber.report    :as report]
            [cucumber.runner    :as cr]
            [cucumber.webdriver :as w]
            [tegere.loader      :as tl]
            [tegere.runner      :as tr]
            [tegere.steps       :as tsteps]))

;;; ---------------------------------------------------------------------------
;;; Feature discovery
;;; ---------------------------------------------------------------------------

(defn all-feature-files [features-dir]
  (->> (file-seq (io/file features-dir))
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".feature"))
       (map #(str/replace (.getPath %) (str features-dir "/") ""))
       sort))

(defn feature-name->file [features-dir]
  (into {}
        (keep (fn [rel]
                (let [content (slurp (io/file features-dir rel))
                      m       (re-find #"(?m)^\s*Feature:\s*(.+?)\s*$" content)]
                  (when m [(str/trim (second m)) rel])))
              (all-feature-files features-dir))))

;;; ---------------------------------------------------------------------------
;;; Run
;;; ---------------------------------------------------------------------------

(defn- classify
  "Classify a feature's already-reduced executables ({:feature :scenario :failure})."
  [reduced-exs]
  (let [fails (vec (keep :failure reduced-exs))]
    (cond (empty? reduced-exs) {:state :skip :fails []}
          (seq fails)          {:state :fail :fails fails}
          :else                {:state :pass :fails []})))

(defn- run-feature
  "Run a single tegere feature (reusing driver); return its executables (reduced
   to {:feature :scenario :failure}). Never throws — an escaping error becomes a
   synthetic failing scenario so the run continues."
  [driver feat {:keys [url query stop]}]
  (let [fname (:tegere.parser/name feat)]
    (try
      (let [results (tr/run [feat] @tsteps/registry
                            {:tegere.query/query-tree query :tegere.runner/stop stop}
                            :initial-ctx {:driver driver :url url})]
        (mapv (fn [ex] {:feature fname :scenario (report/scenario-title ex) :failure (report/ex-failure ex)})
              (:tegere.runner/executables results)))
      (catch Throwable t
        [{:feature fname                                                     :scenario "(feature errored)"
          :failure {:scenario "(feature errored)"   :type :error :step "run"
                    :reason   (str (.getMessage t))}}]))))

(defn -main [config-path]
  (let [{:keys [features-dir
                steps-dir
                url
                headless
                stop
                query
                org
                edn
                retry-failed
                browser-path
                feature-files]} (edn/read-string (slurp config-path))
        query                   (if (string? query) (read-string query) query)
        ;; When sharding, the orchestrator passes this shard's subset of features-dir-relative
        ;; paths; run only those. Absent ⇒ run every file (the N=1 path, unchanged).
        subset                  (when (seq feature-files) (set feature-files))
        all-files               (cond->> (all-feature-files features-dir)
                                  subset (filter subset))
        n->f                    (feature-name->file features-dir)
        status                  (atom (into {} (map (fn [f] [f {:state :pending :fails []}]) all-files)))
        executables             (atom [])
        t0                      (System/nanoTime)
        elapsed                 (fn [] (/ (- (System/nanoTime) t0) 1e9))
        write!                  (fn [& {:keys [summary?]}]
                                  (report/write! {:url     url       :query       query        :headless headless :stop stop
                                                  :elapsed (elapsed) :all-files   all-files
                                                  :status  @status   :executables @executables
                                                  :org     org       :edn         edn}
                                                 :summary? summary?))
        record!                 (fn [file fname exs]
                      ;; replace any prior executables for this feature, then re-add
                                  (swap! executables (fn [all] (into (vec (remove #(= fname (:feature %)) all)) exs)))
                                  (swap! status assoc file (classify exs))
                                  (write!))]
    (println (format "Loading steps from %s" steps-dir))
    (cr/load-steps! (io/file steps-dir))
    (write!)                                   ; initial all-pending org
    (let [features   (cond->> (tl/load-feature-files (io/file features-dir))
                       ;; restrict to this shard's files (match each feature to its file via n->f)
                       subset (filter #(subset (get n->f (:tegere.parser/name %) (:tegere.parser/name %)))))
          file->feat (into {} (for [ft features]
                                (let [nm (:tegere.parser/name ft)]
                                  [(get n->f nm nm) ft])))
          driver     (w/driver {:headless? headless :browser :chrome :browser-path browser-path})]
      (println [:WEBDRIVER driver])
      (try
        ;; main pass — one feature at a time, org rewritten after each
        (loop [fs features]
          (when-let [feat (first fs)]
            (let [fname (:tegere.parser/name feat)
                  file  (get n->f fname fname)]
              (println (format ">>> %s" file))
              (let [exs (run-feature driver feat {:url url :query query :stop stop})]
                (record! file fname exs)
                (if (and stop (= :fail (:state (get @status file))))
                  (println "Stopping on first failure (:stop true).")
                  (recur (rest fs)))))))
        ;; optional retry of failed files (same reused browser)
        (when (pos? retry-failed)
          (loop [i 1]
            (let [failed (->> @status (keep (fn [[f v]] (when (= :fail (:state v)) f))) vec)]
              (when (and (<= i retry-failed) (seq failed))
                (println (format "↻ Retry %d/%d on %d file(s)" i retry-failed (count failed)))
                (doseq [file failed]
                  (when-let [feat (file->feat file)]
                    (println (format ">>> (retry) %s" file))
                    (record! file (:tegere.parser/name feat)
                             (run-feature driver feat {:url url :query query :stop false}))))
                (recur (inc i))))))
        (finally
          (try (w/quit driver) (catch Throwable _ nil)))))
    ;; final write incl. summary (org first, then EDN with :summary last so it isn't clobbered)
    (write! :summary? true)
    (let [summary (report/counts all-files @status @executables)]
      (println (format "\nDONE in %s — Files: %d passed, %d failed, %d skipped | Scenarios: %d passed, %d failed"
                       (report/fmt-duration (elapsed)) (:passed summary) (:failed summary) (:skipped summary)
                       (:scenarios-passed summary) (:scenarios-failed summary))))
    (shutdown-agents)))

(apply -main *command-line-args*)
