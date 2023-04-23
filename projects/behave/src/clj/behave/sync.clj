(ns behave.sync
  (:require [datom-compressor.interface :as c]
            [datom-store.main :as s]
            [transport.interface :refer [clj-> mime->type]]
            [triangulum.logging :refer [log-str]])
  (:import  [java.io ByteArrayInputStream]))

(defn sync-handler [{:keys [request-method params accepts] :as req}]
  (let [res-type (or (mime->type accepts) :edn)]
    (condp = request-method
      :get
      (let [datoms (if (nil? (:tx params))
                     (s/export-datoms @s/conn)
                     (s/latest-datoms @s/conn (:tx params)))]
        {:status  200
         :body    (if (= res-type :msgpack)
                    (ByteArrayInputStream. (c/pack datoms))
                    (clj-> datoms res-type))
         :headers {"Content-Type" accepts}})

      :post
      (let [tx-report (s/sync-datoms s/conn (:tx-data params))]
        {:status  201
         :body    (clj-> {:success true} res-type)
         :headers {"Content-Type" accepts}}))))
