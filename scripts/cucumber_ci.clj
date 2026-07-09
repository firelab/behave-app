#!/usr/bin/env bb
;; Self-contained headless cucumber run.
;;
;; Starts the BehavePlus app (figwheel serve, compile-dev), waits until it is
;; reachable, runs the existing cucumber suite headless (scripts/run_cucumber_tests.clj)
;; against a Selenium-driven Chrome resolved via scripts/browser.clj, then tears the
;; figwheel process down — win or lose.
;;
;; Reuses:
;;   - browser/find-browser        (Chrome path, honors $CHROME_BIN)  → Selenium :browser-path
;;   - the test:ci figwheel-serve pattern (projects/behave/bb.edn)    → app on :8081
;;   - scripts/run_cucumber_tests.clj                                 → all reporting
;;
;; Prerequisite: Selenium `chromedriver` on PATH (or $CHROMEDRIVER_PATH), whose major
;; version matches the installed Chrome.
;;
;; Usage:
;;   bb cucumber:ci [opts]        (or: bb scripts/cucumber_ci.clj [opts])
;;   opts are forwarded to run_cucumber_tests.clj (e.g. --query, --features-dir,
;;   --steps-dir, --retry-failed, --stop, --url); --headless and --browser-path are
;;   supplied automatically.
(ns cucumber-ci
  (:require [babashka.fs      :as fs]
            [babashka.process :as p]
            [browser          :as browser]
            [clojure.string   :as str]))

(def figwheel-log "cucumber_figwheel.log")
(def default-url "http://localhost:8081/worksheets")
(def serve-timeout-secs 180)

;; Seeded app DB (repo-root-relative). figwheel serves with :dir "projects/behave",
;; so the store path is rebased to an absolute path (see init-form).
(def default-db-path "projects/behave/resources/db.sqlite")

