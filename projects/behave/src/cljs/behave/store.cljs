(ns behave.store
  (:require [clojure.set :refer [union]]
            [clojure.edn :as edn]
            [ajax.core :refer [ajax-request]]
            [ajax.edn  :refer [edn-request-format]]
            [ajax.protocols :as pr]
            [datascript.core :as d]
            [re-frame.core :as rf]
            [re-posh.core :as rp]
            [browser-utils.interface :refer [debounce]]
            [datom-compressor.interface :as c]
            [ds-schema-utils.interface :refer [->ds-schema]]
            [datom-utils.interface :refer [split-datom]]
            [behave.schema.core :refer [all-schemas]]
            [austinbirch.reactive-entity :as re]))

;;; State

(defonce conn (atom nil))
(defonce my-txs (atom #{}))
(defonce sync-txs (atom #{}))
(defonce batch (atom []))

;;; Helpers

(defn- txs [datoms]
  (into #{} (map #(nth % 3) datoms)))

(defn- new-datom? [datom]
  (not (contains? (union @my-txs @sync-txs) (nth datom 3))))

(defn- load-data-handler [[ok body]]
  (when ok
    (let [datoms (mapv #(apply d/datom %) (c/unpack body))]
      (swap! sync-txs union (txs datoms))
      (rf/dispatch-sync [:ds/initialize (->ds-schema all-schemas) datoms])
      (rf/dispatch-sync [:state/set :sync-loaded? true]))))

(defn- sync-tx-data-handler [[ok body]]
  (reset! batch [])
  (when ok
    (println ok body)))

(defn load-store! []
  (ajax-request {:uri             "/sync"
                 :handler         load-data-handler
                 :format          {:content-type "application/text" :write str}
                 :response-format {:description  "ArrayBuffer"
                                   :type         :arraybuffer
                                   :content-type "application/msgpack"
                                   :read         pr/-body}}))

(defn- batch-sync-tx-data []
  (when-not (empty? @batch)
    (ajax-request {:uri             "/sync"
                   :params          {:tx-data @batch}
                   :method          :post
                   :handler         sync-tx-data-handler
                   :format          (edn-request-format)
                   :response-format {:description  "EDN"
                                     :format       :text
                                     :type         :text
                                     :content-type "application/edn"
                                     :read         edn/read-string}})))

(def ^:private debounced-batch-sync-tx-data (debounce batch-sync-tx-data 2000))

(defn sync-tx-data [{:keys [tx-data]}]
  (let [datoms (->> tx-data (filter new-datom?) (mapv split-datom))]
    (when-not (empty? datoms)
      (swap! my-txs union (txs datoms))
      (swap! batch concat datoms)
      (debounced-batch-sync-tx-data))))

(defn apply-latest-datoms [[ok body]]
  (when ok
    (let [datoms (->> (c/unpack body)
                      (filter new-datom?)
                      (map (partial apply d/datom)))]
      (when (seq datoms)
        (swap! sync-txs union (txs datoms))
        (d/transact @conn datoms)))))

(defn sync-latest-datoms! []
  (ajax-request {:uri "/sync"
                 :params {:tx (:max-tx @@conn)}
                 :method :get
                 :handler apply-latest-datoms
                 :format {:content-type "plain/text" :write str}
                 :response-format {:description "ArrayBuffer"
                                   :type :arraybuffer
                                   :content-type "application/msgpack"
                                   :read pr/-body}}))

;;; Public Fns

(defn init! [{:keys [datoms schema]}]
  (if @conn
   @conn
   (do
     (reset! conn (d/conn-from-datoms datoms schema))
     (d/listen! @conn :sync-tx-data sync-tx-data)
     (rp/connect! @conn)
     (re/init! @conn)
     #_(js/setInterval sync-latest-datoms! 5000)
     @conn)))

;;; Effects

(rf/reg-fx :ds/init init!)

;;; Events

(rf/reg-event-fx
  :ds/initialize
  (fn [_ [_ schema datoms]]
    {:ds/init {:datoms datoms :schema schema}}))

(rp/reg-event-ds
  :ds/transact
  (fn [_ [_ tx-data]]
    (first tx-data)))
