(ns behave.worksheet-subs-test
  (:require
   [behave.fixtures :as fx]
   [cljs.test :refer [use-fixtures deftest is join-fixtures] :include-macros true]
   [re-frame.core :as rf]))

(use-fixtures :each
  {:before (join-fixtures [fx/setup-empty-db fx/with-new-worksheet fx/with-dummy-results-table])
   :after  (join-fixtures [fx/teardown-db])})

(deftest sub-worksheet-test
  (let [ws-uuid        "test-ws-uuid"
        worksheet-name "test"
        sub-to-test    [:worksheet ws-uuid]
        *worksheet     (rf/subscribe sub-to-test)]
    (rf/dispatch-sync [:transact [{:db/id          -1
                                   :worksheet/uuid ws-uuid
                                   :worksheet/name worksheet-name}]])

    (is (= (:worksheet/uuid @*worksheet) ws-uuid))

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

(deftest shade-set-no-multi-valued-inputs-test
  ;; The dummy results table has no multi-valued inputs, so the sub takes its
  ;; 0-input branch and produces an empty set (nothing to shade). This exercises
  ;; the sub's signal graph + registration end-to-end. Cell-level shading logic
  ;; is covered exhaustively in behave.shading-test.
  (is (= #{}
         @(rf/subscribe [:worksheet/shade-set fx/test-ws-uuid ["output1" "output2"]]))))
