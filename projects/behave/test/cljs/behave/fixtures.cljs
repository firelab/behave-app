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

;; =================================================================================================
;; with-new-worksheet
;; =================================================================================================

(def test-ws-uuid "test-ws-uuid")

(def test-ws-name "test-ws-name")

(defn with-new-worksheet [& [f]]
  (rf/dispatch-sync
   [:worksheet/new {:uuid    test-ws-uuid
                    :name    test-ws-name
                    :modules [:contain :surface]}])
  (when (fn? f) (f)))

;; =================================================================================================
;; with-dummy-results-table
;; =================================================================================================

(defn with-dummy-results-table [& [f]]
  (d/transact @bs/conn
              [{:db/id                  [:worksheet/uuid test-ws-uuid]
                ;; Insert table 3 input columns, 1 output column, and 3 rows of data

                :worksheet/outputs [{:output/group-variable-uuid "output1"
                                     :output/enabled?            true}
                                    {:output/group-variable-uuid "output2"
                                     :output/enabled?            true}]

                :worksheet/result-table {:result-table/headers [{:db/id                             -2
                                                                 :result-header/order               0
                                                                 :result-header/group-variable-uuid "Input1"
                                                                 :result-header/units               "ch/h"
                                                                 :result-header/repeat-id           0}
                                                                {:db/id                             -3
                                                                 :result-header/order               1
                                                                 :result-header/group-variable-uuid "Input2"
                                                                 :result-header/units               "ac"
                                                                 :result-header/repeat-id           0}
                                                                {:db/id                             -4
                                                                 :result-header/order               1
                                                                 :result-header/group-variable-uuid "Input3"
                                                                 :result-header/units               "ratio"
                                                                 :result-header/repeat-id           0}
                                                                {:db/id                             -5
                                                                 :result-header/order               2
                                                                 :result-header/group-variable-uuid "output1"
                                                                 :result-header/units               "ft"
                                                                 :result-header/repeat-id           0}
                                                                {:db/id                             -6
                                                                 :result-header/order               3
                                                                 :result-header/group-variable-uuid "output2"
                                                                 :result-header/units               "ft"
                                                                 :result-header/repeat-id           0}]
                                         :result-table/rows [{:result-row/id    0
                                                              :result-row/cells [;inputs
                                                                                 {:result-cell/header -2
                                                                                  :result-cell/value  10}
                                                                                 {:result-cell/header -3
                                                                                  :result-cell/value  10}
                                                                                 {:result-cell/header -4
                                                                                  :result-cell/value  10}
                                                                                 ;;outputs
                                                                                 {:result-cell/header -5
                                                                                  :result-cell/value  10}
                                                                                 {:result-cell/header -6
                                                                                  :result-cell/value  100}]}
                                                             {:result-row/id    1
                                                              :result-row/cells [;inputs
                                                                                 {:result-cell/header -2
                                                                                  :result-cell/value  20}
                                                                                 {:result-cell/header -3
                                                                                  :result-cell/value  20}
                                                                                 {:result-cell/header -4
                                                                                  :result-cell/value  20}
                                                                                 ;;outputs
                                                                                 {:result-cell/header -5
                                                                                  :result-cell/value  20}
                                                                                 {:result-cell/header -6
                                                                                  :result-cell/value  200}]}
                                                             {:result-row/id    2
                                                              :result-row/cells [;inputs
                                                                                 {:result-cell/header -2
                                                                                  :result-cell/value  30}
                                                                                 {:result-cell/header -3
                                                                                  :result-cell/value  30}
                                                                                 {:result-cell/header -4
                                                                                  :result-cell/value  30}
                                                                                 ;;outputs
                                                                                 {:result-cell/header -5
                                                                                  :result-cell/value  30}
                                                                                 {:result-cell/header -6
                                                                                  :result-cell/value  300}]}]}}])
  (when (fn? f) (f)))
