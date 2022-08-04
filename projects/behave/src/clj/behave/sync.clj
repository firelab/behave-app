(ns behave.sync
  (:require [datom-compressor.interface :as c]
            [datom-store.main :as s]
            [transport.interface :refer [clj-> mime->type]]))

(defn sync-handler [{:keys [request-method params content-type]}]
  (let [res-type (mime->type content-type)]
    (condp = request-method
      :get
      (let [datoms (if (nil? (:tx params))
                     (s/export-datoms @s/conn)
                     (s/latest-datoms @s/conn (:tx params)))]
        {:status 200
         :body (if (= res-type :msgpack)
                 (c/pack datoms)
                 (clj-> datoms res-type))
         :headers {"Content-Type" content-type}})

      :post
      (let [tx-report (s/transact s/conn (:tx-data params))]
        {:status 201
         :body   (clj-> {:success true} res-type)
         :headers {"Content-Type" content-type}}))))
