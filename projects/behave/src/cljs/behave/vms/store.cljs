(ns behave.vms.store
  (:require [ajax.core                  :refer [ajax-request]]
            [ajax.protocols             :as pr]
            [datascript.core            :as d]
            [posh.reagent               :refer [pull pull-many q posh!]
                                        :rename {q posh-query pull posh-pull pull-many posh-pull-many}]
            [reagent.ratom              :refer-macros [reaction]]
            [re-frame.core              :as rf]
            [datom-compressor.interface :as c]
            [ds-schema-utils.interface  :refer [->ds-schema]]
            [behave.schema.core         :refer [all-schemas]]))

;;; State

(defonce vms-conn (atom nil))

;;; Helpers

(defn- load-data-handler [[ok body]]
  (when ok
    (let [datoms (mapv #(apply d/datom %) (c/unpack body))]
      (rf/dispatch-sync [:vms/initialize (->ds-schema all-schemas) datoms])
      (rf/dispatch-sync [:state/set :vms-loaded? true]))))

(defn- reloaded-vms-data [[ok body]]
  (when ok
    (rf/dispatch-sync [:state/set :vms-reloaded? true])))

;;; Public Fns

(defn load-vms! []
  (ajax-request {:uri             "/layout.msgpack"
                 :handler         load-data-handler
                 :format          {:content-type "application/text" :write str}
                 :response-format {:description  "ArrayBuffer"
                                   :type         :arraybuffer
                                   :content-type "application/msgpack"
                                   :read         pr/-body}}))

(defn reload-vms! []
  (ajax-request {:uri     "/vms-sync"
                 :handler reloaded-vms-data}))

;;; Public Fns

(defn init! [{:keys [datoms schema]}]
  (if @vms-conn
   @vms-conn
   (do
     (reset! vms-conn (d/conn-from-datoms datoms schema))
     (posh! @vms-conn)
     @vms-conn)))

;;; Effects

(rf/reg-fx :vms/init init!)

;;; Events

(rf/reg-event-fx
  :vms/initialize
  (fn [_ [_ schema datoms]]
    {:vms/init {:datoms datoms :schema schema}}))

;;; Operations
(defn q [query & variables]
  (apply posh-query query @vms-conn variables))

(defn pull [pattern id]
  (posh-pull @vms-conn pattern id))

(defn pull-many [pattern ids]
  (posh-pull-many @vms-conn pattern ids))
