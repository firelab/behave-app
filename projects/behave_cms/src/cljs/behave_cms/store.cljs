(ns behave-cms.store
  (:require [clojure.core.async         :refer [go <!]]
            [cljs.core.async.interop    :refer-macros [<p!]]
            [clojure.set                :refer [union]]
            [datascript.core            :as d]
            [re-frame.core               :as rf]
            [re-posh.core               :as rp]
            [datom-compressor.interface :as c]
            [datom-utils.interface      :refer [split-datoms]]
            [ds-schema-utils.interface  :refer [->ds-schema]]
            [behave.schema.core         :refer [all-schemas]]
            [behave-cms.utils           :as u]))

;;; State

(defonce conn (atom nil))

;;; Sync Helpers

(defonce my-txs (atom #{}))
(defonce sync-txs (atom #{}))

(defn- txs [datoms]
  (into #{} (map #(nth % 3) datoms)))

(defn- new-datom? [datom]
  (not (contains? (union @my-txs @sync-txs) (nth datom 3))))

(defn- sync-url []
  (let [location (.-location js/window)]
    (str (.-protocol location) "//" (.-host location) "/sync")))

(defn- sync-tx-data [{:keys [tx-data]}]
  (let [datoms (->> tx-data (filter new-datom?) (split-datoms))]
    (when-not (empty? datoms)
      (swap! my-txs union (txs datoms))
      (u/call-remote! :post (sync-url) {:tx-data datoms}))))

(defn- sync-latest-datoms! []
  (go
    (let [res    (<! (u/call-remote! :get (sync-url) {:tx (:max-tx @@conn)}))
          datoms (->> (:body res) (filter new-datom?) (map (partial apply d/datom)))]
      (when-not (empty? datoms)
        (swap! sync-txs union (txs datoms))
        (d/transact @conn datoms)))))

(defn- get-compressed-datoms []
  (go
    (let [res (<! (u/fetch (sync-url) {:method "get"
                                       :headers {"Accept" "application/msgpack"
                                                 "Content-Type" "application/msgpack"}}))
          array-buffer (<p! (.arrayBuffer res))]
      (mapv #(apply d/datom %) (c/unpack array-buffer)))))

(defn- sync-start! []
  (u/refresh-on-interval! sync-latest-datoms! 5000))

(defn- first-sync! [schema]
  (go
    (let [datoms (<! (get-compressed-datoms))]
      (reset! conn (d/conn-from-datoms datoms schema))
      (swap! sync-txs union (txs datoms))
      (d/listen! @conn :sync-listener sync-tx-data)
      (rp/connect! @conn)
      (rf/dispatch [:state/set-state :loading? false])
      (sync-start!))))

;;; Public Fns

(defn connect! []
  (first-sync! (->ds-schema (apply concat all-schemas))))

(rf/reg-event-fx
 :store/connect
 (fn [_]
   (connect!)
   {}))
