(ns behave.worksheet-subs-test
  (:require
   [cljs.test :refer [use-fixtures deftest is join-fixtures] :include-macros true]
   [re-frame.core :as rf]
   [behave.fixtures :as fx]))

(use-fixtures :each
  {:before (join-fixtures [fx/setup-empty-db fx/with-new-worksheet fx/with-dummy-results-table])
   :after  (join-fixtures [fx/teardown-db])})

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

(deftest result-table-cell-data-test
  (is (= @(rf/subscribe [:worksheet/result-table-cell-data fx/test-ws-uuid])
         #{[0 "Input1"  0 10] ; [row-id col-uuid repeat-id value]
           [0 "Input2"  0 10]
           [0 "Input3"  0 10]
           [0 "output1" 0 10]
           [0 "output2" 0 100]

           [1 "Input1"  0 20]
           [1 "Input2"  0 20]
           [1 "Input3"  0 20]
           [1 "output1" 0 20]
           [1 "output2" 0 200]

           [2 "Input1"  0 30]
           [2 "Input2"  0 30]
           [2 "Input3"  0 30]
           [2 "output1" 0 30]
           [2 "output2" 0 300]})))

(deftest output-min+max-values-test
  (is (= @(rf/subscribe [:worksheet/output-min+max-values fx/test-ws-uuid])
         {"output1" [10 30]
          "output2" [100 300]})))
