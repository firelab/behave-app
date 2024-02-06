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
            [clojure.java.io :as io]
            [behave.open :refer [current-worksheet-atom]])
  (:import [java.sql DriverManager]))

(defn download-file [filename]
  (let [file (io/file filename)]
    (if (.exists file)
      (do (log-str "Download of " filename " has started.")
          {:status  200
           :headers {"Access-Control-Expose-Headers" "Content-Disposition"
                     "Content-Disposition"           (str "attachment; filename=" filename)}
           :body    file})
      (log-str filename " not found"))))

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
  (if @current-worksheet-atom
    (download-file @current-worksheet-atom)
    (let [{:keys [ws-uuid]} params
          db-file           (fs/temp-file "behave-db" ".sqlite")
          sql-conn          (DriverManager/getConnection (format "jdbc:sqlite:%s" db-file))
          storage           (storage-sql/make sql-conn {:dbtype :sqlite})
          conn              (d/create-conn (->ds-schema all-schemas) {:storage storage})]
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
              (download-file db-file))
            (do
              ;; worksheet is empty
              (io/delete-file db-file)
              {:status  400
               :body    (clj-> {:success false} :edn)
               :headers {"Content-Type" accept}})))))))

;; Scenarios
;; 1. Saving an already opened worksheet
;;    -Yes
;;      1. Initiate download db of current-worksheet-atom
;;    -No
;;      1. Create new db file
;;      2. Connect to new db
;;      3. Lookup worksheet datoms
;;      4. Transact to db
;;      5. Initiate download db
