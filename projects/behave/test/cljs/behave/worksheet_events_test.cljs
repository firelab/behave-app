(ns behave.worksheet-events-test
  (:require
   [cljs.test :refer [deftest is join-fixtures testing use-fixtures] :include-macros true]
   [re-frame.core :as rf]
   [behave.fixtures :as fx]
   [behave.test-utils :as utils]
   [day8.re-frame.test :as rf-test]))

;; =================================================================================================
;; Fixtures
;; =================================================================================================

(use-fixtures :each
  {:before (join-fixtures [fx/setup-empty-db fx/with-new-worksheet])
   :after  (join-fixtures [fx/teardown-db])})

;; =================================================================================================
;; Tests
;; =================================================================================================

(deftest update-attr
  (is (= 1 0)
      "TODO"))

;; =================================================================================================
;; :worksheet/add-input-group
;; =================================================================================================

(deftest add-input-group-test
  (testing "valid case"
    (let [group-uuid    "some-group-uuid" ;TODO make human readable
          event-to-test [:worksheet/add-input-group fx/test-ws-uuid group-uuid 0]]

      (rf/dispatch-sync event-to-test)

      (let [*worksheet  (rf/subscribe [:worksheet fx/test-ws-uuid])
            input-group (first (:worksheet/input-groups @*worksheet))]

        (is (some? (:worksheet/input-groups @*worksheet)))

        (is (= (:input-group/group-uuid input-group) group-uuid))

        (is (= (:input-group/repeat-id input-group) 0)))))

  (testing "invalid"
    (testing "ws-uuid does not exist"
      (let [non-ext-uuid  "non-exisitng-uuid"
            group-uuid    "some-group-uuid" ;TODO make human readable
            event-to-test [:worksheet/add-input-group non-ext-uuid group-uuid 0]]
        (rf/dispatch-sync event-to-test)
        (let [worksheet @(rf/subscribe [:worksheet non-ext-uuid])]
          (is (nil? worksheet)))))))

;; =================================================================================================
;; :worksheet/upsert-input-variable
;; =================================================================================================

