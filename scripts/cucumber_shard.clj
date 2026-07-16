#!/usr/bin/env bb
;; Parallel, sharded cucumber run.
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
;; Usage:
;;   bb cucumber:shard [opts]   (or: bb scripts/cucumber_shard.clj [opts])
;;   --shards N          number of parallel shards           (default 2)
;;   --query <edn>       tegere query-tree                   (default core, not extended)
;;   --features-dir D    features root                       (default features)
;;   --serve-timeout S   secs to wait per server             (default 300)
;;   --db-prefix P       per-shard sqlite path prefix        (default cucumber-shard-db)
;;   --base-port N       first shard's port (i uses N+i)     (default 8091)
;;   --skip-compile      reuse the existing compiled build (skip step 1)
(ns cucumber-shard
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

(defn merge-summaries [summaries]
  (reduce (fn [acc s]
            (-> acc
                (update :passed           + (:passed s 0))
                (update :failed           + (:failed s 0))
                (update :skipped          + (:skipped s 0))
                (update :scenarios-passed + (:scenarios-passed s 0))
                (update :scenarios-failed + (:scenarios-failed s 0))
                (update :failed-files     into (:failed-files s []))))
          {:passed 0 :failed 0 :skipped 0 :scenarios-passed 0 :scenarios-failed 0 :failed-files []}
          summaries))

;;; ---------------------------------------------------------------------------
;;; Main
;;; ---------------------------------------------------------------------------

(defn -main [args]
  (let [shards       (Integer/parseInt (str (flag-val args "--shards" (str default-shards))))
        query        (flag-val args "--query" "(and \"core\" (not \"extended\"))")
        features-dir (flag-val args "--features-dir" "features")
        timeout      (Integer/parseInt (str (flag-val args "--serve-timeout" (str default-serve-timeout))))
        db-prefix    (flag-val args "--db-prefix" "cucumber-shard-db")
        base-port    (Integer/parseInt (str (flag-val args "--base-port" (str default-base-port))))
        skip-compile (flag-set? args "--skip-compile")
        chrome       (browser/find-browser)
        all-files    (feature-files features-dir)
        n            (max 1 (min shards (count all-files)))
        groups       (shard-split all-files n)]
    (println (format "▶ cucumber:shard — %d shard(s) | %d feature files | Chrome: %s"
                     n (count all-files) chrome))
    (when (empty? all-files)
      (println "ERROR: no .feature files under" features-dir) (System/exit 1))
    (if skip-compile
      (println "• --skip-compile: reusing existing build")
      (compile-once!))
    (ensure-manifest!)

    (let [servers                                                                                                                            (atom [])
          ;; Compute the exit code inside the try so the finally can tear the servers down
          ;; FIRST — System/exit skips finally blocks, so it must come after (see cucumber_ci.clj).
          code
          (try
            ;; 1. start N isolated servers
            (doseq [i (range n)]
              (let [port (+ base-port i)
                    db   (str (fs/absolutize (str db-prefix "-" i ".sqlite")))
                    logf (io/file (format "cucumber_shard_%d_server.log" i))]
                (fs/delete-if-exists db)
                (spit logf "")
                (let [proc (p/process {:out :append :out-file logf :err :append :err-file logf}
                                      "clojure" (str "-J-Dbehave.store.path=" db) (str "-J-Dshard.port=" port)
                                      "-M:dev:behave/app" "scripts/shard_server.clj")]
                  (swap! servers conj {:proc proc :port port :i i :log logf}))))
        ;; wait for all to be reachable
            (let [ready? (every? (fn [{:keys [proc port i]}]
                                   (wait-ready! (format "http://localhost:%d/worksheets" port)
                                                proc timeout (format "shard-%d server" i)))
                                 @servers)]
              (when-not ready?
                (println "ERROR: not all shard servers came up; see cucumber_shard_*_server.log")
                (throw (ex-info "server startup failed" {}))))

        ;; 2. run N drivers in parallel, each on its shard's features
            (let [driver-procs
                  (doall
                   (for [{:keys [port i]} @servers]
                     (let [subset (nth groups i)
                           org    (format "cucumber_shard_%d_results.org" i)
                           edn    (format "cucumber_shard_%d_results.edn" i)
                           cfg    {:features-dir features-dir                                   :steps-dir     steps-dir
                                   :url          (format "http://localhost:%d/worksheets" port)
                                   :headless     true                                           :stop          false     :query        query
                                   :org          org                                            :edn           edn       :retry-failed 0
                                   :browser-path chrome                                         :feature-files subset}
                           cfg-f  (str (fs/create-temp-file {:prefix (format "shard-%d-cfg-" i) :suffix ".edn"}))
                           logf   (io/file (format "cucumber_shard_%d_run.log" i))]
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
                (let [combined (format "cucumber_test_results_shard_%s.org"
                                       (.format (java.time.LocalDateTime/now)
                                                (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd_HH-mm-ss")))]
                  (spit combined
                        (str "# Sharded cucumber run — " n " shards\n\n"
                             (str/join "\n\n" (for [{:keys [i org]} driver-procs]
                                                (str "# ── shard-" i " ──\n"
                                                     (when (fs/exists? org) (slurp org)))))))
                  (println "  Combined org:" combined))
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
              (doseq [{:keys [proc port]} @servers]
                (p/destroy-tree proc)
                ;; destroy-tree kills the `clojure` wrapper, but it forks a java child that
                ;; re-parents and outlives it — SIGKILL anything still holding this shard's
                ;; unique -Dshard.port marker so no JVM leaks between runs.
                (try (p/shell {:out :string :err :string :continue true}
                              "pkill" "-9" "-f" (str "Dshard.port=" port))
                     (catch Exception _ nil))
                (try @proc (catch Exception _ nil)))))]
      (System/exit code))))

(-main *command-line-args*)
