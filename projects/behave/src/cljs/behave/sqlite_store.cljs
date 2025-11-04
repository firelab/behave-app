(ns behave.sqlite-store
  (:require
   [clojure.string     :as str]
   [clojure.edn        :as edn]
   [clojure.string     :as str]
   [datascript.core    :as d]
   [datascript.storage :as storage]
   [promesa.core       :as p]))

;;; State

(defonce sqlite-db (atom nil))
(defonce datasource (atom nil))

;;; Protocol

(defprotocol IStorage
  :extend-via-metadata true
  
  (-store [_ addr+data-seq]
    "Gives you a sequence of `[addr data]` pairs to serialize and store.
     
     `addr`s are 64 bit integers.
     `data`s are clojure-serializable data structure (maps, keywords, lists, integers etc)")
  
  (-restore [_ addr]
    "Read back and deserialize data stored under single `addr`")
  
  (-list-addresses [_]
    "Return seq that lists all addresses currently stored in your storage.
     Will be used during GC to remove keys that are no longer used.")
  
  (-delete [_ addrs-seq]
    "Delete data stored under `addrs` (seq). Will be called during GC"))

;;; Helpers

(defn serializable-datom [^Datom d]
  [(.-e d) (.-a d) (.-v d) (.-tx d)])

(def ^:private root-addr
  0)

(def ^:private tail-addr
  1)

(defonce ^:private *max-addr
  (volatile! 1000000))

(defn- gen-addr []
  (vswap! *max-addr inc))

(defn execute! [conn sql]
  (.execute conn sql))

(defn execute-query [conn sql]
  (.execute conn sql))

(defn upsert-dml [opts]
  (str
    "insert into " (:table opts) " (addr, content) "
    "values (?, ?) "
    "on conflict(addr) do update set content = ?"))

(defn sql-replace [sql value]
  (let [v (if (= (type value) js/String)
            (str "'" value "'")
            value)]
    (str/replace-first sql "?" v)))

(defn store-impl [conn opts addr+data-seq]
  (let [{:keys [table binary? freeze-str freeze-bytes batch-size]} opts
        sql (upsert-dml opts)]
    (doseq [part (partition-all batch-size addr+data-seq)]
      (doseq [[addr data] part]
        (execute! conn 
                  (-> (sql-replace sql addr)
                      (sql-replace sql (freeze-str data))
                      (sql-replace sql (freeze-str content))))))))

(defn restore-impl [conn opts addr on-restore]
  (let [{:keys [table binary? thaw-str thaw-bytes]} opts
        sql (str "select content from " table " where addr = ?")]
    (-> (execute-query (sql-replace sql addr))
        (p/then #(on-restore (thaw-str %))))))

(defn list-impl [conn opts on-list]
  (let [sql (str "select addr from " (:table opts))]

    (-> (execute-query (sql-replace sql addr))
        (p/then #(on-list (map thaw-str %))))))

;;    (loop [res (transient [])]
;;      (if (.next rs)
;;        (recur (conj! res (.getLong rs 1)))
;;        (persistent! res)))))

(defn delete-impl [conn opts addr-seq]
  (let [sql (str "delete from " (:table opts) " where addr = ?")]
    (doseq [part (partition-all (:batch-size opts) addr-seq)]
      (doseq [addr part]
        (.setLong stmt 1 addr)
        (.addBatch stmt))
      (.executeBatch stmt))))

(defn ddl [{:keys [table]}]
  (str
    "create table if not exists " table
    " (addr INTEGER primary key, "
    "  content TEXT)"))

(defn merge-opts [opts]
  (let [opts (merge
               {:freeze-str pr-str
                :thaw-str   edn/read-string
                :batch-size 1000
                :table      "datascript"}
               opts)
        opts (assoc opts
               :binary? (boolean (and (:freeze-bytes opts) (:thaw-bytes opts))))]
    (merge {:ddl (ddl opts)} opts)))

(defn make
  "Create new DataScript storage from Database Connection.
   
   Optional opts:
   
     :batch-size   :: int, default 1000
     :table        :: string, default \"datascript\"
     :ddl          :: custom DDL to create :table. Must have `addr, int` and `content, text` columns
     :freeze-str   :: (fn [any]) -> str, serialize DataScript segments, default pr-str
     :thaw-str     :: (fn [str]) -> any, deserialize DataScript segments, default clojure.edn/read-string
     :freeze-bytes :: (fn [any]) -> bytes, same idea as freeze-str, but for binary serialization
     :thaw-bytes   :: (fn [bytes]) -> any
   
   :freeze-str and :thaw-str, :freeze-bytes and :thaw-bytes should come in pairs, and are mutually exclusive
   (it’s either binary or string serialization)"
  ([conn]
   (make conn {}))
  ([conn opts]
   (let [opts (merge-opts opts)]
     (execute! conn (:ddl opts)))
     (with-meta
       {:conn conn}
       {'datascript.storage/-store
        (fn [_ addr+data-seq]
          (store-impl conn opts addr+data-seq))
        
        'datascript.storage/-restore
        (fn [_ addr]
          (restore-impl conn opts addr))
        
        'datascript.storage/-list-addresses
        (fn [_]
          (list-impl conn opts))
        
        'datascript.storage/-delete
        (fn [_ addr-seq]
          (delete-impl conn opts addr-seq))})))

(defn close!
  "If storage was created with DataSource that also implements AutoCloseable,
   it will close that DataSource"
  [datasource]
  (let [conn (meta datasource :conn)]
    (.close conn)
    (reset! sqlite-db nil)))

(defn init!
  "Initializes SQLite DB with `db-name` (should end in `.db`)"
  [db-name]
  (when @sqlite-db
    (.close @sqlite-db)
    (reset! sqlite-db nil))
  (-> (.default js/sqlite)
      (p/handle (fn [_result error]
                  (if error
                    (js/alert "Unable to start SQLite DB")
                    (js/sqlite.Database.newDatabase db-name))))
      (p/then #(reset! sqlite-db %))
      (p/then #(reset! datasource (make %)))))

(comment 
  (init! "datascript-test.db")

  @sqlite-db
  @datasource
  
  (p/then (.execute @sqlite-db "SELECT * FROM sqlite_master WHERE type='table'")
          #(println %))
  (js/console.log @sqlite-db)

  (require '[me.tonsky.persistent_sorted_set ANode])

  (close! @sqlite-db)
)
