(ns version-utils.test
  (:require [clojure.test            :refer [deftest is testing]]
            [version-utils.interface :as vu]))

(deftest parse-test
  (testing "parses standard version strings"
    (is (= [7 1 4] (vu/parse "7.1.4")))
    (is (= [7]     (vu/parse "7")))
    (is (= [1 0 0] (vu/parse "1.0.0"))))
  (testing "returns nil for invalid input"
    (is (nil? (vu/parse nil)))
    (is (nil? (vu/parse "")))
    (is (nil? (vu/parse "abc")))
    (is (nil? (vu/parse "1.x")))))

(deftest compare-versions-test
  (testing "ordering"
    (is (= -1 (vu/compare-versions "7.1.3" "7.1.4")))
    (is (=  1 (vu/compare-versions "7.1.4" "7.1.3")))
    (is (=  0 (vu/compare-versions "7.1.4" "7.1.4"))))
  (testing "zero-pads shorter vectors"
    (is (= 0 (vu/compare-versions "7.1" "7.1.0")))
    (is (= 0 (vu/compare-versions "7.1.0" "7.1"))))
  (testing "numeric not lexicographic"
    (is (= 1 (vu/compare-versions "10.0.0" "9.9.9"))))
  (testing "nil sorts before any version"
    (is (= -1 (vu/compare-versions nil "7.1.4")))
    (is (=  1 (vu/compare-versions "7.1.4" nil)))
    (is (=  0 (vu/compare-versions nil nil))))
  (testing "accepts pre-parsed vectors"
    (is (= 0 (vu/compare-versions [7 1 4] "7.1.4")))
    (is (= 0 (vu/compare-versions "7.1.4" [7 1 4])))))
