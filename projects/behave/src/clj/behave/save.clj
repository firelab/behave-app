(ns behave.save
  (:require [datom-store.main :as s]
            [transport.interface :refer [clj->]]
            [behave.schema.core :refer [all-schemas]]
            [logging.interface  :refer [log-str]]
            [clojure.walk :refer [prewalk]]
            [datascript.core             :as d]
            [me.raynes.fs                :as fs]
            [ds-schema-utils.interface   :refer [->ds-schema]]
            [datascript.storage.sql.core :as storage-sql]
            [clojure.java.io :as io])
  (:import [java.sql DriverManager]))

(defn- clean-entity
  [entity]
  (prewalk
   (fn [x]
     (cond
       (instance? datascript.impl.entity.Entity x) (dissoc (into {} x) :db/id)
       (set? x)                                    (vec x)
       :else                                       x))
   entity))

(defn save-handler [{:keys [request-method params accept] :as req}]
  (log-str "Request Received:" (select-keys req [:uri :request-method :params]))
  (let [{:keys [ws-uuid file-path]} params
        db-file                     (fs/expand-home file-path)
        sql-conn                    (DriverManager/getConnection (format "jdbc:sqlite:%s" db-file))
        storage                     (storage-sql/make sql-conn {:dbtype :sqlite})
        conn                        (d/create-conn (->ds-schema all-schemas) {:storage storage})]
    (when (= request-method :post)
      (let [worksheet (->> (d/touch (d/entity @@s/conn [:worksheet/uuid ws-uuid]))
                           clean-entity)]
        (if (seq worksheet)
          (do
            (try
              (d/transact conn [worksheet])
              (catch Exception e
                (log-str "An error occured when saving a worksheet:" e)
                (io/delete-file db-file)))
            (storage-sql/close storage)
            {:status  201
             :body    (clj-> {:success true} :edn)
             :headers {"Content-Type" accept}})
          (do
            ;; worksheet is empty
            (io/delete-file db-file)
            {:status  400
             :body    (clj-> {:success false} :edn)
             :headers {"Content-Type" accept}}))))))
