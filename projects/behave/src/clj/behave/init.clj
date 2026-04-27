(ns behave.init
  (:require [behave.open                :refer [current-worksheet-atom]]
            [behave.store               :as store]
            [clojure.java.io            :as io]
            [config.interface           :refer [get-config load-config]]
            [datom-compressor.interface :as c]
            [datom-store.main           :as s]
            [file-utils.interface       :refer [os-path]]
            [logging.interface          :refer [log-str]]
            [transport.interface        :refer [clj-> mime->type]])
  (:import (java.io ByteArrayInputStream)))

;;; Client Tracking

(def active-clients
  "Number of connected browser windows/tabs."
  (atom 0))

(defn register-client!
  "Increments the active client count. Call on each /api/init."
  []
  (let [n (swap! active-clients inc)]
    (log-str [:CLIENTS :register n])))

;;; Helpers

(defn- resource [s]
  (.getResource (ClassLoader/getSystemClassLoader) s))

(defn init! []
  (load-config (resource "config.edn"))
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
      (register-client!)
      (s/release-conn!)
      (reset! current-worksheet-atom nil)
      (init!)
      (let [datoms (s/export-datoms @s/conn)]
        {:status  200
         :body    (if (= res-type :msgpack)
                    (ByteArrayInputStream. (c/pack datoms))
                    (clj-> datoms res-type))
         :headers {"Content-Type" accept}}))))
