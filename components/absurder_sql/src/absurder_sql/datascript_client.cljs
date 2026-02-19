(ns absurder-sql.datascript-client
  (:require [absurder-sql.datascript.sqlite :as ds-sqlite]
            [absurder-sql.datascript.core :as d]
            [absurder-sql.datascript.storage-async :as storage-async]
            [absurder-sql.interface :as sql]
            [cljs.reader :as reader]
            [promesa.core :as p]))

;;; State

(defonce ^:private state
  (atom {:conn     nil
         :sql-conn nil
         :wrapper  nil
         :db-name  "datascript-test.db"}))

(def ^:private demo-schema
  {:aka {:db/cardinality :db.cardinality/many}})

;;; UI Helpers

(defn- log! [msg]
  (let [el (.getElementById js/document "log")]
    (set! (.-textContent el)
          (str (.-textContent el) msg "\n"))
    (set! (.-scrollTop el) (.-scrollHeight el))))

(defn- set-status! [s]
  (set! (.-textContent (.getElementById js/document "status"))
        (str "Status: " s)))

(defn- render-results! [results]
  (let [el (.getElementById js/document "results")]
    (set! (.-textContent el) (pr-str results))))

;;; DataScript Operations

(defn- transact! []
  (try
    (let [{:keys [conn]} @state
          text   (.-value (.getElementById js/document "tx-input"))
          report (d/transact! conn (reader/read-string text))]
      (log! (str "Transacted " (count (:tx-data report)) " datom(s)."))
      (render-results! (mapv str (:tx-data report))))
    (catch :default e
      (log! (str "TX ERROR: " (or (.-message e) (str e)))))))

(defn- query! []
  (let [text   (.-value (.getElementById js/document "q-input"))
        q-form (reader/read-string text)
        db     (d/db (:conn @state))]
    (try
      (let [results (d/q q-form db)]
        (log! (str "Query returned " (count results) " result(s)."))
        (render-results! results))
      (catch :default e
        (log! (str "QUERY ERROR: " (.-message e)))))))

(defn- dump-datoms! []
  (let [db     (d/db (:conn @state))
        datoms (vec (d/datoms db :eavt))]
    (log! (str (count datoms) " datom(s) in DB."))
    (render-results! (mapv str datoms))))

(defn- save! []
  (let [{:keys [conn wrapper]} @state]
    (-> (storage-async/store-impl-sync! (d/db conn) wrapper true)
        (p/then (fn [_] (log! "Saved to SQLite.")))
        (p/catch (fn [e] (log! (str "SAVE ERROR: " (.-message e))))))))

;;; Export / Import

