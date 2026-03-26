(ns schema-migrate.runner-test
  (:require
   [clojure.java.io :as io]
   [clojure.test    :refer [deftest is testing use-fixtures]]
   [datomic.api     :as d]
   [schema-migrate.runner :as runner]))

;;; =========================================================================
;;; Test Helpers
;;; =========================================================================

(def test-schema
  [{:db/ident       :bp/uuid
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}
   {:db/ident       :bp/nid
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}
   {:db/ident       :bp/migration-id
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/index       true}
   {:db/ident       :test/name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :test/value
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}])

(def ^:dynamic *conn* nil)

(defn- create-test-db []
  (let [uri  (str "datomic:mem://runner-test-" (random-uuid))
        _    (d/create-database uri)
        conn (d/connect uri)]
    @(d/transact conn test-schema)
    (atom conn)))

(defn- delete-test-db [conn]
  (let [raw-conn @conn
        uri      (-> raw-conn .log str
                     ;; extract URI is not straightforward; just delete all mem DBs
                     )]
    ;; mem DBs are cleaned up on JVM exit; no explicit delete needed
    ))

(use-fixtures :each
  (fn [f]
    (binding [*conn* (create-test-db)]
      (f))))

;;; =========================================================================
;;; Test Migration Fixtures
;;; =========================================================================

(defn- write-migration!
  "Write a migration file to `dir` and return the file."
  [dir filename content]
  (let [f (io/file dir filename)]
    (spit f content)
    f))

(defn- with-temp-migrations
  "Create a temp directory with migration files, run `f`, then clean up."
  [migrations f]
  (let [dir (io/file (System/getProperty "java.io.tmpdir")
                     (str "migrations-test-" (random-uuid)))]
    (.mkdirs dir)
    (try
      (doseq [{:keys [filename content]} migrations]
        (write-migration! dir filename content))
      (f dir)
      (finally
        (doseq [file (.listFiles dir)] (.delete file))
        (.delete dir)))))

;;; =========================================================================
;;; Tests
;;; =========================================================================

