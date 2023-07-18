(ns behave.sync
  (:require [datom-compressor.interface :as c]
            [datom-store.main :as s]
            [transport.interface :refer [clj-> mime->type]]
            [triangulum.logging :refer [log-str]])
  (:import  [java.io ByteArrayInputStream]))

(defn sync-handler [{:keys [request-method params accept] :as req}]
  (log-str "Request Received:" (select-keys req [:uri :request-method :params]))
  (let [res-type (or (mime->type accept) :edn)]
    (condp = request-method
      :get
      (let [datoms (if (nil? (:tx params))
                     (s/export-datoms @s/conn)
                     (s/latest-datoms @s/conn (:tx params)))]
        {:status  200
         :body    (if (= res-type :msgpack)
                    (ByteArrayInputStream. (c/pack datoms))
                    (clj-> datoms res-type))
         :headers {"Content-Type" accept}})

      :post
      (let [tx-report (s/sync-datoms s/conn (:tx-data params))]
        {:status  201
         :body    (clj-> {:success true} res-type)
         :headers {"Content-Type" accept}}))))
