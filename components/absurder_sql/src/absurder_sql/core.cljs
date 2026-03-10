(ns absurder-sql.core
  (:require
   ["../js/absurder_sql/index.js" :as sqlite :refer [Database]]
   [promesa.core :as p]
   [cljs.core.async :refer [<! go go-loop put! chan sliding-buffer]]
   [cljs.core.async.interop :refer-macros [<p!]]))

;;; State
(defonce ^:private initialized? (atom false))

;;; Helpers

(defn- ensure-connected! []
  (when-not @initialized?
    (throw (js/Error. "SQLite is not connected. Use `sql/init!` to initialize."))))

;;; Auto-sync — flushes VFS to IndexedDB after 100 writes or 5s timeout
;;  A sliding-buffer(1) channel per connection serializes .sync calls:
;;  the go-loop takes one request at a time, and duplicates are coalesced.

(defonce ^:private sync-state (atom {}))
;; Per-connection map: {conn {:count n, :timer-id id, :chan ch}}

(def ^:private sync-batch-size 100)
(def ^:private sync-timeout-ms 2000)

(defn- sync-chan [^js connection]
  (or (get-in @sync-state [connection :chan])
      (let [ch (chan (sliding-buffer 1))]
        (go-loop []
          (when (<! ch)
            (try
              (<p! (.sync connection))
              (catch :default err
                (js/console.error "sync failed:" err)))
            (recur)))
        (swap! sync-state assoc-in [connection :chan] ch)
        ch)))

(defn- do-sync! [^js connection]
  (when-let [tid (get-in @sync-state [connection :timer-id])]
    (js/clearTimeout tid))
  (swap! sync-state update connection assoc :timer-id nil :count 0)
  (put! (sync-chan connection) true))

(defn- schedule-sync! [^js connection]
  (when-not (get-in @sync-state [connection :timer-id])
    (let [tid (js/setTimeout #(do-sync! connection) sync-timeout-ms)]
      (swap! sync-state assoc-in [connection :timer-id] tid))))

(defn- track-execution! [^js connection promise]
  (schedule-sync! connection)
  (p/then promise
          (fn [result]
            (let [n (-> (swap! sync-state update-in [connection :count] (fnil inc 0))
                        (get-in [connection :count]))]
              (when (>= n sync-batch-size)
                (do-sync! connection)))
            result)))

;;; Public API

(defn init!
  "Initializes in-browser SQLite via ES module dynamic import.
   Safe to call multiple times; returns immediately if already initialized."
  []
  (if @initialized?
    (p/resolved true)
    (-> (.load sqlite #js {:module_or_path "/js/absurder_sql_bg.wasm"})
        (p/then (fn [_]
                  (reset! initialized? true))))))

(defn connected?
  "Returns whether the in-browser SQLite is initialized."
  ^bool
  []
  @initialized?)

(defn connect!
  "Creates a new in-browser SQLite database connection to `db-name`.
   Enables Write-Ahead Log (WAL) mode by default."
  ^js
  [db-name]
  (ensure-connected!)
  (-> (.newDatabase Database db-name)
      (p/then (fn [db]
                (.allowNonLeaderWrites db true)
                db))))

(defn close!
  "Closes an existing SQLite database connection."
  [^js connection]
  (ensure-connected!)
  (.close connection))

(defn sync!
  "Flushes in-memory VFS blocks to IndexedDB."
  [^js connection]
  (ensure-connected!)
  (.sync connection))

(defn- row->map
  "Transforms a JS row `{values: [{type, value}, ...]}` into a Clojure map
   keyed by the corresponding column names."
  [columns row]
  (let [values (.-values row)]
    (persistent!
     (reduce (fn [acc i]
               (let [col (keyword (aget columns i))
                     v   (.-value (aget values i))]
                 (assoc! acc col v)))
             (transient {})
             (range (.-length columns))))))

(defn- result->maps
  "Transforms the JS result object from `execute!` into a vector of Clojure maps."
  [result]
  (let [columns (.-columns result)
        rows    (.-rows result)]
    (mapv (partial row->map columns) (array-seq rows))))

(defn execute!
  "Executes `sql` on a SQLite database connection.
   Automatically schedules a sync to IndexedDB after a batch of writes."
  [^js connection sql]
  (ensure-connected!)
  (track-execution!
   connection
   (.execute connection sql)))

(defn- ->column-value
  "Convert a Clojure value to a serde-compatible ColumnValue object.
   The WASM binding expects adjacently tagged enums: {type, value}."
  [v]
  (cond
    (nil? v)     #js {:type "Null"}
    (string? v)  #js {:type "Text"    :value v}
    (int? v)     #js {:type "Integer" :value v}
    (float? v)   #js {:type "Real"    :value v}
    :else        #js {:type "Text"    :value (str v)}))

(defn execute-params!
  "Executes parameterized `sql` with `params` on a SQLite database connection.
   Params is a Clojure vector of values; each is converted to a ColumnValue.
   Automatically schedules a sync to IndexedDB after a batch of writes."
  [^js connection sql params]
  (ensure-connected!)
  (track-execution!
   connection
   (.executeWithParams connection sql (to-array (mapv ->column-value params)))))

(defn select
  "Executes a query and returns a promise of a vector of Clojure maps."
  [^js connection sql]
  (p/then (.execute connection sql) result->maps))

(defn select-params
  "Executes a parameterized query and returns a promise of a vector of Clojure maps."
  [^js connection sql params]
  (p/then (.executeWithParams connection sql (to-array (mapv ->column-value params))) result->maps))

(defn import!
  "Imports `db-bytes` to a SQLite Database."
  [^js connection ^js/Uint8Array db-bytes]
  (ensure-connected!)
  (.importFromFile connection db-bytes))

(defn export!
  "Exports a SQLite Database as bytes to download/share.
   Forces a sync to IndexedDB before exporting to ensure data is flushed."
  ^js/Uint8Array
  [^js connection]
  (ensure-connected!)
  (p/then (.sync connection)
          (fn [_] (.exportToFile connection))))

(defn download!
  "Downloads a SQLite Database."
  [^js connection db-name]
  (ensure-connected!)
  (go
    (let [db-bytes (<p! (export! connection))
          blob     (js/Blob. [db-bytes] {:type "application/octet-stream"})
          url      (.createObjectURL js/URL. blob)
          a        (.createElement js/document "a")]
      (set! (.-href a) url)
      (set! (.-download a) db-name)
      (.click a)
      (.revokeObjectURL js/URL url)
      (close! connection))))
