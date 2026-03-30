(ns csv-parser.test
  (:require [clojure.test :refer [deftest is testing]]
            [csv-parser.interface :as csv]
            [clojure.java.io :as io]))

(deftest fetch-csv-test
  (testing "parses CSV file from classpath resource"
    (let [tmp (java.io.File/createTempFile "test" ".csv")]
      (try
        (spit tmp "name,age,score\nalice,30,95.5\nbob,25,88.0\n")
        (let [result (csv/fetch-csv (str (.toURI tmp)))]
          (is (= 2 (count result)))
          (is (= "alice" (:name (first result))))
          (is (= "30" (:age (first result)))))
        (finally
          (.delete tmp))))))
