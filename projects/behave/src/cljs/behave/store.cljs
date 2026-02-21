(ns behave.store
  (:require [ajax.core                              :refer [ajax-request]]
            [ajax.edn                                :refer [edn-request-format]]
            [ajax.protocols                         :as pr]
            [austinbirch.reactive-entity            :as re]
            [behave-routing.main                    :refer [current-route-order]]
            [behave.schema.core                     :refer [all-schemas]]
            [browser-utils.core                     :refer [download]]
            [absurder-sql.datascript.core           :as d]
            [absurder-sql.datascript.sqlite         :as ds-sqlite]
            [absurder-sql.datascript.storage-async  :as storage-async]
            [absurder-sql.interface                 :as sql]
            [ds-schema-utils.interface              :refer [->ds-schema]]
            [re-frame.core                          :as rf]
            [re-posh.core                           :as rp]
            [promesa.core                           :as p]))

;;; State

(defonce conn (atom nil))
(defonce ^:private worksheet-from-file? (atom false))

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

(defn- new-worksheet-tx [ds-conn ws-name ws-uuid modules]
  (let [version @(rf/subscribe [:state :app-version])
        tx (cond-> {:worksheet/uuid    ws-uuid
                    :worksheet/modules modules
                    :worksheet/created (.now js/Date)}

             version
             (assoc :worksheet/version version)

             ws-name
             (assoc :worksheet/name ws-name))]
    (d/transact ds-conn [tx])))

;;; Conn Initialization

(defn- setup-conn!
  "Wire up a DataScript conn: set the conn atom, register listener, connect
   re-posh and reactive-entity."
  [ds-conn]
  (reset! conn ds-conn)
  (rp/connect! ds-conn)
  (re/init! ds-conn)
  ds-conn)

(defn- reset-conn-state! []
  (reset! conn nil)
  (reset! worksheet-from-file? false))

;;; SQLite Sync (load initial state from AbsurderSQL/IndexedDB)

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
                  (rf/dispatch-sync [:state/set :sync-loaded? true])
                  (rf/dispatch-sync [:state/set :ws-version
                                     @(rf/subscribe [:worksheet/version
                                                     @(rf/subscribe [:worksheet/latest])])])))
        (p/catch (fn [e]
                   (js/console.error "Open worksheet failed:" e))))))

;;; New Worksheet

(defn new-worksheet! [ws-name modules _submodule workflow]
  (let [schema  (->ds-schema all-schemas)
        ws-uuid (str (d/squuid))
        db-name (str "worksheet-" ws-uuid ".db")]
    (rf/dispatch-sync [:state/set :sync-loaded? false])
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
                  (new-worksheet-tx ds-conn ws-name ws-uuid modules)
                  (reset! current-route-order
                          @(rf/subscribe [:wizard/route-order ws-uuid workflow]))
                  (rf/dispatch-sync [:state/set :sync-loaded? true])
                  (rf/dispatch-sync [:navigate (first @current-route-order)])))
        (p/catch (fn [e]
                   (js/console.error "New worksheet failed:" e))))))

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
