(ns behave.store
  (:require [ajax.core :refer [ajax-request]]
            [ajax.protocols :as pr]
            [datascript.core :as d]
            [re-frame.core :as rf]
            [re-posh.core :as rp]
            [datom-compressor.interface :as c]
            [ds-schema-utils.interface :refer [->ds-schema]]
            [behave.schema.core :refer [all-schemas]]))

;;; State

(defonce conn (atom nil))

;;; Helpers

(defn init! [{:keys [datoms schema]}]
  (if @conn
   @conn
   (do
     (reset! conn (d/conn-from-datoms datoms schema))
     (rp/connect! @conn)
     @conn)))

(defn- load-data-handler [[ok body]]
  (when ok
    (println "GOT BODY" body)
    (rf/dispatch-sync [:ds/initialize (->ds-schema all-schemas) (mapv #(apply d/datom %) (c/unpack body))])))

(defn load-store! []
  (ajax-request {:uri "/layout.msgpack"
                 :handler load-data-handler
                 :format {:content-type "application/text" :write str}
                 :response-format {:description "ArrayBuffer"
                                   :type :arraybuffer
                                   :content-type "*/*"
                                   :read pr/-body}}))

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
