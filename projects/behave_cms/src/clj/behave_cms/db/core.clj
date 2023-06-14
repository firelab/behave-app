(ns behave-cms.db.core
  (:require [next.jdbc            :as jdbc]
            [next.jdbc.result-set :as rs]
            [config.interface     :as cfg]
            [honey.sql            :as sql])
  (:import [java.util UUID]))

(defonce conn (atom nil))

(defn- pg-db []
  (if @conn
    @conn
    (reset! conn (-> (cfg/get-config :database)
                     (dissoc :config)
                     (merge {:dbtype "postgresql" :reWriteBatchedInserts true})
                     (jdbc/get-datasource)))))

(defn ->uuid [s]
  (if (uuid? s)
    s
    (UUID/fromString s)))

(defn exec! [sqlmap & [opts]]
  (when (map? sqlmap)
    (jdbc/execute! (jdbc/get-datasource (pg-db))
                   (sql/format sqlmap)
                   (merge {:builder-fn rs/as-unqualified-lower-maps} opts))))

(defn exec-one! [sqlmap & [opts]]
  (when (map? sqlmap)
    (jdbc/execute-one! (jdbc/get-datasource (pg-db))
                       (sql/format sqlmap)
                       (merge {:builder-fn rs/as-unqualified-lower-maps} opts))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Simple CRUD Operations
(defn count-entities [table-name]
  (sql/format {:select [:%count.*] :from table-name}))

(defn get-entities [table-name]
  (exec! {:select [:*] :from table-name}))

(defn get-entity [table-name {:keys [uuid]}]
  (exec-one! {:select [:*]
              :from   table-name
              :where  [:= :uuid (->uuid uuid)]}))

(defn create-entity! [table-name data]
  (exec-one! {:insert-into table-name
              :values      [data]}
             {:return-keys true}))

(defn update-entity! [table-name {:keys [uuid] :as new-entity}]
  (exec-one! {:update table-name
              :set    (dissoc new-entity :uuid)
              :where  [:= :uuid (->uuid uuid)]}
             {:return-keys true}))

(defn delete-entity! [table-name {:keys [uuid]}]
  (exec-one! {:delete-from table-name
              :where       [:= :uuid (->uuid uuid)]}
             {:return-keys true}))
