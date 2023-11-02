(ns behave.utils-test
  (:require [behave.utils :as sut]
            [cljs.test :refer [deftest is] :include-macros true]))

(deftest inclusive-range-test
  (is (= (sut/inclusive-range 0 6 2)
         '(0 2 4 6)))

  (is (= (sut/inclusive-range 3)
         '(0 1 2 3)))

  (is (= (sut/inclusive-range 2 3)
         '(2 3)))

  (is (= (sut/inclusive-range 10 2 -2)
         '(10 8 6 4 2))))
