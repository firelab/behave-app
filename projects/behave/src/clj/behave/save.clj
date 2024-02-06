(ns behave.save
  (:require [behave.open                 :refer [current-worksheet-atom]]
            [behave.schema.core          :refer [all-schemas]]
            [clojure.java.io             :as io]
            [clojure.walk                :refer [prewalk]]
            [datascript.core             :as d]
            [datascript.storage.sql.core :as storage-sql]
            [datom-store.main            :as s]
            [ds-schema-utils.interface   :refer [->ds-schema]]
            [logging.interface           :refer [log-str]]
            [me.raynes.fs                :as fs]
            [transport.interface         :refer [clj->]])
  (:import (java.sql DriverManager)))

(defn- download-file [filename]
  (let [file (io/file filename)]
    (if (.exists file)
      (do (log-str "Download of " filename " has started.")
          {:status  200
           :headers {"Access-Control-Expose-Headers" "Content-Disposition"
                     "Content-Disposition"           (str "attachment; filename=" filename)}
           :body    file})
      (log-str filename " not found"))))

(defn- datascript-entity->clj-map
  [entity]
  (prewalk
   (fn [x]
     (cond
       (instance? datascript.impl.entity.Entity x) (dissoc (into {} x) :db/id)
       (set? x)                                    (vec x)
       :else                                       x))
   entity))

(defn save-handler
  "Checks if the current worksheet was opened, if so download the worksheet, otherwise generate a
  new sqlite db file with all the datoms related to the worksheet uuid given and download it."
  [{:keys [request-method params accept] :as req}]
  (log-str "Request Received:" (select-keys req [:uri :request-method :params]))
  (if @current-worksheet-atom
    (download-file @current-worksheet-atom)
    (let [{:keys [ws-uuid]} params
          db-file           (fs/temp-file "behave-db" ".sqlite")
          sql-conn          (DriverManager/getConnection (format "jdbc:sqlite:%s" db-file))
          storage           (storage-sql/make sql-conn {:dbtype :sqlite})
          conn              (d/create-conn (->ds-schema all-schemas) {:storage storage})]
      (when (= request-method :post)
        (let [worksheet (->> (d/touch (d/entity @@s/conn [:worksheet/uuid ws-uuid]))
                             datascript-entity->clj-map)]
          (if (seq worksheet)
            (do
              (try
                (d/transact conn [worksheet])
                (catch Exception e
                  (log-str "An error occured when saving a worksheet:" e)
                  (io/delete-file db-file)))
              (storage-sql/close storage)
              (download-file db-file))
            (do
              ;; Clean up if worksheet is empty
              (io/delete-file db-file)
              {:status  400
               :body    (clj-> {:success false} :edn)
               :headers {"Content-Type" accept}})))))))
