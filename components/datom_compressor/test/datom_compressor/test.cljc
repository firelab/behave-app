(ns datom-compressor.test
  (:require
    #?(:clj  [clojure.test :refer [deftest is]]
       :cljs [cljs.test :refer-macros [deftest is]])
    [datom-compressor.interface :as compress]))

;;; Data

(def datoms [[1 :name "Alice" 30000 true]
             [1 :age  25      30000 true]
             [2 :name "Alice" 30000 true]
             [2 :age  25      30000 true]])

(defn sort-datoms [datoms]
  (sort-by (juxt first second) datoms))

;;; Tests

(deftest test-compress
  (let [roundtrip (compress/unpack (compress/pack datoms))]
    (is (= (sort-datoms datoms) (sort-datoms roundtrip)))))

(comment
  (require '[#?(:clj clojure.test :cljs cljs.test) #?(:cljs :refer-macros :clj :refer) [run-tests]])

  (run-tests)

  )
