(ns markdown2hiccup.test
  (:require [clojure.test :refer [deftest is testing]]
            [markdown2hiccup.interface :as m2h]))

(defn- contains-tag?
  "Walk hiccup tree looking for a vector starting with `tag`."
  [hiccup tag]
  (some #(and (vector? %) (= tag (first %)))
        (tree-seq sequential? seq hiccup)))

(deftest md->hiccup-heading-test
  (testing "converts markdown heading to hiccup"
    (let [result (m2h/md->hiccup "# Hello")]
      (is (seq result))
      (is (contains-tag? result :h1)))))

(deftest md->hiccup-paragraph-test
  (testing "converts markdown paragraph to hiccup"
    (let [result (m2h/md->hiccup "Hello world")]
      (is (seq result))
      (is (contains-tag? result :p)))))

(deftest md->hiccup-bold-test
  (testing "converts bold markdown"
    (let [result (m2h/md->hiccup "**bold**")]
      (is (seq result))
      (is (contains-tag? result :strong)))))

(deftest md->hiccup-link-test
  (testing "converts markdown link"
    (let [result (m2h/md->hiccup "[link](http://example.com)")]
      (is (seq result))
      (is (contains-tag? result :a)))))
