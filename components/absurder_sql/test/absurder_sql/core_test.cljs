(ns absurder-sql.core-test
  (:require [absurder-sql.core :as sut]
            [cljs.test :as t :include-macros true :refer [deftest is]]))

(deftest a-failing-test
  (is (= 1 2)))
