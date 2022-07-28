(ns transport.test
  (:require
    #?(:clj  [clojure.test :refer [are deftest is]]
       :cljs [cljs.test :refer-macros [are deftest is]])
    [transport.interface :as t]))

(def test-data {:a "b" :c ["d" "e"] :f 1.0 :g #{1 2 3} :h {:i {:j "k"}}})

; WARNING: JSON does not handle sets
(def json-data (assoc test-data :g (vec (:g test-data))))

(deftest roundtrip-edn
  (is (= test-data (t/edn->clj (t/clj->edn test-data)))))

(deftest roundtrip-json
  (is (= json-data (t/json->clj (t/clj->json test-data)))))

(deftest roundtrip-transit
  (is (= test-data (t/transit->clj (t/clj->transit test-data)))))

(deftest roundtrip-msgpack
  (is (= test-data (t/msgpack->clj (t/clj->msgpack test-data)))))

(deftest roundtrip-universal
  (are [transport data] (= data (-> data
                                    (t/clj-> transport)
                                    (t/->clj transport)))
       :edn     test-data
       :json    json-data
       :msgpack test-data
       :transit test-data))

(comment
  (require '[#?(:clj clojure.test :cljs cljs.test) #?(:cljs :refer-macros :clj :refer) [run-tests]])

  (run-tests)

  )
