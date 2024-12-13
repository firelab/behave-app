(ns behave.units-test
  (:require [behave.lib.units :as sut]
            [behave.helpers   :refer [within-hundredth?]]
            [cljs.test :refer [deftest is] :include-macros true]))

(deftest units-conversion-test
  (is (= 1 (sut/convert 1 :area "ac" "ac")))

  (is (= 1 (sut/convert 1 "ac" "ac")))

  (is (within-hundredth? 43560 (sut/convert 1 :area "ac" "ft2")))

  (is (within-hundredth? 43560 (sut/convert 1 "ac" "ft2"))))
