(ns cucumber-run-driver
  "JVM-side cucumber runner + incremental org reporter. Invoked by the babashka
   orchestrator (scripts/run_cucumber_tests.clj) because babashka can't drive
   Selenium/tegere.

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
            [cucumber.runner    :as cr]
            [cucumber.webdriver :as w]
            [tegere.loader      :as tl]
            [tegere.runner      :as tr]
            [tegere.steps       :as tsteps]))

;;; ---------------------------------------------------------------------------
;;; Report helpers (org rendering)
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

(defn fmt-duration [secs]
  (let [s (long secs) m (quot s 60) r (rem s 60)] (format "%dm %02ds" m r)))

(defn clean-reason [r]
  (-> (str r)
      (str/replace #"\s*\|?\s*(Build info:|System info:|Driver info:|Capabilities \{|Session ID:|For documentation on this error|\(Session info:).*$" "")
      (str/replace #"\s*\n\s*" " ")
      str/trim))

(defn- scenario-title [ex]
  (let [sc (:tegere.runner/scenario ex)]
    (or (:tegere.parser/description sc) (:tegere.parser/name sc) "(unnamed scenario)")))

(defn- step-err [step] (get-in step [:tegere.runner/execution :tegere.runner/err]))

(defn- ex-failure
  "nil if the executable passed, else {:scenario :type :step :reason}."
  [ex]
  (when-let [bad (some (fn [st] (when-let [e (step-err st)] [st e]))
                       (:tegere.parser/steps ex))]
    (let [[st e] bad
          msg    (or (not-empty (:tegere.runner/message e))
                     (first (:tegere.runner/stack-trace e))
                     (name (:tegere.runner/type e :fail)))
          stept  (str (some-> (:tegere.parser/type st) name str/capitalize) " "
                      (:tegere.parser/text st))]
      {:scenario (scenario-title ex)
       :type     (:tegere.runner/type e)
       :step     (str/trim stept)
       :reason   (str/replace (str/trim msg) #"\s*\n\s*" " | ")})))

(defn counts [all-files status executables]
  (let [state-of (fn [f] (:state (get status f)))]
    {:passed           (count (filter #(= :pass    (state-of %)) all-files))
     :failed           (count (filter #(= :fail    (state-of %)) all-files))
     :skipped          (count (filter #(= :skip    (state-of %)) all-files))
     :pending          (count (filter #(= :pending (state-of %)) all-files))
     :scenarios-passed (- (count executables) (count (filter :failure executables)))
     :scenarios-failed (count (filter :failure executables))
     :failed-files     (->> all-files (filter #(= :fail (state-of %))) vec)}))

(defn render-org
  [{:keys [url query headless stop elapsed all-files status executables]}]
  (let [{:keys [passed failed skipped pending scenarios-passed scenarios-failed]}
        (counts all-files status executables)
        sb                                                                        (StringBuilder.)]
    (.append sb "#+TITLE: Cucumber Test Results\n")
    (.append sb (str "#+DATE: " (java.time.LocalDate/now) "\n"))
    (.append sb (str "# Config: query " query ", :headless? " (boolean headless)
                     ", :stop " (boolean stop) "\n"))
    (.append sb (str "#         url " url "\n"))
    (.append sb (str "# Elapsed" (when (pos? pending) " so far") ": "
                     (fmt-duration elapsed) " (" (format "%.1f" elapsed) "s)\n"))
    (.append sb (str "# Files: " (count all-files) " total — " passed " passed, " failed " failed, "
                     skipped " skipped" (when (pos? pending) (str ", " pending " pending")) "\n"))
    (.append sb (str "# Scenarios: " scenarios-passed " passed, " scenarios-failed " failed\n\n"))
    (.append sb "* Results\n")
    (doseq [file all-files]
      (let [{:keys [state fails]} (get status file)]
        (case state
          :pass    (.append sb (str "- [X] " file "\n"))
          :skip    (.append sb (str "- [-] " file "  (SKIPPED: no matching scenario for query)\n"))
          :pending (.append sb (str "- [ ] " file "  :PENDING:\n"))
          :fail    (do (.append sb (str "- [ ] " file "\n"))
                       (doseq [f fails]
                         (.append sb (str "  - Scenario \"" (:scenario f) "\": "
                                          (str/upper-case (name (:type f)))
                                          " at step [" (:step f) "] — " (clean-reason (:reason f)) "\n")))))))
    (.toString sb)))

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
        (mapv (fn [ex] {:feature fname :scenario (scenario-title ex) :failure (ex-failure ex)})
              (:tegere.runner/executables results)))
      (catch Throwable t
        [{:feature fname                                                     :scenario "(feature errored)"
          :failure {:scenario "(feature errored)"   :type :error :step "run"
                    :reason   (str (.getMessage t))}}]))))

(defn -main [config-path]
  (let [{:keys [features-dir steps-dir url headless stop query org edn retry-failed browser-path]}
        (edn/read-string (slurp config-path))
        query                                                                                      (if (string? query) (read-string query) query)
        all-files                                                                                  (all-feature-files features-dir)
        n->f                                                                                       (feature-name->file features-dir)
        status                                                                                     (atom (into {} (map (fn [f] [f {:state :pending :fails []}]) all-files)))
        executables                                                                                (atom [])
        t0                                                                                         (System/nanoTime)
        elapsed                                                                                    (fn [] (/ (- (System/nanoTime) t0) 1e9))
        write!                                                                                     (fn []
                                                                                                     (spit org (render-org {:url     url       :query       query        :headless headless :stop stop
                                                                                                                            :elapsed (elapsed) :all-files   all-files
                                                                                                                            :status  @status   :executables @executables}))
                                                                                                     (spit edn (pr-str {:elapsed-seconds (elapsed) :executables @executables})))
        record!                                                                                    (fn [file fname exs]
                      ;; replace any prior executables for this feature, then re-add
                                                                                                     (swap! executables (fn [all] (into (vec (remove #(= fname (:feature %)) all)) exs)))
                                                                                                     (swap! status assoc file (classify exs))
                                                                                                     (write!))]
    (println (format "Loading steps from %s" steps-dir))
    (cr/load-steps! (io/file steps-dir))
    (write!)                                   ; initial all-pending org
    (let [features   (tl/load-feature-files (io/file features-dir))
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
    (let [summary (counts all-files @status @executables)]
      (spit org (render-org {:url     url       :query       query        :headless headless :stop stop
                             :elapsed (elapsed) :all-files   all-files
                             :status  @status   :executables @executables}))
      (spit edn (pr-str {:elapsed-seconds (elapsed) :summary summary :executables @executables}))
      (println (format "\nDONE in %s — Files: %d passed, %d failed, %d skipped | Scenarios: %d passed, %d failed"
                       (fmt-duration (elapsed)) (:passed summary) (:failed summary) (:skipped summary)
                       (:scenarios-passed summary) (:scenarios-failed summary))))
    (shutdown-agents)))

(apply -main *command-line-args*)