(deftest upsert-input-variable-test
  (let [*worksheet          (rf/subscribe [:worksheet fx/test-ws-uuid])
        input-group-uuid    "some-input-group-uuid"
        group-variable-uuid "some-group-variable-uuid"
        value               "some-value"
        units               "some-units"
        repeat-id           0
        setup-event         [:worksheet/add-input-group fx/test-ws-uuid input-group-uuid 0]
        event-to-test       [:worksheet/upsert-input-variable
                             fx/test-ws-uuid
                             input-group-uuid
                             repeat-id
                             group-variable-uuid
                             value
                             units]]

    (rf/dispatch-sync setup-event)

    (is (seq (->> *worksheet
                  deref
                  :worksheet/input-groups
                  (filterv #(= (:input-group/group-uuid %) input-group-uuid))))
        "should have the input-group-uuid in the worksheet before dispatching event-to-test")

    (rf/dispatch-sync event-to-test)

    (is (= (->> *worksheet
                deref
                :worksheet/input-groups
                first
                utils/entity->cljs)
           {:input-group/group-uuid input-group-uuid
            :input-group/repeat-id  repeat-id
            :input-group/inputs     [{:input/group-variable-uuid group-variable-uuid
                                      :input/value               value
                                      :input/units               units}]}))))

(deftest upsert-input-variable-with-non-existing-group-uuid-test
  (let [*worksheet                   (rf/subscribe [:worksheet fx/test-ws-uuid])
        non-exiting-input-group-uuid "non-existing-input-group-uuid"
        group-variable-uuid          "some-group-variable-uuid"
        value                        "some-value"
        units                        "some-units"
        repeat-id                    0
        event-to-test                [:worksheet/upsert-input-variable
                                      fx/test-ws-uuid
                                      non-exiting-input-group-uuid
                                      repeat-id
                                      group-variable-uuid
                                      value
                                      units]]

    (is (->> *worksheet
             deref
             :worksheet/input-groups
             empty?)
        "input-groups should be empty before dispatching event-to-test")

    (rf/dispatch-sync event-to-test)

    (is (->> *worksheet
             deref
             :worksheet/input-groups
             empty?)
        "should not add inputs with this input-group-uuid to the worksheet")))

;; =================================================================================================
;; :worksheet/delete-repeat-input-test
;; =================================================================================================

(deftest delete-repeat-input-group-test
  (let [*worksheet    (rf/subscribe [:worksheet fx/test-ws-uuid])
        setup-events  (for [repeat-id (range 0)]
                        [:worksheet/add-input-group fx/test-ws-uuid "group-uuid" repeat-id])
        event-to-test [:worksheet/delete-repeat-input-group fx/test-ws-uuid 1]]

    (is (some? (:worksheet/input-groups @*worksheet)))

    (doseq [event setup-events]
      (rf/dispatch-sync event))

    (rf/dispatch-sync event-to-test)

    (is (= 1 "TODO"))))

;; =================================================================================================
;; worksheet/upsert-output
;; =================================================================================================

(deftest upsert-output-test
  (testing "output-uuid not already in worksheet"
    (let [*worksheet    (rf/subscribe [:worksheet fx/test-ws-uuid])
          output-uuid   "some-uuid"
          enabled?      true
          event-to-test [:worksheet/upsert-output fx/test-ws-uuid output-uuid enabled?]]

      (is (nil? (:worksheet/outputs @*worksheet))
          "should not have any outputs in worksheet")

      (rf/dispatch-sync event-to-test)

      (is (some? (:worksheet/outputs @*worksheet))
          "should now have some outputs")

      (let [outputs (->> *worksheet
                         deref
                         :worksheet/outputs
                         first)]
        (is (= (into {} outputs)
               {:output/group-variable-uuid output-uuid
                :output/enabled?            enabled?})
            "first output should be what we've passed in the event vector")))))

(deftest upsert-output-already-existing-uuid-test
  (testing "already existing output-uuid in worksheet"
    (let [*worksheet    (rf/subscribe [:worksheet fx/test-ws-uuid])
          output-uuid   "some-uuid"
          setup-event   [:worksheet/upsert-output fx/test-ws-uuid output-uuid false]
          event-to-test [:worksheet/upsert-output fx/test-ws-uuid output-uuid true]]

      (rf/dispatch-sync setup-event)

      (is (false? (->> *worksheet
                       deref
                       :worksheet/outputs
                       first
                       :output/enabled?))
          "output/enabled? should be as false before dispatching event-to-test")

      (rf/dispatch-sync event-to-test)

      (is (true? (->> *worksheet
                      deref
                      :worksheet/outputs
                      first
                      :output/enabled?))
          ":output/eneabled? should be updated to true"))))

;; =================================================================================================
;; :worksheet/add-result-table
;; =================================================================================================

(deftest add-result-table-test
  (let [event-to-test [:worksheet/add-result-table fx/test-ws-uuid]]
    (rf/dispatch-sync event-to-test)
    (let [*worksheet (rf/subscribe [:worksheet fx/test-ws-uuid])]
      (is (some? (:worksheet/result-table @*worksheet))))))

;; =================================================================================================
;; :worksheet/add-result-table-header-test
;; =================================================================================================

(deftest add-result-table-header-test
  (let [*worksheet          (rf/subscribe [:worksheet fx/test-ws-uuid])
        group-variable-uuid "some-group-variable-uuid"
        units               "some-units"
        setup-event         [:worksheet/add-result-table fx/test-ws-uuid]
        event-to-test       [:worksheet/add-result-table-header fx/test-ws-uuid group-variable-uuid units]]

    (rf/dispatch-sync setup-event)

    (is (empty? (-> @*worksheet
                    :worksheet/result-table
                    :result-table/headers))
        "should not have any headers before dispatching event-to-test")

    (rf/dispatch-sync event-to-test)

    (is (= (-> @*worksheet
               :worksheet/result-table
               :result-table/headers
               first
               :result-header/group-variable-uuid)
           group-variable-uuid)
        "should now have the correct header added to result-table")))

(deftest add-result-table-header-order-test
  (let [*worksheet           (rf/subscribe [:worksheet fx/test-ws-uuid])
        group-variable-uuids ["1" "2" "3" "4"]
        new-gv-uuid          "5"
        units                "some-units"
        setup-event          [:worksheet/add-result-table fx/test-ws-uuid]
        event-to-test        [:worksheet/add-result-table-header fx/test-ws-uuid new-gv-uuid units]]

    (rf/dispatch-sync setup-event)

    (doseq [uuid group-variable-uuids]
      (rf/dispatch-sync [:worksheet/add-result-table-header fx/test-ws-uuid uuid units]))

    (is (= (count (-> @*worksheet
                      :worksheet/result-table
                      :result-table/headers))
           4)
        "should exist 4 group variables before dispatching event-to-test")

    (rf/dispatch-sync event-to-test)

    (is (= (->> @*worksheet
                :worksheet/result-table
                :result-table/headers
                (filterv #(= (:result-header/group-variable-uuid %) new-gv-uuid))
                first
                :result-header/order)
           4)
        "order should be set to 4 (0 index) since we already have 4 variables")))

;; =================================================================================================
;; :worksheet/add-result-table-row
;; =================================================================================================

(deftest add-result-table-row-single-test
  (let [*worksheet    (rf/subscribe [:worksheet fx/test-ws-uuid])
        setup-event   [:worksheet/add-result-table fx/test-ws-uuid]
        event-to-test [:worksheet/add-result-table-row fx/test-ws-uuid 0]]

    (rf/dispatch-sync setup-event)

    (is (empty? (-> @*worksheet
                    :worksheet/result-table
                    :result-table/rows))
        "should not have any rows before dispatching event-to-test")

    (rf/dispatch-sync event-to-test)

    (is (= (-> @*worksheet
               :worksheet/result-table
               :result-table/rows
               first
               :result-row/id)
           0)
        "Should now have one row with the correct row-id")))

(deftest add-result-table-row-multiple-test
  (let [*worksheet     (rf/subscribe [:worksheet fx/test-ws-uuid])
        setup-event    [:worksheet/add-result-table fx/test-ws-uuid]
        events-to-test [[:worksheet/add-result-table-row fx/test-ws-uuid 0]
                        [:worksheet/add-result-table-row fx/test-ws-uuid 1]
                        [:worksheet/add-result-table-row fx/test-ws-uuid 2]]]

    (rf/dispatch-sync setup-event)

    (is (empty? (-> @*worksheet
                    :worksheet/result-table
                    :result-table/rows))
        "should not have any rows before dispatching events-to-test")

    (doseq [event events-to-test]
      (rf/dispatch-sync event))

    (is (= (count (-> @*worksheet
                      :worksheet/result-table
                      :result-table/rows))
           3)
        "Should now have 3 rows")))

;; =================================================================================================
;; :worksheet/add-result-table-cell
;; =================================================================================================

(deftest add-result-table-cell
  (is (= 1 0)
      "TODO"))

;; =================================================================================================
;; :worksheet/solve
;; =================================================================================================

(defn upsert-output-events
  [ws-uuid output-uuids setup-events]
  (into setup-events
        (map (fn [output-uuid] [:worksheet/upsert-output ws-uuid output-uuid true]))
        output-uuids))

(defn add-input-group-events [ws-uuid input-args setup-events]
  (into setup-events
        (map (fn [[group-uuid & _rest]]
               (let [repeat-id 0]
                 [:worksheet/add-input-group ws-uuid group-uuid repeat-id])))
        input-args))

(defn upsert-input-events
  [ws-uuid input-args setup-events]
  (into setup-events
        (map (fn [[group-uuid group-variable-uuid value]]
               (let [repeat-id 0
                     units     nil]
                 [:worksheet/upsert-input-variable ws-uuid group-uuid repeat-id group-variable-uuid value units])))
        input-args))

;; TODO add debug printout for uuid->entity
;; TODO Use CSV to populate inputs and outputs and test against csv results -> GET FROM CONTAIN_TESTING
(deftest solver-test-single-row-results-table
  (rf-test/run-test-sync
   (let [output-args   ["b7873139-659e-4475-8d41-0cf6c36da893"]
         input-args    [["a1b35161-e60b-47e7-aad3-b99fbb107784" "fbbf73f6-3a0e-4fdd-b913-dcc50d2db311" "1"] ; [group-uuid group-variable-uuid value]
                        ["1b13a28a-bc30-4c76-827e-e052ab325d67" "30493fc2-a231-41ee-a16a-875f00cf853f" "2"]
                        ["79429082-3217-4c62-b90e-4559de5cbaa7" "41503286-dfe4-457a-9b68-41832e049cc9" "3"]
                        ["fedf0a53-e12c-4504-afc0-af294c96c641" "de9df9ee-dfe5-42fe-b43c-fc1f54f99186" "HeadAttack"]
                        ["d88be382-e59a-4648-94a8-44253710148d" "6577589c-947f-4c0c-9fca-181d3dd7fb7c" "4"]]
         setup-events  (->> []
                            (upsert-output-events fx/test-ws-uuid output-args)
                            (add-input-group-events fx/test-ws-uuid input-args)
                            (upsert-input-events fx/test-ws-uuid input-args))
         event-to-test [:worksheet/solve fx/test-ws-uuid]]

     (println "setup-events:" setup-events)

     (doseq [event setup-events]
       (rf/dispatch event))

     (rf/dispatch event-to-test)


     (let [result-table-cell-data  @(rf/subscribe [:worksheet/result-table-cell-data fx/test-ws-uuid])
           result-header-uuids-set (into #{}
                                         (map second)
                                         result-table-cell-data)]

       (is (seq result-table-cell-data))

       (is (= 1 (inc (apply max (map first result-table-cell-data))))
           "should only have one row of data")

       (is (every? (fn [[_ group-variable-uuid _]]
                     (contains? result-header-uuids-set group-variable-uuid))
                   input-args)
           "all input-uuids should be in the table")

       (is (every? (fn [output-uuids]
                     (contains? result-header-uuids-set output-uuids))
                   output-args)
           "all output-uuids should be in the table")

       (is (= result-table-cell-data
              #{[0 "fbbf73f6-3a0e-4fdd-b913-dcc50d2db311" "1"]
                [0 "30493fc2-a231-41ee-a16a-875f00cf853f" "2"]
                [0 "41503286-dfe4-457a-9b68-41832e049cc9" "3"]
                [0 "de9df9ee-dfe5-42fe-b43c-fc1f54f99186" "HeadAttack"]
                [0 "6577589c-947f-4c0c-9fca-181d3dd7fb7c" "4"]
                [0 "b7873139-659e-4475-8d41-0cf6c36da893" "0"]})
           "should have these values from setup-events")))))

(deftest solver-test-multi-row-results-table
  (rf-test/run-test-sync
   (let [output-args   ["b7873139-659e-4475-8d41-0cf6c36da893"]
         input-args    [["a1b35161-e60b-47e7-aad3-b99fbb107784" "fbbf73f6-3a0e-4fdd-b913-dcc50d2db311" "1,2,3,4"] ; [group-uuid group-variable-uuid value]
                        ["1b13a28a-bc30-4c76-827e-e052ab325d67" "30493fc2-a231-41ee-a16a-875f00cf853f" "2"]
                        ["79429082-3217-4c62-b90e-4559de5cbaa7" "41503286-dfe4-457a-9b68-41832e049cc9" "3"]
                        ["fedf0a53-e12c-4504-afc0-af294c96c641" "de9df9ee-dfe5-42fe-b43c-fc1f54f99186" "HeadAttack"]
                        ["d88be382-e59a-4648-94a8-44253710148d" "6577589c-947f-4c0c-9fca-181d3dd7fb7c" "4"]]
         setup-events  (->> []
                            (upsert-output-events fx/test-ws-uuid output-args)
                            (add-input-group-events fx/test-ws-uuid input-args)
                            (upsert-input-events fx/test-ws-uuid input-args))
         event-to-test [:worksheet/solve fx/test-ws-uuid]]

     (doseq [event setup-events]
       (rf/dispatch event))

     (rf/dispatch event-to-test)

     (let [result-table-cell-data @(rf/subscribe [:worksheet/result-table-cell-data fx/test-ws-uuid])]

       (is (seq result-table-cell-data))

       (is (= 4 (inc (apply max (map first result-table-cell-data)))) ;TODO currently failing until solver is updated
           "should only have four rows of data")))))

;; =================================================================================================
;; :worksheet/toggle-table-settings
;; =================================================================================================

(deftest toggle-table-settings-test
  (is (= 1 0)
      "TODO"))

;; =================================================================================================
;; :worksheet/update-y-axis-limit-attr
;; =================================================================================================

(deftest update-y-axis-limit-attr-test
  (is (= 1 0)
      "TODO"))

;; =================================================================================================
;; :worksheet/update-graph-settings-attr
;; =================================================================================================

(deftest update-graph-settings-attr-test
  (is (= 1 0)
      "TODO"))

;; =================================================================================================
;; :worksheet/create-note
;; =================================================================================================

(deftest create-note-test
  (is (= 1 0)
      "TODO"))

;; =================================================================================================
;; :worksheet/update-note
;; =================================================================================================

(deftest update-note-test
  (is (= 1 0)
      "TODO"))

;; =================================================================================================
;; :worksheet/delete-note-test
;; =================================================================================================

(deftest delete-note-test
  (is (= 1 0)
      "TODO"))

;; =================================================================================================
;; :worksheet/update-furthest-visited-step
;; =================================================================================================

(deftest update-furthest-visited-step-test
  (is (= 1 0)
      "TODO"))
