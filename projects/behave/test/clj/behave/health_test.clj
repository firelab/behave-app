(ns behave.health-test
  (:require [behave.health       :as h]
            [behave.store        :as store]
            [behave.views        :refer [resource]]
            [clojure.edn         :as edn]
            [clojure.java.io     :as io]
            [clojure.test        :refer [deftest is testing use-fixtures]]
            [config.interface    :refer [load-config]]
            [datom-store.main    :as s]
            [health.core         :as hc]
            [me.raynes.fs        :as fs]
            [transport.interface :refer [json->clj]])
  (:import (java.io File RandomAccessFile)))

;;; Fixtures

(def ^:dynamic *db-path*
  "Temp worksheet DB path."
  nil)

(def ^:dynamic *tmp-dir*
  "Isolated java.io.tmpdir."
  nil)

(def ^:private real-required-assets @#'h/required-assets)

(def ^:private test-assets
  "Git-tracked assets; no CLJS build needed."
  ["public/layout.msgpack" "public/js/behave-min.wasm"])

(defn- write-config!
  "Writes a config.edn using `db-path`."
  [dir db-path]
  (doto (io/file dir "config.edn")
    (spit (pr-str {:database {:config {:store {:backend :file :path db-path}}}}))))

(defn- with-temp-store
  "Fresh temp DB per test."
  [f]
  (let [dir     (fs/temp-dir "behave-health")
        db-path (str (io/file dir "db.sqlite"))
        tmp-dir (doto (io/file dir "tmp") (.mkdirs))
        prop    (System/clearProperty "behave.store.path")]
    (load-config (write-config! dir db-path))
    (store/connect! (store/db-config))
    (try
      (binding [*db-path* db-path
                *tmp-dir* tmp-dir]
        (with-redefs [h/temp-dir        (constantly tmp-dir)
                      h/required-assets (constantly test-assets)]
          (f)))
      (finally
        (s/release-conn!)
        (when prop (System/setProperty "behave.store.path" prop))
        (fs/delete-dir dir)))))

(use-fixtures :each with-temp-store)

;;; Helpers

(defn- get-health
  "Handler response, JSON body parsed."
  []
  (update (h/health-handler {}) :body json->clj))

(defn- report
  "Parsed report body."
  []
  (:body (get-health)))

(defn- check
  "First result for check `k`."
  [body k]
  (first (get-in body [:checks k])))

(defmacro ^:private with-disk-pct
  "Every disk reports `pct` % used."
  [pct & body]
  `(with-redefs [hc/used-pct (constantly ~pct)] ~@body))

;;; Response shape

(deftest healthy-response-test
  (with-disk-pct 10.0
    (let [{:keys [status headers body]} (get-health)]
      (testing "200 with health+json, never cached"
        (is (= 200 status))
        (is (= "application/health+json" (get headers "Content-Type")))
        (is (= "no-cache, no-store" (get headers "Cache-Control"))))
      (testing "overall pass and one entry per component"
        (is (= "pass" (:status body)))
        (is (= #{:disk:utilization :sqlite:responseTime :heap:utilization
                 :tempWorksheets:size :assets:availability :uptime}
               (set (keys (:checks body)))))
        (is (= ["database" "tmp"] (map :componentId (get-in body [:checks :disk:utilization]))))))
    (testing "every result is timestamped"
      (is (every? :time (mapcat val (:checks (report))))))))

(deftest routed-test
  (testing "/health is wired into the server handler stack"
    (with-disk-pct 10.0
      ;; Lazy: loading behave.handlers reads config.edn.
      (let [app ((requiring-resolve 'behave.handlers/server-handler-stack) {})
            res (app {:uri "/health" :request-method :get :headers {}})]
        (is (= 200 (:status res)))
        (is (= "pass" (:status (json->clj (:body res)))))))))

;;; Disk

(deftest disk-thresholds-test
  (testing "below 90% passes"
    (with-disk-pct 89.9
      (is (= "pass" (:status (check (report) :disk:utilization))))))
  (testing "90% warns but still answers 200"
    (with-disk-pct 90.0
      (let [{:keys [status body]} (get-health)]
        (is (= 200 status))
        (is (= "warn" (:status body)))
        (is (= 90.0 (:observedValue (check body :disk:utilization)))))))
  (testing "95% fails with 503"
    (with-disk-pct 95.0
      (let [{:keys [status body]} (get-health)]
        (is (= 503 status))
        (is (= "fail" (:status body)))))))

(deftest disk-write-failure-test
  (testing "a disk that can't be written fails even when % used looks fine"
    (with-disk-pct 10.0
      (with-redefs [hc/write-probe! (fn [_] (throw (java.io.IOException. "No space left on device")))]
        (let [{:keys [status body]} (get-health)
              db                    (check body :disk:utilization)]
          (is (= 503 status))
          (is (= "fail" (:status db)))
          (is (= "database" (:componentId db)) "failed checks keep their identity")
          (is (= "No space left on device" (:output db))))))))

(deftest write-probe-cleans-up-test
  (let [dir (.getParentFile (io/file *db-path*))]
    (with-disk-pct 10.0 (report))
    (is (empty? (filter #(.endsWith (str %) ".probe") (.listFiles ^File dir))))))

;;; Database

(deftest sqlite-check-test
  (with-disk-pct 10.0
    (testing "a live connection reports response time in ms"
      (let [db (check (report) :sqlite:responseTime)]
        (is (= "pass" (:status db)))
        (is (= "ms" (:observedUnit db)))
        (is (nat-int? (:observedValue db)))))
    (testing "no DataScript connection fails"
      (s/release-conn!)
      (let [{:keys [status body]} (get-health)]
        (is (= 503 status))
        (is (= "DataScript connection not initialized"
               (:output (check body :sqlite:responseTime))))))))

(deftest missing-db-file-test
  (with-disk-pct 10.0
    (fs/delete *db-path*)
    (let [db (check (report) :sqlite:responseTime)]
      (is (= "fail" (:status db)))
      (is (= "Worksheet database file not found" (:output db))))
    (is (not (fs/exists? *db-path*)) "the probe must not create an empty database")))

(deftest db-config-property-override-test
  (testing "-Dbehave.store.path wins over config.edn, as /api/init expects"
    (let [other "/tmp/behave-health/other.sqlite"]
      (System/setProperty "behave.store.path" other)
      (try
        (is (= other (store/db-file)))
        (finally (System/clearProperty "behave.store.path"))))))

;;; Memory

(deftest heap-check-test
  (with-disk-pct 10.0
    (let [heap (check (report) :heap:utilization)]
      (is (= "jvm" (:componentId heap)))
      (is (= "percent" (:observedUnit heap)))
      (is (<= 0.0 (:observedValue heap) 100.0))
      (is (#{"pass" "warn"} (:status heap)) "heap never fails on its own"))))

;;; Temp worksheets

(defn- sized-file!
  "Sparse `mb` MB file in the temp dir."
  [filename mb]
  (with-open [f (RandomAccessFile. (io/file *tmp-dir* filename) "rw")]
    (.setLength f (* mb 1024 1024))))

(deftest temp-worksheets-test
  (with-disk-pct 10.0
    (testing "an empty temp dir reports zero"
      (let [tmp (check (report) :tempWorksheets:size)]
        (is (= ["pass" 0.0 "MB" 0] ((juxt :status :observedValue :observedUnit :fileCount) tmp)))))
    (sized-file! "a.bp7" 2)
    (sized-file! "behave-db123.bp7" 1)
    (sized-file! "unrelated.log" 5)
    (testing "only .bp7 files are counted"
      (let [tmp (check (report) :tempWorksheets:size)]
        (is (= 3.0 (:observedValue tmp)))
        (is (= 2 (:fileCount tmp)))))
    (testing "over the limit warns, but never fails on its own"
      (with-redefs [h/TEMP-WORKSHEETS-WARN-MB 3]
        (let [{:keys [status body]} (get-health)]
          (is (= 200 status))
          (is (= "warn" (:status (check body :tempWorksheets:size)))))))))

;;; Assets

(deftest assets-test
  (with-disk-pct 10.0
    (testing "present assets pass, one entry per file"
      (let [assets (get-in (report) [:checks :assets:availability])]
        (is (= test-assets (map :componentId assets)))
        (is (every? #(= "pass" (:status %)) assets))))
    (testing "a missing asset fails the whole report"
      (with-redefs [h/required-assets (constantly ["public/layout.msgpack" "public/nope.js"])]
        (let [{:keys [status body]} (get-health)
              missing               (second (get-in body [:checks :assets:availability]))]
          (is (= 503 status))
          (is (= ["public/nope.js" "fail" "missing"]
                 ((juxt :componentId :status :output) missing))))))))

(deftest required-assets-test
  (testing "includes the boot files and the bundle the page actually loads"
    (let [assets (real-required-assets)]
      (is (every? (set assets) ["public/layout.msgpack"
                                "public/js/behave-min.js"
                                "public/js/behave-min.wasm"]))
      (is (some #(re-matches #"public/cljs/.+\.js" %) assets)))))

;;; Version and uptime

(deftest version-test
  (is (= (-> (resource "version.edn") slurp edn/read-string :version)
         (:version (report)))))

(deftest uptime-test
  (let [up (check (report) :uptime)]
    (is (= "s" (:observedUnit up)))
    (is (nat-int? (:observedValue up)))))
