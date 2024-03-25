(ns behave-cms.sync
  (:require [datom-compressor.interface :as c]
            [datomic-store.main         :as s]
            [data-utils.interface       :refer [parse-int]]
            [transport.interface        :refer [clj-> mime->type]]
            [behave-cms.views           :refer [data-response]]
            [behave.schema.core         :refer [all-schemas]])
  (:import  [java.io ByteArrayInputStream]))

(defn sync-handler [{:keys [request-method params accept session]}]
  (let [res-type (or (mime->type accept) :edn)]
    (condp = request-method
      :get
      (let [tx     (:tx params)
            datoms (if (nil? tx)
                     (s/export-datoms s/datomic-conn true all-schemas)
                     (s/latest-datoms s/datomic-conn (parse-int tx) true all-schemas))]
        {:status  200
         :body    (if (= res-type :msgpack)
                    (ByteArrayInputStream. (c/pack datoms))
                    (clj-> datoms res-type))
         :headers {"Content-Type" accept}})

      :post
      (if (:user-uuid session)
        (let [_ (s/sync-datoms s/datomic-conn (:tx-data params) all-schemas)]
          {:status  201
           :body    (clj-> {:success true} res-type)
           :headers {"Content-Type" accept}})
        (data-response {:error (str "You must be logged in to perform that operation.")} {:status 403})))))
