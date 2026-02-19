(ns behave.store
  (:require [ajax.core                              :refer [ajax-request]]
            [ajax.edn                                :refer [edn-request-format]]
            [ajax.protocols                         :as pr]
            [austinbirch.reactive-entity            :as re]
            [behave-routing.main                    :refer [current-route-order]]
            [behave.schema.core                     :refer [all-schemas]]
            [browser-utils.core                     :refer [download]]
            [browser-utils.interface                :refer [debounce]]
            [clojure.set                            :refer [union]]
            [absurder-sql.datascript.core           :as d]
            [absurder-sql.datascript.sqlite         :as ds-sqlite]
            [absurder-sql.datascript.storage-async  :as storage-async]
            [absurder-sql.interface                 :as sql]
            [datom-compressor.interface             :as c]
            [datom-utils.interface                  :refer [split-datom]]
            [ds-schema-utils.interface              :refer [->ds-schema]]
            [re-frame.core                          :as rf]
            [re-posh.core                           :as rp]
            [promesa.core                           :as p]))

;;; State

(defonce conn (atom nil))
(defonce my-txs (atom #{}))
(defonce sync-txs (atom #{}))
(defonce batch (atom []))
(defonce worksheet-from-file? (atom false))

(defonce ^:private sql-state
  (atom {:sql-conn nil
         :wrapper  nil
         :db-name  nil}))

;;; SQLite Helpers

(defn- reset-sql-state! []
  (when-let [old-conn (:sql-conn @sql-state)]
    (sql/close! old-conn))
  (reset! sql-state {:sql-conn nil :wrapper nil :db-name nil}))

(defn- init-sql-conn!
  "Initialize a SQLite connection for `db-name` and create a storage wrapper.
   Returns Promise of {:conn ds-conn, :wrapper wrapper, :sql-conn sql-conn}."
  [schema db-name]
  (-> (sql/connect! db-name)
      (p/then (fn [sql-conn]
                (let [store (ds-sqlite/sqlite-store sql-conn {:db-name db-name})]
                  (-> (storage-async/restore-sync store)
                      (p/then (fn [result]
                                (if result
                                  (let [[db wrapper] result]
                                    {:conn     (d/conn-from-db db)
                                     :wrapper  wrapper
                                     :sql-conn sql-conn})
                                  (let [wrapper (storage-async/make-sync-storage-wrapper store {})]
                                    {:conn     (d/create-conn schema {:storage wrapper})
                                     :wrapper  wrapper
                                     :sql-conn sql-conn}))))))))))

(defn- init-sql-conn-from-datoms!
  "Initialize a SQLite connection for `db-name`, create a conn from `datoms`,
   and persist to storage. Returns Promise of {:conn ds-conn, :wrapper wrapper, :sql-conn sql-conn}."
  [schema datoms db-name]
  (-> (sql/connect! db-name)
      (p/then (fn [sql-conn]
                (let [store   (ds-sqlite/sqlite-store sql-conn {:db-name db-name})
                      wrapper (storage-async/make-sync-storage-wrapper store {})
                      ds-conn (d/conn-from-datoms datoms schema {:storage wrapper})]
                  (-> (storage-async/store-impl-sync! (d/db ds-conn) wrapper true)
                      (p/then (fn [_]
                                {:conn     ds-conn
                                 :wrapper  wrapper
                                 :sql-conn sql-conn}))))))))

(defn- read-file-bytes
  "Read a File object as Uint8Array. Returns a Promise."
  [file]
  (js/Promise.
   (fn [res _reject]
     (let [rdr (js/FileReader.)]
       (set! (.-onload rdr) (fn [_] (res (js/Uint8Array. (.-result rdr)))))
       (.readAsArrayBuffer rdr file)))))

(defn- import-sqlite-bytes!
  "Import raw SQLite bytes into a new database. Returns Promise of
   {:conn ds-conn, :wrapper wrapper, :sql-conn sql-conn}."
  [db-bytes db-name]
  (-> (sql/connect! db-name)
      (p/then (fn [tmp-conn]
                (-> (sql/import! tmp-conn db-bytes)
                    (p/then (fn [_] (sql/close! tmp-conn))))))
      (p/then (fn [_] (sql/connect! db-name)))
      (p/then (fn [sql-conn]
                (let [store (ds-sqlite/sqlite-store sql-conn {:db-name  db-name
                                                              :skip-ddl true})]
                  (-> (storage-async/restore-sync store)
                      (p/then (fn [[db wrapper]]
                                {:conn     (d/conn-from-db db)
                                 :wrapper  wrapper
                                 :sql-conn sql-conn}))))))))

(declare debounced-batch-sync-tx-data)

;;; Helpers

(defn- txs [datoms]
  (into #{} (map #(nth % 3) datoms)))

(defn- new-datom? [datom]
  (not (contains? (union @my-txs @sync-txs) (nth datom 3))))

;;; Conn Initialization

(defn- setup-conn!
  "Wire up a DataScript conn: set the conn atom, register listener, connect
   re-posh and reactive-entity."
  [ds-conn]
  (reset! conn ds-conn)
  (d/listen! ds-conn :sync-tx-data
             (fn [{:keys [tx-data]}]
               (let [datoms (->> tx-data (filter new-datom?) (mapv split-datom))]
                 (when (seq datoms)
                   (swap! my-txs union (txs datoms))
                   (swap! batch concat datoms)
                   (debounced-batch-sync-tx-data)))))
  (rp/connect! ds-conn)
  (re/init! ds-conn)
  ds-conn)

(defn- reset-conn-state! []
  (reset! conn nil)
  (reset! sync-txs #{})
  (reset! my-txs #{})
  (reset! batch []))

;;; Batch Sync (server-side persistence, currently disabled)

(defn- batch-sync-tx-data []
  (when (seq @batch)
    (reset! batch [])))

(def ^:private debounced-batch-sync-tx-data (debounce batch-sync-tx-data 2000))

;;; Auto-save to SQLite

(defn save-to-sqlite!
  "Persist the current DB state to local SQLite storage. Returns a Promise."
  []
  (when-let [{:keys [wrapper]} @sql-state]
    (when (and @conn wrapper)
      (storage-async/store-impl-sync! (d/db @conn) wrapper true))))

(def ^:private debounced-save-to-sqlite (debounce save-to-sqlite! 3000))

;;; Server Sync (load initial state from server)

(defn- load-data-handler [[ok body]]
  (when ok
    (let [datoms  (mapv #(apply d/datom %) (c/unpack body))
          schema  (->ds-schema all-schemas)
          db-name (str "worksheet-" (random-uuid) ".db")]
      (swap! sync-txs union (txs datoms))
      (-> (init-sql-conn-from-datoms! schema datoms db-name)
          (p/then (fn [{ds-conn :conn :keys [wrapper sql-conn]}]
                    (swap! sql-state assoc
                           :sql-conn sql-conn
                           :wrapper  wrapper
                           :db-name  db-name)
                    (setup-conn! ds-conn)
                    (d/listen! ds-conn :auto-save (fn [_] (debounced-save-to-sqlite)))
                    (rf/dispatch-sync [:state/set :sync-loaded? true])))
          (p/catch (fn [e]
                     (js/console.error "Failed to initialize SQLite storage:" e)
                     ;; Fallback: in-memory conn
                     (setup-conn! (d/conn-from-datoms datoms schema))
                     (rf/dispatch-sync [:state/set :sync-loaded? true])))))))

(defn load-store! []
  (-> (sql/init!)
      (p/then (fn [_]
                (ajax-request {:uri             "/api/sync"
                               :handler         load-data-handler
                               :format          {:content-type "application/text" :write str}
                               :response-format {:description  "ArrayBuffer"
                                                 :type         :arraybuffer
                                                 :content-type "application/msgpack"
                                                 :read         pr/-body}})))))

(defn load-store-local!
  "Initialize a local DataScript connection backed by SQLite.
   When `ws-uuid` is provided, attempts to restore an existing DB named
   `worksheet-<ws-uuid>.db`. Otherwise creates a fresh DB with a random name."
  ([] (load-store-local! nil))
  ([ws-uuid]
   (let [schema  (->ds-schema all-schemas)
         db-name (str "worksheet-" (or ws-uuid (random-uuid)) ".db")]
     (-> (sql/init!)
         (p/then (fn [_] (init-sql-conn! schema db-name)))
         (p/then (fn [{ds-conn :conn :keys [wrapper sql-conn]}]
                   (swap! sql-state assoc
                          :sql-conn sql-conn
                          :wrapper  wrapper
                          :db-name  db-name)
                   (setup-conn! ds-conn)
                   (d/listen! ds-conn :auto-save (fn [_] (debounced-save-to-sqlite)))
                   (rf/dispatch-sync [:state/set :sync-loaded? true])))
         (p/catch (fn [e]
                    (js/console.error "Failed to initialize local store:" e)
                    (setup-conn! (d/create-conn schema))
                    (rf/dispatch-sync [:state/set :sync-loaded? true])))))))

;;; Save Worksheet (export .bp7)

(defn save-worksheet! [{:keys [file-name]}]
  (let [{:keys [sql-conn wrapper]} @sql-state]
    (if (and sql-conn wrapper)
      (-> (storage-async/store-impl-sync! (d/db @conn) wrapper true)
          (p/then (fn [_] (sql/export! sql-conn)))
          (p/then (fn [db-bytes]
                    (download db-bytes file-name "application/x-sqlite3")))
          (p/catch (fn [e]
                     (js/console.error "Save failed:" e))))
      ;; Fallback: server-side save
      (ajax-request {:uri             "/api/save"
                     :params          {:file-name file-name}
                     :method          :post
                     :handler         (fn [[ok body]]
                                        (when ok (download body file-name "application/x-sqlite3")))
                     :format          (edn-request-format)
                     :response-format {:description  "ArrayBuffer"
                                       :type         :arraybuffer
                                       :content-type "application/x-sqlite3"
                                       :read         pr/-body}}))))

;;; Open Worksheet (import .bp7)

(defn open-worksheet! [{:keys [file]}]
  (let [db-name (.-name file)]
    (reset-conn-state!)
    (reset-sql-state!)
    (reset! worksheet-from-file? true)
    (-> (read-file-bytes file)
        (p/then (fn [db-bytes]
                  (import-sqlite-bytes! db-bytes db-name)))
        (p/then (fn [{ds-conn :conn :keys [wrapper sql-conn]}]
                  (swap! sql-state assoc
                         :sql-conn sql-conn
                         :wrapper  wrapper
                         :db-name  db-name)
                  (setup-conn! ds-conn)
                  (d/listen! ds-conn :auto-save (fn [_] (debounced-save-to-sqlite)))
                  (rf/dispatch-sync [:state/set :sync-loaded? true])
                  (rf/dispatch-sync [:state/set :ws-version
                                     @(rf/subscribe [:worksheet/version
                                                     @(rf/subscribe [:worksheet/latest])])])))
        (p/catch (fn [e]
                   (js/console.error "Open worksheet failed:" e))))))

;;; New Worksheet

(defn new-worksheet! [nname modules _submodule workflow]
  (let [schema  (->ds-schema all-schemas)
        ws-uuid (str (d/squuid))
        db-name (str "worksheet-" ws-uuid ".db")]
    (reset-conn-state!)
    (reset-sql-state!)
    (reset! worksheet-from-file? false)
    (-> (init-sql-conn! schema db-name)
        (p/then (fn [{ds-conn :conn :keys [wrapper sql-conn]}]
                  (swap! sql-state assoc
                         :sql-conn sql-conn
                         :wrapper  wrapper
                         :db-name  db-name)
                  (setup-conn! ds-conn)
                  (d/listen! ds-conn :auto-save (fn [_] (debounced-save-to-sqlite)))
                  (rf/dispatch-sync [:state/set :sync-loaded? true])
                  (rf/dispatch-sync [:worksheet/new {:name    nname
                                                     :modules (vec modules)
                                                     :uuid    ws-uuid
                                                     :version @(rf/subscribe [:state :app-version])}])
                  (reset! current-route-order
                          @(rf/subscribe [:wizard/route-order ws-uuid workflow]))
                  (rf/dispatch-sync [:navigate (first @current-route-order)])))
        (p/catch (fn [e]
                   (js/console.error "New worksheet failed:" e))))))

;;; Sync Helpers (kept for future server-sync support)

(defn apply-latest-datoms [[ok body]]
  (when ok
    (let [datoms (->> (c/unpack body)
                      (filter new-datom?)
                      (map (partial apply d/datom)))]
      (when (seq datoms)
        (swap! sync-txs union (txs datoms))
        (d/transact @conn datoms)))))

;;; Public Fns

(defn init! [{:keys [datoms schema]}]
  (if @conn
    @conn
    (do
      (setup-conn! (d/conn-from-datoms datoms schema))
      @conn)))

;;; Effects

(rf/reg-fx :ds/init init!)

;;; Events

(rf/reg-event-fx
 :ds/initialize
 (fn [_ [_ schema datoms]]
   {:ds/init {:datoms datoms :schema schema}}))

(rp/reg-event-ds
 :ds/transact
 (fn [_ [_ tx-data]]
   (first tx-data)))

(rp/reg-event-ds
 :ds/transact-many
 (fn [_ [_ tx-data]]
   tx-data))
