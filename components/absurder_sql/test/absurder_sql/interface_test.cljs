(ns absurder-sql.interface-test
  (:require [absurder-sql.interface :as sut]
            [cljs.test :as t :include-macros true :refer [deftest is use-fixtures]]))

(use-fixtures :once
  {:before (fn [] (sut/init!))
   :after  (fn [] (sut/close!))})

(deftest connect-test
  (delay 1000)
  (is (= true (sut/connected?))))
