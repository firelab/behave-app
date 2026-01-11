(ns absurder-sql.core
  (:require [promesa.core :as p]
            [shadow.dom :as dom]))

;;; State
(defonce ^:private wasm-initialized (atom false))

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

(defn- init-sqlite! []
  (-> (.default js/sqlite)
      (p/handle (fn [_result error]
                  (if error
                    (js/alert "Unable to start SQLite DB")
                    (reset! wasm-initialized true))))))

(defn- ensure-connected! []
  (when-not @wasm-initialized
    (throw (js/Error. "SQLite is not connected. Use `sqlite-js/init!` to initialize."))))

;;; Public API

(defn init! []
  (cond
    (not (script-added?))
    (load-external-script! "/js/sqlite.js" #(do (delay 500) (init!)))

    (not (script-loaded?))
    (do (delay 1000)
        (init!))

    :else
    (init-sqlite!)))

(defn connected? ^bool []
  @wasm-initialized)

(defn connect! ^js/sqlite.Database [db-name]
  (ensure-connected!)
  (js/sqlite.Database.newDatabase db-name))

(defn close! [^js/sqlite.Database connection]
  (ensure-connected!)
  (.close connection))

(defn execute! [^js/sqlite.Database connection sql & [callback]]
  (if callback 
    (p/then (.execute connection sql) callback)
    (.execute connection sql)))

(defn import! [^js/sqlite.Database connection ^js/Uint8Array bytes]
  (.importFromFile connection bytes))

(defn export! [^js/sqlite.Database connection callback]
  (p/then (.exportToFile connection) callback))

(defn download! [^js/sqlite.Database connection db-name]
  (let [callback
        (fn [db-bytes]
          (let [blob (js/Blob. [db-bytes] {:type "application/octet-stream"})
                url  (.createObjectURL js/URL. blob)
                a    (.createElement js/document "a")]
            (set! (.-href a) url)
            (set! (.-download a) db-name)
            (.click a)
            (.revokeObjectURL js/URL url)
            (close! connection)))]
    (export! connection callback)))
