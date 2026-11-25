(ns behave-cms.health-test
  (:require [behave-cms.health  :as h]
            [clojure.data.json  :as json]
            [clojure.test       :refer [deftest is testing use-fixtures]]
            [datomic-store.main :as s]
            [datomic.api        :as d]
            [health.core        :as hc]))

;;; Fixtures

(def ^:private real-required-assets @#'h/required-assets)

(def ^:private test-assets
  "Assets tracked in git, so the default run passes without a CLJS build."
  ["public/js/katex.min.js" "public/js/fontawesome.js"])

(defn- with-mem-datomic
  "Connects to a fresh in-memory Datomic DB, with every disk at 10% used."
  [f]
  (let [uri (str "datomic:mem://cms-health-" (random-uuid))]
    (d/create-database uri)
    (reset! s/datomic-conn (d/connect uri))
    (try
      (with-redefs [hc/used-pct       (constantly 10.0)
                    h/required-assets (constantly test-assets)]
        (f))
      (finally
        (reset! s/datomic-conn nil)
        (d/delete-database uri)))))

(use-fixtures :each with-mem-datomic)

;;; Helpers

(defn- get-health
  "Calls `handler` (default: the health handler) with GET /health and parses the JSON body."
  ([] (get-health h/health-handler))
  ([handler]
   (update (handler {:uri "/health" :request-method :get :headers {}})
           :body #(json/read-str (if (string? %) % (slurp %)) :key-fn keyword))))

(defn- check [body k] (first (get-in body [:checks k])))

;;; Response shape

(deftest healthy-response-test
  (let [{:keys [status body]} (get-health)]
    (is (= 200 status))
    (is (= "pass" (:status body)))
    (is (= #{:disk:utilization :datomic:responseTime :heap:utilization
             :assets:availability :uptime}
           (set (keys (:checks body)))))
    (is (not (contains? body :version)) "the CMS has no version.edn")))

(deftest routed-test
  (testing "/health answers without a login, through the full handler stack"
    (let [app ((requiring-resolve 'behave-cms.handler/create-handler-stack) false false)
          res (app {:uri "/health" :request-method :get :headers {}})]
      (is (= 200 (:status res)))
      (is (= "application/health+json" (get-in res [:headers "Content-Type"]))))))

;;; Datomic

(deftest datomic-check-test
  (testing "a live connection reports sync time in ms"
    (let [db (check (:body (get-health)) :datomic:responseTime)]
      (is (= ["vms" "datastore" "pass" "ms"]
             ((juxt :componentId :componentType :status :observedUnit) db)))
      (is (nat-int? (:observedValue db)))))
  (testing "no connection fails with 503"
    (reset! s/datomic-conn nil)
    (let [{:keys [status body]} (get-health)]
      (is (= 503 status))
      (is (= "Datomic connection not initialized"
             (:output (check body :datomic:responseTime)))))))

(deftest transactor-timeout-test
  (testing "a transactor that never answers fails instead of hanging"
    (with-redefs [d/sync               (fn [_] (promise))
                  h/DATOMIC-TIMEOUT-MS 50]
      (let [{:keys [status body]} (get-health)]
        (is (= 503 status))
        (is (= "Datomic transactor did not respond"
               (:output (check body :datomic:responseTime))))))))

;;; Assets

(deftest assets-test
  (testing "present assets pass"
    (is (every? #(= "pass" (:status %))
                (get-in (:body (get-health)) [:checks :assets:availability]))))
  (testing "a missing asset fails the whole report"
    (with-redefs [h/required-assets (constantly ["public/js/katex.min.js" "public/nope.js"])]
      (let [{:keys [status body]} (get-health)]
        (is (= 503 status))
        (is (= ["public/nope.js" "fail" "missing"]
               ((juxt :componentId :status :output)
                (second (get-in body [:checks :assets:availability])))))))))

(deftest required-assets-test
  (testing "includes the bundle the page actually loads"
    (let [assets (real-required-assets)]
      (is (some #(re-matches #"public/cljs/.+\.js" %) assets))
      (is (every? (set assets) test-assets)))))
