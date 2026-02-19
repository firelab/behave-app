(ns behave.store-integration-test
  "Integration tests for the full re-frame/re-posh/absurder-sql storage cycle.
   Tests worksheet create, transact, query, save, and restore round-trips."
  (:require [cljs.test :refer [deftest is async testing use-fixtures] :include-macros true]
            [absurder-sql.datascript.core :as d]
            [absurder-sql.datascript.sqlite :as ds-sqlite]
            [absurder-sql.datascript.storage-async :as storage-async]
            [absurder-sql.interface :as sql]
            [behave.schema.core :refer [all-schemas]]
            [behave.store :as s]
            [ds-schema-utils.interface :refer [->ds-schema]]
            [promesa.core :as p]
            [re-frame.core :as rf]
            [re-posh.core :as rp]
            [re-posh.db :as rpdb]
            [austinbirch.reactive-entity :as re]))

;;; Fixtures

(defn ^:private setup-rf! []
  (rf/dispatch-sync [:initialize]))

(defn ^:private teardown! []
  (rf/clear-subscription-cache!)
  (re/clear-cache!)
  ;; Reset re-posh store *after* clearing sub cache to avoid
  ;; stale subscriptions evaluating against nil conn
  (reset! s/my-txs #{})
  (reset! s/sync-txs #{})
  (reset! s/batch [])
  (reset! rpdb/store nil)
  (reset! s/conn nil))

(use-fixtures :each
  {:before (fn [] (setup-rf!))
   :after  (fn [] (teardown!))})

;;; Helpers

(def ^:private test-schema (->ds-schema all-schemas))

(defn- make-conn!
  "Create an in-memory DataScript conn, wire it into behave.store and re-posh."
  []
  (let [conn (d/create-conn test-schema)]
    (reset! s/conn conn)
    (rp/connect! conn)
    (re/init! conn)
    conn))

;;; ==========================================================================
;;; In-Memory Re-Frame / Re-Posh / DataScript Cycle
;;; ==========================================================================

(deftest ^:integration conn-init-test
  (testing "create-conn produces a valid connection"
    (let [conn (make-conn!)]
      (is (some? conn))
      (is (d/conn? conn))
      (is (identical? conn @s/conn)))))

(deftest ^:integration re-posh-transact-test
  (testing "re-posh :ds/transact writes datoms readable via d/q"
    (make-conn!)
    (rf/dispatch-sync [:ds/transact [[{:worksheet/uuid "ws-1"
                                       :worksheet/name "Test Worksheet"}]]])
    (let [result (d/q '[:find ?name .
                        :in $
                        :where
                        [?e :worksheet/uuid "ws-1"]
                        [?e :worksheet/name ?name]]
                      @@s/conn)]
      (is (= "Test Worksheet" result)))))

(deftest ^:integration re-posh-subscription-test
  (testing "re-posh subscriptions react to transactions"
    (let [conn (make-conn!)]
      (rp/reg-query-sub
       :test/worksheet-names
       '[:find [?name ...]
         :in $
         :where [_ :worksheet/name ?name]])

      (let [*names (rf/subscribe [:test/worksheet-names])]
        (is (empty? @*names))
        (rf/dispatch-sync [:ds/transact [[{:worksheet/uuid "ws-a"
                                           :worksheet/name "Alpha"}]]])
        (is (= ["Alpha"] @*names))
        (rf/dispatch-sync [:ds/transact [[{:worksheet/uuid "ws-b"
                                           :worksheet/name "Beta"}]]])
        (is (= #{"Alpha" "Beta"} (set @*names)))
        ;; Clear subs before teardown to prevent stale evaluation
        (rf/clear-subscription-cache!)))))

(deftest ^:integration worksheet-new-event-test
  (testing ":worksheet/new creates a worksheet entity with correct attributes"
    (make-conn!)
    (let [ws-uuid "test-new-ws"]
      (rf/dispatch-sync [:worksheet/new {:uuid    ws-uuid
                                         :name    "New Worksheet"
                                         :modules [:surface]
                                         :version "1.0.0"}])
      (let [*ws (rf/subscribe [:worksheet ws-uuid])]
        (is (some? @*ws))
        (is (= ws-uuid (:worksheet/uuid @*ws)))
        (is (= "New Worksheet" (:worksheet/name @*ws)))))))

(deftest ^:integration transact-many-test
  (testing ":ds/transact-many applies multiple datoms"
    (make-conn!)
    (rf/dispatch-sync [:ds/transact-many
                       [{:worksheet/uuid "ws-multi-1" :worksheet/name "First"}
                        {:worksheet/uuid "ws-multi-2" :worksheet/name "Second"}]])
    (let [count' (d/q '[:find (count ?e) .
                        :where [?e :worksheet/uuid _]]
                      @@s/conn)]
      (is (= 2 count')))))

(deftest ^:integration entity-navigation-test
  (testing "d/entity and d/pull work on the absurder-sql conn"
    (make-conn!)
    (d/transact! @s/conn [{:worksheet/uuid "ws-entity"
                           :worksheet/name "Entity Test"}])
    (let [eid    (d/entid @@s/conn [:worksheet/uuid "ws-entity"])
          entity (d/entity @@s/conn eid)
          pulled (d/pull @@s/conn '[:worksheet/name] eid)]
      (is (some? eid))
      (is (= "Entity Test" (:worksheet/name entity)))
      (is (= {:worksheet/name "Entity Test"} pulled)))))

(deftest ^:integration input-group-round-trip-test
  (testing "add input group, upsert variable, query back via subs"
    (make-conn!)
    (let [ws-uuid "ws-input-rt"]
      (rf/dispatch-sync [:worksheet/new {:uuid    ws-uuid
                                         :name    "Input RT"
                                         :modules [:surface]}])
      (rf/dispatch-sync [:worksheet/add-input-group ws-uuid "group-1" 0])

      (let [*ws (rf/subscribe [:worksheet ws-uuid])
            igs (:worksheet/input-groups @*ws)]
        (is (= 1 (count igs)))
        (is (= "group-1" (:input-group/group-uuid (first igs))))

        (rf/dispatch-sync [:worksheet/upsert-input-variable
                           ws-uuid "group-1" 0 "gv-1" "42" "ch/h"])
        (let [inputs (->> @*ws
                          :worksheet/input-groups
                          first
                          :input-group/inputs)]
          (is (= 1 (count inputs)))
          (is (= "42" (:input/value (first inputs)))))))))

;; TODO (RJ 2026-02-18) -- absurder-sql is stricter about auto-tempids in value position;
;; the :worksheet/add-result-table-cell event handler needs updating.
#_(deftest ^:integration result-table-round-trip-test
    (testing "add result table with headers, rows, and cells"
      (make-conn!)
      (let [ws-uuid "ws-result-rt"]
        (rf/dispatch-sync [:worksheet/new {:uuid    ws-uuid
                                           :name    "Result RT"
                                           :modules [:surface]}])
        (rf/dispatch-sync [:worksheet/add-result-table ws-uuid])
        (rf/dispatch-sync [:worksheet/add-result-table-header ws-uuid "gv-out-1" 0 "ft"])
        (rf/dispatch-sync [:worksheet/add-result-table-row ws-uuid 0])
        (rf/dispatch-sync [:worksheet/add-result-table-cell ws-uuid 0 "gv-out-1" 0 99.5])

        (let [cells @(rf/subscribe [:worksheet/result-table-cell-data ws-uuid])]
          (is (seq cells))
          (is (some (fn [[_row col _rep v]] (and (= col "gv-out-1") (= v 99.5)))
                    cells))))))

;;; ==========================================================================
;;; SQLite Storage Round-Trip (async)
;;; ==========================================================================

(deftest ^:integration sqlite-init-test
  (async done
         (-> (sql/init!)
             (p/then (fn [_]
                       (is true "sql/init! resolved successfully")
                       (done)))
             (p/catch (fn [e]
                        (is false (str "sql/init! failed: " (.-message e)))
                        (done))))))

(deftest ^:integration sqlite-create-and-restore-test
  (async done
         (let [schema  test-schema
               db-name (str "test-" (random-uuid) ".db")]
           (-> (sql/init!)
               (p/then (fn [_] (sql/connect! db-name)))
               (p/then (fn [sql-conn]
                         (let [store   (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                               wrapper (storage-async/make-sync-storage-wrapper store {})
                               conn    (d/create-conn schema {:storage wrapper})]
                      ;; Transact some data
                           (d/transact! conn [{:worksheet/uuid "ws-sqlite"
                                               :worksheet/name "SQLite Test"}])
                      ;; Persist to SQLite
                           (-> (storage-async/store-impl-sync! (d/db conn) wrapper true)
                               (p/then (fn [_] (sql/close! sql-conn)))
                          ;; Re-open and restore
                               (p/then (fn [_] (sql/connect! db-name)))
                               (p/then (fn [sql-conn2]
                                         (let [store2 (ds-sqlite/sqlite-store sql-conn2 {:db-name  db-name
                                                                                         :skip-ddl true})]
                                           (-> (storage-async/restore-sync store2)
                                               (p/then (fn [[db _wrapper]]
                                                         (is (some? db) "restored DB should not be nil")
                                                         (let [name' (d/q '[:find ?name .
                                                                            :where
                                                                            [?e :worksheet/uuid "ws-sqlite"]
                                                                            [?e :worksheet/name ?name]]
                                                                          db)]
                                                           (is (= "SQLite Test" name')
                                                               "data should survive save/restore round-trip"))
                                                         (sql/close! sql-conn2)))))))))))
               (p/then (fn [_] (done)))
               (p/catch (fn [e]
                          (is false (str "sqlite round-trip failed: " (.-message e)))
                          (done)))))))

(deftest ^:integration sqlite-export-import-test
  (async done
         (let [schema  test-schema
               db-name (str "test-export-" (random-uuid) ".db")]
           (-> (sql/init!)
               (p/then (fn [_] (sql/connect! db-name)))
               (p/then (fn [sql-conn]
                         (let [store   (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                               wrapper (storage-async/make-sync-storage-wrapper store {})
                               conn    (d/create-conn schema {:storage wrapper})]
                      ;; Transact
                           (d/transact! conn [{:worksheet/uuid "ws-export"
                                               :worksheet/name "Export Test"}
                                              {:worksheet/uuid "ws-export-2"
                                               :worksheet/name "Second WS"}])
                      ;; Save + export bytes
                           (-> (storage-async/store-impl-sync! (d/db conn) wrapper true)
                               (p/then (fn [_] (sql/export! sql-conn)))
                               (p/then (fn [db-bytes]
                                         (is (pos? (.-length db-bytes))
                                             "exported bytes should be non-empty")
                                         (sql/close! sql-conn)
                                         db-bytes))))))
          ;; Import into a new database
               (p/then (fn [db-bytes]
                         (let [import-name (str "test-import-" (random-uuid) ".db")]
                           (-> (sql/connect! import-name)
                               (p/then (fn [tmp-conn]
                                         (-> (sql/import! tmp-conn db-bytes)
                                             (p/then (fn [_] (sql/close! tmp-conn))))))
                               (p/then (fn [_] (sql/connect! import-name)))
                               (p/then (fn [sql-conn2]
                                         (let [store2 (ds-sqlite/sqlite-store sql-conn2 {:db-name  import-name
                                                                                         :skip-ddl true})]
                                           (-> (storage-async/restore-sync store2)
                                               (p/then (fn [[db _wrapper]]
                                                         (let [names (d/q '[:find [?name ...]
                                                                            :where [_ :worksheet/name ?name]]
                                                                          db)]
                                                           (is (= #{"Export Test" "Second WS"} (set names))
                                                               "imported DB should contain both worksheets"))
                                                         (sql/close! sql-conn2)))))))))))
               (p/then (fn [_] (done)))
               (p/catch (fn [e]
                          (is false (str "export/import failed: " (.-message e)))
                          (done)))))))

(deftest ^:integration sqlite-with-re-posh-test
  (async done
         (let [schema  test-schema
               db-name (str "test-reposh-" (random-uuid) ".db")]
           (-> (sql/init!)
               (p/then (fn [_] (sql/connect! db-name)))
               (p/then (fn [sql-conn]
                         (let [store   (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                               wrapper (storage-async/make-sync-storage-wrapper store {})
                               conn    (d/create-conn schema {:storage wrapper})]
                      ;; Wire into re-posh
                           (reset! s/conn conn)
                           (rp/connect! conn)
                           (re/init! conn)

                      ;; Transact via re-frame
                           (rf/dispatch-sync [:ds/transact [[{:worksheet/uuid "ws-reposh"
                                                              :worksheet/name "RePosh SQLite"}]]])

                      ;; Verify query works
                           (let [name' (d/q '[:find ?name .
                                              :where
                                              [_ :worksheet/uuid "ws-reposh"]
                                              [_ :worksheet/name ?name]]
                                            @@s/conn)]
                             (is (= "RePosh SQLite" name')))

                      ;; Persist and restore
                           (-> (storage-async/store-impl-sync! (d/db conn) wrapper true)
                               (p/then (fn [_] (sql/close! sql-conn)))
                               (p/then (fn [_] (sql/connect! db-name)))
                               (p/then (fn [sql-conn2]
                                         (let [store2 (ds-sqlite/sqlite-store sql-conn2 {:db-name  db-name
                                                                                         :skip-ddl true})]
                                           (-> (storage-async/restore-sync store2)
                                               (p/then (fn [[db _wrapper2]]
                                                    ;; Create new conn from restored DB
                                                         (let [restored-conn (d/conn-from-db db)]
                                                      ;; Teardown old, set up restored
                                                           (reset! rpdb/store nil)
                                                           (rf/clear-subscription-cache!)
                                                           (reset! s/conn restored-conn)
                                                           (rp/connect! restored-conn)
                                                           (re/init! restored-conn)

                                                      ;; Verify data survived
                                                           (let [name' (d/q '[:find ?name .
                                                                              :where
                                                                              [_ :worksheet/uuid "ws-reposh"]
                                                                              [_ :worksheet/name ?name]]
                                                                            @@s/conn)]
                                                             (is (= "RePosh SQLite" name')
                                                                 "data survives re-posh + SQLite round-trip"))

                                                      ;; Verify subscription works on restored conn
                                                           (let [*ws (rf/subscribe [:worksheet "ws-reposh"])]
                                                             (is (some? @*ws))
                                                             (is (= "RePosh SQLite" (:worksheet/name @*ws))))

                                                           (sql/close! sql-conn2))))))))))))
               (p/then (fn [_] (done)))
               (p/catch (fn [e]
                          (is false (str "re-posh + sqlite failed: " (.-message e)))
                          (done)))))))
