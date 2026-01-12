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
      (p/handle (fn [_result error]
                  (if error
                    (js/alert "Unable to start SQLite DB")
                    (do 
                      (p/resolve! promise nil)
                      (reset! initialized? true)))))))

(defn- ensure-connected! []
  (when-not @initialized?
    (throw (js/Error. "SQLite is not connected. Use `sqlite-js/init!` to initialize."))))

;;; Public API

(defn init!
  "Initializes in-browser SQLite via [AbsurderSQL](https://github.com/npiesco/absurder-sql)."
  []
  (let [load-promise (p/deferred)
        init-promise (p/deferred)]
    (load-external-script! "/js/sqlite.js"
                           #(p/resolve! load-promise nil))
    (go 
      (let [_ (<p! load-promise)])
      (init-sqlite! init-promise))
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

(defn execute!
  "Executes `sql` on a SQLite database connection."
  [^js/sqlite.Database connection sql]
  (ensure-connected!)
  (.execute connection sql))

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
