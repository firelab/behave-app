(ns behave.fixtures
  (:require
   [behave.schema.core :refer [all-schemas]]
   [behave.store :as bs]
   [datascript.core :as d]
   [ds-schema-utils.interface :refer [->ds-schema]]
   [re-frame.core :as rf]
   [re-posh.core :as rp]
   [re-posh.db :as rpdb]
   [austinbirch.reactive-entity :as re]))

;;; Re-Frame Event Logging

(defn log-rf-events [& [f]]
  (rf/add-post-event-callback
   :log-events
   (fn [& args]
     (println "[re-frame]" args)))
  (when (fn? f) (f)))

(defn stop-logging-rf-events [& [f]]
  (rf/remove-post-event-callback :log-events)
  (when (fn? f) (f)))

;;; DataScript/Re-Posh

(defn setup-empty-db [& [f]]
  (let [conn (d/create-conn (->ds-schema all-schemas))]
    (reset! bs/conn conn)
    (re/init! conn)
    (rp/connect! conn))
  (when (fn? f) (f))) ; necessary for allowing composition of fixtures

(defn teardown-db [& [f]]
  (reset! rpdb/store nil)
  (reset! bs/conn nil)
  (rf/clear-subscription-cache!)
  (when (fn? f) (f)))

;;; Worksheets

(def test-ws-uuid "test-ws-uuid")

(def test-ws-name "test-ws-name")

(defn with-new-worksheet [& [f]]
  (rf/dispatch-sync
   [:worksheet/new {:uuid    test-ws-uuid
                    :name    test-ws-name
                    :modules [:contain :surface]}])
  (when (fn? f) (f)))
