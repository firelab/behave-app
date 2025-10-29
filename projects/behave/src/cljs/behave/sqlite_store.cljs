(ns behave.sqlite-store
  (:require
   [clojure.string     :as str]
   [clojure.edn        :as edn]
   [clojure.string     :as str]
   [datascript.core    :as d]
   [datascript.storage :as storage]
   [promesa.core       :as p]))

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

(defn restore-impl [^Connection conn opts addr]
  (let [{:keys [table binary? thaw-str thaw-bytes]} opts
        sql (str "select content from " table " where addr = ?")]
    (let [results (execute-query (sql-replace sql addr))]
      (thaw-str (p/then ))))))))

(defn list-impl [^Connection conn opts]
  (with-open [stmt (.prepareStatement conn (str "select addr from " (:table opts)))
              rs   (.executeQuery stmt)]
    (loop [res (transient [])]
      (if (.next rs)
        (recur (conj! res (.getLong rs 1)))
        (persistent! res)))))

(defn delete-impl [^Connection conn opts addr-seq]
  (with-tx conn
    (with-open [stmt (.prepareStatement conn (str "delete from " (:table opts) " where addr = ?"))]
      (doseq [part (partition-all (:batch-size opts) addr-seq)]
        (doseq [addr part]
          (.setLong stmt 1 addr)
          (.addBatch stmt))
        (.executeBatch stmt)))))

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
  "Create new DataScript storage from javax.sql.DataSource.
   
   Mandatory opts:
   
     :dbtype       :: keyword, one of :h2, :mysql, :postgresql or :sqlite
   
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
  ([datasource]
   {:pre [(instance? DataSource datasource)]}
   (make datasource {}))
  ([datasource opts]
   (let [opts (merge-opts opts)]
     (with-conn [conn datasource]
       (execute! conn (:ddl opts)))
     (with-meta
       {:datasource datasource}
       {'datascript.storage/-store
        (fn [_ addr+data-seq]
          (with-conn [conn datasource]
            (store-impl conn opts addr+data-seq)))
        
        'datascript.storage/-restore
        (fn [_ addr]
          (with-conn [conn datasource]
            (restore-impl conn opts addr)))
        
        'datascript.storage/-list-addresses
        (fn [_]
          (with-conn [conn datasource]
            (list-impl conn opts)))
        
        'datascript.storage/-delete
        (fn [_ addr-seq]
          (with-conn [conn datasource]
            (delete-impl conn opts addr-seq)))}))))

(defn close
  "If storage was created with DataSource that also implements AutoCloseable,
   it will close that DataSource"
  [storage]
  (let [datasource (:datasource storage)]
    (when (instance? AutoCloseable datasource)
      (.close ^AutoCloseable datasource))))

(defmacro with-lock [lock & body]
  `(let [^Lock lock# ~lock]
     (try
       (.lock lock#)
       ~@body
       (finally
         (.unlock lock#)))))

(defrecord Pool [*atom ^Lock lock ^Condition condition ^DataSource datasource opts]
  AutoCloseable
  (close [_]
    (let [[{:keys [taken free]} _] (swap-vals! *atom #(-> % (update :taken empty) (update :idle empty)))]
      (doseq [conn (concat free taken)]
        (try
          (.close ^Connection conn)
          (catch Exception e
            (.printStackTrace e))))))
  
  DataSource
  (getConnection [this]
    (let [^Connection conn (with-lock lock
                             (loop []
                               (let [atom @*atom]
                                 (cond
                                   ;; idle connections available
                                   (> (count (:idle atom)) 0)
                                   (let [conn (peek (:idle atom))]
                                     (swap! *atom #(-> %
                                                     (update :taken conj conn)
                                                     (update :idle pop)))
                                     conn)
                       
                                   ;; has space for new connection
                                   (< (count (:taken atom)) (:max-conn opts))
                                   (let [conn (.getConnection datasource)]
                                     (swap! *atom update :taken conj conn)
                                     conn)
                       
                                   ;; already at limit
                                   :else
                                   (do
                                     (.await condition)
                                     (recur))))))
          *closed? (volatile! false)]
      (Proxy/newProxyInstance
        (.getClassLoader Connection)
        (into-array Class [Connection])
        (reify InvocationHandler
          (invoke [this proxy method args]
            (let [method ^Method method]
              (case (.getName method)
                "close"
                (do
                  (when-not (.getAutoCommit conn)
                    (.rollback conn)
                    (.setAutoCommit conn true))
                  (vreset! *closed? true)
                  (with-lock lock
                    (if (< (count (:idle @*atom)) (:max-idle-conn opts))
                      ;; normal return to pool
                      (do
                        (swap! *atom #(-> %
                                        (update :taken disj conn)
                                        (update :idle conj conn)))
                        (.signal condition))
                      ;; excessive idle conn
                      (do
                        (swap! *atom update :taken disj conn)
                        (.close conn))))
                  nil)
                
                "isClosed"
                (or @*closed? (.invoke method conn args))
                
                ;; else
                (.invoke method conn args)))))))))

(defn pool
  "Simple connection pool.
   
   Accepts javax.sql.DataSource, returns javax.sql.DataSource implementation
   that creates java.sql.Connection on demand, up to :max-conn, and keeps up
   to :max-idle-conn when no demand.
   
   Implements AutoCloseable, which closes all pooled connections."
  (^DataSource [datasource]
    (pool datasource {}))
  (^DataSource [datasource opts]
    {:pre [(instance? DataSource datasource)]}
    (let [lock (ReentrantLock.)]
      (Pool.
        (atom {:taken #{}
               :idle  []})
        lock
        (.newCondition lock)
        datasource
        (merge
          {:max-idle-conn 4
           :max-conn      10}
          opts)))))
