(ns absurder-sql.connect-test
  (:require [clojure.test :as ct :refer-macros [deftest is testing]]
            [promesa.core :as p]
            [absurder-sql.interface :as absurder-sql]))

(deftest simple-test
  (testing "db function"
    (p/then (absurder-sql/init!)
            (fn []
              (println "DB Function exists?" js/window.sqlite.Database)
              (println "DB Function exists?" js/window.sqlite)
              (is (some? js/window.sqlite))))))
