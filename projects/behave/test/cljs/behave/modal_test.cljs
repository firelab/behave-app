(ns behave.modal-test
  (:require [behave.modal.events :as sut]
            [cljs.test           :refer [deftest is testing] :include-macros true]))

(deftest open-test
  (testing "opening pushes the id and its context onto the stack"
    (is (= [{:ws-uuid "abc" :modal/id :graph}]
           (sut/open [] :graph {:ws-uuid "abc"}))))

  (testing "opening from nothing works (no modal has been opened yet)"
    (is (= [{:modal/id :help-image}]
           (sut/open nil :help-image {}))))

  (testing "a modal opened from a modal stacks on top of it"
    (is (= [{:modal/id :graph} {:modal/id :help-image}]
           (-> []
               (sut/open :graph {})
               (sut/open :help-image {}))))))

(deftest close-test
  (testing "closing pops only the top-most modal"
    (is (= [{:modal/id :graph}]
           (-> []
               (sut/open :graph {})
               (sut/open :help-image {})
               (sut/close)))))

  (testing "closing the last modal empties the stack"
    (is (= [] (-> [] (sut/open :graph {}) (sut/close)))))

  (testing "closing an empty stack is a no-op"
    (is (= [] (sut/close [])))
    (is (= [] (sut/close nil)))))