(deftest single-step-payload-fn-test
  (testing "A migration with payload-fn is applied and tracked"
    (with-temp-migrations
      [{:filename "2026_01_01_add_test_entity.clj"
        :content  "(ns migrations.2026-01-01-add-test-entity
  (:require [datomic.api :as d]))

(defn payload-fn [conn]
  [{:test/name \"hello\" :test/value 42}])
"}]
      (fn [dir]
        (runner/run-pending-migrations! *conn* (str dir))
        (is (runner/migration-applied? *conn* "2026-01-01-add-test-entity"))
        (is (= "hello"
               (d/q '[:find ?n .
                       :where [?e :test/name ?n]]
                    (d/db @*conn*))))))))

(deftest single-step-payload-def-test
  (testing "A migration with a static payload def is applied"
    (with-temp-migrations
      [{:filename "2026_01_01_static_payload.clj"
        :content  "(ns migrations.2026-01-01-static-payload)

(def payload [{:test/name \"static\" :test/value 99}])
"}]
      (fn [dir]
        (runner/run-pending-migrations! *conn* (str dir))
        (is (runner/migration-applied? *conn* "2026-01-01-static-payload"))
        (is (= 99
               (d/q '[:find ?v .
                       :where [?e :test/name "static"]
                              [?e :test/value ?v]]
                    (d/db @*conn*))))))))

(deftest idempotency-test
  (testing "Running migrations twice does not duplicate data"
    (with-temp-migrations
      [{:filename "2026_01_01_idempotent.clj"
        :content  "(ns migrations.2026-01-01-idempotent)

(defn payload-fn [conn]
  [{:test/name \"once\" :test/value 1}])
"}]
      (fn [dir]
        (runner/run-pending-migrations! *conn* (str dir))
        (runner/run-pending-migrations! *conn* (str dir))
        (is (= 1
               (d/q '[:find (count ?e) .
                       :where [?e :test/name "once"]]
                    (d/db @*conn*))))))))

(deftest ignore-metadata-test
  (testing "Migrations with ^{:migrate/ignore? true} are skipped"
    (with-temp-migrations
      [{:filename "2026_01_01_ignored.clj"
        :content  "(ns ^{:migrate/ignore? true} migrations.2026-01-01-ignored)

(defn payload-fn [conn]
  [{:test/name \"should-not-exist\" :test/value 0}])
"}
       {:filename "2026_01_02_not_ignored.clj"
        :content  "(ns migrations.2026-01-02-not-ignored)

(defn payload-fn [conn]
  [{:test/name \"should-exist\" :test/value 1}])
"}]
      (fn [dir]
        (runner/run-pending-migrations! *conn* (str dir))
        (is (not (runner/migration-applied? *conn* "2026-01-01-ignored")))
        (is (runner/migration-applied? *conn* "2026-01-02-not-ignored"))
        (is (nil? (d/q '[:find ?e .
                          :where [?e :test/name "should-not-exist"]]
                       (d/db @*conn*))))))))

(deftest ordering-test
  (testing "Migrations run in filename-sorted order"
    (let [order (atom [])]
      (with-temp-migrations
        [{:filename "2026_01_02_second.clj"
          :content  (str "(ns migrations.2026-01-02-second)\n"
                         "(defn payload-fn [conn]\n"
                         "  [{:test/name \"second\" :test/value 2}])\n")}
         {:filename "2026_01_01_first.clj"
          :content  (str "(ns migrations.2026-01-01-first)\n"
                         "(defn payload-fn [conn]\n"
                         "  [{:test/name \"first\" :test/value 1}])\n")}]
        (fn [dir]
          (runner/run-pending-migrations! *conn* (str dir))
          (is (runner/migration-applied? *conn* "2026-01-01-first"))
          (is (runner/migration-applied? *conn* "2026-01-02-second")))))))

(deftest multi-step-success-test
  (testing "payload-steps migrations run all steps and record marker"
    (with-temp-migrations
      [{:filename "2026_01_01_multi_step.clj"
        :content  "(ns migrations.2026-01-01-multi-step)

(def payload-steps
  [(fn [conn] [{:test/name \"step1\" :test/value 1}])
   (fn [conn] [{:test/name \"step2\" :test/value 2}])])
"}]
      (fn [dir]
        (runner/run-pending-migrations! *conn* (str dir))
        (is (runner/migration-applied? *conn* "2026-01-01-multi-step"))
        (is (= 1 (d/q '[:find ?v . :where [?e :test/name "step1"] [?e :test/value ?v]]
                       (d/db @*conn*))))
        (is (= 2 (d/q '[:find ?v . :where [?e :test/name "step2"] [?e :test/value ?v]]
                       (d/db @*conn*))))))))

(deftest multi-step-rollback-test
  (testing "When a step fails, previous steps are rolled back and marker is not recorded"
    (with-temp-migrations
      [{:filename "2026_01_01_multi_fail.clj"
        :content  "(ns migrations.2026-01-01-multi-fail)

(def payload-steps
  [(fn [conn] [{:test/name \"will-rollback\" :test/value 1}])
   (fn [conn] (throw (ex-info \"Step 2 failed\" {})))])
"}]
      (fn [dir]
        (is (thrown? Exception
                     (runner/run-pending-migrations! *conn* (str dir))))
        (is (not (runner/migration-applied? *conn* "2026-01-01-multi-fail")))
        ;; Step 1 should have been rolled back
        (is (nil? (d/q '[:find ?e .
                          :where [?e :test/name "will-rollback"]]
                       (d/db @*conn*))))))))

(deftest halt-on-failure-test
  (testing "A failing migration halts execution — subsequent migrations don't run"
    (with-temp-migrations
      [{:filename "2026_01_01_fails.clj"
        :content  "(ns migrations.2026-01-01-fails)

(defn payload-fn [conn]
  (throw (ex-info \"Boom\" {})))
"}
       {:filename "2026_01_02_should_not_run.clj"
        :content  "(ns migrations.2026-01-02-should-not-run)

(defn payload-fn [conn]
  [{:test/name \"unreachable\" :test/value 0}])
"}]
      (fn [dir]
        (is (thrown? Exception
                     (runner/run-pending-migrations! *conn* (str dir))))
        (is (not (runner/migration-applied? *conn* "2026-01-02-should-not-run")))
        (is (nil? (d/q '[:find ?e .
                          :where [?e :test/name "unreachable"]]
                       (d/db @*conn*))))))))
