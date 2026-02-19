(ns absurder-sql.core
  (:require [promesa.core :as p]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]))

;;; State
(defonce ^:private sqlite-mod (atom nil))
(defonce ^:private initialized? (atom false))

;;; Helpers

(defn- ensure-connected! []
  (when-not @initialized?
    (throw (js/Error. "SQLite is not connected. Use `sql/init!` to initialize."))))

(defn- dynamic-import
  "Calls the browser's native `import()` from a non-module script context."
  [url]
  ((js/Function. (str "return import('" url "')"))))

(defn- db-class
  "Returns the Database class from the dynamically loaded module."
  []
  (.-Database @sqlite-mod))

;;; Public API

(defn init!
  "Initializes in-browser SQLite via ES module dynamic import."
  []
  (-> (dynamic-import "/js/absurder_sql.js")
      (p/then (fn [mod]
                (reset! sqlite-mod mod)
                ((.-default mod) #js {:module_or_path "/js/absurder_sql_bg.wasm"})))
      (p/then (fn [_]
                (reset! initialized? true)))))

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
  (-> (.newDatabase (db-class) db-name)
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
  "Executes `sql` on a SQLite database connection."
  [^js connection sql]
  (ensure-connected!)
  (.execute connection sql))

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
   Params is a Clojure vector of values; each is converted to a ColumnValue."
  [^js connection sql params]
  (ensure-connected!)
  (.executeWithParams connection sql (to-array (mapv ->column-value params))))

(defn select
  "Executes a query and returns a promise of a vector of Clojure maps."
  [^js connection sql]
  (p/then (execute! connection sql) result->maps))

(defn select-params
  "Executes a parameterized query and returns a promise of a vector of Clojure maps."
  [^js connection sql params]
  (p/then (execute-params! connection sql params) result->maps))

(defn import!
  "Imports `db-bytes` to a SQLite Database."
  [^js connection ^js/Uint8Array db-bytes]
  (ensure-connected!)
  (.importFromFile connection db-bytes))

(defn export!
  "Exports a SQLite Database as bytes to download/share."
  ^js/Uint8Array
  [^js connection]
  (ensure-connected!)
  (.exportToFile connection))

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
