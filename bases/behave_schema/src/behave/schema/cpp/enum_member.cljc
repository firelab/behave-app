(ns behave.schema.cpp.enum-member
  (:require [clojure.spec.alpha :as s]))

(s/def :enum-member/name  string?)
(s/def :enum-member/value number?)

(s/def :cpp/enum-member (s/keys :req [:enum-member/name
                                      :enum-member/value]))

(def schema
  [{:db/ident       :enum-member/name
    :db/doc         "Enum member's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :enum-member/value
    :db/doc         "Enum member's value."
    :db/valueType   :db.type/number
    :db/cardinality :db.cardinality/one}])
