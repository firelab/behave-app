(ns behave.init
  (:require [datom-compressor.interface :as c]
            [datom-store.main :as s]
            [config.interface             :refer [get-config load-config]]
            [behave.store                 :as store]
            [clojure.java.io              :as io]
            [file-utils.interface         :refer [os-path]]
            [transport.interface :refer [clj-> mime->type]]
            [logging.interface  :refer [log-str]])
  (:import  [java.io ByteArrayInputStream]))

(defn init! []
  (load-config (io/resource "config.edn"))
  (let [config (update-in (get-config :database :config)
                          [:store :path]
                          os-path)]
    (log-str "LOADED CONFIG" (get-config :database :config))
    (io/make-parents (get-in config [:store :path]))
    (store/connect! config)))

(defn init-handler [{:keys [request-method accept] :as req}]
  (log-str "Request Received:" (select-keys req [:uri :request-method :params]))
  (let [res-type (or (mime->type accept) :edn)]
    (when (= request-method :get)
      (s/release-conn!)
      (init!)
      (let [datoms (s/export-datoms @s/conn)]
        {:status  200
         :body    (if (= res-type :msgpack)
                    (ByteArrayInputStream. (c/pack datoms))
                    (clj-> datoms res-type))
         :headers {"Content-Type" accept}}))))
