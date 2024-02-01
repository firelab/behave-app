(ns behave.open
  (:require [datom-store.main :as s]
            [datom-compressor.interface :as c]
            [transport.interface :refer [clj-> mime->type]]
            [behave.schema.core :refer [all-schemas]]
            [logging.interface  :refer [log-str]])
  (:import  [java.io ByteArrayInputStream]))

(defn open-handler [{:keys [request-method params accept] :as req}]
  (log-str "Request Received:" (select-keys req [:uri :request-method :params]))
  (let [{:keys [file-path]} params
        res-type            (or (mime->type accept) :edn)]
    (when (= request-method :get)
      (s/release-conn!)
      (s/default-conn all-schemas
                      {:store {:backend :file
                               :path    file-path}})
      (let [datoms (s/export-datoms @s/conn)]
        {:status  200
         :body    (if (= res-type :msgpack)
                    (ByteArrayInputStream. (c/pack datoms))
                    (clj-> datoms res-type))
         :headers {"Content-Type" accept}}))))
