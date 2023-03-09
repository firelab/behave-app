(ns behave.tests-used-in-fixtures
  (:require
   [cljs.test :refer [use-fixtures deftest is join-fixtures] :include-macros true]
   [re-frame.core :as rf]
   [behave.fixtures :refer [setup-empty-db teardown-db]]))

;; =================================================================================================
;; Test utils and fixtures
;; =================================================================================================

(use-fixtures :each
  {:before (join-fixtures [setup-empty-db])
   :after  (join-fixtures [teardown-db])})

;; =================================================================================================
;; Basic Test
;; =================================================================================================

(deftest first-name-test
  (let [event-to-test [:transact [{:db/id     -1
                                   :user/name "kenny"}]]]
    (rf/dispatch-sync event-to-test)
    (let [result @(rf/subscribe [:query '[:find ?name .
                                          :where [?e :user/name ?name]]])]
      (is (= result "kenny")))))

(deftest second-name-test
  (let [event-to-test [:transact [{:db/id     -1
                                   :user/name "RJ"}]]]
    (rf/dispatch-sync event-to-test))
  (let [result @(rf/subscribe [:query '[:find ?name .
                                        :where [?e :user/name ?name]]])]
    (is (= result "RJ"))))

;; =================================================================================================
;; behave.worksheet.events tests
;; =================================================================================================

(deftest worksheet-new-test
  (let [uuid           "test-ws-uuid"
        worksheet-name "test"
        event-to-test  [:worksheet/new {:name    worksheet-name
                                        :modules [:contain :surface]
                                        :uuid    uuid}]
        *worksheet     (rf/subscribe [:worksheet uuid])]

    (is (nil? @*worksheet)
        "worksheet should not exist yet.")

    (rf/dispatch-sync event-to-test)

    (is (= (:worksheet/uuid @*worksheet) uuid)
        "worksheet should have the correct uuid")

    (is (= (:worksheet/name @*worksheet) worksheet-name)
        "worksheet should have the correct name")))
