(ns behave-cms.store
  (:require [clojure.set                 :refer [union]]
            [ajax.core                   :refer [ajax-request]]
            [ajax.edn                    :refer [edn-request-format
                                                 edn-response-format]]
            [ajax.protocols              :as pr]
            [datascript.core             :as d]
            [re-frame.core               :as rf]
            [re-posh.core                :as rp]
            [datom-compressor.interface  :as c]
            [datom-utils.interface       :refer [db-attrs
                                                 datoms->map
                                                 safe-deref
                                                 split-datom]]
            [ds-schema-utils.interface   :refer [->ds-schema]]
            [behave.schema.core          :refer [all-schemas]]
            [behave-cms.config           :refer [get-config]]
            [austinbirch.reactive-entity :as re]))

;;; State

(defonce conn (atom nil))
(defonce my-txs (atom #{}))
(defonce sync-txs (atom #{}))

;;; Helpers
(defn- txs [datoms]
  (into #{} (map #(nth % 3) datoms)))

(defn- new-datom? [datom]
  (not (contains? (union @my-txs @sync-txs) (nth datom 3))))

(defn- load-data-handler [[ok body]]
  (when ok
    (let [raw-datoms (c/unpack body)
          bad-attrs  (db-attrs raw-datoms)
          datoms     (remove #(bad-attrs (second %)) raw-datoms)
          datoms-map (datoms->map datoms)]
      (swap! sync-txs union (txs datoms))
      (rf/dispatch-sync [:ds/initialize (->ds-schema all-schemas) datoms-map]))))

(defn load-store!
  "Loads/syncs the Datom store from datoms the backend."
  []
  (ajax-request {:uri             (str "/sync?auth-token="
                                       (get-config :secret-token))
                 :handler         load-data-handler
                 :format          {:content-type "application/text" :write str}
                 :response-format {:description  "ArrayBuffer"
                                   :type         :arraybuffer
                                   :content-type "application/msgpack"
                                   :read         pr/-body}}))

(defn sync-tx-data [{:keys [tx-data]}]
  (let [datoms (->> tx-data (filter new-datom?) (mapv split-datom))]
    (when-not (empty? datoms)
      (swap! my-txs union (txs datoms))
      (ajax-request {:uri             (str "/sync?auth-token="
                                           (get-config :secret-token))
                     :params          {:tx-data datoms}
                     :method          :post
                     :handler         println
                     :format          (edn-request-format)
                     :response-format (edn-response-format)}))))

(defn apply-latest-datoms [[ok body]]
  (when ok
    (let [datoms (->> (c/unpack body)
                      (filter new-datom?)
                      (map (partial apply d/datom)))]
      (when (seq datoms)
        (swap! sync-txs union (txs datoms))
        (d/transact @conn datoms)))))

(defn sync-latest-datoms! []
  (ajax-request {:uri             "/sync"
                 :params          {:tx (:max-tx @@conn)}
                 :method          :get
                 :handler         apply-latest-datoms
                 :format          {:content-type "plain/text" :write str}
                 :response-format {:description  "ArrayBuffer"
                                   :type         :arraybuffer
                                   :content-type "application/msgpack"
                                   :read         pr/-body}}))

;;; Public Fns

(defn init! [{:keys [datoms schema]}]
  (if @conn
    @conn
    (do
      (reset! conn (d/create-conn schema))
      (d/transact @conn datoms)
      (d/listen! @conn :sync-tx-data sync-tx-data)
      (rp/connect! @conn)
      (re/init! @conn)
      #_(js/setInterval sync-latest-datoms! 5000)
      (rf/dispatch [:state/set-state :loaded? true])
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

(defn entity-from-uuid [db uuid]
  (d/entity (safe-deref db) [:bp/uuid uuid]))
