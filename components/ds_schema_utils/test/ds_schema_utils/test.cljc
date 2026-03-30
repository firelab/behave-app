(ns ds-schema-utils.test
  (:require [clojure.test :refer [deftest is testing]]
            [ds-schema-utils.interface :as dsu]))

(deftest ->ds-schema-test
  (testing "converts ref attributes"
    (let [datomic-schema [{:db/ident       :group/children
                           :db/valueType   :db.type/ref
                           :db/cardinality :db.cardinality/many}]]
      (is (= {:group/children {:db/valueType   :db.type/ref
                               :db/cardinality :db.cardinality/many}}
             (dsu/->ds-schema datomic-schema)))))

  (testing "converts indexed attributes"
    (let [datomic-schema [{:db/ident       :bp/uuid
                           :db/valueType   :db.type/string
                           :db/cardinality :db.cardinality/one
                           :db/unique      :db.unique/identity
                           :db/index       true}]]
      (is (= {:bp/uuid {:db/unique :db.unique/identity
                         :db/index true}}
             (dsu/->ds-schema datomic-schema)))))

  (testing "filters out simple string attributes"
    (let [datomic-schema [{:db/ident       :group/name
                           :db/valueType   :db.type/string
                           :db/cardinality :db.cardinality/one}]]
      (is (= {} (dsu/->ds-schema datomic-schema)))))

  (testing "handles mixed schema"
    (let [datomic-schema [{:db/ident       :group/name
                           :db/valueType   :db.type/string
                           :db/cardinality :db.cardinality/one}
                          {:db/ident       :group/children
                           :db/valueType   :db.type/ref
                           :db/cardinality :db.cardinality/many}
                          {:db/ident       :bp/uuid
                           :db/valueType   :db.type/string
                           :db/cardinality :db.cardinality/one
                           :db/unique      :db.unique/identity
                           :db/index       true}]]
      (is (= 2 (count (dsu/->ds-schema datomic-schema)))))))
