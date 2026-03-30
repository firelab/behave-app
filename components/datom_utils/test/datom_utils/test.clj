(ns datom-utils.test
  (:require [clojure.test :refer [deftest is testing]]
            [datom-utils.interface :as du]))

(deftest unsafe-attrs-test
  (testing "includes tuple type attrs and hardcoded unsafe attrs"
    (let [schema [{:db/ident     :foo/bar
                   :db/valueType :db.type/tuple}
                  {:db/ident     :foo/baz
                   :db/valueType :db.type/string}]
          result (du/unsafe-attrs schema)]
      (is (contains? result :foo/bar))
      (is (not (contains? result :foo/baz)))
      (is (contains? result :user/password))
      (is (contains? result :user/reset-key))
      (is (contains? result :db/txInstant)))))

(deftest safe-attr?-test
  (testing "filters safe datoms"
    (let [unsafe #{:user/password :db/txInstant}]
      (is (du/safe-attr? unsafe [1 :user/name "alice" 100 true]))
      (is (not (du/safe-attr? unsafe [1 :user/password "secret" 100 true]))))))

(deftest safe-deref-test
  (testing "derefs nested atoms"
    (is (= 42 (du/safe-deref (atom (atom 42)))))
    (is (= 42 (du/safe-deref (atom 42))))
    (is (= 42 (du/safe-deref 42)))))

(deftest db-attrs-test
  (testing "extracts :db/* and :fressian/* attrs from datoms"
    (let [datoms [[1 :db/ident :foo 100 true]
                  [2 :user/name "alice" 100 true]
                  [3 :db/valueType :db.type/string 100 true]
                  [4 :fressian/tag "bar" 100 true]]]
      (is (= #{:db/ident :db/valueType :fressian/tag}
             (du/db-attrs datoms))))))

(deftest ref-attrs-test
  (testing "returns set of ref-type attribute idents"
    (let [schema [{:db/ident     :group/children
                   :db/valueType :db.type/ref}
                  {:db/ident     :group/name
                   :db/valueType :db.type/string}
                  {:db/ident     :group/parent
                   :db/valueType :db.type/ref}]]
      (is (= #{:group/children :group/parent}
             (du/ref-attrs schema))))))

(deftest datoms->map-test
  (testing "converts datom vectors to entity maps"
    (let [datoms [[1 :user/name "alice"]
                  [1 :user/age 30]
                  [2 :user/name "bob"]]]
      (is (= [{:db/id 1 :user/name "alice" :user/age 30}
              {:db/id 2 :user/name "bob"}]
             (du/datoms->map datoms)))))
  (testing "handles multiple values for same attr"
    (let [datoms [[1 :user/role "admin"]
                  [1 :user/role "editor"]]]
      (let [result (first (du/datoms->map datoms))]
        (is (= ["admin" "editor"]
               (sort (:user/role result))))))))

