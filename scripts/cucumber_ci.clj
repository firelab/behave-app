#!/usr/bin/env bb
;; Headless CI cucumber run (optionally sharded in parallel).
;;
;; The BehavePlus app is single-session (behave.init/init! reloads config.edn and, in cucumber
;; config, deletes+reconnects a single global store on every /api/init), so N browsers cannot
;; share one server. This orchestrator instead:
;;   1. compiles the CLJS once (advanced build via `clojure -M:compile-cljs`) — fast SPA boot,
;;   2. starts N isolated app servers (scripts/shard_server.clj), each on its own port + DB,
;;   3. splits the feature files round-robin across the shards,
;;   4. runs N cucumber drivers (scripts/cucumber_run_driver.clj) in parallel, one per shard,
;;      each with its feature subset (`:feature-files`) and its own Chrome,
;;   5. merges the per-shard reports and tears every server down.
;;
;; Sharding is a flag feature: --shards defaults to 2, so `--shards 1` runs a single
;; unsharded server + driver.
;;
;; Usage:
;;   bb cucumber:ci [opts]   (or: bb scripts/cucumber_ci.clj [opts])
;;   --shards N          number of parallel shards           (default 2 with --headless;
;;                       forced to 1 in the default visible mode unless set)
;;   --feature F         run only this one feature file      (rel path, nested path, or bare
;;                       (forces 1 shard; runs all its scenarios unless --query given)
;;   --query <edn>       tegere query-tree                   (default core, not extended)
;;   --features-dir D    features root                       (default features)
;;   --serve-timeout S   secs to wait per server             (default 300)
;;   --db-prefix P       per-shard sqlite path prefix        (default cucumber-shard-db)
;;   --base-port N       first shard's port (i uses N+i)     (default 8091)
;;   --skip-compile      reuse the existing compiled build (skip step 1)
;;   --headless          run headless (no visible browser); enables parallel sharding and
;;                       auto-closes on finish. DEFAULT is a visible browser: 1 shard, and
;;                       browser + server stay open until Ctrl-C. Visible mode needs a
;;                       display (WSLg on WSL2).
;;   --stop              halt at the first failing scenario; forces 1 shard unless
;;                       --shards given
(ns cucumber-ci
  (:require [babashka.fs      :as fs]
            [babashka.process :as p]
            [browser          :as browser]
            [clojure.edn      :as edn]
            [clojure.java.io  :as io]
            [clojure.string   :as str]))

(def default-shards        2)
(def default-serve-timeout 300)
(def default-base-port     8091)
(def steps-dir             "steps")
(def cljs-dir              "projects/behave/resources/public/cljs")

;; config.edn is gitignored (dev-local), so it's absent on a fresh checkout / CI. Each shard
;; server loads it via behave.server/init-config!, so it must exist before the JVMs boot.
(def app-config    "projects/behave/resources/config.edn")
(def ci-config-src "projects/behave/resources/config.ci.edn")

;;; ---------------------------------------------------------------------------
;;; Arg helpers
;;; ---------------------------------------------------------------------------

(defn flag-val [args flag default]
  (loop [in (seq args)]
    (cond
      (empty? in)                                        default
      (= (first in) flag)                                (second in)
      (str/starts-with? (str (first in)) (str flag "=")) (subs (first in) (inc (count flag)))
      :else                                              (recur (rest in)))))

(defn flag-set? [args flag] (boolean (some #(= % flag) args)))

;;; ---------------------------------------------------------------------------
;;; Build
;;; ---------------------------------------------------------------------------

(defn compile-once! []
  (println "▶ compiling advanced CLJS build (clojure -M:compile-cljs)…")
  (let [{:keys [exit]} (p/shell {:dir "projects/behave" :continue true} "clojure" "-M:compile-cljs")]
    (when-not (zero? exit)
      (println "ERROR: CLJS compile failed") (System/exit 1))))

(defn ensure-config! []
  ;; Provision config.edn from the tracked config.ci.edn when absent (fresh checkout / CI).
  ;; Never clobbers a dev's real config.
  (when-not (fs/exists? app-config)
    (fs/copy ci-config-src app-config)
    (println (format "cucumber:ci: provisioned %s from %s (was absent)" app-config ci-config-src))))

(defn ensure-manifest! []
  ;; The advanced build is fingerprinted (app-<hash>.js) but the -M:compile-cljs path writes no
  ;; manifest.edn, so behave.views/find-app-js would fall back to a non-existent /cljs/app.js.
  ;; Map the logical name to the built file so the served HTML references the right script.
  (let [fp (->> (fs/glob cljs-dir "app-*.js")
                (map str)
                (remove #(str/ends-with? % ".map"))
                sort first)]
    (if fp
      (let [base (fs/file-name fp)]
        (spit (str cljs-dir "/manifest.edn")
              (pr-str {"resources/public/cljs/app.js" (str "resources/public/cljs/" base)}))
        (println "✓ manifest.edn →" base))
      (when-not (fs/exists? (str cljs-dir "/app.js"))
        (println "ERROR: no compiled app.js in" cljs-dir "— run without --skip-compile")
        (System/exit 1)))))

;;; ---------------------------------------------------------------------------
;;; Feature discovery + sharding (mirrors cucumber_run_driver/all-feature-files)
;;; ---------------------------------------------------------------------------

(defn feature-files [features-dir]
  (->> (file-seq (io/file features-dir))
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".feature"))
       (map #(str/replace (.getPath %) (str features-dir "/") ""))
       sort vec))

(defn resolve-feature
  "Resolve a user-supplied --feature value to a features-dir-relative path from
   `discovered`. Accepts an exact relative path, a features-dir-prefixed path, a
   nested-path suffix, or a bare filename. Returns nil when nothing matches."
  [input discovered features-dir]
  (let [input  (str/replace (str input) #"^\./" "")
        prefix (str features-dir "/")
        norm   (if (str/starts-with? input prefix) (subs input (count prefix)) input)
        base   (str (fs/file-name norm))]
    (or (some #{norm} discovered)                                      ; exact relative path
        (first (filter #(str/ends-with? % (str "/" norm)) discovered)) ; nested-path suffix
        (first (filter #(= (str (fs/file-name %)) base) discovered))))) ; bare filename

(defn shard-split [files n]
  ;; Round-robin so heavy (results-page / extended) files spread evenly across shards.
  (->> (map-indexed vector files)
       (group-by #(mod (first %) n))
       (into (sorted-map))
       vals
       (mapv #(mapv second %))))

;;; ---------------------------------------------------------------------------
;;; Readiness poll
;;; ---------------------------------------------------------------------------

(defn http-status [url]
  (try (str/trim (:out (p/shell {:out :string :err :string}
                                "curl" "-s" "-o" "/dev/null" "-w" "%{http_code}"
                                "--max-time" "5" url)))
       (catch Exception _ "000")))

(defn wait-ready! [url proc timeout-secs label]
  (let [deadline (+ (System/currentTimeMillis) (* 1000 timeout-secs))]
    (loop []
      (cond
        (not (.isAlive ^Process (:proc proc)))
        (do (println (format "ERROR: %s exited before becoming reachable" label)) false)
        (= "200" (http-status url))
        (do (println (format "✓ %s reachable at %s" label url)) true)
        (> (System/currentTimeMillis) deadline)
        (do (println (format "ERROR: %s not reachable within %ds" label timeout-secs)) false)
        :else (do (Thread/sleep 2000) (recur))))))

;;; ---------------------------------------------------------------------------
;;; Merge
;;; ---------------------------------------------------------------------------

(defn read-summary [edn-file]
  (when (fs/exists? edn-file)
    (try (:summary (edn/read-string (slurp edn-file))) (catch Exception _ nil))))

(defn read-elapsed [edn-file]
  (when (fs/exists? edn-file)
    (try (:elapsed-seconds (edn/read-string (slurp edn-file))) (catch Exception _ nil))))

(defn fmt-elapsed [secs]
  (if secs
    (let [s (long secs)] (format "%dm %02ds" (quot s 60) (rem s 60)))
    "?"))

(defn results-body
  "The file-list section of a shard's org, re-indented +2 spaces so it nests under
   a `- Results` bullet (file items → col 2, failure-detail lines → col 4)."
  [org]
  (when (fs/exists? org)
    (let [content (slurp org)
          marker  "* Results\n"
          idx     (str/index-of content marker)]
      (when idx
        (->> (str/split-lines (subs content (+ idx (count marker))))
             (remove str/blank?)
             (map #(str "  " %))
             (str/join "\n"))))))

(defn merge-summaries [summaries]
  (reduce (fn [acc s]
            (-> acc
                (update :passed           + (:passed s 0))
                (update :failed           + (:failed s 0))
                (update :skipped          + (:skipped s 0))
                (update :pending          + (:pending s 0))
                (update :scenarios-passed + (:scenarios-passed s 0))
                (update :scenarios-failed + (:scenarios-failed s 0))
                (update :failed-files     into (:failed-files s []))))
          {:passed 0 :failed 0 :skipped 0 :pending 0 :scenarios-passed 0 :scenarios-failed 0 :failed-files []}
          summaries))

;;; ---------------------------------------------------------------------------
;;; Main
;;; ---------------------------------------------------------------------------

(defn -main [args]
  (let [headless?     (flag-set? args "--headless")
        ;; Visible browser is the DEFAULT now; --headless opts into the headless/CI path.
        headed?       (not headless?)
        stop?         (flag-set? args "--stop")
        shards-given? (or (flag-set? args "--shards")
                          (boolean (some #(str/starts-with? (str %) "--shards=") args)))
        ;; Each shard opens its own maximized Chrome, and each shard stops at its OWN first
        ;; failure — so a visible or stop-on-failure run defaults to a single shard (one
        ;; window, one global stop point) unless the user asked for a specific --shards.
        shards        (if (and (or headed? stop?) (not shards-given?))
                        1
                        (Integer/parseInt (str (flag-val args "--shards" (str default-shards)))))
        features-dir  (flag-val args "--features-dir" "features")
        feature       (flag-val args "--feature" nil)
        query-given?  (or (flag-set? args "--query")
                          (boolean (some #(str/starts-with? (str %) "--query=") args)))
        ;; A single --feature run defaults to "all scenarios in that file" (query nil) so
        ;; the file runs regardless of its core/extended tags — override with --query.
        query         (let [q (flag-val args "--query" "(and \"core\" (not \"extended\"))")]
                        (if (and feature (not query-given?)) nil q))
        timeout       (Integer/parseInt (str (flag-val args "--serve-timeout" (str default-serve-timeout))))
        db-prefix     (flag-val args "--db-prefix" "cucumber-shard-db")
        base-port     (Integer/parseInt (str (flag-val args "--base-port" (str default-base-port))))
        skip-compile  (flag-set? args "--skip-compile")
        chrome        (browser/find-browser)
        discovered    (feature-files features-dir)
        all-files     (if feature
                        (if-let [f (resolve-feature feature discovered features-dir)]
                          [f]
                          (do (println (format "ERROR: no feature file matching '%s' under %s" feature features-dir))
                              (System/exit 1)))
                        discovered)
        n             (max 1 (min shards (count all-files)))
        groups        (shard-split all-files n)
        ts            (.format (java.time.LocalDateTime/now)
                               (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd_HH-mm-ss"))
        run-dir       (str "logs/cucumber/cucumber_test_results_" ts)]
    (fs/create-dirs run-dir)
    (println (format "▶ cucumber:ci — %d shard(s) | %d feature files | mode: %s | Chrome: %s"
                     n (count all-files) (if headed? "headed" "headless") chrome))
    (when feature
      (println (format "▶ single feature: %s | query: %s" (first all-files) (or query "all scenarios"))))
    (when stop?
      (println "▶ --stop: halting at first failure"))
    (when headed?
      (println "▶ visible browser: 1 shard; on failure the browser + server stay open for inspection (Ctrl-C to close), on success everything tears down (use --headless for a headless, sharded run)"))
    (println "▶ run outputs →" run-dir)
    (when (empty? all-files)
      (println "ERROR: no .feature files under" features-dir) (System/exit 1))
    (ensure-config!)                           ; provision config.edn before any server JVM boots
    (if skip-compile
      (println "• --skip-compile: reusing existing build")
      (compile-once!))
    (ensure-manifest!)

    (let [servers                                                                                                                            (atom [])
          ;; Compute the exit code inside the try so the finally can tear the servers down
          ;; FIRST — System/exit skips finally blocks, so it must come after.
          code
          (try
            ;; 1. start N isolated servers
            (doseq [i (range n)]
              (let [port (+ base-port i)
                    db   (str (fs/absolutize (str db-prefix "-" i ".sqlite")))
                    logf (io/file run-dir (format "cucumber_shard_%d_server.log" i))]
                (fs/delete-if-exists db)
                (spit logf "")
                (let [proc (p/process {:out :append :out-file logf :err :append :err-file logf}
                                      "clojure" (str "-J-Dbehave.store.path=" db) (str "-J-Dshard.port=" port)
                                      "-M:dev:behave/app" "scripts/shard_server.clj")]
                  (swap! servers conj {:proc proc :port port :i i :log logf :db db}))))
        ;; wait for all to be reachable
            (let [ready? (every? (fn [{:keys [proc port i]}]
                                   (wait-ready! (format "http://localhost:%d/worksheets" port)
                                                proc timeout (format "shard-%d server" i)))
                                 @servers)]
              (when-not ready?
                (println "ERROR: not all shard servers came up; see" (str run-dir "/cucumber_shard_*_server.log"))
                (throw (ex-info "server startup failed" {}))))

        ;; 2. run N drivers in parallel, each on its shard's features
            (let [driver-procs
                  (doall
                   (for [{:keys [port i]} @servers]
                     (let [subset (nth groups i)
                           org    (str run-dir "/" (format "cucumber_shard_%d_results.org" i))
                           edn    (str run-dir "/" (format "cucumber_shard_%d_results.edn" i))
                           cfg    {:features-dir features-dir                                   :steps-dir     steps-dir
                                   :url          (format "http://localhost:%d/worksheets" port)
                                   :headless     headless?                                      :stop          stop?     :query        query
                                   :org          org                                            :edn           edn       :retry-failed 0
                                   :browser-path chrome                                         :feature-files subset
                                   :keep-open    headed?}
                           cfg-f  (str (fs/create-temp-file {:prefix (format "shard-%d-cfg-" i) :suffix ".edn"}))
                           logf   (io/file run-dir (format "cucumber_shard_%d_run.log" i))]
                       (spit cfg-f (pr-str cfg))
                       (spit logf "")
                       (println (format "▶ shard-%d: %d feature(s) → %s (log: %s)" i (count subset) org logf))
                       {:i    i                                                                                 :edn edn :org org
                        :proc (p/process {:out :append :out-file logf :err :append :err-file logf}
                                         "clojure" "-M:dev:behave/cms" "scripts/cucumber_run_driver.clj" cfg-f)})))]
          ;; wait for all shards
              (doseq [{:keys [proc]} driver-procs] @proc)

          ;; 3. merge + report
              (let [summaries (mapv (comp read-summary :edn) driver-procs)
                    merged    (merge-summaries (keep identity summaries))
                    missing   (keep-indexed (fn [idx s] (when (nil? s) idx)) summaries)]
                (println "\n==== SHARDED RUN COMPLETE ====")
                (doseq [{:keys [i edn org]} driver-procs]
                  (let [s (read-summary edn)]
                    (println (format "  shard-%d: %s  (org: %s)" i
                                     (if s (format "%d passed, %d failed, %d skipped | scenarios %d/%d pass/fail"
                                                   (:passed s) (:failed s) (:skipped s)
                                                   (:scenarios-passed s) (:scenarios-failed s))
                                         "NO SUMMARY (crashed?)")
                                     org))))
                (println (format "  TOTAL: %d files passed, %d failed, %d skipped | Scenarios: %d passed, %d failed"
                                 (:passed merged) (:failed merged) (:skipped merged)
                                 (:scenarios-passed merged) (:scenarios-failed merged)))
                (when (seq (:failed-files merged))
                  (println "  Failing files:") (doseq [f (:failed-files merged)] (println "    -" f)))
            ;; combined org (concatenate the per-shard bodies)
                (let [combined    (str run-dir "/cucumber_test_summary.org")
                      max-elapsed (apply max 0.0 (keep read-elapsed (map :edn driver-procs)))
                      total-files (+ (:passed merged) (:failed merged) (:skipped merged) (:pending merged))]
                  (spit combined
                        (str "#+TITLE: Cucumber Test Summary\n"
                             "# Sharded cucumber run — " n " shards\n\n"
                             "* Summary\n"
                             (format "- Feature files: %d passed, %d failed, %d skipped%s (%d total)\n"
                                     (:passed merged) (:failed merged) (:skipped merged)
                                     (if (pos? (:pending merged)) (format ", %d pending" (:pending merged)) "")
                                     total-files)
                             (format "- Scenarios: %d passed, %d failed\n"
                                     (:scenarios-passed merged) (:scenarios-failed merged))
                             (format "- Max shard runtime: %s\n\n" (fmt-elapsed max-elapsed))
                             (str/join "\n"
                                       (for [{:keys [i org edn]} driver-procs]
                                         (let [s       (read-summary edn)
                                               elapsed (read-elapsed edn)
                                               summary (if s
                                                         (format " — %d passed, %d failed, %d skipped | scenarios %d/%d pass/fail | %s"
                                                                 (:passed s) (:failed s) (:skipped s)
                                                                 (:scenarios-passed s) (:scenarios-failed s) (fmt-elapsed elapsed))
                                                         " — NO SUMMARY")]
                                           (str "* shard-" i summary "\n"
                                                "- Results\n"
                                                (results-body org) "\n"))))))
                  (println "  Combined org:" combined))
            ;; per-shard EDNs are intermediate — their summaries are now folded into the
            ;; combined org above, so drop them once everything's been read.
                (doseq [{:keys [edn]} driver-procs]
                  (try (fs/delete-if-exists edn) (catch Exception _ nil)))
            ;; non-zero on any failure or missing summary — returned, exited after teardown
                (if (and (empty? missing)
                         (zero? (:failed merged))
                         (zero? (:scenarios-failed merged)))
                  0 1)))
            (catch Throwable t
              (println "ERROR: sharded run failed —" (.getMessage t))
              1)
            (finally
              (println "\nStopping shard servers…")
              (doseq [{:keys [proc port db]} @servers]
                (p/destroy-tree proc)
                ;; destroy-tree kills the `clojure` wrapper, but it forks a java child that
                ;; re-parents and outlives it — SIGKILL anything still holding this shard's
                ;; unique -Dshard.port marker so no JVM leaks between runs.
                (try (p/shell {:out :string :err :string :continue true}
                              "pkill" "-9" "-f" (str "Dshard.port=" port))
                     (catch Exception _ nil))
                (try @proc (catch Exception _ nil))
                ;; remove this shard's sqlite store (+ WAL/SHM/journal sidecars) now the
                ;; JVM holding it is dead — they're throwaway per-run scratch DBs.
                (doseq [suffix ["" "-wal" "-shm" "-journal"]]
                  (try (fs/delete-if-exists (str db suffix)) (catch Exception _ nil))))))]
      (System/exit code))))

(-main *command-line-args*)
