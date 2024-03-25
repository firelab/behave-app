(ns datom-compressor.test
  (:require
   [clojure.test :refer [deftest is]]
   [datom-compressor.interface :as compress]))

;;; Helpers

(defn today [] (java.util.Date.))

;;; Data

(def datoms [[1 :name "Alice" 30000 true]
             [1 :age  25      30000 true]
             [2 :name "Alice" 30000 true]
             [2 :age  25      30000 true]
             [3 :date (today) 30000 true]])

(defn sort-datoms [datoms]
  (sort-by (juxt first second) datoms))

;;; Tests

(deftest test-compress
  (let [roundtrip (compress/unpack (compress/pack datoms))]
    (is (= (sort-datoms datoms) (sort-datoms roundtrip)))))