;; The app hardcodes the resource name "config.edn" in TWO places — behave.server/
;; init-config! AND the /api/init handler behave.init/init! — but config.edn is
;; gitignored (dev-local), so it's absent on a fresh checkout / CI. ensure-config!
;; provisions it from this tracked template when missing (never clobbers a dev's own).
(def app-config    "projects/behave/resources/config.edn")
(def ci-config-src "projects/behave/resources/config.ci.edn")

;;; ---------------------------------------------------------------------------
;;; Arg helpers (forward user opts to run_cucumber_tests.clj)
;;; ---------------------------------------------------------------------------

(defn flag-val
  "Value of `flag` in args (`--flag v` or `--flag=v`), else `default`."
  [args flag default]
  (loop [in (seq args)]
    (cond
      (empty? in)                                        default
      (= (first in) flag)                                (second in)
      (str/starts-with? (str (first in)) (str flag "=")) (subs (first in) (inc (count flag)))
      :else                                              (recur (rest in)))))

(defn strip-flag
  "Remove `flag` (and its value in the `--flag v` form) from args."
  [args flag]
  (loop [in (seq args) out []]
    (if (empty? in)
      out
      (let [a (first in)]
        (cond
          (= a flag)                                        (recur (drop 2 in) out) ; drop flag + value
          (str/starts-with? (str a) (str flag "="))         (recur (rest in) out)   ; drop --flag=v
          :else                                             (recur (rest in) (conj out a)))))))

(defn url->port
  "Port from an http(s) URL, defaulting to 8081."
  [url]
  (or (some-> (re-find #"://[^/:]+:(\d+)" (str url)) second) "8081"))

;;; ---------------------------------------------------------------------------
;;; App server init (runs in the figwheel JVM, before it serves)
;;; ---------------------------------------------------------------------------

(defn ensure-config!
  "Guarantee a `config.edn` resource exists (the app hardcodes that name in
   init-config! and the /api/init handler). If it's missing — fresh checkout / CI —
   copy the tracked config.ci.edn into place. Never overwrites an existing config.edn
   (a dev's real one). Runs before figwheel starts so the file is on the classpath."
  []
  (when-not (fs/exists? app-config)
    (fs/copy ci-config-src app-config)
    (println (format "cucumber:ci: provisioned %s from %s (was absent)" app-config ci-config-src))))

(defn init-form
  "Clojure source (as a string) run via `clojure.main -e` in the figwheel JVM before
   figwheel serves. figwheel only starts the ring server; it never runs the app's
   server-side init, so DB-backed endpoints (e.g. /api/init) fail without this. Mirrors
   the manual dev step (init-config! + init-db!); vms-sync! is intentionally skipped
   (remote, network-dependent — the app serves the pre-exported layout.msgpack).
   config.edn is guaranteed present by ensure-config!. The store path is overridden to
   an absolute `db-path` because figwheel runs with :dir \"projects/behave\", where the
   config's repo-root-relative path misresolves."
  [db-path]
  (str "(do "
       "(require '[behave.server :as server] '[config.interface :refer [get-config]]) "
       "(println \"cucumber:ci: init-config! + init-db!\") "
       "(server/init-config!) "
       "(server/init-db! (assoc-in (get-config :database :config) [:store :path] "
       (pr-str db-path) ")))"))

;;; ---------------------------------------------------------------------------
;;; Readiness poll
;;; ---------------------------------------------------------------------------

(defn http-status [url]
  (try (str/trim (:out (p/shell {:out :string :err :string}
                                "curl" "-s" "-o" "/dev/null" "-w" "%{http_code}"
                                "--max-time" "5" url)))
       (catch Exception _ "000")))

(defn wait-until-ready!
  "Poll `url` until HTTP 200 or timeout. Fails fast if the figwheel process dies.
   Returns true on success; false on timeout/death."
  [url proc timeout-secs]
  (let [deadline (+ (System/currentTimeMillis) (* 1000 timeout-secs))]
    (loop []
      (cond
        (not (.isAlive ^Process (:proc proc)))
        (do (println "ERROR: figwheel exited before the app became reachable. See" figwheel-log) false)

        (= "200" (http-status url))
        (do (println (format "✓ App reachable at %s" url)) true)

        (> (System/currentTimeMillis) deadline)
        (do (println (format "ERROR: app not reachable at %s within %ds. See %s" url timeout-secs figwheel-log)) false)

        :else
        (do (print ".") (flush) (Thread/sleep 2000) (recur))))))

;;; ---------------------------------------------------------------------------
;;; Main
;;; ---------------------------------------------------------------------------

(defn -main [args]
  (let [chrome   (browser/find-browser)
        url      (flag-val args "--url" default-url)
        port     (url->port url)
        ;; Absolute so it resolves regardless of figwheel's :dir (projects/behave).
        db-path  (str (fs/absolutize (flag-val args "--db-path" default-db-path)))
        ;; Seconds to wait for the app to become reachable (cold CI compile can be slow).
        timeout  (Integer/parseInt (str (flag-val args "--serve-timeout" (str serve-timeout-secs))))
        fwo      (pr-str {:open-url false :ring-server-options {:port (Integer/parseInt port) :join? false}})
        ;; forward the user's opts, but drop our internal ones + force headless/Chrome
        fwd-args (-> (vec args)
                     (strip-flag "--db-path")
                     (strip-flag "--serve-timeout")
                     (strip-flag "--headless")
                     (strip-flag "--browser-path")
                     (into ["--headless" "true" "--browser-path" chrome]))]
    (println (format "▶ cucumber:ci — Chrome: %s | serve :%s | DB: %s | figwheel log: %s"
                     chrome port db-path figwheel-log))
    (ensure-config!)
    (spit figwheel-log "")
    ;; Compute the exit code inside the try, tear figwheel down in the finally,
    ;; then exit AFTER — System/exit skips finally blocks, so it must come last.
    (let [logf (fs/file figwheel-log)
          proc (p/process {:dir "projects/behave" :out :append :out-file logf :err :append :err-file logf}
                          "clojure" "-M:figwheel-lib"
                          "-e" (init-form db-path)         ; init-config! + init-db! in the figwheel JVM
                          "-m" "figwheel.main"
                          "-fwo" fwo "-b" "compile-dev" "-s")
          code (try
                 (println (format "Starting app (figwheel serve)… (tail -f %s)" figwheel-log))
                 (if-not (wait-until-ready! url proc timeout)
                   1
                   (:exit (apply p/shell {:continue true}
                                 "bb" "scripts/run_cucumber_tests.clj" fwd-args)))
                 (finally
                   (println "\nStopping app (figwheel)…")
                   (p/destroy-tree proc)
                   (try @proc (catch Exception _ nil))))]
      (System/exit (or code 0)))))

(-main *command-line-args*)