(defn- export-db! []
  (let [{:keys [conn wrapper sql-conn db-name]} @state]
    (-> (storage-async/store-impl-sync! (d/db conn) wrapper true)
        (p/then (fn [_] (sql/export! sql-conn)))
        (p/then (fn [db-bytes]
                  (let [blob (js/Blob. #js [db-bytes] #js {:type "application/octet-stream"})
                        url  (.createObjectURL js/URL blob)
                        a    (.createElement js/document "a")]
                    (set! (.-href a) url)
                    (set! (.-download a) db-name)
                    (.click a)
                    (.revokeObjectURL js/URL url)
                    (log! (str "Exported " (.-length db-bytes) " bytes.")))))
        (p/catch (fn [e] (log! (str "EXPORT ERROR: " (.-message e))))))))

(defn- read-file-bytes
  "Read a File object as Uint8Array. Returns a Promise."
  [file]
  (js/Promise.
   (fn [res _reject]
     (let [rdr (js/FileReader.)]
       (set! (.-onload rdr) (fn [_] (res (js/Uint8Array. (.-result rdr)))))
       (.readAsArrayBuffer rdr file)))))

(defn- import-bytes!
  "Import raw SQLite bytes: connect, import, reconnect, restore DataScript.
   Returns a Promise."
  [db-bytes db-name]
  (-> (sql/connect! db-name)
      (p/then (fn [tmp-conn]
                (-> (sql/import! tmp-conn db-bytes)
                    (p/then (fn [_] (sql/close! tmp-conn))))))
      (p/then (fn [_] (sql/connect! db-name)))
      (p/then (fn [fresh-conn]
                (let [store (ds-sqlite/sqlite-store fresh-conn {:db-name  db-name
                                                                :skip-ddl true})]
                  (-> (storage-async/restore-sync store)
                      (p/then (fn [[db wrapper]]
                                (let [ds-conn (d/conn-from-db db)]
                                  (swap! state assoc
                                         :conn     ds-conn
                                         :sql-conn fresh-conn
                                         :wrapper  wrapper
                                         :db-name  db-name)
                                  (set-status! "connected (imported)")
                                  (log! (str "Imported " db-name ". "
                                             (count (d/datoms db :eavt)) " datom(s).")))))))))))

(defn- import-db! []
  (let [input (.createElement js/document "input")]
    (set! (.-type input) "file")
    (set! (.-accept input) ".db,.bp7")
    (.addEventListener
     input "change"
     (fn [_]
       (when-let [file (aget (.-files input) 0)]
         (let [db-name (.-name file)]
           (-> (read-file-bytes file)
               (p/then (fn [db-bytes]
                         (log! (str "Importing " db-name " (" (.-length db-bytes) " bytes)..."))
                         (when-let [old-conn (:sql-conn @state)]
                           (sql/close! old-conn))
                         (import-bytes! db-bytes db-name)))
               (p/catch (fn [e] (log! (str "IMPORT ERROR: " (.-message e))))))))))
    (.click input)))

;;; Init

(defn- init-conn!
  "Given a live SQLite connection, create a SQLiteStorage and either restore an
   existing DataScript DB or create a fresh one. Returns a Promise of
   {:conn ds-conn :wrapper wrapper}."
  [sql-conn schema db-name]
  (let [store (ds-sqlite/sqlite-store sql-conn {:db-name db-name})]
    (-> (storage-async/restore-sync store)
        (p/then (fn [result]
                  (if result
                    (let [[db wrapper] result]
                      {:conn (d/conn-from-db db) :wrapper wrapper})
                    (let [wrapper (storage-async/make-sync-storage-wrapper store {})]
                      {:conn (d/create-conn schema {:storage wrapper}) :wrapper wrapper})))))))

(defn ^:export init []
  (set-status! "initializing SQLite + DataScript...")
  (log! "Starting up...")
  (-> (sql/init!)
      (p/then (fn [_]
                (let [db-name (:db-name @state)]
                  (-> (sql/connect! db-name)
                      (p/then (fn [sql-conn]
                                (-> (init-conn! sql-conn demo-schema db-name)
                                    (p/then (fn [{:keys [conn wrapper]}]
                                              (swap! state assoc
                                                     :conn     conn
                                                     :sql-conn sql-conn
                                                     :wrapper  wrapper)
                                              (set-status! "connected")
                                              (log! "DataScript + SQLite ready."))))))))))
      (p/catch (fn [e]
                 (let [msg (or (.-message e) (str e))]
                   (set-status! "error")
                   (log! (str "Init failed: " msg))
                   (js/console.error "Init failed:" e)))))

  (.addEventListener (.getElementById js/document "btn-transact")
                     "click" (fn [_] (transact!)))
  (.addEventListener (.getElementById js/document "btn-query")
                     "click" (fn [_] (query!)))
  (.addEventListener (.getElementById js/document "btn-datoms")
                     "click" (fn [_] (dump-datoms!)))
  (.addEventListener (.getElementById js/document "btn-save")
                     "click" (fn [_] (save!)))
  (.addEventListener (.getElementById js/document "btn-export")
                     "click" (fn [_] (export-db!)))
  (.addEventListener (.getElementById js/document "btn-import")
                     "click" (fn [_] (import-db!)))
  (.addEventListener (.getElementById js/document "btn-clear")
                     "click" (fn [_]
                               (set! (.-textContent (.getElementById js/document "log")) "")
                               (set! (.-textContent (.getElementById js/document "results")) ""))))
