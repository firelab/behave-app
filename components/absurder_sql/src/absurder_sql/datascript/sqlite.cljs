(ns absurder-sql.datascript.sqlite
  (:require
   [clojure.edn             :as edn]
   [absurder-sql.interface  :as sql]
   [absurder-sql.datascript.protocols :refer [IStorage]]))

;;; State

(defonce ^:private sqlite-db       (atom nil))
#_(defonce ^:private datascript-db   (atom nil))
#_(defonce ^:private datascript-conn (atom nil))

;;; Helpers

(defn- store-impl [conn opts addr+data-seq]
  (let [{:keys [table binary? freeze-str freeze-bytes batch-size]} opts
        upsert (str "insert into " table " (addr, content) values (?, ?) "
                    "on conflict(addr) do update set content = excluded.content")
        promises (for [part (partition-all batch-size addr+data-seq)
                       [addr data] part]
                   (let [content (if binary? (freeze-bytes data) (freeze-str data))]
                     (sql/execute-params! conn upsert [addr content])))]
    (js/Promise.all (to-array promises))))

(defn- restore-impl [conn opts addr]
  (let [sql (str "select content from " (:table opts) " where addr = ?")]
    (-> (sql/select-params conn sql [addr])
        (.then (fn [results]
                 (let [{:keys [binary? thaw-str thaw-bytes]} opts
                       content (-> results first :content)]
                   (when content
                     (if binary?
                       (thaw-bytes content)
                       (thaw-str content)))))))))

(defn- list-impl [conn opts]
  (-> (sql/select conn (str "select addr from " (:table opts)))
      (.then (fn [results]
               (mapv :addr results)))))

(defn- delete-impl [conn opts addr-seq]
  (let [sql      (str "delete from " (:table opts) " where addr = ?")
        promises (for [part (partition-all (:batch-size opts) addr-seq)
                       addr part]
                   (sql/execute-params! conn sql [addr]))]
    (js/Promise.all (to-array promises))))

(defn- ddl [{:keys [table]}]
  (str
   "create table if not exists " table
   " (addr INTEGER primary key, "
   "  content TEXT)"))

(defn- merge-opts [opts]
  (let [opts (merge
              {:freeze-str pr-str
               :thaw-str   edn/read-string
               :batch-size 1000
               :table      "datascript"}
              opts)
        opts (assoc opts
                    :binary? (boolean (and (:freeze-bytes opts) (:thaw-bytes opts))))]
    (merge {:ddl (ddl opts)} opts)))

(deftype SQLiteStorage [conn opts]
  Object
  IStorage
  (-store [_ addr+data-seq]
    (store-impl conn opts addr+data-seq))

  (-restore [_ addr]
    (restore-impl conn opts addr))

  (-list-addresses [_]
    (list-impl conn opts))

  (-delete [_ addr-seq]
    (delete-impl conn opts addr-seq))

  (-sync [_]
    (sql/sync! conn)))

(defn sqlite-store
  "Create new DataScript storage from in-browser SQLite DB.
   
   Optional opts:
   
     :db-name      :: string, default \"datascript.db\"
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
   (sqlite-store conn {}))
  ([conn opts]
   (let [opts (merge-opts opts)]
     (when-not (:skip-ddl opts)
       (sql/execute! conn (:ddl opts)))
     (SQLiteStorage. conn opts))))

(defn close!
  "If storage was created with DataSource that also implements AutoCloseable,
   it will close that DataSource"
  [datasource]
  (let [conn (:conn (meta datasource))]
    (sql/close! conn)
    (reset! sqlite-db nil)
    #_(reset! datascript-conn nil)))

#_(defn init!
    "Initializes DataScript backed by in-browser SQLite DB with:
  - `datoms`  [required] - should end in `.db`
  - `schema`  [required]
  - `db-name` [required] - should end in `.db`"
    [datoms schema db-name]
    (-> (sql/init!)
        (.then (fn [_] (sql/connect! db-name)))
        (.then (fn [conn]
                 (println [:DS-CONN conn])
                 (reset! sqlite-db conn)
                 (let [store        (sqlite-store conn {:db-name db-name})
                       sync-adapter (storage/make-sync-storage-wrapper store {})]
                   (println [:SQLITE-STORE store])
                   (reset! datascript-db (db/init-db datoms schema {:storage sync-adapter}))
                   (reset! datascript-conn (d/conn-from-db @datascript-db))
                   @datascript-conn)))))

#_(comment
    (def schema {:aka {:db/cardinality :db.cardinality/many}})
    (def db (init! [] schema "ds-second.db"))
    (require '[promesa.core :as p])
    (p/wait-all db)
    *1
    (init!)

    (d/transact! conn [{:db/id -1
                        :name   "Maksim"
                        :age    45
                        :aka    ["Max Otto von Stierlitz", "Jack Ryan"]}]))
