(ns absurder-sql.core-test
  (:require [clojure.test :refer [deftest testing is]]))

(deftest passing-tests
  (is (= 1 (inc 0)))

  (is (pos-int? 5))

  (is (thrown? cljs.core/ExceptionInfo (throw (ex-info "Oh no!" {:pos :thrown})))))
