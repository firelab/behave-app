(ns absurder-sql.core
  (:require [promesa.core :as p]
            [shadow.dom :as dom]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]))

;;; State
(defonce ^:private initialized? (atom false))

;;; Helpers

(defn- script-added? []
   (dom/query-one "script[src='/js/sqlite.js']"))

(defn- script-loaded? []
  (.-sqlite js/window))

(defn- load-external-script! [url callback]
  (let [script (.createElement js/document "script")]
    (aset script "type" "text/javascript")
    (aset script "src" url)
    (aset script "onload" callback)
    (.appendChild (.-body js/document) script)))

(defn- init-sqlite! [& [promise]]
  (-> (.default js/sqlite)
      (p/handle (fn [result error]
                  (println "Starting up!")
                  (if error
                    (js/alert "Unable to start SQLite DB")
                    (do 
                      (reset! initialized? true)
                      (p/resolve! promise result)))))))

(defn- ensure-connected! []
  (when-not @initialized?
    (throw (js/Error. "SQLite is not connected. Use `sqlite-js/init!` to initialize."))))

;;; Public API

(defn init!
  "Initializes in-browser SQLite via [AbsurderSQL](https://github.com/npiesco/absurder-sql)."
  []
  (let [load-promise (p/deferred)
        init-promise (p/deferred)]
    (println "Loading...")
    (load-external-script! "/js/sqlite.js" #(do
                                              (println "SQLite JS Loaded!")
                                              (p/resolve! load-promise nil)))
    (go 
      (let [_ (<p! load-promise)]
        (println "Initializing...")
        (init-sqlite! init-promise)))
    init-promise))

(defn connected?
  "Returns whether the in-browser SQLite is initialized."
  ^bool
  []
  @initialized?)

(defn connect!
  "Creates a new in-browser SQLite database connection to `db-name`."
  ^js/sqlite.Database
  [db-name]
  (ensure-connected!)
  (js/sqlite.Database.newDatabase db-name))

(defn close!
  "Closes an existing SQLite database connection."
  [^js/sqlite.Database connection]
  (ensure-connected!)
  (.close connection))

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
  [^js/sqlite.Database connection sql]
  (ensure-connected!)
  (.execute connection sql))

(defn select
  "Executes a query and returns a promise of a vector of Clojure maps."
  [^js/sqlite.Database connection sql]
  (p/then (execute! connection sql) result->maps))

(defn import!
  "Imports `db-bytes` to a SQLite Database."
  [^js/sqlite.Database connection ^js/Uint8Array db-bytes]
  (ensure-connected!)
  (.importFromFile connection db-bytes))

(defn export!
  "Exports a SQLite Database as bytes to download/share."
  ^js/Uint8Array
  [^js/sqlite.Database connection]
  (ensure-connected!)
  (.exportToFile connection))

(defn download!
  "Downloads a SQLite Database."
  [^js/sqlite.Database connection db-name]
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
