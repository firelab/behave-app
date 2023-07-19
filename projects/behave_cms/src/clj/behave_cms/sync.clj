(ns behave-cms.sync
  (:require [datom-compressor.interface :as c]
            [datom-store.main           :as s]
            [data-utils.interface       :refer [parse-int]]
            [transport.interface        :refer [clj-> mime->type]])
  (:import  [java.io ByteArrayInputStream]))

(defn sync-handler [{:keys [request-method params accept] :as req}]
  (let [res-type (or (mime->type accept) :edn)]
    (condp = request-method
      :get
      (let [tx     (:tx params)
            datoms (if (nil? tx)
                     (s/export-datoms @s/conn)
                     (s/latest-datoms @s/conn (parse-int tx)))]
        {:status  200
         :body    (if (= res-type :msgpack)
                    (ByteArrayInputStream. (c/pack datoms))
                    (clj-> datoms res-type))
         :headers {"Content-Type" accept}})

      :post
      (let [_ (s/sync-datoms s/conn (:tx-data params))]
        {:status  201
         :body    (clj-> {:success true} res-type)
         :headers {"Content-Type" accept}}))))
