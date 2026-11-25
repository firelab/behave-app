(ns health.test
  (:require [clojure.data.json :as json]
            [clojure.java.io   :as io]
            [clojure.test      :refer [deftest is testing]]
            [health.core       :as c]
            [health.interface  :as health])
  (:import (java.io File IOException)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

;;; Helpers

(defn- tmp-dir ^File []
  (.toFile (Files/createTempDirectory "health-test" (make-array FileAttribute 0))))

(defn- delete-dir! [^File dir]
  (run! io/delete-file (reverse (file-seq dir))))

(defmacro ^:private with-disk-pct
  "Runs `body` with every disk reporting `pct` percent used."
  [pct & body]
  `(with-redefs [c/used-pct (constantly ~pct)] ~@body))

(defn- body [res] (json/read-str (:body res) :key-fn keyword))

;;; Disk

(deftest disk-thresholds-test
  (let [dir (tmp-dir)]
    (try
      (is (= "pass" (:status (with-disk-pct 89.9 (health/disk-check "d" dir)))))
      (is (= "warn" (:status (with-disk-pct 90.0 (health/disk-check "d" dir)))))
      (is (= "fail" (:status (with-disk-pct 95.0 (health/disk-check "d" dir)))))
      (is (= {:componentId "d" :componentType "system" :observedValue 42.0 :observedUnit "percent"}
             (-> (with-disk-pct 42.0 (health/disk-check "d" dir))
                 (select-keys [:componentId :componentType :observedValue :observedUnit]))))
      (finally (delete-dir! dir)))))

(deftest disk-write-failure-test
  (testing "a disk that can't be written fails even when % used looks fine"
    (with-disk-pct 10.0
      (with-redefs [c/write-probe! (fn [_] (throw (IOException. "No space left on device")))]
        (let [result (health/disk-check "database" (io/file (System/getProperty "java.io.tmpdir")))]
          (is (= "fail" (:status result)))
          (is (= "database" (:componentId result)) "failed checks keep their identity")
          (is (= "No space left on device" (:output result))))))))

(deftest write-probe-cleans-up-test
  (let [dir (tmp-dir)]
    (try
      (health/disk-check "d" dir)
      (is (empty? (.listFiles ^File dir)))
      (finally (delete-dir! dir)))))

;;; Memory and uptime

(deftest heap-check-test
  (let [heap (health/heap-check)]
    (is (= "jvm" (:componentId heap)))
    (is (= "percent" (:observedUnit heap)))
    (is (<= 0.0 (:observedValue heap) 100.0))
    (is (#{"pass" "warn"} (:status heap)) "heap never fails on its own")))

(deftest uptime-check-test
  (let [up (health/uptime-check)]
    (is (= ["pass" "s"] ((juxt :status :observedUnit) up)))
    (is (nat-int? (:observedValue up)))))

;;; Assets

(deftest asset-check-test
  (is (= "pass" (:status (health/asset-check io/resource "clojure/core.clj"))))
  (is (= ["nope.js" "fail" "missing"]
         ((juxt :componentId :status :output) (health/asset-check io/resource "nope.js")))))

;;; run-check

(deftest run-check-test
  (testing "results are timestamped and keep the component keys"
    (let [result (health/run-check {:componentId "x"} (constantly {:status "pass"}))]
      (is (= "x" (:componentId result)))
      (is (string? (:time result)))))
  (testing "exceptions become fail with their message, or class when there is none"
    (is (= "boom" (:output (health/run-check {} #(throw (ex-info "boom" {}))))))
    (is (= "class java.lang.NullPointerException"
           (:output (health/run-check {} #(throw (NullPointerException.))))))))

;;; Report and response

(deftest worst-status-test
  (is (= "pass" (c/worst-status [])))
  (is (= "pass" (c/worst-status ["pass" "pass"])))
  (is (= "warn" (c/worst-status ["pass" "warn" "pass"])))
  (is (= "fail" (c/worst-status ["warn" "fail" "pass"]))))

(deftest report-test
  (let [checks {"a:b" [{:status "pass"} {:status "warn"}]}]
    (is (= {:status "warn" :checks checks :version "1.2"} (health/report checks "1.2")))
    (is (not (contains? (health/report checks nil) :version)) "nil version is omitted")))

(deftest response-test
  (testing "pass and warn answer 200 with health+json, never cached"
    (doseq [status ["pass" "warn"]]
      (let [res (health/response {:status status :checks {}})]
        (is (= 200 (:status res)))
        (is (= "application/health+json" (get-in res [:headers "Content-Type"])))
        (is (= "no-cache, no-store" (get-in res [:headers "Cache-Control"])))
        (is (= status (:status (body res)))))))
  (testing "fail answers 503"
    (is (= 503 (:status (health/response {:status "fail" :checks {}}))))))
