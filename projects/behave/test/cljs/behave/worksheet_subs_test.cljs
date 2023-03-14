(ns behave.worksheet-subs-test
  (:require
   [cljs.test :refer [use-fixtures deftest is join-fixtures] :include-macros true]
   [re-frame.core :as rf]
   [behave.fixtures :refer [setup-empty-db teardown-db]]))

(use-fixtures :each
  {:before (join-fixtures [setup-empty-db])
   :after  (join-fixtures [teardown-db])})

(deftest sub-worksheet-test
  (let [uuid           "test-ws-uuid"
        worksheet-name "test"
        sub-to-test    [:worksheet uuid]
        *worksheet     (rf/subscribe sub-to-test)]
    (rf/dispatch-sync [:transact [{:db/id          -1
                                   :worksheet/uuid uuid
                                   :worksheet/name worksheet-name}]])

    (is (= (:worksheet/uuid @*worksheet) uuid))

    (is (= (:worksheet/name @*worksheet) worksheet-name))))

;; (deftest result-table-cell-data-test
;;   (is (= 1 "TODO")))
