(ns absurder-sql.client
  (:require [absurder-sql.core :as sql]
            [promesa.core :as p]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]))

(defonce ^:private db (atom nil))

(defn- log! [msg]
  (let [el (.getElementById js/document "log")]
    (set! (.-textContent el)
          (str (.-textContent el) msg "\n"))
    (set! (.-scrollTop el) (.-scrollHeight el))))

(defn- set-status! [s]
  (set! (.-textContent (.getElementById js/document "status"))
        (str "Status: " s)))

(defn- render-results! [rows]
  (let [el (.getElementById js/document "results")]
    (set! (.-innerHTML el) "")
    (when (and rows (pos? (.-length rows)))
      (let [cols  (js/Object.keys (aget rows 0))
            thead (.createElement js/document "thead")
            tbody (.createElement js/document "tbody")
            tr    (.createElement js/document "tr")]
        (.forEach cols
                  (fn [col]
                    (let [th (.createElement js/document "th")]
                      (set! (.-textContent th) col)
                      (.appendChild tr th))))
        (.appendChild thead tr)
        (.appendChild el thead)
        (.forEach rows
                  (fn [row]
                    (let [tr (.createElement js/document "tr")]
                      (.forEach cols
                                (fn [col]
                                  (let [td (.createElement js/document "td")]
                                    (set! (.-textContent td) (aget row col))
                                    (.appendChild tr td))))
                      (.appendChild tbody tr))))
        (.appendChild el tbody)))))

(defn- exec-sql! []
  (let [text (.-value (.getElementById js/document "sql-input"))
        stmts (remove empty? (map clojure.string/trim (.split text ";")))]
    (go
      (try
        (doseq [stmt stmts]
          (log! (str "> " stmt))
          (let [result (<p! (sql/execute! @db (str stmt ";")))]
            (when (and result (.-length result) (pos? (.-length result)))
              (render-results! result)
              (log! (str "  " (.-length result) " row(s)")))))
        (log! "Done.")
        (catch :default e
          (log! (str "ERROR: " (.-message e))))))))

(defn ^:export init []
  (set-status! "initializing SQLite...")
  (println "Initializing AbsurderSQL...")
  (-> (sql/init!)
      (p/then (fn [_]
                (println "Initialized promise resolved.")
                (set-status! "connected")
                (log! "SQLite ready.")
                (-> (sql/connect! "test.db")
                    (p/then (fn [conn]
                              (reset! db conn))))
                (log! "Database 'test.db' created.")))
      (p/catch (fn [e]
                 (println "Error" e)

                 (set-status! "error")
                 (log! (str "Init failed: " (.-message e))))))

  (.addEventListener (.getElementById js/document "btn-exec")
                     "click" (fn [_] (exec-sql!)))

  (.addEventListener (.getElementById js/document "btn-clear")
                     "click" (fn [_]
                               (set! (.-textContent (.getElementById js/document "log")) "")
                               (set! (.-innerHTML (.getElementById js/document "results")) "")))

  (.addEventListener (.getElementById js/document "btn-export")
                     "click" (fn [_]
                               (when @db
                                 (sql/download! @db "test.db")
                                 (log! "Exporting database...")))))
